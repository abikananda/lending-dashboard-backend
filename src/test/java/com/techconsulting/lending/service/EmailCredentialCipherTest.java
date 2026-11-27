package com.techconsulting.lending.service;

import org.junit.jupiter.api.Test;
import java.util.Base64;
import static org.assertj.core.api.Assertions.assertThat;

class EmailCredentialCipherTest {
    @Test void encryptsWithRandomIvAndDecryptsAppPassword() {
        EmailCredentialCipher cipher = new EmailCredentialCipher(Base64.getEncoder().encodeToString(new byte[32]));
        String first=cipher.encrypt("gmail-app-password"), second=cipher.encrypt("gmail-app-password");
        assertThat(first).isNotEqualTo(second);
        assertThat(cipher.decrypt(first)).isEqualTo("gmail-app-password");
    }
}
