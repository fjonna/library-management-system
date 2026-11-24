package com.fjonna.library.keycloakadmin;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Calls the Keycloak Admin REST API directly over the internal Docker network (never through the
 * public-facing issuer URL). Authenticates as the confidential "library-backend" service account
 * (client_credentials grant) and caches the access token until shortly before it expires.
 */
@Component
public class RestClientKeycloakAdminClient implements KeycloakAdminClient {

    /** This app only ever has one realm. */
    private static final String REALM = "library";

    /** Refill the token a bit early so a request never races an about-to-expire token. */
    private static final long EXPIRY_SAFETY_MARGIN_SECONDS = 10;

    private final RestClient restClient;
    private final String tokenUri;
    private final String clientId;
    private final String clientSecret;

    private volatile String cachedToken;
    private volatile Instant cachedTokenExpiresAt = Instant.EPOCH;

    public RestClientKeycloakAdminClient(
            RestClient.Builder builder,
            @Value("${keycloak.admin.base-url:http://keycloak:8080/auth}") String baseUrl,
            @Value("${keycloak.admin.client-id:}") String clientId,
            @Value("${keycloak.admin.client-secret:}") String clientSecret) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.tokenUri = baseUrl + "/realms/" + REALM + "/protocol/openid-connect/token";
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Override
    public void deleteUser(String keycloakUserId) {
        try {
            restClient.delete()
                .uri("/admin/realms/{realm}/users/{id}", REALM, keycloakUserId)
                .headers(h -> h.setBearerAuth(accessToken()))
                .retrieve()
                .toBodilessEntity();
        } catch (HttpClientErrorException.NotFound e) {
            // already gone in Keycloak - nothing left to do
        } catch (RestClientException e) {
            throw new KeycloakAdminException("Could not delete the user's Keycloak login account", e);
        }
    }

    private synchronized String accessToken() {
        Instant now = Instant.now();
        if (cachedToken != null && now.isBefore(cachedTokenExpiresAt)) {
            return cachedToken;
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        try {
            TokenResponse token = restClient.post()
                .uri(tokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TokenResponse.class);
            if (token == null) {
                throw new KeycloakAdminException("Keycloak returned an empty token response", null);
            }
            cachedToken = token.accessToken();
            long ttl = Math.max(1, token.expiresIn() - EXPIRY_SAFETY_MARGIN_SECONDS);
            cachedTokenExpiresAt = now.plusSeconds(ttl);
            return cachedToken;
        } catch (RestClientException e) {
            throw new KeycloakAdminException("Could not authenticate with Keycloak", e);
        }
    }

    private record TokenResponse(@JsonProperty("access_token") String accessToken, @JsonProperty("expires_in") long expiresIn) {
    }
}
