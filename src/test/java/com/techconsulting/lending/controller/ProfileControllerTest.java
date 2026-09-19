package com.techconsulting.lending.controller;

import com.techconsulting.lending.domain.LendingPortfolio;
import com.techconsulting.lending.domain.User;
import com.techconsulting.lending.repository.LendingPortfolioRepository;
import com.techconsulting.lending.repository.UserRepository;
import com.techconsulting.lending.security.JwtFilter.AppPrincipal;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProfileControllerTest {
    @Test
    void returnsProfileWithoutMailboxCredentials() {
        UserRepository users = mock(UserRepository.class);
        LendingPortfolioRepository portfolios = mock(LendingPortfolioRepository.class);
        User user = new User();
        user.setId(7L);
        user.setUsername("owner");
        user.setEmail("owner@gmail.com");
        user.setLenderId("LENDER123");
        LendingPortfolio portfolio = new LendingPortfolio();
        portfolio.setInvestmentPrincipalAmount(new BigDecimal("500000"));
        when(users.findById(7L)).thenReturn(Optional.of(user));
        when(portfolios.findByUserId(7L)).thenReturn(Optional.of(portfolio));
        ProfileController controller = new ProfileController(users, portfolios);

        var response = controller.profile(new AppPrincipal(7L, "owner"));

        assertThat(response.userId()).isEqualTo(7L);
        assertThat(response.username()).isEqualTo("owner");
        assertThat(response.email()).isEqualTo("owner@gmail.com");
        assertThat(response.lenderId()).isEqualTo("LENDER123");
        assertThat(response.investmentPrincipal()).isEqualByComparingTo("500000");
    }
}
