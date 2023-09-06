package com.sergiostefanizzi.profilemicroservice.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sergiostefanizzi.profilemicroservice.model.Follows;
import com.sergiostefanizzi.profilemicroservice.model.Profile;
import com.sergiostefanizzi.profilemicroservice.model.ProfileFollowList;
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
    void testAddFollows_PublicProfile_Return_ACCEPTED_Then_200(){
        // Creo un primo profilo
        Profile publicProfile1 = createProfile("pincoPallino1", false);
        // Creo un secondo profilo
        Profile publicProfile2 = createProfile("pincoPallino2", false);


        ResponseEntity<Follows> response = this.testRestTemplate.exchange(
                this.baseUrl+"/{profileId}/follows/{followsId}?unfollow={unfollow}",
                HttpMethod.PUT,
                HttpEntity.EMPTY,
                Follows.class,
                publicProfile1.getId(),
                publicProfile2.getId(),
                false);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        Follows savedFollows = response.getBody();
        assertEquals(publicProfile1.getId(), savedFollows.getFollowerId());
        assertEquals(publicProfile2.getId(), savedFollows.getFollowedId());
        assertEquals(Follows.RequestStatusEnum.ACCEPTED, savedFollows.getRequestStatus());

        // visualizzo il post salvato
        log.info(savedFollows.toString());
    }

    @Test
    void testAddFollows_PrivateProfile_Return_Pending_Then_200(){
        // Creo un primo profilo
        Profile publicProfile = createProfile("pincoPallino3", false);
        // Creo un secondo profilo
        Profile privateProfile = createProfile("pincoPallino4", true);


        ResponseEntity<Follows> response = this.testRestTemplate.exchange(
                this.baseUrl+"/{profileId}/follows/{followsId}?unfollow={unfollow}",
                HttpMethod.PUT,
                HttpEntity.EMPTY,
                Follows.class,
                publicProfile.getId(),
                privateProfile.getId(),
                false);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        Follows savedFollows = response.getBody();
        assertEquals(publicProfile.getId(), savedFollows.getFollowerId());
        assertEquals(privateProfile.getId(), savedFollows.getFollowedId());
        assertEquals(Follows.RequestStatusEnum.PENDING, savedFollows.getRequestStatus());

        // visualizzo il post salvato
        log.info(savedFollows.toString());
    }

    @Test
    void testAddFollows_Unfollow_Return_Rejected_Then_200(){
        // Creo un primo profilo
        Profile publicProfile1 = createProfile("pincoPallino5", false);
        // Creo un secondo profilo
        Profile publicProfile2 = createProfile("pincoPallino6", false);

        ResponseEntity<Follows> responseFollows = this.testRestTemplate.exchange(
                this.baseUrl+"/{profileId}/follows/{followsId}?unfollow={unfollow}",
                HttpMethod.PUT,
                HttpEntity.EMPTY,
                Follows.class,
                publicProfile1.getId(),
                publicProfile2.getId(),
                false);

        assertEquals(HttpStatus.OK, responseFollows.getStatusCode());
        assertNotNull(responseFollows.getBody());
        Follows savedFollows = responseFollows.getBody();
        assertEquals(publicProfile1.getId(), savedFollows.getFollowerId());
        assertEquals(publicProfile2.getId(), savedFollows.getFollowedId());
        assertEquals(Follows.RequestStatusEnum.ACCEPTED, savedFollows.getRequestStatus());


        ResponseEntity<Follows> responseUnfollows = this.testRestTemplate.exchange(
                this.baseUrl+"/{profileId}/follows/{followsId}?unfollow={unfollow}",
                HttpMethod.PUT,
                HttpEntity.EMPTY,
                Follows.class,
                publicProfile1.getId(),
                publicProfile2.getId(),
                true);

        assertEquals(HttpStatus.OK, responseUnfollows.getStatusCode());
        assertNotNull(responseUnfollows.getBody());
        Follows savedUnfollow = responseUnfollows.getBody();
        assertEquals(publicProfile1.getId(), savedUnfollow.getFollowerId());
        assertEquals(publicProfile2.getId(), savedUnfollow.getFollowedId());
        assertEquals(Follows.RequestStatusEnum.REJECTED, savedUnfollow.getRequestStatus());

        // visualizzo il post salvato
        log.info(savedUnfollow.toString());
    }


    @Test
    void testAddFollows_UnfollowOnCreation_Then_400() throws Exception {
        // messaggio d'errore che mi aspetto d'ottenere
        errors.add("Unfollows on creation is not possible!");

        Profile publicProfile1 = createProfile("pincoPallino7", false);
        Profile publicProfile2 = createProfile("pincoPallino8", false);

        ResponseEntity<String> response = this.testRestTemplate.exchange(
                this.baseUrl+"/{profileId}/follows/{followsId}?unfollow={unfollow}",
                HttpMethod.PUT,
                HttpEntity.EMPTY,
                String.class,
                publicProfile1.getId(),
                publicProfile2.getId(),
                true);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode node = this.objectMapper.readTree(response.getBody());
        // In questo caso l'errore NON è un array di dimensione 1
        assertEquals(errors.get(0) ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }
    @Test
    void testAddFollows_InvalidId_Then_400() throws Exception {
        errors.add("ID is not valid!");

        Profile publicProfile = createProfile("pincoPallino9", false);

        ResponseEntity<String> response = this.testRestTemplate.exchange(
                this.baseUrl+"/{profileId}/follows/{followsId}?unfollow={unfollow}",
                HttpMethod.PUT,
                HttpEntity.EMPTY,
                String.class,
                publicProfile.getId(),
                "IdNotLong",
                false);

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

        Profile publicProfile = createProfile("pincoPallino10", false);

        ResponseEntity<String> response = this.testRestTemplate.exchange(
                this.baseUrl+"/{profileId}/follows/{followsId}?unfollow={unfollow}",
                HttpMethod.PUT,
                HttpEntity.EMPTY,
                String.class,
                publicProfile.getId(),
                invalidProfileId,
                false);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode node = this.objectMapper.readTree(response.getBody());
        // In questo caso l'errore NON è un array di dimensione 1
        assertEquals(errors.get(0) ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }

    @Test
    void testAcceptFollows_Then_200() throws Exception {
        // Creo un primo profilo
        Profile publicProfile = createProfile("pincoPallino11", false);
        // Creo un secondo profilo
        Profile privateProfile = createProfile("pincoPallino12", true);

        ResponseEntity<Follows> responseFollows = this.testRestTemplate.exchange(
                this.baseUrl+"/{profileId}/follows/{followsId}?unfollow={unfollow}",
                HttpMethod.PUT,
                HttpEntity.EMPTY,
                Follows.class,
                publicProfile.getId(),
                privateProfile.getId(),
                false);

        assertEquals(HttpStatus.OK, responseFollows.getStatusCode());
        assertNotNull(responseFollows.getBody());
        Follows savedFollows = responseFollows.getBody();
        assertEquals(publicProfile.getId(), savedFollows.getFollowerId());
        assertEquals(privateProfile.getId(), savedFollows.getFollowedId());
        assertEquals(Follows.RequestStatusEnum.PENDING, savedFollows.getRequestStatus());

        log.info(savedFollows.toString());

        ResponseEntity<Follows> responseAccept = this.testRestTemplate.exchange(
                this.baseUrl+"/{profileId}/followedBy/{followsId}?rejectFollow={rejectFollow}",
                HttpMethod.PUT,
                HttpEntity.EMPTY,
                Follows.class,
                privateProfile.getId(),
                publicProfile.getId(),
                false);

        assertEquals(HttpStatus.OK, responseAccept.getStatusCode());
        assertNotNull(responseAccept.getBody());
        Follows savedAccepted= responseAccept.getBody();
        assertEquals(publicProfile.getId(), savedAccepted.getFollowerId());
        assertEquals(privateProfile.getId(), savedAccepted.getFollowedId());
        assertEquals(Follows.RequestStatusEnum.ACCEPTED, savedAccepted.getRequestStatus());

        // visualizzo il post salvato
        log.info(savedAccepted.toString());
    }

    @Test
    void testAcceptFollows_AlreadyAccepted_Then_200() throws Exception {
        // Creo un primo profilo
        Profile publicProfile1 = createProfile("pincoPallino13", false);
        // Creo un secondo profilo
        Profile publicProfile2 = createProfile("pincoPallino14", false);

        ResponseEntity<Follows> responseFollows = this.testRestTemplate.exchange(
                this.baseUrl+"/{profileId}/follows/{followsId}?unfollow={unfollow}",
                HttpMethod.PUT,
                HttpEntity.EMPTY,
                Follows.class,
                publicProfile1.getId(),
                publicProfile2.getId(),
                false);

        assertEquals(HttpStatus.OK, responseFollows.getStatusCode());
        assertNotNull(responseFollows.getBody());
        Follows savedFollows = responseFollows.getBody();
        assertEquals(publicProfile1.getId(), savedFollows.getFollowerId());
        assertEquals(publicProfile2.getId(), savedFollows.getFollowedId());
        assertEquals(Follows.RequestStatusEnum.ACCEPTED, savedFollows.getRequestStatus());

        log.info(savedFollows.toString());

        ResponseEntity<Follows> responseAccept = this.testRestTemplate.exchange(
                this.baseUrl+"/{profileId}/followedBy/{followsId}?rejectFollow={rejectFollow}",
                HttpMethod.PUT,
                HttpEntity.EMPTY,
                Follows.class,
                publicProfile2.getId(),
                publicProfile1.getId(),
                false);

        assertEquals(HttpStatus.OK, responseAccept.getStatusCode());
        assertNotNull(responseAccept.getBody());
        Follows savedAccepted= responseAccept.getBody();
        assertEquals(publicProfile1.getId(), savedAccepted.getFollowerId());
        assertEquals(publicProfile2.getId(), savedAccepted.getFollowedId());
        assertEquals(Follows.RequestStatusEnum.ACCEPTED, savedAccepted.getRequestStatus());

        // visualizzo il post salvato
        log.info(savedAccepted.toString());
    }

    @Test
    void testAcceptFollows_Reject_Then_200() throws Exception {
        // Creo un primo profilo
        Profile publicProfile = createProfile("pincoPallino15", false);
        // Creo un secondo profilo
        Profile privateProfile = createProfile("pincoPallino16", true);

        ResponseEntity<Follows> responseFollows = this.testRestTemplate.exchange(
                this.baseUrl+"/{profileId}/follows/{followsId}?unfollow={unfollow}",
                HttpMethod.PUT,
                HttpEntity.EMPTY,
                Follows.class,
                publicProfile.getId(),
                privateProfile.getId(),
                false);

        assertEquals(HttpStatus.OK, responseFollows.getStatusCode());
        assertNotNull(responseFollows.getBody());
        Follows savedFollows = responseFollows.getBody();
        assertEquals(publicProfile.getId(), savedFollows.getFollowerId());
        assertEquals(privateProfile.getId(), savedFollows.getFollowedId());
        assertEquals(Follows.RequestStatusEnum.PENDING, savedFollows.getRequestStatus());

        log.info(savedFollows.toString());

        ResponseEntity<Follows> responseReject = this.testRestTemplate.exchange(
                this.baseUrl+"/{profileId}/followedBy/{followsId}?rejectFollow={rejectFollow}",
                HttpMethod.PUT,
                HttpEntity.EMPTY,
                Follows.class,
                privateProfile.getId(),
                publicProfile.getId(),
                true);

        assertEquals(HttpStatus.OK, responseReject.getStatusCode());
        assertNotNull(responseReject.getBody());
        Follows savedReject= responseReject.getBody();
        assertEquals(publicProfile.getId(), savedReject.getFollowerId());
        assertEquals(privateProfile.getId(), savedReject.getFollowedId());
        assertEquals(Follows.RequestStatusEnum.REJECTED, savedReject.getRequestStatus());

        // visualizzo il post salvato
        log.info(savedReject.toString());

    }

    @Test
    void testAcceptFollows_BlockProfile_Reject_Then_200() throws Exception {
        // Creo un primo profilo
        Profile publicProfile1 = createProfile("pincoPallino17", false);
        // Creo un secondo profilo
        Profile publicProfile2 = createProfile("pincoPallino18", false);

        ResponseEntity<Follows> responseFollows = this.testRestTemplate.exchange(
                this.baseUrl+"/{profileId}/follows/{followsId}?unfollow={unfollow}",
                HttpMethod.PUT,
                HttpEntity.EMPTY,
                Follows.class,
                publicProfile1.getId(),
                publicProfile2.getId(),
                false);

        assertEquals(HttpStatus.OK, responseFollows.getStatusCode());
        assertNotNull(responseFollows.getBody());
        Follows savedFollows = responseFollows.getBody();
        assertEquals(publicProfile1.getId(), savedFollows.getFollowerId());
        assertEquals(publicProfile2.getId(), savedFollows.getFollowedId());
        assertEquals(Follows.RequestStatusEnum.ACCEPTED, savedFollows.getRequestStatus());

        log.info(savedFollows.toString());

        ResponseEntity<Follows> responseReject = this.testRestTemplate.exchange(
                this.baseUrl+"/{profileId}/followedBy/{followsId}?rejectFollow={rejectFollow}",
                HttpMethod.PUT,
                HttpEntity.EMPTY,
                Follows.class,
                publicProfile2.getId(),
                publicProfile1.getId(),
                true);

        assertEquals(HttpStatus.OK, responseReject.getStatusCode());
        assertNotNull(responseReject.getBody());
        Follows savedReject= responseReject.getBody();
        assertEquals(publicProfile1.getId(), savedReject.getFollowerId());
        assertEquals(publicProfile2.getId(), savedReject.getFollowedId());
        assertEquals(Follows.RequestStatusEnum.REJECTED, savedReject.getRequestStatus());


        log.info(savedReject.toString());

    }

    @Test
    void testAcceptFollows_InvalidId_Then_400() throws Exception {
        errors.add("ID is not valid!");
        // Creo un primo profilo
        Profile publicProfile1 = createProfile("pincoPallino19", false);
        ResponseEntity<String> response = this.testRestTemplate.exchange(
                this.baseUrl+"/{profileId}/followedBy/{followsId}?rejectFollow={rejectFollow}",
                HttpMethod.PUT,
                HttpEntity.EMPTY,
                String.class,
                publicProfile1.getId(),
                "IdNotLong",
                false);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode node = this.objectMapper.readTree(response.getBody());
        // In questo caso l'errore NON è un array di dimensione 1
        assertEquals(errors.get(0) ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }

    //TODO 401, 403


    @Test
    void testAcceptFollows_FollowNotFound_Then_404() throws Exception {
        errors.add("Follows not found!");
        // Creo un primo profilo
        Profile publicProfile1 = createProfile("pincoPallino20", false);
        Profile publicProfile2 = createProfile("pincoPallino21", false);
        ResponseEntity<String> responseReject = this.testRestTemplate.exchange(
                this.baseUrl+"/{profileId}/followedBy/{followsId}?rejectFollow={rejectFollow}",
                HttpMethod.PUT,
                HttpEntity.EMPTY,
                String.class,
                publicProfile2.getId(),
                publicProfile1.getId(),
                true);

        assertEquals(HttpStatus.NOT_FOUND, responseReject.getStatusCode());
        assertNotNull(responseReject.getBody());

        JsonNode node = this.objectMapper.readTree(responseReject.getBody());
        // In questo caso l'errore NON è un array di dimensione 1
        assertEquals(errors.get(0) ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }

    @Test
    void testFindAllFollowers_Then_200() throws Exception {
        Profile publicProfile1 = createProfile("pincoPallino22", false);
        Profile publicProfile2 = createProfile("pincoPallino23", false);
        Profile publicProfile3 = createProfile("pincoPallino24", false);
        createFollow(publicProfile2, publicProfile1);
        createFollow(publicProfile3, publicProfile1);

        ResponseEntity<ProfileFollowList> responseFollowerList = this.testRestTemplate.exchange(
                this.baseUrl+"/{profileId}/followedBy",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                ProfileFollowList.class,
                publicProfile1.getId());

        assertEquals(HttpStatus.OK, responseFollowerList.getStatusCode());
        assertNotNull(responseFollowerList.getBody());
        ProfileFollowList profileFollowList = responseFollowerList.getBody();
        assertEquals(2,profileFollowList.getProfileCount());
        assertEquals(publicProfile2, profileFollowList.getProfiles().get(0));
        assertEquals(publicProfile3, profileFollowList.getProfiles().get(1));

        log.info(responseFollowerList.toString());
    }

    @Test
    void testFindAllFollowers_NoFollowers_Then_200() throws Exception {
        Profile publicProfile1 = createProfile("pincoPallino25", false);


        ResponseEntity<ProfileFollowList> responseFollowerList = this.testRestTemplate.exchange(
                this.baseUrl+"/{profileId}/followedBy",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                ProfileFollowList.class,
                publicProfile1.getId());

        assertEquals(HttpStatus.OK, responseFollowerList.getStatusCode());
        assertNotNull(responseFollowerList.getBody());
        ProfileFollowList profileFollowList = responseFollowerList.getBody();
        assertEquals(0,profileFollowList.getProfileCount());
        assertTrue(profileFollowList.getProfiles().isEmpty());


        log.info(responseFollowerList.toString());
    }


    @Test
    void testFindAllFollowers_Then_400() throws Exception {
        errors.add("ID is not valid!");

        ResponseEntity<String> response = this.testRestTemplate.exchange(
                this.baseUrl+"/{profileId}/followedBy",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                String.class,
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
    void testFindAllFollowers_ProfileNotFound_Then_404() throws Exception {
        // messaggio d'errore che mi aspetto d'ottenere
        errors.add("Profile "+invalidProfileId+" not found!");


        ResponseEntity<String> response = this.testRestTemplate.exchange(
                this.baseUrl+"/{profileId}/followedBy",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                String.class,
                invalidProfileId);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode node = this.objectMapper.readTree(response.getBody());
        // In questo caso l'errore NON è un array di dimensione 1
        assertEquals(errors.get(0) ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }

    @Test
    void testFindAllFollowings_Then_200() throws Exception {
        Profile publicProfile1 = createProfile("pincoPallino26", false);
        Profile publicProfile2 = createProfile("pincoPallino27", false);
        Profile publicProfile3 = createProfile("pincoPallino28", false);
        createFollow(publicProfile1, publicProfile2);
        createFollow(publicProfile1, publicProfile3);

        ResponseEntity<ProfileFollowList> responseFollowerList = this.testRestTemplate.exchange(
                this.baseUrl+"/{profileId}/follows",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                ProfileFollowList.class,
                publicProfile1.getId());

        assertEquals(HttpStatus.OK, responseFollowerList.getStatusCode());
        assertNotNull(responseFollowerList.getBody());
        ProfileFollowList profileFollowList = responseFollowerList.getBody();
        assertEquals(2,profileFollowList.getProfileCount());
        assertEquals(publicProfile2, profileFollowList.getProfiles().get(0));
        assertEquals(publicProfile3, profileFollowList.getProfiles().get(1));

        log.info(responseFollowerList.toString());
    }

    @Test
    void testFindAllFollowings_NoFollowings_Then_200() throws Exception {
        Profile publicProfile1 = createProfile("pincoPallino29", false);


        ResponseEntity<ProfileFollowList> responseFollowerList = this.testRestTemplate.exchange(
                this.baseUrl+"/{profileId}/follows",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                ProfileFollowList.class,
                publicProfile1.getId());

        assertEquals(HttpStatus.OK, responseFollowerList.getStatusCode());
        assertNotNull(responseFollowerList.getBody());
        ProfileFollowList profileFollowList = responseFollowerList.getBody();
        assertEquals(0,profileFollowList.getProfileCount());
        assertTrue(profileFollowList.getProfiles().isEmpty());


        log.info(responseFollowerList.toString());
    }

    @Test
    void testFindAllFollowings_Then_400() throws Exception {
        errors.add("ID is not valid!");
        ResponseEntity<String> response = this.testRestTemplate.exchange(
                this.baseUrl+"/{profileId}/follows",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                String.class,
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
    void testFindAllFollowings_ProfileNotFound_Then_404() throws Exception {
        // messaggio d'errore che mi aspetto d'ottenere
        errors.add("Profile "+invalidProfileId+" not found!");


        ResponseEntity<String> response = this.testRestTemplate.exchange(
                this.baseUrl+"/{profileId}/follows",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                String.class,
                invalidProfileId);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode node = this.objectMapper.readTree(response.getBody());
        // In questo caso l'errore NON è un array di dimensione 1
        assertEquals(errors.get(0) ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
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

    void createFollow(Profile follower, Profile followed){
        ResponseEntity<Follows> responseFollows = this.testRestTemplate.exchange(
                this.baseUrl+"/{profileId}/follows/{followsId}?unfollow={unfollow}",
                HttpMethod.PUT,
                HttpEntity.EMPTY,
                Follows.class,
                follower.getId(),
                followed.getId(),
                false);

        assertEquals(HttpStatus.OK, responseFollows.getStatusCode());
        assertNotNull(responseFollows.getBody());
        Follows savedFollows = responseFollows.getBody();
        assertEquals(follower.getId(), savedFollows.getFollowerId());
        assertEquals(followed.getId(), savedFollows.getFollowedId());
        assertEquals(Follows.RequestStatusEnum.ACCEPTED, savedFollows.getRequestStatus());

        log.info(savedFollows.toString());
    }
}