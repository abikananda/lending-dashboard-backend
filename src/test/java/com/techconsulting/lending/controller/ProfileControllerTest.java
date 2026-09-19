package com.techconsulting.lending.controller;

import com.techconsulting.lending.domain.User;
import com.techconsulting.lending.repository.LendingPortfolioRepository;
import com.techconsulting.lending.repository.UserRepository;
import com.techconsulting.lending.security.JwtFilter.AppPrincipal;
import com.techconsulting.lending.service.EmailCredentialCipher;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProfileControllerTest {
    @Test
    void storesWriteOnlyEncryptedEmailAppPassword() {
        UserRepository users = mock(UserRepository.class);
        User user = new User();
        user.setId(7L);
        user.setEmail("owner@gmail.com");
        when(users.findById(7L)).thenReturn(Optional.of(user));
        EmailCredentialCipher cipher = new EmailCredentialCipher(
                Base64.getEncoder().encodeToString(new byte[32]));
        ProfileController controller = new ProfileController(users,
                mock(LendingPortfolioRepository.class), cipher);

        var response = controller.updateEmailCredentials(new AppPrincipal(7L, "owner"),
                new ProfileController.EmailPasswordRequest("abcd efgh ijkl mnop"));

        assertThat(response.email()).isEqualTo("owner@gmail.com");
        assertThat(response.credentialsConfigured()).isTrue();
        assertThat(user.getEmailPassword()).isNotEqualTo("abcdefghijklmnop");
        assertThat(cipher.decrypt(user.getEmailPassword())).isEqualTo("abcdefghijklmnop");
        verify(users).save(user);
    }
}
