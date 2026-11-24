package com.fjonna.library.keycloakadmin;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * No starter on this app's classpath auto-configures a {@link RestClient.Builder} bean, so
 * {@link RestClientKeycloakAdminClient} needs one provided explicitly.
 */
@Configuration
class KeycloakAdminConfig {

    @Bean
    RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
