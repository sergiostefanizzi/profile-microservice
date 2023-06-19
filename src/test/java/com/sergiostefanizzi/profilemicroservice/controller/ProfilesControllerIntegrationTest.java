package com.sergiostefanizzi.profilemicroservice.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sergiostefanizzi.profilemicroservice.model.Profile;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Slf4j
class ProfilesControllerIntegrationTest {
    @LocalServerPort
    private int port;

    private String baseUrl = "http://localhost";

    @Autowired
    private TestRestTemplate testRestTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    private Profile newProfile;
    //private Profile savedProfile;
    String profileName = "giuseppe_verdi";
    Boolean isPrivate = false;
    Long accountId = 1L;
    String bio = "This is Giuseppe's profile!";
    String pictureUrl = "https://upload.wikimedia.org/wikipedia/commons/7/7e/Circle-icons-profile.svg";

    @BeforeEach
    void setUp() {
        this.baseUrl += ":" + port + "/profiles";
        this.newProfile = new Profile(profileName,isPrivate,accountId);
        this.newProfile.setBio(bio);
        this.newProfile.setPictureUrl(pictureUrl);
        /*
        this.savedProfile = new Profile(profileName,isPrivate,accountId);
        this.savedProfile.setBio(bio);
        this.savedProfile.setPictureUrl(pictureUrl);
        this.savedProfile.setId(profileId);
         */
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void testAddProfile_Then_201(){
        HttpEntity<Profile> request = new HttpEntity<>(newProfile);
        ResponseEntity<Profile> response = this.testRestTemplate.exchange(this.baseUrl,
                HttpMethod.POST,
                request,
                Profile.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertInstanceOf(Profile.class, response.getBody());
        Profile savedProfile = response.getBody();
        assertNotNull(savedProfile.getId());
        assertEquals(newProfile.getProfileName(), savedProfile.getProfileName());
        assertEquals(newProfile.getBio(), savedProfile.getBio());
        assertEquals(newProfile.getPictureUrl(), savedProfile.getPictureUrl());
        assertEquals(newProfile.getIsPrivate(), savedProfile.getIsPrivate());
        assertEquals(newProfile.getAccountId(), savedProfile.getAccountId());

        // visulazzo il profilo salvato
        log.info(savedProfile.toString());
    }

    @Test
    void testAddProfile_RequiredFields_Then_201(){
        // per l'esecuzione in parallelo dei test
        // cambio il nome del profile perche' essendo un campo unique
        // genererebbe un 409 CONFLICT
        this.newProfile.setProfileName(profileName+"_1");
        // imposto a null i campi non richiesti
        this.newProfile.setBio(null);
        this.newProfile.setPictureUrl(null);

        HttpEntity<Profile> request = new HttpEntity<>(newProfile);
        ResponseEntity<Profile> response = this.testRestTemplate.exchange(this.baseUrl,
                HttpMethod.POST,
                request,
                Profile.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertInstanceOf(Profile.class, response.getBody());
        Profile savedProfile = response.getBody();
        assertNotNull(savedProfile.getId());
        assertEquals(newProfile.getProfileName(), savedProfile.getProfileName());
        assertNull(savedProfile.getBio());
        assertNull(savedProfile.getPictureUrl());
        assertEquals(newProfile.getIsPrivate(), savedProfile.getIsPrivate());
        assertEquals(newProfile.getAccountId(), savedProfile.getAccountId());

        // visulazzo il profilo salvato
        log.info(savedProfile.toString());
    }

    @Test
    void testAddProfile_MissingRequired_Then_400() throws Exception{
        // arrayList contenente i messaggi di errore
        List<String> errors = new ArrayList<>();
        errors.add("profileName must not be null");
        errors.add("isPrivate must not be null");
        errors.add("accountId must not be null");
        // Non ho bisogno di cambiare il nome del profilo, perche' verranno generati altri tipi di errore prima di quello relativo al nome
        // Imposto a null i campi richiesti
        this.newProfile.setProfileName(null);
        this.newProfile.setIsPrivate(null);
        this.newProfile.setAccountId(null);

        HttpEntity<Profile> request = new HttpEntity<>(newProfile);
        ResponseEntity<String> response = this.testRestTemplate.exchange(this.baseUrl,
                HttpMethod.POST,
                request,
                String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode node = this.objectMapper.readTree(response.getBody());
        // itero gli errori ottenuti dalla risposta per confrontarli con quelli che mi aspetto di ottenere
        assertEquals(errors.size() ,node.get("error").size());
        for (JsonNode objNode : node.get("error")) {
            assertTrue(errors.contains(objNode.asText()));
            log.info("Error -> "+objNode.asText());
        }
    }

    @Test
    void testAddProfile_ProfileNameFormat_Then_400() throws Exception{
        // arrayList contenente i messaggi di errore
        List<String> errors = new ArrayList<>();
        errors.add("profileName size must be between 8 and 20");
        errors.add("profileName must match \"^(?=.{8,20}$)(?![_.])(?!.*[_.]{2})[a-zA-Z0-9._]+(?<![_.])$\"");

        // imposto il profileName in modo che non rispetti le regole d'inserimento
        this.newProfile.setProfileName("g_v");

        HttpEntity<Profile> request = new HttpEntity<>(newProfile);
        ResponseEntity<String> response = this.testRestTemplate.exchange(this.baseUrl,
                HttpMethod.POST,
                request,
                String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode node = this.objectMapper.readTree(response.getBody());
        // itero gli errori ottenuti dalla risposta per confrontarli con quelli che mi aspetto di ottenere
        assertEquals(errors.size() ,node.get("error").size());
        for (JsonNode objNode : node.get("error")) {
            assertTrue(errors.contains(objNode.asText()));
            log.info("Error -> "+objNode.asText());
        }
    }

    @Test
    void testAddProfile_BioLength_Then_400() throws Exception{
        // messaggio d'errore che mi aspetto d'ottenere
        String error = "bio size must be between 0 and 150";

        // Imposto una bio di 160 caratteri
        this.newProfile.setBio("Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Aenean commodo ligula eget dolor. Aenean massa. Cum sociis natoque penatibus et magnis dis parturient.");

        HttpEntity<Profile> request = new HttpEntity<>(newProfile);
        ResponseEntity<String> response = this.testRestTemplate.exchange(this.baseUrl,
                HttpMethod.POST,
                request,
                String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode node = this.objectMapper.readTree(response.getBody());
        // Anche se è solo un errore, ottengo sempre un array di dimensione 1
        assertTrue(node.get("error").isArray());
        assertEquals(1, node.get("error").size());
        assertEquals(error ,node.get("error").get(0).asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error").get(0));
    }

    @Test
    void testAddProfile_InvalidPictureUrl_Then_400() throws Exception{
        // messaggio d'errore che mi aspetto d'ottenere
        String error = "pictureUrl must be a valid URL";
        // imposto un url non valido
        this.newProfile.setPictureUrl("https://upload.wikimedia.o/ ra-%%$^&& iuyi");

        HttpEntity<Profile> request = new HttpEntity<>(newProfile);
        ResponseEntity<String> response = this.testRestTemplate.exchange(this.baseUrl,
                HttpMethod.POST,
                request,
                String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode node = this.objectMapper.readTree(response.getBody());
        // Anche se è solo un errore, ottengo sempre un array di dimensione 1
        assertTrue(node.get("error").isArray());
        assertEquals(1, node.get("error").size());
        assertEquals(error ,node.get("error").get(0).asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error").get(0));
    }

    @Test
    void testAddProfile_InvalidIsPrivate_Then_400() throws Exception{
        // messaggio d'errore che mi aspetto d'ottenere
        String error = "JSON parse error: Cannot deserialize value of type `java.lang.Boolean` from String \"FalseString\": only \"true\" or \"false\" recognized";

        // per impostare una stringa in un campo boolean come isPrivate
        // devo convertire il Profile in formato json e modificare il campo direttamente
        String newAccountJson = this.objectMapper.writeValueAsString(newProfile);
        JsonNode jsonNode = this.objectMapper.readTree(newAccountJson);
        ((ObjectNode) jsonNode).put("is_private", "FalseString");
        newAccountJson = this.objectMapper.writeValueAsString(jsonNode);

        // Dato che invio direttamente il json del profile, devo impostare il contentType application/json
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> request = new HttpEntity<>(newAccountJson, headers);
        ResponseEntity<String> response = this.testRestTemplate.exchange(this.baseUrl,
                HttpMethod.POST,
                request,
                String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode node = this.objectMapper.readTree(response.getBody());
        // In questo caso l'errore NON è un array di dimensione 1
        assertEquals(error ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }

    @Test
    void testAddProfile_InvalidAccountId_Then_400() throws Exception{
        // messaggio d'errore che mi aspetto d'ottenere
        String error = "JSON parse error: Cannot deserialize value of type `java.lang.Long` from String \"IdNotLong\": not a valid `java.lang.Long` value";

        // per impostare una stringa in un campo Long come accountId
        // devo convertire il Profile in formato json e modificare il campo direttamente
        String newAccountJson = this.objectMapper.writeValueAsString(newProfile);
        JsonNode jsonNode = this.objectMapper.readTree(newAccountJson);
        ((ObjectNode) jsonNode).put("account_id", "IdNotLong");
        newAccountJson = this.objectMapper.writeValueAsString(jsonNode);

        // Dato che invio direttamente il json del profile, devo impostare il contentType application/json
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> request = new HttpEntity<>(newAccountJson, headers);
        ResponseEntity<String> response = this.testRestTemplate.exchange(this.baseUrl,
                HttpMethod.POST,
                request,
                String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode node = this.objectMapper.readTree(response.getBody());
        // In questo caso l'errore NON è un array di dimensione 1
        assertEquals(error ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }

    //TODO: controllo permessi d'accesso account id, JWT
    /*
    @Test
    void testAddProfile_Authentication_AccountId_Then_401() throws Exception{

    }
    */
    @Test
    void testAddProfile_ProfileNameExists_Then_409() throws Exception {
        // Per l'esecuzione in singolo di questo test e' necessario
        // inserire prima un profilo. Quindi eseguire la riga sottostante
        //testAddProfile_Then_201();

        // messaggio d'errore che mi aspetto d'ottenere
        String error = "Conflict! Profile with name "+profileName+" already created!";

        HttpEntity<Profile> request = new HttpEntity<>(newProfile);
        ResponseEntity<String> response = this.testRestTemplate.exchange(this.baseUrl,
                HttpMethod.POST,
                request,
                String.class);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode node = this.objectMapper.readTree(response.getBody());
        // In questo caso l'errore NON è un array di dimensione 1
        assertEquals(error ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }
}