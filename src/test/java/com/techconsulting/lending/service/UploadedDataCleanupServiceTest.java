package com.techconsulting.lending.service;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UploadedDataCleanupServiceTest {
    @Test
    void deletesOnlyRowsBelongingToAuthenticatedUser() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.update(anyString(), eq(7L))).thenReturn(1);

        var result = new UploadedDataCleanupService(jdbc).cleanup(7L);

        assertThat(result.loansDeleted()).isOne();
        assertThat(result.importBatchesDeleted()).isOne();
        verify(jdbc).update("delete from manual_lending where user_id = ?", 7L);
        verify(jdbc).update("delete from import_batch where user_id = ?", 7L);
    }
}
