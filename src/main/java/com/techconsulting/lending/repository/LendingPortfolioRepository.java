package com.techconsulting.lending.repository;

import com.techconsulting.lending.domain.LendingPortfolio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LendingPortfolioRepository extends JpaRepository<LendingPortfolio, Long> {
    Optional<LendingPortfolio> findByUserId(Long userId);
}
