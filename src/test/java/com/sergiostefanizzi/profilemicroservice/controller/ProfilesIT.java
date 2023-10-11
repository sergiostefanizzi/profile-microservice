package com.sergiostefanizzi.profilemicroservice.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.Criteria;
import com.sergiostefanizzi.profilemicroservice.model.*;
import com.sergiostefanizzi.profilemicroservice.model.converter.ProfileToProfileJpaConverter;
import com.sergiostefanizzi.profilemicroservice.repository.PostsRepository;
import com.sergiostefanizzi.profilemicroservice.repository.ProfilesRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
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
import java.util.Optional;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Slf4j
class ProfilesIT {
    @LocalServerPort
    private int port;

    private String baseUrl = "http://localhost";

    @Autowired
    private TestRestTemplate testRestTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    private Profile savedProfile1;
    private Profile savedProfile2;
    private Profile savedProfile3;
    private Profile savedProfile4;
    private Profile newProfile;
    String profileName = "giuseppe_verdi";
    Boolean isPrivate = false;
    Boolean updatedIsPrivate = true;
    Long accountId = 1L;
    Long profileId;
    String bio = "This is Giuseppe's profile!";
    String updatedBio = "New Giuseppe's bio";
    String pictureUrl = "https://upload.wikimedia.org/wikipedia/commons/7/7e/Circle-icons-profile.svg";
    String pictureUrlXSS = "http://www.example.com?d=<script type=\"javascript\" src=\"http://www.google.it\"/>"; //Cross site scripting XSS
    String updatedPictureUrl = "https://icons-for-free.com/iconfiles/png/512/avatar+person+profile+user+icon-1320086059654790795.png";

