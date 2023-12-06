package com.sergiostefanizzi.profilemicroservice.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public final class JwtTestUtilityClass {
    private JwtTestUtilityClass() {
    }

    public static String getAccessToken(String email, TestRestTemplate testRestTemplate, ObjectMapper objectMapper) throws JsonProcessingException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        String password = "dshjdfkdjsf32!";
        String loginBody = "grant_type=password&client_id=accounts-micro&username="+email+"&password="+ password;

        ResponseEntity<String> responseLogin = testRestTemplate.exchange(
                "http://localhost:8082/realms/social-accounts/protocol/openid-connect/token",
                HttpMethod.POST,
                new HttpEntity<>(loginBody, headers),
                String.class);


        assertEquals(HttpStatus.OK, responseLogin.getStatusCode());
        JsonNode node = objectMapper.readTree(responseLogin.getBody());
        String accessToken = node.get("access_token").asText();
        log.info("Access token = "+accessToken);
        return accessToken;
    }
}
