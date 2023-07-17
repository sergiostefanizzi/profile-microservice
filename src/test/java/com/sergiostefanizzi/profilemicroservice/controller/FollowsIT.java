package com.sergiostefanizzi.profilemicroservice.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sergiostefanizzi.profilemicroservice.model.Follows;
import com.sergiostefanizzi.profilemicroservice.model.Post;
import com.sergiostefanizzi.profilemicroservice.model.Profile;
import com.sergiostefanizzi.profilemicroservice.repository.FollowsRepository;
import com.sergiostefanizzi.profilemicroservice.repository.PostsRepository;
import com.sergiostefanizzi.profilemicroservice.repository.ProfilesRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Slf4j
class FollowsIT {

    @LocalServerPort
    private int port;
    private String baseUrl = "http://localhost";
    @Autowired
    private TestRestTemplate testRestTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    Long openProfileId1 = 1L;
    Long openProfileId2 = 2L;
    Long privateProfileId = 3L;
    Long invalidProfileId = Long.MIN_VALUE;
    List<String> errors;

    @BeforeEach
    void setUp() {
        this.baseUrl += ":" + port + "/profiles";
        errors = new ArrayList<>();
    }

    @AfterEach
    void tearDown() {
        errors.clear();
    }

    @Test
    void testAddFollows_OpenProfile_Return_ACCEPTED_Then_201() {
        // Creo un primo profilo
        Profile publicProfile1 = createProfile("pincoPallino1", false);
        // Creo un secondo profilo
        Profile publicProfile2 = createProfile("pincoPallino2", false);


        ResponseEntity<Follows> response = this.testRestTemplate.exchange(
                this.baseUrl+"/{profileId}/follows/{followsId}",
                HttpMethod.POST,
                HttpEntity.EMPTY,
                Follows.class,
                publicProfile1.getId(),
                publicProfile2.getId());

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        Follows savedFollows = response.getBody();
        assertEquals(publicProfile1.getId(), savedFollows.getFollowerId());
        assertEquals(publicProfile2.getId(), savedFollows.getFollowedId());
        assertEquals(Follows.RequestStatusEnum.ACCEPTED, savedFollows.getRequestStatus());

        // visualizzo il post salvato
        log.info(savedFollows.toString());
    }

    @Test
    void testAddFollows_PrivateProfile_Or_RequestAlreadyRejected_Then_201(){
        // Creo un primo profilo
        Profile publicProfile = createProfile("pincoPallino3", false);
        // Creo un secondo profilo
        Profile privateProfile = createProfile("pincoPallino4", true);


        ResponseEntity<Follows> response = this.testRestTemplate.exchange(
                this.baseUrl+"/{profileId}/follows/{followsId}",
                HttpMethod.POST,
                HttpEntity.EMPTY,
                Follows.class,
                publicProfile.getId(),
                privateProfile.getId());

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        Follows savedFollows = response.getBody();
        assertEquals(publicProfile.getId(), savedFollows.getFollowerId());
        assertEquals(privateProfile.getId(), savedFollows.getFollowedId());
        assertEquals(Follows.RequestStatusEnum.PENDING, savedFollows.getRequestStatus());

        // visualizzo il post salvato
        log.info(savedFollows.toString());
    }

    //TODO da fare dopo accetta o rifiuta follower
    /*
    @Test
    void testAddFollows_RequestAlreadyRejected_Then_201(){
        // Creo un primo profilo
        Profile publicProfile = createProfile("pincoPallino5", false);
        // Creo un secondo profilo
        Profile privateProfile = createProfile("pincoPallino6", true);

        ResponseEntity<Follows> responseFollows1 = this.testRestTemplate.exchange(
                this.baseUrl+"/{profileId}/follows/{followsId}",
                HttpMethod.POST,
                HttpEntity.EMPTY,
                Follows.class,
                publicProfile.getId(),
                privateProfile.getId());

        assertEquals(HttpStatus.CREATED, responseFollows1.getStatusCode());
        assertNotNull(responseFollows1.getBody());
        Follows savedFollows1 = responseFollows1.getBody();
        assertEquals(publicProfile.getId(), savedFollows1.getFollowerId());
        assertEquals(privateProfile.getId(), savedFollows1.getFollowedId());
        assertEquals(Follows.RequestStatusEnum.PENDING, savedFollows1.getRequestStatus());


        ResponseEntity<Follows> responseFollows2 = this.testRestTemplate.exchange(
                this.baseUrl+"/{profileId}/follows/{followsId}",
                HttpMethod.POST,
                HttpEntity.EMPTY,
                Follows.class,
                openProfile.getId(),
                privateProfile.getId());

        assertEquals(HttpStatus.CREATED, responseFollows2.getStatusCode());
        assertNotNull(responseFollows2.getBody());
        Follows savedFollows2 = responseFollows2.getBody();
        assertEquals(openProfile.getId(), savedFollows2.getFollowerId());
        assertEquals(privateProfile.getId(), savedFollows2.getFollowedId());
        assertEquals(Follows.RequestStatusEnum.PENDING, savedFollows2.getRequestStatus());

        // visualizzo il post salvato
        log.info(savedFollows2.toString());
    }
     */

