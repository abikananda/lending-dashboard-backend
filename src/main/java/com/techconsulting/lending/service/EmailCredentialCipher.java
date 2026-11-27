package com.techconsulting.lending.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class EmailCredentialCipher {
    private static final int IV_BYTES = 12;
    private final byte[] key;

    public EmailCredentialCipher(@Value("${email-reconciliation.credential-encryption-key:}") String encodedKey) {
        key = encodedKey == null || encodedKey.isBlank() ? null : Base64.getDecoder().decode(encodedKey);
        if (key != null && key.length != 32) throw new IllegalStateException(
                "EMAIL_CREDENTIAL_ENCRYPTION_KEY must be a base64-encoded 32-byte key");
    }

    public String encrypt(String plaintext) {
        requireKey();
        try {
            byte[] iv = new byte[IV_BYTES]; new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] result = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(result);
        } catch (Exception ex) { throw new IllegalStateException("Unable to encrypt Gmail app password", ex); }
    }

    public String decrypt(String value) {
        requireKey();
        try {
            byte[] input = Base64.getDecoder().decode(value);
            byte[] iv = java.util.Arrays.copyOfRange(input, 0, IV_BYTES);
            byte[] encrypted = java.util.Arrays.copyOfRange(input, IV_BYTES, input.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception ex) { throw new IllegalStateException("Unable to decrypt Gmail app password", ex); }
    }

    private void requireKey() {
        if (key == null) throw new IllegalStateException("EMAIL_CREDENTIAL_ENCRYPTION_KEY is required");
    }
}
