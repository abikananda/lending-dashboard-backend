package com.techconsulting.lending.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techconsulting.lending.domain.ImportBatch;
import com.techconsulting.lending.domain.LoanReportStaging;
import com.techconsulting.lending.domain.ManualLending;
import com.techconsulting.lending.repository.ImportBatchRepository;
import com.techconsulting.lending.repository.LoanReportStagingRepository;
import com.techconsulting.lending.repository.ManualLendingRepository;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LoanExcelImportServiceTest {
    @Test
    void importsFormattedLenDenClubManualLendingReport() throws Exception {
        ImportBatchRepository batches = mock(ImportBatchRepository.class);
        LoanReportStagingRepository staging = mock(LoanReportStagingRepository.class);
        ManualLendingRepository loans = mock(ManualLendingRepository.class);
        LoanCalculationService calculations = mock(LoanCalculationService.class);
        when(batches.findByUserIdAndReportTypeAndFileChecksum(any(), any(), any())).thenReturn(Optional.empty());
        when(batches.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(loans.findByUserIdAndSchemeIdIgnoreCase(7L, "LOA-KLBUNNZP")).thenReturn(Optional.empty());
        when(calculations.calculate(any(), any(), any(), any())).thenReturn(new ManualLending());

        LoanExcelImportService service = new LoanExcelImportService(
                batches, staging, loans, calculations, new ObjectMapper());
        ImportBatch result = service.upload(7L, new MockMultipartFile(
                "file", "manual-lending.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", reportBytes()));

        ArgumentCaptor<LoanReportStaging> captor = ArgumentCaptor.forClass(LoanReportStaging.class);
        verify(calculations).calculate(eq(7L), isNull(), captor.capture(), any());
        LoanReportStaging row = captor.getValue();
        assertThat(row.getRowNumber()).isEqualTo(21);
        assertThat(row.getSchemeId()).isEqualTo("LOA-KLBUNNZP");
        assertThat(row.getLoanId()).isEqualTo("LOA-KLBUNNZP");
        assertThat(row.getInvestedAmount()).isEqualByComparingTo("500");
        assertThat(row.getAmountReceived()).isEqualByComparingTo("135.55");
        assertThat(row.getTenure()).isEqualByComparingTo("4");
        assertThat(row.getInvestmentDate()).isEqualTo(LocalDate.of(2026, 8, 10));
        assertThat(row.getDpd()).isZero();
        assertThat(row.getLoanType()).isEqualTo("Monthly");
        assertThat(row.getSource()).isEqualTo("LENDENCLUB_MANUAL_LENDING_REPORT");
        assertThat(row.getRawRowJson()).contains("principalreceived", "platformfee", "lendenclubscore");
        assertThat(result.getStatus()).isEqualTo(ImportBatch.Status.COMPLETED);
        assertThat(result.getTotalRows()).isOne();
        assertThat(result.getInsertedRows()).isOne();
    }

    private byte[] reportBytes() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Sheet");
            sheet.createRow(0).createCell(0).setCellValue("Innofin Solutions Private Limited");
            sheet.createRow(7).createCell(0).setCellValue("Manual Lending Summary");
            String[] headers = {"Order ID", "Loan ID", "Disbursement Date", "Disbursed Amount (₹)",
                    "Repayment Type", "Repayment Start Date (expected)",
                    "Total Repayment Amount (illustrative) (₹)", "Total Amount Received (₹)",
                    "Principal Received (₹)", "Interest Received (₹)", "Platform Fee (₹)",
                    "Profit & Loss (₹)", "NPA", "Loan Status", "Loan Closure/NPA Date",
                    "DPD (days past due)", "Interest Rate (%)", "Tenure (months)", "LenDenClub Score"};
            var header = sheet.createRow(19);
            for (int i = 0; i < headers.length; i++) header.createCell(i).setCellValue(headers[i]);
            Object[] values = {"50397065657571", "LOA-KLBUNNZP", "10/08/2026", 500, "Monthly",
                    "01/09/2026", 557.25, 135.55, 125, 10.55, 3.76, 0, 0, "ACTIVE", "", 0, 36.48, 4, 800};
            var data = sheet.createRow(20);
            for (int i = 0; i < values.length; i++) {
                if (values[i] instanceof Number number) data.createCell(i).setCellValue(number.doubleValue());
                else data.createCell(i).setCellValue(values[i].toString());
            }
            workbook.write(output);
            return output.toByteArray();
        }
    }
}
