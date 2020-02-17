package org.opentripplanner.api.common;

import org.junit.Test;

import java.time.OffsetDateTime;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class CryptoTest {

    OffsetDateTime expiry = OffsetDateTime.parse("2020-02-17T17:04:55+01:00");

    String plainText = "Roxanne! You don't need to put out the red light!";

    @Test
    public void shouldEncryptAndDecrypt() throws Exception {
        String cipherText = Crypto.encrypt(plainText);

        assertThat(cipherText, is("zal31JId-7PA4zSWSlhququ6b5jogbdqUHE1-QTaCdeuONKTo4Zj8-fmvNPTKcdW4Dv1KYDQuNsq6OI3abC8SA"));

        assertThat(Crypto.decrypt(cipherText), is(plainText));
    }

    @Test
    public void shouldEncryptAndDecryptWithExpiry() throws Exception {
        String cipherText = Crypto.encryptWithExpiry(plainText, expiry);

        assertThat(cipherText, is("zal31JId-7PA4zSWSlhququ6b5jogbdqUHE1-QTaCdeuONKTo4Zj8-fmvNPTKcdW0vBJuQgEn_U-nn0E0HpnzewlNqjOsEQTBMKJDPRqW1w"));
        assertThat(Crypto.decrypt(cipherText), is("Roxanne! You don't need to put out the red light!___-___1581955495"));

        Crypto.DecryptionResult result = Crypto.decryptWithExpiry(cipherText);

        assertThat(result.expiry.isEqual(expiry), is(true));
        assertThat(result.plainText, is(plainText));

    }
}
