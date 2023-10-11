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
    private Profile savedProfile1;
    private Profile savedProfile2;
    private Profile savedProfile3;
    private Profile savedProfile4;
    private Profile savedPrivateProfile1;
    String pictureUrl = "https://upload.wikimedia.org/wikipedia/commons/7/7e/Circle-icons-profile.svg";

    @BeforeEach
    void setUp() {
        this.baseUrl += ":" + port + "/profiles";

        this.savedProfile1 = new Profile("pinco_pallino", false, 101L);
        this.savedProfile1.setId(101L);
        this.savedProfile1.setBio("Profilo di Pinco");
        this.savedProfile1.setPictureUrl(pictureUrl);

        this.savedProfile2 = new Profile("luigiBros", false, 103L);
        this.savedProfile2.setId(103L);
        this.savedProfile2.setBio("Benvenuti!!");
        this.savedProfile2.setPictureUrl(pictureUrl);

        this.savedProfile3 = new Profile("pinco_pallino2", false, 101L);
        this.savedProfile3.setId(104L);
        this.savedProfile3.setBio("Secondo profilo di Pinco");
        this.savedProfile3.setPictureUrl(pictureUrl);

        this.savedProfile4 = new Profile("matt_murdock", false, 105L);
        this.savedProfile4.setId(106L);
        this.savedProfile4.setBio("Profilo di Murdock");
        this.savedProfile4.setPictureUrl(pictureUrl);

        this.savedPrivateProfile1 = new Profile("tony_stark", true, 104L);
        this.savedPrivateProfile1.setId(105L);
        this.savedPrivateProfile1.setBio("Profilo di Tony");
        this.savedPrivateProfile1.setPictureUrl(pictureUrl);
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void testAddFollows_PublicProfile_Return_ACCEPTED_Then_200(){
        ResponseEntity<Follows> response = this.testRestTemplate.exchange(
                this.baseUrl+"/{profileId}/follows/{followsId}?unfollow={unfollow}",
                HttpMethod.PUT,
                HttpEntity.EMPTY,
                Follows.class,
                this.savedProfile1.getId(),
                this.savedProfile4.getId(),
                false);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        Follows savedFollows = response.getBody();
        assertEquals(this.savedProfile1.getId(), savedFollows.getFollowerId());
        assertEquals(this.savedProfile4.getId(), savedFollows.getFollowedId());
        log.info(savedFollows.toString());
        assertEquals(Follows.RequestStatusEnum.ACCEPTED, savedFollows.getRequestStatus());


    }

    @Test
    void testAddFollows_PrivateProfile_Return_Pending_Then_200(){
        ResponseEntity<Follows> response = this.testRestTemplate.exchange(
                this.baseUrl+"/{profileId}/follows/{followsId}?unfollow={unfollow}",
                HttpMethod.PUT,
                HttpEntity.EMPTY,
                Follows.class,
                this.savedProfile1.getId(),
                this.savedPrivateProfile1.getId(),
                false);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        Follows savedFollows = response.getBody();
        assertEquals(this.savedProfile1.getId(), savedFollows.getFollowerId());
        assertEquals(this.savedPrivateProfile1.getId(), savedFollows.getFollowedId());
        assertEquals(Follows.RequestStatusEnum.PENDING, savedFollows.getRequestStatus());

        // visualizzo il post salvato
        log.info(savedFollows.toString());
    }

    @Test
    void testAddFollows_Unfollow_Return_Rejected_Then_200(){
        ResponseEntity<Follows> responseUnfollows = this.testRestTemplate.exchange(
                this.baseUrl+"/{profileId}/follows/{followsId}?unfollow={unfollow}",
                HttpMethod.PUT,
                HttpEntity.EMPTY,
                Follows.class,
                this.savedProfile1.getId(),
                this.savedProfile3.getId(),
                true);

        assertEquals(HttpStatus.OK, responseUnfollows.getStatusCode());
        assertNotNull(responseUnfollows.getBody());
        Follows savedUnfollow = responseUnfollows.getBody();
        assertEquals(this.savedProfile1.getId(), savedUnfollow.getFollowerId());
        assertEquals(this.savedProfile3.getId(), savedUnfollow.getFollowedId());
        assertEquals(Follows.RequestStatusEnum.REJECTED, savedUnfollow.getRequestStatus());

        // visualizzo il post salvato
        log.info(savedUnfollow.toString());
    }

    @Test
    void testAddFollows_FollowItself_Then_400() throws Exception {
        // messaggio d'errore che mi aspetto d'ottenere
        String error = "Profile cannot follow itself!";

        ResponseEntity<String> response = this.testRestTemplate.exchange(
                this.baseUrl+"/{profileId}/follows/{followsId}?unfollow={unfollow}",
                HttpMethod.PUT,
                HttpEntity.EMPTY,
                String.class,
                this.savedProfile1.getId(),
                this.savedProfile1.getId(),
                false);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode node = this.objectMapper.readTree(response.getBody());
        // In questo caso l'errore NON è un array di dimensione 1
        assertEquals(error ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }


    @Test
    void testAddFollows_UnfollowOnCreation_Then_400() throws Exception {
        // messaggio d'errore che mi aspetto d'ottenere
        String error = "Unfollows on creation is not possible!";

        ResponseEntity<String> response = this.testRestTemplate.exchange(
                this.baseUrl+"/{profileId}/follows/{followsId}?unfollow={unfollow}",
                HttpMethod.PUT,
                HttpEntity.EMPTY,
                String.class,
                this.savedProfile1.getId(),
                this.savedProfile2.getId(),
                true);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode node = this.objectMapper.readTree(response.getBody());
        // In questo caso l'errore NON è un array di dimensione 1
        assertEquals(error ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }
    @Test
    void testAddFollows_InvalidId_Then_400() throws Exception {
        String error = "ID is not valid!";

        ResponseEntity<String> response = this.testRestTemplate.exchange(
                this.baseUrl+"/{profileId}/follows/{followsId}?unfollow={unfollow}",
                HttpMethod.PUT,
                HttpEntity.EMPTY,
                String.class,
                this.savedProfile1.getId(),
                "IdNotLong",
                false);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode node = this.objectMapper.readTree(response.getBody());
        // In questo caso l'errore NON è un array di dimensione 1
        assertEquals(error ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }

    //TODO 401

    @Test
    void testAddFollows_ProfileNotFound_Then_404() throws Exception {
        // messaggio d'errore che mi aspetto d'ottenere
        String error = "Profile "+Long.MAX_VALUE+" not found!";

        ResponseEntity<String> response = this.testRestTemplate.exchange(
                this.baseUrl+"/{profileId}/follows/{followsId}?unfollow={unfollow}",
                HttpMethod.PUT,
                HttpEntity.EMPTY,
                String.class,
                this.savedProfile1.getId(),
                Long.MAX_VALUE,
                false);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode node = this.objectMapper.readTree(response.getBody());
        // In questo caso l'errore NON è un array di dimensione 1
        assertEquals(error ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }

    @Test
    void testAcceptFollows_Then_200() throws Exception {
        ResponseEntity<Follows> responseAccept = this.testRestTemplate.exchange(
                this.baseUrl+"/{profileId}/followedBy/{followsId}?rejectFollow={rejectFollow}",
                HttpMethod.PUT,
                HttpEntity.EMPTY,
                Follows.class,
                this.savedPrivateProfile1.getId(),
                this.savedProfile2.getId(),
                false);

        assertEquals(HttpStatus.OK, responseAccept.getStatusCode());
        assertNotNull(responseAccept.getBody());
        Follows savedAccepted= responseAccept.getBody();
        assertEquals(this.savedPrivateProfile1.getId(), savedAccepted.getFollowedId());
        assertEquals(this.savedProfile2.getId(), savedAccepted.getFollowerId());
        assertEquals(Follows.RequestStatusEnum.ACCEPTED, savedAccepted.getRequestStatus());

        // visualizzo il post salvato
        log.info(savedAccepted.toString());
    }

    @Test
    void testAcceptFollows_AlreadyAccepted_Then_200() throws Exception {

        ResponseEntity<Follows> responseAccept = this.testRestTemplate.exchange(
                this.baseUrl+"/{profileId}/followedBy/{followsId}?rejectFollow={rejectFollow}",
                HttpMethod.PUT,
                HttpEntity.EMPTY,
                Follows.class,
                this.savedProfile1.getId(),
                this.savedProfile3.getId(),
                false);

        assertEquals(HttpStatus.OK, responseAccept.getStatusCode());
        assertNotNull(responseAccept.getBody());
        Follows savedAccepted= responseAccept.getBody();
        assertEquals(this.savedProfile1.getId(), savedAccepted.getFollowedId());
        assertEquals(this.savedProfile3.getId(), savedAccepted.getFollowerId());
        assertEquals(Follows.RequestStatusEnum.ACCEPTED, savedAccepted.getRequestStatus());

        // visualizzo il post salvato
        log.info(savedAccepted.toString());
    }

    @Test
    void testAcceptFollows_Reject_Then_200() throws Exception {
        ResponseEntity<Follows> responseReject = this.testRestTemplate.exchange(
                this.baseUrl+"/{profileId}/followedBy/{followsId}?rejectFollow={rejectFollow}",
                HttpMethod.PUT,
                HttpEntity.EMPTY,
                Follows.class,
                this.savedPrivateProfile1.getId(),
                this.savedProfile3.getId(),
                true);

        assertEquals(HttpStatus.OK, responseReject.getStatusCode());
        assertNotNull(responseReject.getBody());
        Follows savedReject= responseReject.getBody();
        assertEquals(this.savedProfile3.getId(), savedReject.getFollowerId());
        assertEquals(this.savedPrivateProfile1.getId(), savedReject.getFollowedId());
        assertEquals(Follows.RequestStatusEnum.REJECTED, savedReject.getRequestStatus());

        // visualizzo il post salvato
        log.info(savedReject.toString());

    }

    @Test
    void testAcceptFollows_BlockProfile_Reject_Then_200() throws Exception {
        ResponseEntity<Follows> responseReject = this.testRestTemplate.exchange(
                this.baseUrl+"/{profileId}/followedBy/{followsId}?rejectFollow={rejectFollow}",
                HttpMethod.PUT,
                HttpEntity.EMPTY,
                Follows.class,
                this.savedProfile1.getId(),
                this.savedProfile3.getId(),
                true);

        assertEquals(HttpStatus.OK, responseReject.getStatusCode());
        assertNotNull(responseReject.getBody());
        Follows savedReject= responseReject.getBody();
        assertEquals(this.savedProfile3.getId(), savedReject.getFollowerId());
        assertEquals(this.savedProfile1.getId(), savedReject.getFollowedId());
        assertEquals(Follows.RequestStatusEnum.REJECTED, savedReject.getRequestStatus());


        log.info(savedReject.toString());

    }

    @Test
    void testAcceptFollows_InvalidId_Then_400() throws Exception {
        String error = "ID is not valid!";

        ResponseEntity<String> response = this.testRestTemplate.exchange(
                this.baseUrl+"/{profileId}/followedBy/{followsId}?rejectFollow={rejectFollow}",
                HttpMethod.PUT,
                HttpEntity.EMPTY,
                String.class,
                this.savedProfile1.getId(),
                "IdNotLong",
                false);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode node = this.objectMapper.readTree(response.getBody());
        // In questo caso l'errore NON è un array di dimensione 1
        assertEquals(error ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }

    //TODO 401, 403


    @Test
    void testAcceptFollows_FollowNotFound_Then_404() throws Exception {
        String error = "Follows not found!";

        ResponseEntity<String> responseReject = this.testRestTemplate.exchange(
                this.baseUrl+"/{profileId}/followedBy/{followsId}?rejectFollow={rejectFollow}",
                HttpMethod.PUT,
                HttpEntity.EMPTY,
                String.class,
                this.savedProfile2.getId(),
                this.savedProfile3.getId(),
                true);

        assertEquals(HttpStatus.NOT_FOUND, responseReject.getStatusCode());
        assertNotNull(responseReject.getBody());

        JsonNode node = this.objectMapper.readTree(responseReject.getBody());
        // In questo caso l'errore NON è un array di dimensione 1
        assertEquals(error ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }

    @Test
    void testFindAllFollowers_Then_200() throws Exception {
        ResponseEntity<ProfileFollowList> responseFollowerList = this.testRestTemplate.exchange(
                this.baseUrl+"/{profileId}/followedBy",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                ProfileFollowList.class,
                this.savedProfile1.getId());

        assertEquals(HttpStatus.OK, responseFollowerList.getStatusCode());
        assertNotNull(responseFollowerList.getBody());
        ProfileFollowList profileFollowList = responseFollowerList.getBody();
        assertTrue(profileFollowList.getProfileCount()>0);


        log.info(responseFollowerList.toString());
    }



    @Test
    void testFindAllFollowers_Then_400() throws Exception {
        String error = "ID is not valid!";

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
        assertEquals(error ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }

    //TODO 401

    @Test
    void testFindAllFollowers_ProfileNotFound_Then_404() throws Exception {
        // messaggio d'errore che mi aspetto d'ottenere
        String error = "Profile "+Long.MAX_VALUE+" not found!";


        ResponseEntity<String> response = this.testRestTemplate.exchange(
                this.baseUrl+"/{profileId}/followedBy",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                String.class,
                Long.MAX_VALUE);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode node = this.objectMapper.readTree(response.getBody());
        // In questo caso l'errore NON è un array di dimensione 1
        assertEquals(error ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }

    @Test
    void testFindAllFollowings_Then_200() throws Exception {
        ResponseEntity<ProfileFollowList> responseFollowerList = this.testRestTemplate.exchange(
                this.baseUrl+"/{profileId}/follows",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                ProfileFollowList.class,
                this.savedProfile2.getId());

        assertEquals(HttpStatus.OK, responseFollowerList.getStatusCode());
        assertNotNull(responseFollowerList.getBody());
        ProfileFollowList profileFollowList = responseFollowerList.getBody();
        assertTrue(profileFollowList.getProfileCount()>=3);


        log.info(responseFollowerList.toString());
    }

    @Test
    void testFindAllFollowings_Then_400() throws Exception {
        String error = "ID is not valid!";
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
        assertEquals(error ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }

    //TODO 401

    @Test
    void testFindAllFollowings_ProfileNotFound_Then_404() throws Exception {
        // messaggio d'errore che mi aspetto d'ottenere
        String error = "Profile "+Long.MAX_VALUE+" not found!";


        ResponseEntity<String> response = this.testRestTemplate.exchange(
                this.baseUrl+"/{profileId}/follows",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                String.class,
                Long.MAX_VALUE);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode node = this.objectMapper.readTree(response.getBody());
        // In questo caso l'errore NON è un array di dimensione 1
        assertEquals(error ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }

}