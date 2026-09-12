package com.techconsulting.lending.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techconsulting.lending.domain.ImportBatch;
import com.techconsulting.lending.domain.LoanReportStaging;
import com.techconsulting.lending.domain.ManualLending;
import com.techconsulting.lending.repository.ImportBatchRepository;
import com.techconsulting.lending.repository.LoanReportStagingRepository;
import com.techconsulting.lending.repository.ManualLendingRepository;
import com.techconsulting.lending.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class LoanExcelImportService {
    private static final int HEADER_SCAN_LIMIT = 100;
    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("dd/MM/yyyy"), DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd-MM-yyyy"));

    private final ImportBatchRepository batches;
    private final LoanReportStagingRepository staging;
    private final ManualLendingRepository loans;
    private final LoanCalculationService calculations;
    private final BorrowerNameResolver borrowerNames;
    private final UserRepository users;
    private final ObjectMapper json;

    @Transactional
    public ImportBatch upload(Long userId, MultipartFile file) throws Exception {
        String filename = file == null ? "" : Objects.requireNonNullElse(file.getOriginalFilename(), "");
        if (file == null || file.isEmpty() || !filename.toLowerCase(Locale.ROOT).endsWith(".xlsx"))
            throw new IllegalArgumentException("A non-empty .xlsx file is required");
        validateReportOwner(userId, filename);
        byte[] bytes = file.getBytes();
        String sum = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        var old = batches.findByUserIdAndReportTypeAndFileChecksum(userId, ImportBatch.ReportType.LOAN_REPORT, sum);
        if (old.isPresent()) return old.get();
        ImportBatch batch = new ImportBatch();
        batch.setUserId(userId); batch.setReportType(ImportBatch.ReportType.LOAN_REPORT);
        batch.setOriginalFileName(filename); batch.setFileChecksum(sum); batch.setFileSize(bytes.length);
        batch.setStatus(ImportBatch.Status.PROCESSING); batch.setStartedAt(Instant.now());
        batch = batches.save(batch);
        parseAndProcess(userId, batch, bytes);
        return batches.save(batch);
    }

    private void validateReportOwner(Long userId, String filename) {
        String lenderId = users.findById(userId)
                .map(user -> user.getLenderId())
                .filter(value -> value != null && !value.isBlank())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No LenDenClub lender ID is configured for the logged-in user"));
        String safeFilename = filename.replace('\\', '/');
        safeFilename = safeFilename.substring(safeFilename.lastIndexOf('/') + 1);
        String expectedPrefix = "MANUAL_LENDING_REPORT_" + lenderId.trim() + "_";
        if (!safeFilename.regionMatches(true, 0, expectedPrefix, 0, expectedPrefix.length())) {
            throw new IllegalArgumentException(
                    "This report does not belong to the logged-in lender. Expected filename starting with "
                            + expectedPrefix);
        }
    }

    private void parseAndProcess(Long userId, ImportBatch batch, byte[] bytes) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Table table = findLoanTable(workbook);
            Sheet sheet = table.sheet();
            for (int i = table.headerRowIndex() + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isBlank(row, table.headers())) continue;
                batch.setTotalRows(batch.getTotalRows() + 1);
                LoanReportStaging staged = new LoanReportStaging();
                staged.setImportBatchId(batch.getId()); staged.setRowNumber(i + 1);
                Map<String, String> raw = rawRow(row, table.headers());
                staged.setRawRowJson(json.writeValueAsString(raw));
                try {
                    map(raw, staged);
                    List<String> errors = validate(staged);
                    staged.setValidationStatus(errors.isEmpty() ? "VALID" : "INVALID");
                    staged.setValidationErrors(String.join("; ", errors));
                    staged.setProcessingStatus(errors.isEmpty() ? "PENDING" : "SKIPPED");
                    staging.save(staged);
                    if (!errors.isEmpty()) { batch.setInvalidRows(batch.getInvalidRows() + 1); continue; }
                    batch.setValidRows(batch.getValidRows() + 1);
                    processValidRow(userId, batch, staged);
                } catch (RuntimeException ex) {
                    staged.setValidationStatus("INVALID");
                    staged.setValidationErrors("Unable to parse row: " + ex.getMessage());
                    staged.setProcessingStatus("SKIPPED"); staging.save(staged);
                    batch.setInvalidRows(batch.getInvalidRows() + 1);
                }
            }
            batch.setStatus(batch.getFailedRows() > 0 || batch.getInvalidRows() > 0
                    ? ImportBatch.Status.PARTIALLY_COMPLETED : ImportBatch.Status.COMPLETED);
            batch.setCompletedAt(Instant.now());
        } catch (Exception ex) {
            batch.setStatus(ImportBatch.Status.FAILED); batch.setErrorMessage(ex.getMessage());
            batch.setCompletedAt(Instant.now()); throw ex;
        }
    }

    private void processValidRow(Long userId, ImportBatch batch, LoanReportStaging staged) {
        try {
            borrowerNames.resolve(staged.getLoanId()).ifPresent(identity -> {
                staged.setBorrowerPublicId(identity.borrowerId());
                if (staged.getBorrowerName() == null) staged.setBorrowerName(identity.name());
            });
            var existing = loans.findByUserIdAndSchemeIdIgnoreCase(userId, staged.getSchemeId());
            loans.save(calculations.calculate(userId, batch.getId(), staged,
                    existing.orElseGet(ManualLending::new)));
            staged.setProcessingStatus("PROCESSED"); staged.setProcessedAt(Instant.now()); staging.save(staged);
            if (existing.isPresent()) batch.setUpdatedRows(batch.getUpdatedRows() + 1);
            else batch.setInsertedRows(batch.getInsertedRows() + 1);
        } catch (Exception ex) {
            staged.setProcessingStatus("FAILED"); staged.setValidationErrors(ex.getMessage()); staging.save(staged);
            batch.setFailedRows(batch.getFailedRows() + 1);
        }
    }

    private Table findLoanTable(Workbook workbook) {
        for (Sheet sheet : workbook) {
            int last = Math.min(sheet.getLastRowNum(), sheet.getFirstRowNum() + HEADER_SCAN_LIMIT);
            for (int i = sheet.getFirstRowNum(); i <= last; i++) {
                Row row = sheet.getRow(i); if (row == null) continue;
                Map<String, Integer> headers = headers(row);
                if (hasAny(headers, "loanid", "schemeid", "schemeidentifier")
                        && hasAny(headers, "disbursedamount", "investedamount", "investmentamount")
                        && hasAny(headers, "tenure", "tenuremonths")
                        && hasAny(headers, "totalamountreceived", "amountreceived")
                        && hasAny(headers, "disbursementdate", "investmentdate")
                        && hasAny(headers, "loanstatus", "status")) return new Table(sheet, i, headers);
            }
        }
        throw new IllegalArgumentException("Could not find the manual-lending table. Expected Loan ID, "
                + "Disbursed Amount, Tenure, Total Amount Received, Disbursement Date and Loan Status columns.");
    }

    private void map(Map<String, String> raw, LoanReportStaging staged) {
        String loanId = get(raw, "loanid"), schemeId = get(raw, "schemeid", "schemeidentifier");
        // Order ID is shared by multiple loans. Loan ID is the stable row-level identifier.
        staged.setSchemeId(firstNonBlank(schemeId, loanId)); staged.setLoanId(firstNonBlank(loanId, schemeId));
        staged.setBorrowerName(get(raw, "borrowername", "name"));
        staged.setTenure(decimal(get(raw, "tenure", "tenuremonths")));
        staged.setInvestedAmount(decimal(get(raw, "disbursedamount", "investedamount", "investmentamount")));
        staged.setAmountReceived(decimal(get(raw, "totalamountreceived", "amountreceived")));
        staged.setReportedPrincipalReceived(optionalDecimal(get(raw, "principalreceived")));
        staged.setReportedInterestReceived(optionalDecimal(get(raw, "interestreceived")));
        staged.setReportedNpaAmount(optionalDecimal(get(raw, "npa")));
        staged.setInvestmentDate(date(get(raw, "disbursementdate", "investmentdate")));
        staged.setLoanStatus(get(raw, "loanstatus", "status"));
        staged.setDpd(integer(get(raw, "dpddayspastdue", "dpd")));
        staged.setLoanType(get(raw, "repaymenttype", "loantype")); staged.setTenureType("MONTHS");
        staged.setSource("LENDENCLUB_MANUAL_LENDING_REPORT");
    }

    private Map<String, Integer> headers(Row row) {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (Cell cell : row) { String key = normalize(text(cell)); if (!key.isBlank()) result.put(key, cell.getColumnIndex()); }
        return result;
    }
    private Map<String, String> rawRow(Row row, Map<String, Integer> headers) {
        Map<String, String> raw = new LinkedHashMap<>();
        headers.forEach((key, column) -> raw.put(key, text(row.getCell(column)))); return raw;
    }
    private boolean isBlank(Row row, Map<String, Integer> headers) {
        return headers.values().stream().allMatch(column -> text(row.getCell(column)).isBlank());
    }
    private boolean hasAny(Map<String, Integer> headers, String... aliases) {
        return Arrays.stream(aliases).anyMatch(headers::containsKey);
    }
    private String normalize(String value) { return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", ""); }
    private String get(Map<String, String> values, String... aliases) {
        for (String alias : aliases) { String value = values.get(alias); if (value != null && !value.isBlank()) return value.trim(); }
        return null;
    }
    private String firstNonBlank(String... values) {
        for (String value : values) if (value != null && !value.isBlank()) return value; return null;
    }
    private String text(Cell cell) { return cell == null ? "" : new DataFormatter(Locale.ENGLISH).formatCellValue(cell).trim(); }
    private BigDecimal decimal(String value) {
        if (value == null || value.isBlank() || "-".equals(value)) return BigDecimal.ZERO;
        return new BigDecimal(value.replaceAll("[₹,%\\s]", "").replace(",", ""));
    }
    private Integer integer(String value) { return decimal(value).intValue(); }
    private BigDecimal optionalDecimal(String value) { return value == null || value.isBlank() ? null : decimal(value); }
    private LocalDate date(String value) {
        if (value == null || value.isBlank()) return null;
        for (DateTimeFormatter format : DATE_FORMATS) try { return LocalDate.parse(value, format); }
        catch (RuntimeException ignored) { }
        return null;
    }
    private List<String> validate(LoanReportStaging staged) {
        List<String> errors = new ArrayList<>();
        if (staged.getSchemeId() == null) errors.add("Loan or Scheme ID is required");
        if (staged.getInvestedAmount().signum() <= 0) errors.add("Disbursed amount must be positive");
        if (staged.getTenure().signum() <= 0) errors.add("Tenure must be positive");
        if (staged.getInvestmentDate() == null) errors.add("Invalid disbursement date");
        if (staged.getLoanStatus() == null) errors.add("Loan status is required");
        if (staged.getReportedNpaAmount() != null && staged.getReportedNpaAmount().signum() < 0)
            errors.add("NPA amount cannot be negative");
        return errors;
    }
    private record Table(Sheet sheet, int headerRowIndex, Map<String, Integer> headers) { }
}
