package com.fjonna.library.keycloakadmin;

/** Thrown when the Keycloak Admin REST API can't be reached or returns an unexpected error. */
public class KeycloakAdminException extends RuntimeException {

    public KeycloakAdminException(String message, Throwable cause) {
        super(message, cause);
    }
}
