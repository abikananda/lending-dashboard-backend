package com.techconsulting.lending.controller;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class LendingControllerTest {

    @Test
    void classifiesHealthyAtInclusiveLimits() {
        assertThat(LendingController.portfolioHealth(new BigDecimal("15"), new BigDecimal("30")))
                .isEqualTo("Healthy");
    }

    @Test
    void classifiesNeedsAttentionWhenEitherWarningLimitIsExceeded() {
        assertThat(LendingController.portfolioHealth(new BigDecimal("15.01"), new BigDecimal("10")))
                .isEqualTo("Needs attention");
        assertThat(LendingController.portfolioHealth(new BigDecimal("10"), new BigDecimal("30.01")))
                .isEqualTo("Needs attention");
    }

    @Test
    void classifiesCriticalBeforeNeedsAttention() {
        assertThat(LendingController.portfolioHealth(new BigDecimal("20.01"), BigDecimal.ZERO))
                .isEqualTo("Critical");
        assertThat(LendingController.portfolioHealth(BigDecimal.ZERO, new BigDecimal("40.01")))
                .isEqualTo("Critical");
    }
}
