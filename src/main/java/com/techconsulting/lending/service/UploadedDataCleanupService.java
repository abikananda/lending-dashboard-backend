package com.techconsulting.lending.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UploadedDataCleanupService {
    private final JdbcTemplate jdbc;

    @Transactional
    public CleanupResult cleanup(Long userId) {
        int reconciliations = jdbc.update("delete from reconciliation_record where user_id = ?", userId);
        int notifications = jdbc.update("delete from payment_notification where user_id = ?", userId);
        int bankCredits = jdbc.update("delete from bank_credit where user_id = ?", userId);
        int walletTransactions = jdbc.update("delete from wallet_transaction where user_id = ?", userId);
        int loans = jdbc.update("delete from manual_lending where user_id = ?", userId);
        int stagingRows = jdbc.update("delete from loan_report_staging where import_batch_id in (select id from import_batch where user_id = ?)", userId);
        int importBatches = jdbc.update("delete from import_batch where user_id = ?", userId);
        return new CleanupResult(loans, stagingRows, importBatches, walletTransactions,
                bankCredits, notifications, reconciliations);
    }

    public record CleanupResult(int loansDeleted, int stagingRowsDeleted, int importBatchesDeleted,
                                int walletTransactionsDeleted, int bankCreditsDeleted,
                                int paymentNotificationsDeleted, int reconciliationsDeleted) { }
}
