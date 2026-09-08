package com.techconsulting.lending.repository; import com.techconsulting.lending.domain.LoanReportStaging; import org.springframework.data.jpa.repository.JpaRepository; import java.util.List;
public interface LoanReportStagingRepository extends JpaRepository<LoanReportStaging,Long> { List<LoanReportStaging> findByImportBatchIdOrderByRowNumber(Long batchId); }
