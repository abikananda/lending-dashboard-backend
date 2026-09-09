package com.techconsulting.lending.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "lending_portfolio", uniqueConstraints = @UniqueConstraint(columnNames = "user_id"))
@Getter
@Setter
public class LendingPortfolio {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "investment_principal_amount", nullable = false) private BigDecimal investmentPrincipalAmount;
    @Version private long version;
}
