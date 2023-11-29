package com.sergiostefanizzi.profilemicroservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sergiostefanizzi.profilemicroservice.model.*;
import com.sergiostefanizzi.profilemicroservice.service.KeycloakService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;

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
    @Autowired
    private Keycloak keycloak;
    @Autowired
    private KeycloakService keycloakService;
    private Profile savedProfile1;
    private Profile savedProfile2;
    private Profile savedProfile3;
    private Profile savedProfile4;
    private Profile newProfile;
    String profileName = "giuseppe_verdi";
    Boolean isPrivate = false;
    Boolean updatedIsPrivate = true;
    String accountId = "d7c2334d-9015-4b97-9768-5ba4d2f730dd";
    Long profileId;
    String bio = "This is Giuseppe's profile!";
    String updatedBio = "New Giuseppe's bio";
    String pictureUrl = "https://upload.wikimedia.org/wikipedia/commons/7/7e/Circle-icons-profile.svg";
    String pictureUrlXSS = "http://www.example.com?d=<script type=\"javascript\" src=\"http://www.google.it\"/>"; //Cross site scripting XSS
    String updatedPictureUrl = "https://icons-for-free.com/iconfiles/png/512/avatar+person+profile+user+icon-1320086059654790795.png";

    private final String password = "dshjdfkdjsf32!";
    private Map<String, List<String>> profileMap = new HashMap<>();

    @BeforeEach
    void setUp() {
        this.baseUrl += ":" + port + "/profiles";
        this.newProfile = new Profile(profileName,isPrivate);
        //this.newProfile.setAccountId(accountId);
        this.newProfile.setBio(bio);
        this.newProfile.setPictureUrl(pictureUrl);

        this.savedProfile1 = new Profile("pinco_palla", false);
        //this.savedProfile1.setAccountId("1fac5b86-7a95-439c-9940-d42691f0d9e5");
        this.savedProfile1.setId(101L);
        this.savedProfile1.setBio("Profilo di Pinco");
        this.savedProfile1.setPictureUrl(pictureUrl);

        this.savedProfile2 = new Profile("marioBros", false);
        //this.savedProfile2.setAccountId("757c2d08-7ffb-4e75-967e-6d5c9be26713");
        this.savedProfile2.setId(102L);
        this.savedProfile2.setBio("Profilo di Mario");
        this.savedProfile2.setPictureUrl(pictureUrl);

        this.savedProfile3 = new Profile("luigiBros", false);
        //this.savedProfile3.setAccountId("c365e7da-0650-4f29-9954-64cc9ba91ff1");
        this.savedProfile3.setId(103L);
        this.savedProfile3.setBio("Benvenuti!!");
        this.savedProfile3.setPictureUrl(pictureUrl);

        this.savedProfile4 = new Profile("pinco_palla2", false);
        //this.savedProfile4.setAccountId("1fac5b86-7a95-439c-9940-d42691f0d9e5");
        this.savedProfile4.setId(104L);
        this.savedProfile4.setBio("Secondo profilo di Pinco");
        this.savedProfile4.setPictureUrl(pictureUrl);


        this.profileMap.put(this.newProfile.getProfileName(), List.of("giuseppe.verdi@gmail.com"));
        this.profileMap.put(this.newProfile.getProfileName()+"2", List.of("giuseppe.verdi@gmail.com"));
        this.profileMap.put(this.savedProfile1.getProfileName(), List.of("pinco.palla@gmail.com","1fac5b86-7a95-439c-9940-d42691f0d9e5"));
        this.profileMap.put(this.savedProfile2.getProfileName(), List.of("mario.bros@gmail.com","757c2d08-7ffb-4e75-967e-6d5c9be26713"));
        this.profileMap.put(this.savedProfile3.getProfileName(), List.of("luigi.bros@gmail.com","c365e7da-0650-4f29-9954-64cc9ba91ff1"));
        this.profileMap.put(this.savedProfile4.getProfileName(), List.of("pinco.palla@gmail.com","1fac5b86-7a95-439c-9940-d42691f0d9e5"));
    }

    @AfterEach
    void tearDown() {
        this.profileMap.clear();
    }

    private String getAccessToken(String email) throws JsonProcessingException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        String loginBody = "grant_type=password&client_id=accounts-micro&username="+email+"&password="+this.password;
        HttpEntity<String> request = new HttpEntity<>(loginBody,headers);

        ResponseEntity<String> responseLogin = this.testRestTemplate.exchange(
                "http://localhost:8082/realms/social-accounts/protocol/openid-connect/token",
                HttpMethod.POST,
                request,
                String.class);


        assertEquals(HttpStatus.OK, responseLogin.getStatusCode());
        JsonNode node = this.objectMapper.readTree(responseLogin.getBody());
        String accessToken = node.get("access_token").asText();
        log.info("Access token = "+accessToken);
        return accessToken;
    }

    private void restoreProfileList(String accountId, Long profileId) {
        UserResource userResource = this.keycloak.realm("social-accounts")
                .users()
                .get(accountId);
        UserRepresentation userRepresentation = userResource
                .toRepresentation();

        Map<String, List<String>> attributes = userRepresentation.getAttributes();

        List<String> profileList = attributes.get("profileList");
        if(profileList == null){
            profileList = Collections.singletonList(profileId.toString());
        }else {
            profileList.add(profileId.toString());
        }


        attributes.put("profileList", profileList);
        userRepresentation.setAttributes(attributes);
        userResource
                .update(userRepresentation);
    }

    @Test
    void testAddProfile_Then_201() throws JsonProcessingException {
        String accessToken = getAccessToken(this.profileMap.get(this.newProfile.getProfileName()).get(0));
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Profile> request = new HttpEntity<>(this.newProfile, headers);
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
        assertNotNull(savedProfile.getAccountId());
        this.profileId = savedProfile.getId();

        // visualizzo il profilo salvato
        log.info(savedProfile.toString());
    }

    @Test
    void testAddProfile_RequiredFields_Then_201() throws JsonProcessingException {
        // per l'esecuzione in parallelo dei test
        // cambio il nome del profile perche' essendo un campo unique
        // genererebbe un 409 CONFLICT
        this.newProfile.setProfileName(profileName+"2");
        // imposto a null i campi non richiesti
        this.newProfile.setBio(null);
        this.newProfile.setPictureUrl(null);

        String accessToken = getAccessToken(this.profileMap.get(this.newProfile.getProfileName()).get(0));
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Profile> request = new HttpEntity<>(this.newProfile, headers);

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
        assertNotNull(savedProfile.getAccountId());

        // visualizzo il profilo salvato
        log.info(savedProfile.toString());
    }

    @Test
    void testAddProfile_MissingRequired_Then_400() throws Exception{
        // arrayList contenente i messaggi di errore
        List<String> errors = new ArrayList<>();
        errors.add("profileName must not be null");
        errors.add("isPrivate must not be null");


        String accessToken = getAccessToken(this.profileMap.get(this.newProfile.getProfileName()).get(0));
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        // Imposto a null i campi richiesti
        this.newProfile.setProfileName(null);
        this.newProfile.setIsPrivate(null);

        HttpEntity<Profile> request = new HttpEntity<>(this.newProfile, headers);
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



        String accessToken = getAccessToken(this.profileMap.get(this.newProfile.getProfileName()).get(0));
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        // imposto il profileName in modo che non rispetti le regole d'inserimento
        this.newProfile.setProfileName("g_v");

        HttpEntity<Profile> request = new HttpEntity<>(this.newProfile, headers);
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

        String accessToken = getAccessToken(this.profileMap.get(this.newProfile.getProfileName()).get(0));
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Profile> request = new HttpEntity<>(this.newProfile, headers);
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

        String accessToken = getAccessToken(this.profileMap.get(this.newProfile.getProfileName()).get(0));
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Profile> request = new HttpEntity<>(this.newProfile, headers);
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
    void testAddProfile_InvalidPictureUrlXSS_Then_400() throws Exception{
        // messaggio d'errore che mi aspetto d'ottenere
        String error = "pictureUrl must be a valid URL";
        // imposto un url non valido
        this.newProfile.setPictureUrl(pictureUrlXSS);
        String accessToken = getAccessToken(this.profileMap.get(this.newProfile.getProfileName()).get(0));
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Profile> request = new HttpEntity<>(this.newProfile, headers);
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
        String error = "Message is not readable";
        // per impostare una stringa in un campo boolean come isPrivate
        // devo convertire il Profile in formato json e modificare il campo direttamente
        String newProfileJson = this.objectMapper.writeValueAsString(this.newProfile);
        JsonNode jsonNode = this.objectMapper.readTree(newProfileJson);
        ((ObjectNode) jsonNode).put("is_private", "FalseString");
        newProfileJson = this.objectMapper.writeValueAsString(jsonNode);

        // Dato che invio direttamente il json del profile, devo impostare il contentType application/json
        String accessToken = getAccessToken(this.profileMap.get(this.newProfile.getProfileName()).get(0));
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
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
    void testAddProfile_Then_401(){
        ResponseEntity<Profile> response = this.testRestTemplate.exchange(this.baseUrl,
                HttpMethod.POST,
                HttpEntity.EMPTY,
                Profile.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }


    @Test
    void testAddProfile_ProfileNameExists_Then_409() throws Exception {
        // Per l'esecuzione in singolo di questo test e' necessario
        // inserire prima un profilo. Quindi eseguire la riga sottostante
        //testAddProfile_Then_201();

        // messaggio d'errore che mi aspetto d'ottenere
        String error = "Conflict! Profile with name "+this.savedProfile1.getProfileName()+" already created!";
        this.savedProfile1.setId(null);
        String accessToken = getAccessToken(this.profileMap.get(this.savedProfile1.getProfileName()).get(0));
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Profile> request = new HttpEntity<>(this.savedProfile1, headers);
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
        String accessToken = getAccessToken(this.profileMap.get(this.savedProfile2.getProfileName()).get(0));
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<String> request = new HttpEntity<>(headers);
        ResponseEntity<Void> responseDelete = this.testRestTemplate.exchange(this.baseUrl+"/{profileId}",
                HttpMethod.DELETE,
                request,
                Void.class,
                this.savedProfile2.getId());

        assertEquals(HttpStatus.NO_CONTENT, responseDelete.getStatusCode());
        assertNull(responseDelete.getBody());
        this.keycloakService.updateProfileList(this.profileMap.get(this.savedProfile2.getProfileName()).get(1), this.savedProfile2.getId());
    }

    @Test
    void testDeleteProfileById_Then_400() throws Exception{
        // messaggio d'errore che mi aspetto d'ottenere
        //String error = "Failed to convert value of type 'java.lang.String' to required type 'java.lang.Long'; For input string: \"IdNotLong\"";
        String error = "ID is not valid!";

        String accessToken = getAccessToken(this.profileMap.get(this.savedProfile2.getProfileName()).get(0));
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Profile> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = this.testRestTemplate.exchange(this.baseUrl+"/IdNotLong",
                HttpMethod.DELETE,
                request,
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
    void testDeleteProfileById_Then_401(){
        ResponseEntity<Profile> response = this.testRestTemplate.exchange(this.baseUrl,
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                Profile.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }
    @Test
    void testDeleteProfileById_Then_403() throws Exception{
        // messaggio d'errore che mi aspetto d'ottenere
        String error = "Profile "+this.savedProfile1.getId()+" is not inside the profile list!";

        String accessToken = getAccessToken(this.profileMap.get(this.savedProfile2.getProfileName()).get(0));
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Profile> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = this.testRestTemplate.exchange(this.baseUrl+"/{profileId}",
                HttpMethod.DELETE,
                request,
                String.class,
                this.savedProfile1.getId());

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode node = this.objectMapper.readTree(response.getBody());
        // In questo caso l'errore NON è un array di dimensione 1
        assertEquals(error ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }

    @Test
    void testDeleteProfileById_Then_404() throws Exception{
        Long invalidProfileId = Long.MAX_VALUE;
        // messaggio d'errore che mi aspetto d'ottenere
        String error = "Profile "+invalidProfileId+" not found!";

        String accessToken = getAccessToken(this.profileMap.get(this.savedProfile2.getProfileName()).get(0));
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Profile> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = this.testRestTemplate.exchange(this.baseUrl+"/{profileId}",
                HttpMethod.DELETE,
                request,
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

        String accessToken = getAccessToken(this.profileMap.get(this.savedProfile3.getProfileName()).get(0));
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<ProfilePatch> request = new HttpEntity<>(profilePatch, headers);
        ResponseEntity<Profile> responsePatch = this.testRestTemplate.exchange(this.baseUrl+"/{profileId}",
                HttpMethod.PATCH,
                request,
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
        assertEquals(this.profileMap.get(this.savedProfile3.getProfileName()).get(1), updatedProfile.getAccountId());
        // visualizzo il profilo aggiornato
        log.info(updatedProfile.toString());
    }

    @Test
    void testUpdateProfile_InvalidId_Then_400() throws Exception{
        //String error = "Failed to convert value of type 'java.lang.String' to required type 'java.lang.Long'; For input string: \"IdNotLong\"";
        String error = "ID is not valid!";
        // Definisco un o piu' campi del profilo da aggiornare tramite l'oggetto ProfilePatch
        ProfilePatch profilePatch = new ProfilePatch();

        String accessToken = getAccessToken(this.profileMap.get(this.savedProfile3.getProfileName()).get(0));
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<ProfilePatch> request = new HttpEntity<>(profilePatch, headers);
        ResponseEntity<String> response = this.testRestTemplate.exchange(this.baseUrl+"/{profileId}",
                HttpMethod.PATCH,
                request,
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

        String accessToken = getAccessToken(this.profileMap.get(this.savedProfile3.getProfileName()).get(0));
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<ProfilePatch> request = new HttpEntity<>(profilePatch, headers);
        ResponseEntity<String> response = this.testRestTemplate.exchange(this.baseUrl+"/{profileId}",
                HttpMethod.PATCH,
                request,
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

        String accessToken = getAccessToken(this.profileMap.get(this.savedProfile3.getProfileName()).get(0));
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<ProfilePatch> request = new HttpEntity<>(profilePatch, headers);
        ResponseEntity<String> response = this.testRestTemplate.exchange(this.baseUrl+"/{profileId}",
                HttpMethod.PATCH,
                request,
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
        String error = "Message is not readable";
        // Definisco un o piu' campi del profilo da aggiornare tramite l'oggetto ProfilePatch
        ProfilePatch profilePatch = new ProfilePatch();
        profilePatch.setIsPrivate(false);

        String profilePatchJson = this.objectMapper.writeValueAsString(profilePatch);

        JsonNode jsonNode = this.objectMapper.readTree(profilePatchJson);
        ((ObjectNode) jsonNode).put("is_private", "FalseString");
        profilePatchJson = this.objectMapper.writeValueAsString(jsonNode);


        String accessToken = getAccessToken(this.profileMap.get(this.savedProfile3.getProfileName()).get(0));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        HttpEntity<String> request = new HttpEntity<>(profilePatchJson, headers);
        ResponseEntity<String> response = this.testRestTemplate.exchange(this.baseUrl+"/{profileId}",
                HttpMethod.PATCH,
                request,
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
    void testUpdateProfile_Then_401() throws Exception{
        ResponseEntity<String> response = this.testRestTemplate.exchange(this.baseUrl,
                HttpMethod.PATCH,
                HttpEntity.EMPTY,
                String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());


    }

    @Test
    void testUpdateProfile_Then_403() throws Exception{
        String error = "Profile "+this.savedProfile3.getId()+" is not inside the profile list!";
        // Definisco un o piu' campi del profilo da aggiornare tramite l'oggetto ProfilePatch
        ProfilePatch profilePatch = new ProfilePatch();

        String accessToken = getAccessToken(this.profileMap.get(this.savedProfile1.getProfileName()).get(0));
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<ProfilePatch> request = new HttpEntity<>(profilePatch, headers);
        ResponseEntity<String> response = this.testRestTemplate.exchange(this.baseUrl+"/{profileId}",
                HttpMethod.PATCH,
                request,
                String.class,
                this.savedProfile3.getId());
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode node = this.objectMapper.readTree(response.getBody());
        // In questo caso l'errore NON è un array di dimensione 1
        assertEquals(error ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));

    }

    @Test
    void testUpdateProfile_Then_404() throws Exception{
        Long invalidProfileId = Long.MAX_VALUE;
        String error = "Profile "+invalidProfileId+" not found!";
        // Definisco un o piu' campi del profilo da aggiornare tramite l'oggetto ProfilePatch
        ProfilePatch profilePatch = new ProfilePatch();

        String accessToken = getAccessToken(this.profileMap.get(this.savedProfile3.getProfileName()).get(0));
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<ProfilePatch> request = new HttpEntity<>(profilePatch, headers);
        ResponseEntity<String> response = this.testRestTemplate.exchange(this.baseUrl+"/{profileId}",
                HttpMethod.PATCH,
                request,
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
        this.savedProfile1.setAccountId(this.profileMap.get(this.savedProfile1.getProfileName()).get(1));
        this.savedProfile4.setAccountId(this.profileMap.get(this.savedProfile4.getProfileName()).get(1));

        String accessToken = getAccessToken(this.profileMap.get(this.savedProfile1.getProfileName()).get(0));
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<ProfilePatch> request = new HttpEntity<>(headers);
        ResponseEntity<String> responseGet = this.testRestTemplate.exchange(this.baseUrl+"/search?profileName={profileName}",
                HttpMethod.GET,
                request,
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
        String accessToken = getAccessToken(this.profileMap.get(this.savedProfile1.getProfileName()).get(0));
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<ProfilePatch> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = this.testRestTemplate.exchange(this.baseUrl+"/search?profileName={profileName}",
                HttpMethod.GET,
                request,
                String.class,
                RandomStringUtils.randomAlphabetic(21));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode node = this.objectMapper.readTree(response.getBody());
        // In questo caso l'errore NON è un array di dimensione 1
        assertEquals("searchProfileByProfileName.profileName: size must be between 0 and 20", node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }

    @Test
    void testFindProfileByName_Then_400_2() throws Exception{
        String accessToken = getAccessToken(this.profileMap.get(this.savedProfile1.getProfileName()).get(0));
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<ProfilePatch> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = this.testRestTemplate.exchange(this.baseUrl+"/search",
                HttpMethod.GET,
                request,
                String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode node = this.objectMapper.readTree(response.getBody());
        // In questo caso l'errore NON è un array di dimensione 1
        assertEquals("Required request parameter 'profileName' for method parameter type String is not present", node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }

    @Test
    void testFindProfileByName_Then_401(){
        ResponseEntity<String> response = this.testRestTemplate.exchange(this.baseUrl+"/search?profileName={profileName}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                String.class,
                RandomStringUtils.randomAlphabetic(2));

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }


    @Test
    void testFindFull_Then_200() throws Exception{
        String accessToken = getAccessToken(this.profileMap.get(this.savedProfile3.getProfileName()).get(0));
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        ResponseEntity<FullProfile> responseGet = this.testRestTemplate.exchange(this.baseUrl+"/{profileId}?selectedUserProfileId={selectedUserProfileId}",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                FullProfile.class,
                this.savedProfile1.getId(),
                this.savedProfile3.getId());

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

    //TODO continuare da qui e modificare le HttpEntity dei metodi precedenti
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