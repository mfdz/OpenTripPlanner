package org.opentripplanner.api.common;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class CryptoTest {
    @Test
    public void shouldEncryptAndDecrypt() throws Exception {
        String plainText = "Roxanne! You don't need to put out the red light!";
        String cipherText = Crypto.encrypt(plainText);

        assertThat(cipherText, is("zal31JId-7PA4zSWSlhququ6b5jogbdqUHE1-QTaCdeuONKTo4Zj8-fmvNPTKcdW4Dv1KYDQuNsq6OI3abC8SA"));

        assertThat(Crypto.decrypt(cipherText), is(plainText));
    }

}
