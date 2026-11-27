package com.techconsulting.lending.repository; import com.techconsulting.lending.domain.ManualLending; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param; import java.math.BigDecimal; import java.util.*;
public interface ManualLendingRepository extends JpaRepository<ManualLending,Long> { Optional<ManualLending> findByUserIdAndSchemeIdIgnoreCase(Long userId,String schemeId); List<ManualLending> findByUserIdOrderByInvestmentDateDesc(Long userId); List<ManualLending> findByUserIdAndBorrowerPublicIdOrderByInvestmentDateAsc(Long userId,String borrowerPublicId);
 @Query("select count(m) from ManualLending m where m.userId=:u and m.loanStatus=:s") long countStatus(@Param("u") Long userId,@Param("s") String status);
 @Query("select coalesce(sum(m.investedAmount),0) from ManualLending m where m.userId=:u") BigDecimal totalInvested(@Param("u") Long userId);
 @Query("select coalesce(sum(m.amountReceived),0) from ManualLending m where m.userId=:u") BigDecimal totalReceived(@Param("u") Long userId);
 @Query("select coalesce(sum(m.calculatedInterestReceived),0) from ManualLending m where m.userId=:u") BigDecimal totalInterestEarned(@Param("u") Long userId);
 @Query("select coalesce(sum(m.outstandingPrincipal),0) from ManualLending m where m.userId=:u") BigDecimal totalOutstanding(@Param("u") Long userId);
 @Query("select coalesce(sum(case when m.loanStatus='CLOSED' and m.amountReceived<m.investedAmount then m.investedAmount-m.amountReceived else 0 end),0) from ManualLending m where m.userId=:u") BigDecimal totalPrincipalLoss(@Param("u") Long userId);
 @Query(value="select coalesce(sum(case when transaction_type in ('DEPOSIT','WALLET_LOAD','CREDIT','ADD_MONEY') then amount else 0 end),0) from wallet_transaction where user_id=:u",nativeQuery=true) BigDecimal totalWalletAdded(@Param("u") Long userId);
 @Query(value="select coalesce(sum(case when transaction_type in ('WITHDRAWAL','DEBIT','WITHDRAW') then amount else 0 end),0) from wallet_transaction where user_id=:u",nativeQuery=true) BigDecimal totalWalletWithdrawn(@Param("u") Long userId);
 @Query(value="select coalesce(sum(amount),0) from bank_credit where user_id=:u",nativeQuery=true) BigDecimal totalBankReceived(@Param("u") Long userId);
 @Query("select count(m) from ManualLending m where m.userId=:u and m.npa=true") long countNpa(@Param("u") Long userId);
 @Query("select coalesce(sum(m.npaAmount),0) from ManualLending m where m.userId=:u and m.npa=true") BigDecimal totalNpaAmount(@Param("u") Long userId);
 @Query("select count(m) from ManualLending m where m.userId=:u and m.probableNpa=true") long countProbableNpa(@Param("u") Long userId);
 @Query("select coalesce(sum(m.probableNpaAmount),0) from ManualLending m where m.userId=:u and m.probableNpa=true") BigDecimal totalProbableNpaAmount(@Param("u") Long userId); }
