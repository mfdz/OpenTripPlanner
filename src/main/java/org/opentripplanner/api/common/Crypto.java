package org.opentripplanner.api.common;

import org.apache.commons.codec.binary.Base64;
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
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class Crypto {

    private static final Logger LOG = LoggerFactory.getLogger(Crypto.class);

    private static SecretKeySpec keySpec;

    private static String separator = "___-___";

    static {

        String secretKey = Optional.ofNullable(System.getenv("ENCRYPTION_SECRET_KEY"))
                .filter(s -> !s.trim().isEmpty())
                .orElseGet(() -> {
                    LOG.error("No environment variable ENCRYPTION_SECRET_KEY defined! Falling back on default secret key!");
                    return "very secret key";
                });

        MessageDigest sha;
        try {
            sha = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = sha.digest(secretKey.getBytes(StandardCharsets.UTF_8));
            keySpec = new SecretKeySpec(keyBytes, "AES");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    private static Cipher getCipher() throws NoSuchPaddingException, NoSuchAlgorithmException {
        return Cipher.getInstance("AES/ECB/PKCS5Padding");
    }

    public static String encrypt(String plainText) throws GeneralSecurityException {
        Cipher cipher = getCipher();
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);
        byte[] encrypted = cipher.doFinal(plainText.getBytes());
        return Base64.encodeBase64URLSafeString(encrypted);
    }

    public static String decrypt(String cipherText) throws GeneralSecurityException {
        Cipher cipher = getCipher();
        cipher.init(Cipher.DECRYPT_MODE, keySpec);
        return new String(cipher.doFinal(Base64.decodeBase64(cipherText)));
    }

    public static String encryptWithExpiry(String plainText, OffsetDateTime expiry) throws GeneralSecurityException {
        long time = expiry.toInstant().getEpochSecond();
        String withSeparator = plainText + separator + time;
        return encrypt(withSeparator);
    }

    static class DecryptionResult {

        public final OffsetDateTime expiry;
        public final String plainText;

        DecryptionResult(OffsetDateTime expiry, String plainText) {
            this.expiry = expiry;
            this.plainText = plainText;
        }
    }

    public static DecryptionResult decryptWithExpiry(String cipherText) throws GeneralSecurityException {
        List<String> plainTextWithExpiry = Arrays.asList(decrypt(cipherText).split(separator));
        System.out.println(plainTextWithExpiry);
        OffsetDateTime expiry = OffsetDateTime.ofInstant(Instant.ofEpochSecond(Long.parseLong(plainTextWithExpiry.get(1))), ZoneOffset.UTC);
        return new DecryptionResult(expiry, plainTextWithExpiry.get(0));
    }
}