    @Test
    void testAddFollows_InvalidId_Then_400() throws Exception {
        errors.add("Failed to convert value of type 'java.lang.String' to required type 'java.lang.Long'; For input string: \"IdNotLong\"");

        Profile publicProfile = createProfile("pincoPallino7", false);

        ResponseEntity<String> response = this.testRestTemplate.exchange(
                this.baseUrl+"/{profileId}/follows/{followsId}",
                HttpMethod.POST,
                HttpEntity.EMPTY,
                String.class,
                publicProfile.getId(),
                "IdNotLong");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode node = this.objectMapper.readTree(response.getBody());
        // In questo caso l'errore NON è un array di dimensione 1
        assertEquals(errors.get(0) ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }

    //TODO 401

    @Test
    void testAddFollows_ProfileNotFound_Then_404() throws Exception {
        // messaggio d'errore che mi aspetto d'ottenere
        errors.add("Profile "+invalidProfileId+" not found!");

        Profile publicProfile = createProfile("pincoPallino8", false);

        ResponseEntity<String> response = this.testRestTemplate.exchange(
                this.baseUrl+"/{profileId}/follows/{followsId}",
                HttpMethod.POST,
                HttpEntity.EMPTY,
                String.class,
                publicProfile.getId(),
                invalidProfileId);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode node = this.objectMapper.readTree(response.getBody());
        // In questo caso l'errore NON è un array di dimensione 1
        assertEquals(errors.get(0) ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }
    @Test
    void testAddFollows_FollowAlreadyCreated_Private_Then_409() throws Exception {
        errors.add("Conflict! Follows already created!");

        // Creo un primo profilo
        Profile publicProfile = createProfile("pincoPallino9", false);
        // Creo un secondo profilo
        Profile privateProfile = createProfile("pincoPallino10", true);

        ResponseEntity<Follows> responseFollows1 = this.testRestTemplate.exchange(
                this.baseUrl+"/{profileId}/follows/{followsId}",
                HttpMethod.POST,
                HttpEntity.EMPTY,
                Follows.class,
                publicProfile.getId(),
                privateProfile.getId());

        assertEquals(HttpStatus.CREATED, responseFollows1.getStatusCode());
        assertNotNull(responseFollows1.getBody());
        Follows savedFollows1 = responseFollows1.getBody();
        assertEquals(publicProfile.getId(), savedFollows1.getFollowerId());
        assertEquals(privateProfile.getId(), savedFollows1.getFollowedId());
        assertEquals(Follows.RequestStatusEnum.PENDING, savedFollows1.getRequestStatus());


        ResponseEntity<String> responseFollows2 = this.testRestTemplate.exchange(
                this.baseUrl+"/{profileId}/follows/{followsId}",
                HttpMethod.POST,
                HttpEntity.EMPTY,
                String.class,
                publicProfile.getId(),
                privateProfile.getId());

        assertEquals(HttpStatus.CONFLICT, responseFollows2.getStatusCode());
        assertNotNull(responseFollows2.getBody());

        JsonNode node = this.objectMapper.readTree(responseFollows2.getBody());
        // In questo caso l'errore NON è un array di dimensione 1
        assertEquals(errors.get(0) ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));

        // visualizzo il post salvato
        log.info(responseFollows2.toString());
    }

    @Test
    void testAddFollows_FollowAlreadyCreated_Public_Then_409() throws Exception {
        errors.add("Conflict! Follows already created!");

        // Creo un primo profilo
        Profile publicProfile1 = createProfile("pincoPallino11", false);
        // Creo un secondo profilo
        Profile publicProfile2 = createProfile("pincoPallino12", false);

        ResponseEntity<Follows> responseFollows1 = this.testRestTemplate.exchange(
                this.baseUrl+"/{profileId}/follows/{followsId}",
                HttpMethod.POST,
                HttpEntity.EMPTY,
                Follows.class,
                publicProfile1.getId(),
                publicProfile2.getId());

        assertEquals(HttpStatus.CREATED, responseFollows1.getStatusCode());
        assertNotNull(responseFollows1.getBody());
        Follows savedFollows1 = responseFollows1.getBody();
        assertEquals(publicProfile1.getId(), savedFollows1.getFollowerId());
        assertEquals(publicProfile2.getId(), savedFollows1.getFollowedId());
        assertEquals(Follows.RequestStatusEnum.ACCEPTED, savedFollows1.getRequestStatus());


        ResponseEntity<String> responseFollows2 = this.testRestTemplate.exchange(
                this.baseUrl+"/{profileId}/follows/{followsId}",
                HttpMethod.POST,
                HttpEntity.EMPTY,
                String.class,
                publicProfile1.getId(),
                publicProfile2.getId());

        assertEquals(HttpStatus.CONFLICT, responseFollows2.getStatusCode());
        assertNotNull(responseFollows2.getBody());

        JsonNode node = this.objectMapper.readTree(responseFollows2.getBody());
        // In questo caso l'errore NON è un array di dimensione 1
        assertEquals(errors.get(0) ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));

        // visualizzo il post salvato
        log.info(responseFollows2.toString());
    }

    Profile createProfile(String profileName, Boolean isPrivate){
        Profile newProfile = new Profile(profileName,isPrivate,1L);
        // Creo prima un profilo
        HttpEntity<Profile> requestProfile = new HttpEntity<>(newProfile);
        ResponseEntity<Profile> responseProfile = this.testRestTemplate.exchange(
                this.baseUrl,
                HttpMethod.POST,
                requestProfile,
                Profile.class);
        assertEquals(HttpStatus.CREATED, responseProfile.getStatusCode());
        assertNotNull(responseProfile.getBody());
        Profile savedProfile = responseProfile.getBody();
        assertNotNull(savedProfile.getId());

        log.info(responseProfile.toString());

        return savedProfile;
    }
}