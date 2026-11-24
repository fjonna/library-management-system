package com.fjonna.library.keycloakadmin;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Real HTTP shapes via MockRestServiceServer: token fetch, caching, 404-as-success, failure wrapping. */
class RestClientKeycloakAdminClientTest {

    private static final String BASE_URL = "http://keycloak.test/auth";
    private static final String TOKEN_URI = BASE_URL + "/realms/library/protocol/openid-connect/token";

    private RestClientKeycloakAdminClient newClient(MockRestServiceServer[] serverOut) {
        RestClient.Builder builder = RestClient.builder();
        serverOut[0] = MockRestServiceServer.bindTo(builder).build();
        return new RestClientKeycloakAdminClient(builder, BASE_URL, "library-backend", "secret");
    }

    @Test
    void fetchesATokenThenDeletesTheUserWithIt() {
        MockRestServiceServer[] holder = new MockRestServiceServer[1];
        RestClientKeycloakAdminClient client = newClient(holder);
        MockRestServiceServer server = holder[0];

        server.expect(requestTo(TOKEN_URI))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess("{\"access_token\":\"tok-123\",\"expires_in\":60}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL + "/admin/realms/library/users/abc-123"))
            .andExpect(method(HttpMethod.DELETE))
            .andExpect(header("Authorization", "Bearer tok-123"))
            .andRespond(withNoContent());

        client.deleteUser("abc-123");

        server.verify();
    }

    @Test
    void aMissingUserInKeycloakIsTreatedAsSuccess() {
        MockRestServiceServer[] holder = new MockRestServiceServer[1];
        RestClientKeycloakAdminClient client = newClient(holder);
        MockRestServiceServer server = holder[0];

        server.expect(requestTo(TOKEN_URI))
            .andRespond(withSuccess("{\"access_token\":\"tok-123\",\"expires_in\":60}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL + "/admin/realms/library/users/already-gone"))
            .andRespond(withResourceNotFound());

        assertThatNoException().isThrownBy(() -> client.deleteUser("already-gone"));
        server.verify();
    }

    @Test
    void aServerErrorIsWrappedInKeycloakAdminException() {
        MockRestServiceServer[] holder = new MockRestServiceServer[1];
        RestClientKeycloakAdminClient client = newClient(holder);
        MockRestServiceServer server = holder[0];

        server.expect(requestTo(TOKEN_URI))
            .andRespond(withSuccess("{\"access_token\":\"tok-123\",\"expires_in\":60}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL + "/admin/realms/library/users/abc"))
            .andRespond(withServerError());

        assertThatThrownBy(() -> client.deleteUser("abc")).isInstanceOf(KeycloakAdminException.class);
    }

    @Test
    void aFailedTokenFetchIsWrappedInKeycloakAdminException() {
        MockRestServiceServer[] holder = new MockRestServiceServer[1];
        RestClientKeycloakAdminClient client = newClient(holder);
        MockRestServiceServer server = holder[0];

        server.expect(requestTo(TOKEN_URI)).andRespond(withServerError());

        assertThatThrownBy(() -> client.deleteUser("abc")).isInstanceOf(KeycloakAdminException.class);
    }

    @Test
    void theTokenIsCachedAcrossCalls() {
        MockRestServiceServer[] holder = new MockRestServiceServer[1];
        RestClientKeycloakAdminClient client = newClient(holder);
        MockRestServiceServer server = holder[0];

        // Exactly one token request is expected; if the client re-fetched it for the second
        // delete, that second call would have no matching expectation and server.verify() (or
        // the request itself) would fail.
        server.expect(requestTo(TOKEN_URI))
            .andRespond(withSuccess("{\"access_token\":\"tok-123\",\"expires_in\":60}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL + "/admin/realms/library/users/u1")).andRespond(withNoContent());
        server.expect(requestTo(BASE_URL + "/admin/realms/library/users/u2")).andRespond(withNoContent());

        client.deleteUser("u1");
        client.deleteUser("u2");

        server.verify();
    }
}