    @BeforeEach
    void setUp() {
        this.baseUrl += ":" + port + "/profiles";
        this.newProfile = new Profile(profileName,isPrivate,accountId);
        this.newProfile.setBio(bio);
        this.newProfile.setPictureUrl(pictureUrl);

        this.savedProfile1 = new Profile("pinco_pallino", false, 101L);
        this.savedProfile1.setId(101L);
        this.savedProfile1.setBio("Profilo di Pinco");
        this.savedProfile1.setPictureUrl(pictureUrl);

        this.savedProfile2 = new Profile("marioBros", false, 102L);
        this.savedProfile2.setId(102L);
        this.savedProfile2.setBio("Profilo di Mario");
        this.savedProfile2.setPictureUrl(pictureUrl);

        this.savedProfile3 = new Profile("luigiBros", false, 103L);
        this.savedProfile3.setId(103L);
        this.savedProfile3.setBio("Benvenuti!!");
        this.savedProfile3.setPictureUrl(pictureUrl);

        this.savedProfile4 = new Profile("pinco_pallino2", false, 101L);
        this.savedProfile4.setId(104L);
        this.savedProfile4.setBio("Secondo profilo di Pinco");
        this.savedProfile4.setPictureUrl(pictureUrl);
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void testAddProfile_Then_201(){
        HttpEntity<Profile> request = new HttpEntity<>(this.newProfile);
        ResponseEntity<Profile> response = this.testRestTemplate.exchange(this.baseUrl,
                HttpMethod.POST,
                request,
                Profile.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertInstanceOf(Profile.class, response.getBody());
        Profile savedProfile = response.getBody();
        assertNotNull(savedProfile.getId());
        assertEquals(this.newProfile.getProfileName(), savedProfile.getProfileName());
        assertEquals(this.newProfile.getBio(), savedProfile.getBio());
        assertEquals(this.newProfile.getPictureUrl(), savedProfile.getPictureUrl());
        assertEquals(this.newProfile.getIsPrivate(), savedProfile.getIsPrivate());
        assertEquals(this.newProfile.getAccountId(), savedProfile.getAccountId());
        this.profileId = savedProfile.getId();

        // visulazzo il profilo salvato
        log.info(savedProfile.toString());
    }

    @Test
    void testAddProfile_RequiredFields_Then_201(){
        // per l'esecuzione in parallelo dei test
        // cambio il nome del profile perche' essendo un campo unique
        // genererebbe un 409 CONFLICT
        this.newProfile.setProfileName(profileName+"2");
        // imposto a null i campi non richiesti
        this.newProfile.setBio(null);
        this.newProfile.setPictureUrl(null);

        HttpEntity<Profile> request = new HttpEntity<>(this.newProfile);
        ResponseEntity<Profile> response = this.testRestTemplate.exchange(this.baseUrl,
                HttpMethod.POST,
                request,
                Profile.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertInstanceOf(Profile.class, response.getBody());
        Profile savedProfile = response.getBody();
        assertNotNull(savedProfile.getId());
        assertEquals(this.newProfile.getProfileName(), savedProfile.getProfileName());
        assertNull(savedProfile.getBio());
        assertNull(savedProfile.getPictureUrl());
        assertEquals(this.newProfile.getIsPrivate(), savedProfile.getIsPrivate());
        assertEquals(this.newProfile.getAccountId(), savedProfile.getAccountId());

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

        HttpEntity<Profile> request = new HttpEntity<>(this.newProfile);
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

        HttpEntity<Profile> request = new HttpEntity<>(this.newProfile);
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

        HttpEntity<Profile> request = new HttpEntity<>(this.newProfile);
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
        //Test XSS
        //this.newProfile.setPictureUrl(pictureUrlXSS);
        HttpEntity<Profile> request = new HttpEntity<>(this.newProfile);
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
        String newProfileJson = this.objectMapper.writeValueAsString(this.newProfile);
        JsonNode jsonNode = this.objectMapper.readTree(newProfileJson);
        ((ObjectNode) jsonNode).put("is_private", "FalseString");
        newProfileJson = this.objectMapper.writeValueAsString(jsonNode);

        // Dato che invio direttamente il json del profile, devo impostare il contentType application/json
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> request = new HttpEntity<>(newProfileJson, headers);
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
        String newAccountJson = this.objectMapper.writeValueAsString(this.newProfile);
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

    //TODO: In inserimento fare controllo permessi d'accesso account id, JWT
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
        String error = "Conflict! Profile with name "+this.savedProfile1.getProfileName()+" already created!";
        this.savedProfile1.setId(null);
        HttpEntity<Profile> request = new HttpEntity<>(this.savedProfile1);
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

    @Test
    void testDeleteProfileById_Then_204() throws Exception{

        ResponseEntity<Void> responseDelete = this.testRestTemplate.exchange(this.baseUrl+"/{profileId}",
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                Void.class,
                this.savedProfile2.getId());

        assertEquals(HttpStatus.NO_CONTENT, responseDelete.getStatusCode());
        assertNull(responseDelete.getBody());
    }

    @Test
    void testDeleteProfileById_Then_400() throws Exception{
        // messaggio d'errore che mi aspetto d'ottenere
        //String error = "Failed to convert value of type 'java.lang.String' to required type 'java.lang.Long'; For input string: \"IdNotLong\"";
        String error = "ID is not valid!";
        ResponseEntity<String> response = this.testRestTemplate.exchange(this.baseUrl+"/IdNotLong",
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode node = this.objectMapper.readTree(response.getBody());
        // In questo caso l'errore NON è un array di dimensione 1
        assertEquals(error ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }

    //TODO: In rimozione fare controllo permessi d'accesso account id, JWT

    @Test
    void testDeleteProfileById_Then_404() throws Exception{
        Long invalidProfileId = Long.MIN_VALUE;
        // messaggio d'errore che mi aspetto d'ottenere
        String error = "Profile "+invalidProfileId+" not found!";
        ResponseEntity<String> response = this.testRestTemplate.exchange(this.baseUrl+"/{profileId}",
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                String.class,
                invalidProfileId);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode node = this.objectMapper.readTree(response.getBody());
        // In questo caso l'errore NON è un array di dimensione 1
        assertEquals(error ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }

    @Test
    void testUpdateProfile_Then_200() throws Exception{

        // Definisco un o piu' campi del profilo da aggiornare tramite l'oggetto ProfilePatch
        ProfilePatch profilePatch = new ProfilePatch();
        profilePatch.setBio(updatedBio);
        profilePatch.setPictureUrl(updatedPictureUrl);
        profilePatch.setIsPrivate(updatedIsPrivate);

        HttpEntity<ProfilePatch> requestPatch = new HttpEntity<>(profilePatch);
        ResponseEntity<Profile> responsePatch = this.testRestTemplate.exchange(this.baseUrl+"/{profileId}",
                HttpMethod.PATCH,
                requestPatch,
                Profile.class,
                this.savedProfile3.getId());

        assertEquals(HttpStatus.OK, responsePatch.getStatusCode());
        assertNotNull(responsePatch.getBody());
        assertInstanceOf(Profile.class, responsePatch.getBody());
        Profile updatedProfile = responsePatch.getBody();
        assertEquals(this.savedProfile3.getId(), updatedProfile.getId());
        assertEquals(this.savedProfile3.getProfileName(), updatedProfile.getProfileName());
        assertEquals(profilePatch.getBio() != null ? updatedBio : this.savedProfile3.getBio(), updatedProfile.getBio());
        assertEquals(profilePatch.getPictureUrl() != null ? updatedPictureUrl : this.savedProfile3.getPictureUrl(), updatedProfile.getPictureUrl());
        assertEquals(profilePatch.getIsPrivate() != null ? updatedIsPrivate : this.savedProfile3.getIsPrivate(), updatedProfile.getIsPrivate());
        assertEquals(this.savedProfile3.getAccountId(), updatedProfile.getAccountId());
        // visualizzo il profilo aggiornato
        log.info(updatedProfile.toString());
    }

    @Test
    void testUpdateProfile_InvalidId_Then_400() throws Exception{
        //String error = "Failed to convert value of type 'java.lang.String' to required type 'java.lang.Long'; For input string: \"IdNotLong\"";
        String error = "ID is not valid!";
        // Definisco un o piu' campi del profilo da aggiornare tramite l'oggetto ProfilePatch
        ProfilePatch profilePatch = new ProfilePatch();

        HttpEntity<ProfilePatch> requestPatch = new HttpEntity<>(profilePatch);
        ResponseEntity<String> response = this.testRestTemplate.exchange(this.baseUrl+"/{profileId}",
                HttpMethod.PATCH,
                requestPatch,
                String.class,
                "IdNotLong");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode node = this.objectMapper.readTree(response.getBody());
        // In questo caso l'errore NON è un array di dimensione 1
        assertEquals(error ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }

    @Test
    void testUpdateProfile_BioLength_Then_400() throws Exception {
        String error = "bio size must be between 0 and 150";

        // Definisco un o piu' campi del profilo da aggiornare tramite l'oggetto ProfilePatch
        ProfilePatch profilePatch = new ProfilePatch();
        profilePatch.setBio("Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Aenean commodo ligula eget dolor. Aenean massa. Cum sociis natoque penatibus et magnis dis parturient.");

        HttpEntity<ProfilePatch> requestPatch = new HttpEntity<>(profilePatch);
        ResponseEntity<String> response = this.testRestTemplate.exchange(this.baseUrl+"/{profileId}",
                HttpMethod.PATCH,
                requestPatch,
                String.class,
                this.savedProfile3.getId());
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode node = this.objectMapper.readTree(response.getBody());
        // In questo caso l'errore e' un array di dimensione 1
        assertTrue(node.get("error").isArray());
        assertEquals(1, node.get("error").size());
        assertEquals(error ,node.get("error").get(0).asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error").get(0));
    }

    @Test
    void testUpdateProfile_InvalidPictureUrl_Then_400() throws Exception{
        String error = "pictureUrl must be a valid URL";

        // Definisco un o piu' campi del profilo da aggiornare tramite l'oggetto ProfilePatch
        ProfilePatch profilePatch = new ProfilePatch();
        profilePatch.setPictureUrl("https://upload.wikimedia.o/ ra-%%$^&& iuyi");
        //Test XSS
        //profilePatch.setPictureUrl(pictureUrlXSS);

        HttpEntity<ProfilePatch> requestPatch = new HttpEntity<>(profilePatch);
        ResponseEntity<String> response = this.testRestTemplate.exchange(this.baseUrl+"/{profileId}",
                HttpMethod.PATCH,
                requestPatch,
                String.class,
                this.savedProfile3.getId());
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode node = this.objectMapper.readTree(response.getBody());
        // In questo caso l'errore e' un array di dimensione 1
        assertTrue(node.get("error").isArray());
        assertEquals(1, node.get("error").size());
        assertEquals(error ,node.get("error").get(0).asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error").get(0));
    }

    @Test
    void testUpdateProfile_InvalidIsPrivate_Then_400() throws Exception{
        String error = "JSON parse error: Cannot deserialize value of type `java.lang.Boolean` from String \"FalseString\": only \"true\" or \"false\" recognized";

        // Definisco un o piu' campi del profilo da aggiornare tramite l'oggetto ProfilePatch
        ProfilePatch profilePatch = new ProfilePatch();
        profilePatch.setIsPrivate(false);

        String profilePatchJson = this.objectMapper.writeValueAsString(profilePatch);

        JsonNode jsonNode = this.objectMapper.readTree(profilePatchJson);
        ((ObjectNode) jsonNode).put("is_private", "FalseString");
        profilePatchJson = this.objectMapper.writeValueAsString(jsonNode);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> requestPatch = new HttpEntity<>(profilePatchJson, headers);
        ResponseEntity<String> response = this.testRestTemplate.exchange(this.baseUrl+"/{profileId}",
                HttpMethod.PATCH,
                requestPatch,
                String.class,
                this.savedProfile3.getId());
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode node = this.objectMapper.readTree(response.getBody());
        // In questo caso l'errore NON è un array di dimensione 1
        assertEquals(error ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }

    //:TODO 401 e 403
    @Test
    void testUpdateProfile_Then_404() throws Exception{
        Long invalidProfileId = Long.MAX_VALUE;
        String error = "Profile "+invalidProfileId+" not found!";
        // Definisco un o piu' campi del profilo da aggiornare tramite l'oggetto ProfilePatch
        ProfilePatch profilePatch = new ProfilePatch();

        HttpEntity<ProfilePatch> requestPatch = new HttpEntity<>(profilePatch);
        ResponseEntity<String> response = this.testRestTemplate.exchange(this.baseUrl+"/{profileId}",
                HttpMethod.PATCH,
                requestPatch,
                String.class,
                invalidProfileId);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode node = this.objectMapper.readTree(response.getBody());
        // In questo caso l'errore NON è un array di dimensione 1
        assertEquals(error ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));

    }

    @Test
    void testFindProfileByName_Then_200() throws Exception{
        ResponseEntity<String> responseGet = this.testRestTemplate.exchange(this.baseUrl+"/search?profileName={profileName}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                String.class,
                "pinc");

        assertEquals(HttpStatus.OK, responseGet.getStatusCode());
        assertNotNull(responseGet.getBody());
        List<Profile> profileList =asList(objectMapper.readValue(responseGet.getBody(), Profile[].class));
        // Se eseguito singolarmente sono 2
        assertEquals(asList(this.savedProfile1, this.savedProfile4), profileList);
        // visualizzo il profilo aggiornato
        log.info(profileList.toString());
    }

    @Test
    void testFindProfileByName_Then_400() throws Exception{

        ResponseEntity<String> response = this.testRestTemplate.exchange(this.baseUrl+"/search?profileName={profileName}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                String.class,
                RandomStringUtils.randomAlphabetic(21));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode node = this.objectMapper.readTree(response.getBody());
        // In questo caso l'errore NON è un array di dimensione 1
        assertNotNull(node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }

    //TODO 401

    @Test
    void testFindFull_Then_200() throws Exception{

        ResponseEntity<FullProfile> responseGet = this.testRestTemplate.exchange(this.baseUrl+"/{profileId}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                FullProfile.class,
                this.savedProfile1.getId());

        assertEquals(HttpStatus.OK, responseGet.getStatusCode());
        assertNotNull(responseGet.getBody());
        assertInstanceOf(FullProfile.class, responseGet.getBody());
        FullProfile fullProfile = responseGet.getBody();
        assertEquals(this.savedProfile1.getId(), fullProfile.getProfile().getId());
        assertTrue(fullProfile.getPostList().size()>=3);
        assertTrue(fullProfile.getPostCount()>=3);
        assertTrue(fullProfile.getProfileGranted());

        // visualizzo il profilo aggiornato
        log.info(fullProfile.toString());
    }

    @Test
    void testFindFull_Then_400() throws Exception{
        ResponseEntity<String> response = this.testRestTemplate.exchange(this.baseUrl+"/{profileId]",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                String.class,
                "IdNotLong");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode node = this.objectMapper.readTree(response.getBody());
        // In questo caso l'errore NON è un array di dimensione 1
        assertNotNull(node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }

    //TODO 401

    @Test
    void testFindFull_Then_404() throws Exception{
        Long invalidProfileId = Long.MAX_VALUE;
        String error = "Profile "+invalidProfileId+" not found!";
        ResponseEntity<String> response = this.testRestTemplate.exchange(this.baseUrl+"/{profileId}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                String.class,
                invalidProfileId);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode node = this.objectMapper.readTree(response.getBody());
        // In questo caso l'errore NON è un array di dimensione 1
        assertEquals(error ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }


}