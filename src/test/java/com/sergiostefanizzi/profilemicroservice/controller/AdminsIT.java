package com.sergiostefanizzi.profilemicroservice.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sergiostefanizzi.profilemicroservice.model.*;
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

import java.time.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Slf4j
public class AdminsIT {

    @LocalServerPort
    private int port;
    private String baseUrl = "http://localhost";
    private String baseUrlAdminProfiles;
    private String baseUrlAdminAlerts;
    private String baseUrlProfile;
    private String baseUrlPost;
    private String baseUrlAlerts;
    private String baseUrlComment;
    private String contentUrl = "https://upload.wikimedia.org/wikipedia/commons/9/9a/Cape_may.jpg";
    private String alertOwnerProfileName = "giovanni2";
    private String postOwnerProfileName = "pincoPalla";
    private String alertReason = "Motivo della segnalazione";
    private Long managedByAdminId = 3L;
    @Autowired
    private TestRestTemplate testRestTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        this.baseUrl += ":" + port;
        this.baseUrlProfile = this.baseUrl + "/profiles";
        this.baseUrlPost = this.baseUrl + "/posts";
        this.baseUrlComment = this.baseUrl + "/posts/comments";
        this.baseUrlAdminProfiles =  this.baseUrl + "/admins/profiles";
        this.baseUrlAdminAlerts =  this.baseUrl + "/admins/alerts";
        this.baseUrlAlerts =  this.baseUrl + "/alerts";
    }

    @AfterEach
    void tearDown() {

    }

    @Test
    void testBlockProfileById_Block_Then_200(){
        Profile profileToBlock = createPublicProfile("marioBros",1L);
        Profile updatedProfile = blockProfile( profileToBlock, true, 3);
        log.info("Profile ID: "+profileToBlock.getId()+" blocked until "+updatedProfile.getBlockedUntil());
    }

    @Test
    void testBlockProfileById_Unblock_Then_200(){
        Profile profileToBlock = createPublicProfile("marioBros2",1L);
        Profile updatedProfile = blockProfile(profileToBlock, false, null);
        assertNull(updatedProfile.getBlockedUntil());
        log.info("Profile ID: "+profileToBlock.getId()+" unblocked");
    }

    @Test
    void testBlockProfileById_Extends_Block_Then_200(){
        Profile profileToBlock = createPublicProfile("marioBros3",1L);
        Profile updatedProfile = blockProfile( profileToBlock, true, 3);
        log.info("Profile ID: "+profileToBlock.getId()+" blocked until "+updatedProfile.getBlockedUntil());
        updatedProfile = blockProfile( profileToBlock, true, 4);
        log.info("Profile ID: "+profileToBlock.getId()+" blocked until "+updatedProfile.getBlockedUntil());
    }

    @Test
    void testBlockProfileById_Unblock_anotherTime_Then_200(){
        Profile profileToBlock = createPublicProfile("marioBros4",1L);
        Profile updatedProfile = blockProfile(profileToBlock, false, null);
        assertNull(updatedProfile.getBlockedUntil());
        log.info("Profile ID: "+profileToBlock.getId()+" unblocked");

        updatedProfile = blockProfile(profileToBlock, false, null);
        assertNull(updatedProfile.getBlockedUntil());
        log.info("Profile ID: "+profileToBlock.getId()+" unblocked");
    }

    @Test
    void testBlockProfileById_Block_PastDate_Then_400() throws Exception {
        Profile profileToBlock = createPublicProfile("marioBros6", 1L);

        ProfileAdminPatch profileAdminPatch = new ProfileAdminPatch();

        profileAdminPatch.setBlockedUntil(OffsetDateTime.of(
                LocalDateTime.now().minusDays(3),
                ZoneOffset.UTC
        ));


        HttpEntity<ProfileAdminPatch> requestProfileAdminPatch = new HttpEntity<>(profileAdminPatch);
        ResponseEntity<String> response = this.testRestTemplate.exchange(
                this.baseUrlAdminProfiles+"/{profileId}",
                HttpMethod.PATCH,
                requestProfileAdminPatch,
                String.class,
                profileToBlock.getId()
        );
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode node = this.objectMapper.readTree(response.getBody());
        // itero gli errori ottenuti dalla risposta per confrontarli con quelli che mi aspetto di ottenere
        assertEquals(1 ,node.get("error").size());
        for (JsonNode objNode : node.get("error")) {
            assertEquals("blockedUntil must be a future date", objNode.asText());
            log.info("Error -> "+objNode.asText());
        }

    }

    @Test
    void testBlockProfileById_Block_IdNotLong_Then_400() throws Exception {
        ProfileAdminPatch profileAdminPatch = new ProfileAdminPatch();

        profileAdminPatch.setBlockedUntil(OffsetDateTime.of(
                LocalDateTime.now().plusDays(3),
                ZoneOffset.UTC
        ));


        HttpEntity<ProfileAdminPatch> requestProfileAdminPatch = new HttpEntity<>(profileAdminPatch);
        ResponseEntity<String> response = this.testRestTemplate.exchange(
                this.baseUrlAdminProfiles+"/{profileId}",
                HttpMethod.PATCH,
                requestProfileAdminPatch,
                String.class,
                "IdNotLong"
        );
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode node = this.objectMapper.readTree(response.getBody());
        // In questo caso l'errore NON è un array di dimensione 1
        assertEquals("ID is not valid!" ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }

    @Test
    void testBlockProfileById_Block_DateNotReadable_Then_400() throws Exception {
        Profile profileToBlock = createPublicProfile("marioBros7",1L);

        ProfileAdminPatch profileAdminPatch = new ProfileAdminPatch();
        String profileAdminPatchJson = this.objectMapper.writeValueAsString(profileAdminPatch);
        JsonNode jsonNode = this.objectMapper.readTree(profileAdminPatchJson);
        ((ObjectNode) jsonNode).put("blocked_until", "NotValidDate");
        profileAdminPatchJson = this.objectMapper.writeValueAsString(jsonNode);

        // Dato che invio direttamente il json del profile, devo impostare il contentType application/json
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> requestProfileAdminPatch = new HttpEntity<>(profileAdminPatchJson, headers);
        ResponseEntity<String> response = this.testRestTemplate.exchange(
                this.baseUrlAdminProfiles+"/{profileId}",
                HttpMethod.PATCH,
                requestProfileAdminPatch,
                String.class,
                profileToBlock.getId()
        );
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode node = this.objectMapper.readTree(response.getBody());
        // In questo caso l'errore NON è un array di dimensione 1
        assertEquals("JSON parse error: Cannot deserialize value of type `java.time.OffsetDateTime` from String \"NotValidDate\": Failed to deserialize java.time.OffsetDateTime: (java.time.format.DateTimeParseException) Text 'NotValidDate' could not be parsed at index 0" ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }

    @Test
    void testBlockProfileById_Block_ProfileNotFound_Then_404() throws Exception {
        ProfileAdminPatch profileAdminPatch = new ProfileAdminPatch();
        profileAdminPatch.setBlockedUntil(OffsetDateTime.of(LocalDateTime.now().plusDays(3), ZoneOffset.UTC));


        HttpEntity<ProfileAdminPatch> requestProfileAdminPatch = new HttpEntity<>(profileAdminPatch);
        ResponseEntity<String> response = this.testRestTemplate.exchange(
                this.baseUrlAdminProfiles+"/{profileId}",
                HttpMethod.PATCH,
                requestProfileAdminPatch,
                String.class,
                Long.MAX_VALUE
        );
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode node = this.objectMapper.readTree(response.getBody());
        // In questo caso l'errore NON è un array di dimensione 1
        assertEquals("Profile "+Long.MAX_VALUE+" not found!" ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }

    //TODO risolvere, usare repository
    /*
    @Test
    void testFindAllProfiles_RemovedTrue_Then_200() throws Exception{
        Profile profile1 = createPublicProfile("pincoPallino1");
        Profile profile2 = createPublicProfile("pincoPallino2");
        Profile profile3 = createPublicProfile("pincoPallino3");
        removeProfile(profile2);

        ResponseEntity<List<Profile>> responseProfileList = this.testRestTemplate.exchange(
                this.baseUrlAdminProfiles+"?removedProfile={removedProfile}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<Profile>>() {
                },
                true);
        assertEquals(HttpStatus.OK, responseProfileList.getStatusCode());
        assertNotNull(responseProfileList.getBody());
        List<Profile> savedProfileList = responseProfileList.getBody();
        assertEquals(asList(profile1, profile2, profile3), savedProfileList);
        log.info(responseProfileList.toString());
    }

    @Test
    void testFindAllProfiles_RemovedFalse_Then_200() throws Exception{
        Profile profile1 = createPublicProfile("pincoPallino121");
        Profile profile2 = createPublicProfile("pincoPallino221");
        Profile profile3 = createPublicProfile("pincoPallino312");
        removeProfile(profile2);

        ResponseEntity<List<Profile>> responseProfileList = this.testRestTemplate.exchange(
                this.baseUrlAdminProfiles+"?removedProfile={removedProfile}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<Profile>>() {
                },
                false);
        assertEquals(HttpStatus.OK, responseProfileList.getStatusCode());
        assertNotNull(responseProfileList.getBody());
        List<Profile> savedProfileList = responseProfileList.getBody();
        assertEquals(asList(profile1, profile3), savedProfileList);
        log.info(responseProfileList.toString());
    }

     */

    @Test
    void testFindAllProfiles_Then_400() throws Exception{
        ResponseEntity<String> response = this.testRestTemplate.exchange(
                this.baseUrlAdminProfiles+"?removedProfile={removedProfile}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                String.class,
                "NotBoolean");
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode node = this.objectMapper.readTree(response.getBody());
        // In questo caso l'errore NON è un array di dimensione 1
        assertEquals("Failed to convert value of type 'java.lang.String' to required type 'java.lang.Boolean'; Invalid boolean value [NotBoolean]" ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }

    @Test
    void testFindAlertById_Then_200() throws Exception {
        Alert savedAlert = createProfilePostAlert(this.alertOwnerProfileName+"1", this.postOwnerProfileName+"1");

        ResponseEntity<Alert> responseAlert = this.testRestTemplate.exchange(
                this.baseUrlAdminAlerts+"/{alertId}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                Alert.class,
                savedAlert.getId());
        assertEquals(HttpStatus.OK, responseAlert.getStatusCode());
        assertNotNull(responseAlert.getBody());
        Alert returnedAlert = responseAlert.getBody();
        log.info(returnedAlert.toString());
        assertEquals(savedAlert, returnedAlert);
    }

    @Test
    void testFindAlertById_IdNotLong_Then_400() throws Exception {
        ResponseEntity<String> response = this.testRestTemplate.exchange(
                this.baseUrlAdminAlerts+"/{alertId}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                String.class,
                "IdNotLong"
        );
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode node = this.objectMapper.readTree(response.getBody());
        // In questo caso l'errore NON è un array di dimensione 1
        assertEquals("ID is not valid!" ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }

    @Test
    void testFindAlertById_NotFound_Then_404() throws Exception {
        ResponseEntity<String> response = this.testRestTemplate.exchange(
                this.baseUrlAdminAlerts+"/{alertId}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                String.class,
                Long.MAX_VALUE
        );
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode node = this.objectMapper.readTree(response.getBody());
        // In questo caso l'errore NON è un array di dimensione 1
        assertEquals("Alert "+Long.MAX_VALUE+" not found!" ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }

    @Test
    void testUpdateAlertById_Then_200() throws Exception {
        Alert savedAlert = createProfilePostAlert(this.alertOwnerProfileName+"2", this.postOwnerProfileName+"2");
        AlertPatch alertPatch = new AlertPatch(this.managedByAdminId);

        HttpEntity<AlertPatch> request = new HttpEntity<>(alertPatch);
        ResponseEntity<Alert> responseAlert = this.testRestTemplate.exchange(
                this.baseUrlAdminAlerts+"/{alertId}",
                HttpMethod.PATCH,
                request,
                Alert.class,
                savedAlert.getId());
        assertEquals(HttpStatus.OK, responseAlert.getStatusCode());
        assertNotNull(responseAlert.getBody());
        Alert updatedAlert = responseAlert.getBody();
        log.info(updatedAlert.toString());
        // imposto il managedBy in savedAlert solo nper confronto
        savedAlert.setManagedBy(this.managedByAdminId);
        assertEquals(savedAlert, updatedAlert);
    }

    @Test
    void testUpdateAlertById_IdNotLong_Then_400() throws Exception {
        AlertPatch alertPatch = new AlertPatch(this.managedByAdminId);

        HttpEntity<AlertPatch> request = new HttpEntity<>(alertPatch);
        ResponseEntity<String> response = this.testRestTemplate.exchange(
                this.baseUrlAdminAlerts+"/{alertId}",
                HttpMethod.PATCH,
                request,
                String.class,
                "IdNotLong");
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode node = this.objectMapper.readTree(response.getBody());
        // In questo caso l'errore NON è un array di dimensione 1
        assertEquals("ID is not valid!" ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }

    @Test
    void testUpdateAlertById_ManagedByNotLong_Then_400() throws Exception {
        Alert savedAlert = createProfilePostAlert(this.alertOwnerProfileName+"3", this.postOwnerProfileName+"3");

        String alertPatchJson = this.objectMapper.writeValueAsString(new AlertPatch(this.managedByAdminId));
        JsonNode jsonNode = this.objectMapper.readTree(alertPatchJson);
        ((ObjectNode) jsonNode).put("managed_by", "IdNotLong");
        alertPatchJson = this.objectMapper.writeValueAsString(jsonNode);

        // Dato che invio direttamente il json del profile, devo impostare il contentType application/json
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> request = new HttpEntity<>(alertPatchJson, headers);
        ResponseEntity<String> response = this.testRestTemplate.exchange(
                this.baseUrlAdminAlerts+"/{alertId}",
                HttpMethod.PATCH,
                request,
                String.class,
                savedAlert.getId());
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode node = this.objectMapper.readTree(response.getBody());
        // In questo caso l'errore NON è un array di dimensione 1
        assertEquals("JSON parse error: Cannot deserialize value of type `java.lang.Long` from String \"IdNotLong\": not a valid `java.lang.Long` value" ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }

    @Test
    void testUpdateAlertById_AlertNotFound_Then_404() throws Exception {
        AlertPatch alertPatch = new AlertPatch(this.managedByAdminId);

        HttpEntity<AlertPatch> request = new HttpEntity<>(alertPatch);
        ResponseEntity<String> response = this.testRestTemplate.exchange(
                this.baseUrlAdminAlerts+"/{alertId}",
                HttpMethod.PATCH,
                request,
                String.class,
                Long.MAX_VALUE);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode node = this.objectMapper.readTree(response.getBody());
        // In questo caso l'errore NON è un array di dimensione 1
        assertEquals("Alert "+Long.MAX_VALUE+" not found!" ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }

    private Alert createProfilePostAlert(String alertOwnerProfileName, String postOwnerProfileName) {
        // Profilo di chi segnalera' un post
        Profile alertOwnerProfile = createPublicProfile(alertOwnerProfileName, 1L);
        // Profilo di chi possiede il post che verra' segnalato
        Profile postOwnerProfile = createPublicProfile(postOwnerProfileName, 2L);
        // Post da segnalare
        Post postToAlert = createPost(postOwnerProfile.getId());

        Alert newAlert = new Alert(alertOwnerProfile.getId(), alertReason);
        newAlert.setPostId(postToAlert.getId());

        return createAlert(newAlert, true);
    }

    Alert createAlert(Alert newAlert, Boolean isPost) {
        HttpEntity<Alert> requestAlert = new HttpEntity<>(newAlert);
        ResponseEntity<Alert> responseAlert = this.testRestTemplate.exchange(
                this.baseUrlAlerts+"?isPost={isPost}",
                HttpMethod.POST,
                requestAlert,
                Alert.class,
                isPost);
        assertEquals(HttpStatus.CREATED, responseAlert.getStatusCode());
        assertNotNull(responseAlert.getBody());
        Alert savedAlert = responseAlert.getBody();
        log.info(savedAlert.toString());
        assertNotNull(savedAlert.getId());
        assertEquals(newAlert.getCreatedBy(), savedAlert.getCreatedBy());
        assertEquals(newAlert.getPostId(), savedAlert.getPostId());
        assertNull(savedAlert.getCommentId());
        assertEquals(newAlert.getReason(), savedAlert.getReason());

        return savedAlert;
    }


    Post createPost(Long profileId) {
        Post newPost = new Post(this.contentUrl, Post.PostTypeEnum.POST, profileId);

        HttpEntity<Post> request = new HttpEntity<>(newPost);
        ResponseEntity<Post> response = this.testRestTemplate.exchange(
                this.baseUrlPost,
                HttpMethod.POST,
                request,
                Post.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        Post savedPost = response.getBody();
        assertNotNull(savedPost);
        assertNotNull(savedPost.getId());
        assertEquals(newPost.getContentUrl(), savedPost.getContentUrl());
        assertEquals(newPost.getPostType(), savedPost.getPostType());
        assertEquals(newPost.getProfileId(), savedPost.getProfileId());

        // visualizzo il post salvato
        log.info(savedPost.toString());
        return savedPost;
    }

    private Profile blockProfile(Profile profileToBlock, Boolean isBlock, Integer days) {
        ProfileAdminPatch profileAdminPatch = new ProfileAdminPatch();
        if(isBlock && days != null){
            profileAdminPatch.setBlockedUntil(OffsetDateTime.of(
                    LocalDateTime.now().plusDays(days),
                    ZoneOffset.UTC
            ));
        }

        HttpEntity<ProfileAdminPatch> requestProfileAdminPatch = new HttpEntity<>(profileAdminPatch);
        ResponseEntity<Profile> responseProfile = this.testRestTemplate.exchange(
                this.baseUrlAdminProfiles+"/{profileId}",
                HttpMethod.PATCH,
                requestProfileAdminPatch,
                Profile.class,
                profileToBlock.getId()
        );
        assertEquals(HttpStatus.OK, responseProfile.getStatusCode());
        assertNotNull(responseProfile.getBody());
        Profile updatedProfile = responseProfile.getBody();
        assertEquals(profileAdminPatch.getBlockedUntil(), updatedProfile.getBlockedUntil());
        return updatedProfile;
    }

    private void removeProfile(Profile profileToBlock) {
        ResponseEntity<Void> responseDelete = this.testRestTemplate.exchange(this.baseUrlProfile+"/{profileId}",
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                Void.class,
                profileToBlock.getId());

        assertEquals(HttpStatus.NO_CONTENT, responseDelete.getStatusCode());
        assertNull(responseDelete.getBody());
    }



    Profile createPublicProfile(String profileName, Long accountId){
        Profile newProfile = new Profile(profileName,false,accountId);

        HttpEntity<Profile> requestProfile = new HttpEntity<>(newProfile);
        ResponseEntity<Profile> responseProfile = this.testRestTemplate.exchange(
                this.baseUrlProfile,
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
