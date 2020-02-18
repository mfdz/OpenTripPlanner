package org.opentripplanner.api.resource;

import org.opentripplanner.api.common.Crypto;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.time.OffsetDateTime;
import java.util.Optional;

@Path("/redirect/{cipherText}")
public class EncryptedRedirect {

    public static String API_URL = Optional.ofNullable(System.getenv("API_URL")).filter(s -> !s.trim().isEmpty()).orElse("").replaceAll("/$","");
    public static String REDIRECT_PREFIX = API_URL + "/redirect/";

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    public static Response redirect(@PathParam("cipherText") String cipherText) {
        try {
            Crypto.DecryptionResult res = Crypto.decryptWithExpiry(cipherText);
            if(res.expiry.isAfter(OffsetDateTime.now())){
                return Response.temporaryRedirect(new URI(res.plainText)).build();
            } else return Response.status(Response.Status.FORBIDDEN).entity("URL redirect has expired.").build();
        } catch (GeneralSecurityException | URISyntaxException e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

}
