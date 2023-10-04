package com.sergiostefanizzi.profilemicroservice.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sergiostefanizzi.profilemicroservice.model.Profile;
import com.sergiostefanizzi.profilemicroservice.model.ProfileAdminPatch;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.time.*;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Slf4j
public class AdminsIT {

    @LocalServerPort
    private int port;
    private String baseUrl = "http://localhost";
    private String baseUrlAdminProfiles;
    private String baseUrlProfile;
    private String baseUrlPost;
    private String baseUrlComment;
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
    }

    @AfterEach
    void tearDown() {

    }

    @Test
    void testBlockProfileById_Block_Then_200(){
        Profile profileToBlock = createPublicProfile("marioBros");
        Profile updatedProfile = blockProfile( profileToBlock, true, 3);
        log.info("Profile ID: "+profileToBlock.getId()+" blocked until "+updatedProfile.getBlockedUntil());
    }

    @Test
    void testBlockProfileById_Unblock_Then_200(){
        Profile profileToBlock = createPublicProfile("marioBros2");
        Profile updatedProfile = blockProfile(profileToBlock, false, null);
        assertNull(updatedProfile.getBlockedUntil());
        log.info("Profile ID: "+profileToBlock.getId()+" unblocked");
    }

    @Test
    void testBlockProfileById_Extends_Block_Then_200(){
        Profile profileToBlock = createPublicProfile("marioBros3");
        Profile updatedProfile = blockProfile( profileToBlock, true, 3);
        log.info("Profile ID: "+profileToBlock.getId()+" blocked until "+updatedProfile.getBlockedUntil());
        updatedProfile = blockProfile( profileToBlock, true, 4);
        log.info("Profile ID: "+profileToBlock.getId()+" blocked until "+updatedProfile.getBlockedUntil());
    }

    @Test
    void testBlockProfileById_Unblock_anotherTime_Then_200(){
        Profile profileToBlock = createPublicProfile("marioBros4");
        Profile updatedProfile = blockProfile(profileToBlock, false, null);
        assertNull(updatedProfile.getBlockedUntil());
        log.info("Profile ID: "+profileToBlock.getId()+" unblocked");

        updatedProfile = blockProfile(profileToBlock, false, null);
        assertNull(updatedProfile.getBlockedUntil());
        log.info("Profile ID: "+profileToBlock.getId()+" unblocked");
    }

    @Test
    void testBlockProfileById_Block_PastDate_Then_400() throws Exception {
        Profile profileToBlock = createPublicProfile("marioBros6");

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
        Profile profileToBlock = createPublicProfile("marioBros7");

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



    Profile createPublicProfile(String profileName){
        Profile newProfile = new Profile(profileName,false,1L);

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
