package com.fjonna.library.keycloakadmin;

/** Talks to the Keycloak Admin REST API. Only the one operation the app currently needs. */
public interface KeycloakAdminClient {

    /**
     * Deletes the login account with this Keycloak user id. A user that's already gone (404 from
     * Keycloak) is treated as success. Any other failure throws {@link KeycloakAdminException}.
     */
    void deleteUser(String keycloakUserId);
}
