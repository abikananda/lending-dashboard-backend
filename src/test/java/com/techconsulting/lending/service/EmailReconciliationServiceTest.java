package com.techconsulting.lending.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class EmailReconciliationServiceTest {
    @Test
    void calculatesFiveWorkingDayCreditDeadline() {
        assertThat(EmailReconciliationService.addBusinessDays(LocalDate.of(2026, 9, 11), 5))
                .isEqualTo(LocalDate.of(2026, 9, 18));
    }
}
