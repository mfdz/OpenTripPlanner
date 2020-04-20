package org.opentripplanner.routing.core;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class PolylineAssert {

    static void assertThatPolylinesAreEqual(String actual, String expected ){

        var reason = "Actual polyline is not equal to the expected one. View them on a map: \n" +
                "Actual:    https://leonard.io/polyline-visualiser/?base64=" + toBase64(actual) + "\n" +
                "Expected:  https://leonard.io/polyline-visualiser/?base64=" + toBase64(expected) + "\n";
        assertThat(reason, actual, is(expected));
    }

    private static String toBase64(String line) {
        return Base64.getUrlEncoder().encodeToString(line.getBytes(StandardCharsets.UTF_8));
    }
}
