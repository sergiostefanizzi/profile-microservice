package com.sergiostefanizzi.profilemicroservice.system.util;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class KeycloackConfiguration {
    private final String serverUrl = "http://localhost:8082";

    private final String realm = "social-accounts";
    private final String clientId = "admin-cli";
    private final String clientSecret = "bVduZVhwrK944qnQCWBb2mSI3W1dTifP";
    @Bean
    Keycloak keycloak() {
        return KeycloakBuilder.builder()
                .serverUrl(this.serverUrl)
                .realm(this.realm)
                .clientId(this.clientId)
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .clientSecret(this.clientSecret)
                .build();
    }
}
