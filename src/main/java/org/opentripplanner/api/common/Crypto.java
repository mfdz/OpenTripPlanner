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
import java.util.Optional;

public class Crypto {

    private static SecretKeySpec keySpec;
    private static final Logger LOG = LoggerFactory.getLogger(Crypto.class);

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
}
