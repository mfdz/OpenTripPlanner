package org.opentripplanner.api.common;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

public class Crypto {

    private static final Logger LOG = LoggerFactory.getLogger(Crypto.class);

    private static SecretKeySpec keySpec;

    private static String separator = "___-___";

    static {

        String secretKey = Optional.ofNullable(System.getenv("ENCRYPTION_SECRET_KEY"))
                .filter(s -> !s.trim().isEmpty())
                .orElseGet(() -> {
                    LOG.error("No environment variable ENCRYPTION_SECRET_KEY defined! Falling back on a random secret key. This means that your redirect URLs will be invalid after restarting OTP.");
                    return RandomStringUtils.random(64);
                });

        MessageDigest sha;
        try {
            sha = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = sha.digest(secretKey.getBytes(StandardCharsets.UTF_8));
            keySpec = new SecretKeySpec(keyBytes, "AES");
        } catch (NoSuchAlgorithmException e) {
            LOG.error("Could not initialise encryption library.", e);
        }
    }

    private static Cipher getCipher() throws NoSuchPaddingException, NoSuchAlgorithmException {
        return Cipher.getInstance("AES/ECB/PKCS5Padding");
    }

    public static String encrypt(String plainText) {
        try {
            Cipher cipher = getCipher();
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(plainText.getBytes());
            return Base64.encodeBase64URLSafeString(encrypted);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String decrypt(String cipherText) throws GeneralSecurityException {
        Cipher cipher = getCipher();
        cipher.init(Cipher.DECRYPT_MODE, keySpec);
        return new String(cipher.doFinal(Base64.decodeBase64(cipherText)));
    }

    public static String encryptWithExpiry(String plainText, OffsetDateTime expiry) {
        long time = expiry.toInstant().getEpochSecond();
        String withSeparator = plainText + separator + time;
        return encrypt(withSeparator);
    }

    public static class DecryptionResult {

        public final OffsetDateTime expiry;
        public final String plainText;

        DecryptionResult(OffsetDateTime expiry, String plainText) {
            this.expiry = expiry;
            this.plainText = plainText;
        }
    }

    public static DecryptionResult decryptWithExpiry(String cipherText) throws GeneralSecurityException {
        String[] plainTextWithExpiry = decrypt(cipherText).split(separator);
        OffsetDateTime expiry = OffsetDateTime.ofInstant(Instant.ofEpochSecond(Long.parseLong(plainTextWithExpiry[1])), ZoneOffset.UTC);
        return new DecryptionResult(expiry, plainTextWithExpiry[0]);
    }
}
