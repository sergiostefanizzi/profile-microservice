package com.sergiostefanizzi.profilemicroservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sergiostefanizzi.profilemicroservice.model.*;
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
public class AlertsIT {
    @LocalServerPort
    private int port;
    private String baseUrl = "http://localhost";
    private String baseUrlProfile = "http://localhost";
    private String baseUrlPost = "http://localhost";
    private String baseUrlComment = "http://localhost";
    @Autowired
    private TestRestTemplate testRestTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    String alertOwnerProfileName = "pinco_pallino";
    String postOwnerProfileName = "giuseppe_verdi";
    String contentUrl = "https://upload.wikimedia.org/wikipedia/commons/9/9a/Cape_may.jpg";
    String alertReason = "Motivo della segnalazione";
    String content = "Commento al post";

    @BeforeEach
    void setUp() {
        this.baseUrlProfile += ":" + port + "/profiles";
        this.baseUrlPost += ":" + port + "/posts";
        this.baseUrlComment += ":" + port + "/posts/comments";
        this.baseUrl += ":" + port + "/alerts";
    }

    @AfterEach
    void tearDown() {

    }

    @Test
    void testCreateAlert_PostAlert_Then_201(){
        // Profilo di chi segnalera' un post
        Profile alertOwnerProfile = createPublicProfile(alertOwnerProfileName);
        // Profilo di chi possiede il post che verra' segnalato
        Profile postOwnerProfile = createPublicProfile(postOwnerProfileName);
        // Post da segnalare
        Post postToAlert = createPost(postOwnerProfile.getId());

        Alert newAlert = new Alert(alertOwnerProfile.getId(), alertReason);
        newAlert.setPostId(postToAlert.getId());

        Alert savedAlert = createAlert(newAlert, true);

        log.info(savedAlert.toString());
        assertNotNull(savedAlert.getId());
        assertEquals(newAlert.getCreatedBy(), savedAlert.getCreatedBy());
        assertEquals(newAlert.getPostId(), savedAlert.getPostId());
        assertNull(savedAlert.getCommentId());
        assertEquals(newAlert.getReason(), savedAlert.getReason());
    }

    @Test
    void testCreateAlert_CommentAlert_Then_201(){
        // Profilo di chi segnalera' un post
        Profile alertOwnerProfile = createPublicProfile(alertOwnerProfileName+"_1");
        // Profilo di chi possiede il post che verra' segnalato
        Profile postOwnerProfile = createPublicProfile(postOwnerProfileName+"_1");
        // Post del commento da segnalare
        Post postToAlert = createPost(postOwnerProfile.getId());
        // Commento del owner del post che verra' segnalato
        Comment commentToAlert = createComment(postOwnerProfile.getId(), postToAlert.getId());

        Alert newAlert = new Alert(alertOwnerProfile.getId(), alertReason);
        newAlert.setCommentId(commentToAlert.getId());

        Alert savedAlert = createAlert(newAlert, false);

        assertNotNull(savedAlert.getId());
        assertEquals(newAlert.getCreatedBy(), savedAlert.getCreatedBy());
        assertEquals(newAlert.getCommentId(), savedAlert.getCommentId());
        assertNull(savedAlert.getPostId());
        assertEquals(newAlert.getReason(), savedAlert.getReason());
    }

    @Test
    void testCreateAlert_PostAlert_AlertTypeErrorException_isPost_False_Then_400() throws JsonProcessingException {
        // Profilo di chi segnalera' un post
        Profile alertOwnerProfile = createPublicProfile(alertOwnerProfileName+"_2");
        // Profilo di chi possiede il post che verra' segnalato
        Profile postOwnerProfile = createPublicProfile(postOwnerProfileName+"_2");
        // Post da segnalare
        Post postToAlert = createPost(postOwnerProfile.getId());

        Alert newAlert = new Alert(alertOwnerProfile.getId(), alertReason);
        newAlert.setPostId(postToAlert.getId());

        HttpEntity<Alert> requestAlert = new HttpEntity<>(newAlert);
        ResponseEntity<String> responseAlert = this.testRestTemplate.exchange(
                this.baseUrl+"?isPost={isPost}",
                HttpMethod.POST,
                requestAlert,
                String.class,
                false);
        assertEquals(HttpStatus.BAD_REQUEST, responseAlert.getStatusCode());
        assertNotNull(responseAlert.getBody());
        String alertError = responseAlert.getBody();

        JsonNode node = this.objectMapper.readTree(alertError);
        // In questo caso l'errore NON è un array di dimensione 1
        assertEquals("Alert type error" ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }

    @Test
    void testCreateAlert_CommentAlert_AlertTypeErrorException_isPost_False_Then_400() throws JsonProcessingException {
        // Profilo di chi segnalera' un post
        Profile alertOwnerProfile = createPublicProfile(alertOwnerProfileName+"_3");
        // Profilo di chi possiede il post che verra' segnalato
        Profile postOwnerProfile = createPublicProfile(postOwnerProfileName+"_3");
        // Post da segnalare
        Post postToAlert = createPost(postOwnerProfile.getId());
        // Commento del owner del post che verra' segnalato
        Comment commentToAlert = createComment(postOwnerProfile.getId(), postToAlert.getId());

        Alert newAlert = new Alert(alertOwnerProfile.getId(), alertReason);
        newAlert.setCommentId(commentToAlert.getId());

        HttpEntity<Alert> requestAlert = new HttpEntity<>(newAlert);
        ResponseEntity<String> responseAlert = this.testRestTemplate.exchange(
                this.baseUrl+"?isPost={isPost}",
                HttpMethod.POST,
                requestAlert,
                String.class,
                true);
        assertEquals(HttpStatus.BAD_REQUEST, responseAlert.getStatusCode());
        assertNotNull(responseAlert.getBody());
        String alertError = responseAlert.getBody();

        JsonNode node = this.objectMapper.readTree(alertError);
        // In questo caso l'errore NON è un array di dimensione 1
        assertEquals("Alert type error" ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }

    @Test
    void testCreateAlert_PostAlert_MethodArgumentTypeMismatchException_isPost_Missing_Then_400() throws JsonProcessingException {
        // Profilo di chi segnalera' un post
        Profile alertOwnerProfile = createPublicProfile(alertOwnerProfileName+"_4");
        // Profilo di chi possiede il post che verra' segnalato
        Profile postOwnerProfile = createPublicProfile(postOwnerProfileName+"_4");
        // Post da segnalare
        Post postToAlert = createPost(postOwnerProfile.getId());

        Alert newAlert = new Alert(alertOwnerProfile.getId(), alertReason);
        newAlert.setPostId(postToAlert.getId());

        HttpEntity<Alert> requestAlert = new HttpEntity<>(newAlert);
        ResponseEntity<String> responseAlert = this.testRestTemplate.exchange(
                this.baseUrl+"?isPost={isPost}",
                HttpMethod.POST,
                requestAlert,
                String.class,
                "NotBoolean");
        assertEquals(HttpStatus.BAD_REQUEST, responseAlert.getStatusCode());
        assertNotNull(responseAlert.getBody());
        String alertError = responseAlert.getBody();

        JsonNode node = this.objectMapper.readTree(alertError);
        // In questo caso l'errore NON è un array di dimensione 1
        assertEquals("Failed to convert value of type 'java.lang.String' to required type 'java.lang.Boolean'; Invalid boolean value [NotBoolean]" ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }

    @Test
    void testCreateAlert_PostAlert_AlertTypeNotSpecifiedException_isPost_Missing_Then_400() throws JsonProcessingException {
        // Profilo di chi segnalera' un post
        Profile alertOwnerProfile = createPublicProfile(alertOwnerProfileName+"_5");
        // Profilo di chi possiede il post che verra' segnalato
        Profile postOwnerProfile = createPublicProfile(postOwnerProfileName+"_5");
        // Post da segnalare
        Post postToAlert = createPost(postOwnerProfile.getId());

        Alert newAlert = new Alert(alertOwnerProfile.getId(), alertReason);
        newAlert.setPostId(postToAlert.getId());

        HttpEntity<Alert> requestAlert = new HttpEntity<>(newAlert);
        ResponseEntity<String> responseAlert = this.testRestTemplate.exchange(
                this.baseUrl,
                HttpMethod.POST,
                requestAlert,
                String.class);
        assertEquals(HttpStatus.BAD_REQUEST, responseAlert.getStatusCode());
        assertNotNull(responseAlert.getBody());
        String alertError = responseAlert.getBody();

        JsonNode node = this.objectMapper.readTree(alertError);
        // In questo caso l'errore NON è un array di dimensione 1
        assertEquals("Required request parameter 'isPost' for method parameter type Boolean is not present" ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }

    @Test
    void testCreateAlert_PostAlert_ProfileNotFoundException_Then_404() throws Exception {
        // Profilo di chi possiede il post che verra' segnalato
        Profile postOwnerProfile = createPublicProfile(postOwnerProfileName+"_6");
        // Post da segnalare
        Post postToAlert = createPost(postOwnerProfile.getId());

        Alert newAlert = new Alert(Long.MAX_VALUE, alertReason);
        newAlert.setPostId(postToAlert.getId());

        HttpEntity<Alert> requestAlert = new HttpEntity<>(newAlert);
        ResponseEntity<String> responseAlert = this.testRestTemplate.exchange(
                this.baseUrl+"?isPost={isPost}",
                HttpMethod.POST,
                requestAlert,
                String.class,
                true);
        assertEquals(HttpStatus.NOT_FOUND, responseAlert.getStatusCode());
        assertNotNull(responseAlert.getBody());
        String alertError = responseAlert.getBody();

        JsonNode node = this.objectMapper.readTree(alertError);
        // In questo caso l'errore NON è un array di dimensione 1
        assertEquals("Profile "+Long.MAX_VALUE+" not found!" ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }

    @Test
    void testCreateAlert_PostAlert_PostNotFoundException_Then_404() throws Exception {
        // Profilo di chi possiede il post che verra' segnalato
        Profile postOwnerProfile = createPublicProfile(postOwnerProfileName+"_7");

        Alert newAlert = new Alert(postOwnerProfile.getId(), alertReason);
        newAlert.setPostId(Long.MAX_VALUE);

        HttpEntity<Alert> requestAlert = new HttpEntity<>(newAlert);
        ResponseEntity<String> responseAlert = this.testRestTemplate.exchange(
                this.baseUrl+"?isPost={isPost}",
                HttpMethod.POST,
                requestAlert,
                String.class,
                true);
        assertEquals(HttpStatus.NOT_FOUND, responseAlert.getStatusCode());
        assertNotNull(responseAlert.getBody());
        String alertError = responseAlert.getBody();

        JsonNode node = this.objectMapper.readTree(alertError);
        // In questo caso l'errore NON è un array di dimensione 1
        assertEquals("Post "+Long.MAX_VALUE+" not found!" ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }

    @Test
    void testCreateAlert_CommentAlert_PostNotFoundException_Then_404() throws Exception {
        // Profilo di chi possiede il post che verra' segnalato
        Profile postOwnerProfile = createPublicProfile(postOwnerProfileName+"_8");

        Alert newAlert = new Alert(postOwnerProfile.getId(), alertReason);
        newAlert.setCommentId(Long.MAX_VALUE);

        HttpEntity<Alert> requestAlert = new HttpEntity<>(newAlert);
        ResponseEntity<String> responseAlert = this.testRestTemplate.exchange(
                this.baseUrl+"?isPost={isPost}",
                HttpMethod.POST,
                requestAlert,
                String.class,
                false);
        assertEquals(HttpStatus.NOT_FOUND, responseAlert.getStatusCode());
        assertNotNull(responseAlert.getBody());
        String alertError = responseAlert.getBody();

        JsonNode node = this.objectMapper.readTree(alertError);
        // In questo caso l'errore NON è un array di dimensione 1
        assertEquals("Comment "+Long.MAX_VALUE+" not found!" ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }

    private Alert createAlert(Alert newAlert, Boolean isPost) {
        HttpEntity<Alert> requestAlert = new HttpEntity<>(newAlert);
        ResponseEntity<Alert> responseAlert = this.testRestTemplate.exchange(
                this.baseUrl+"?isPost={isPost}",
                HttpMethod.POST,
                requestAlert,
                Alert.class,
                isPost);
        assertEquals(HttpStatus.CREATED, responseAlert.getStatusCode());
        assertNotNull(responseAlert.getBody());
        Alert savedAlert = responseAlert.getBody();
        log.info(savedAlert.toString());
        return savedAlert;
    }

    private String createAlertError(Alert newAlert, Boolean isPost) {
        HttpEntity<Alert> requestAlert = new HttpEntity<>(newAlert);
        ResponseEntity<String> responseAlert = this.testRestTemplate.exchange(
                this.baseUrl+"?isPost={isPost}",
                HttpMethod.POST,
                requestAlert,
                String.class,
                isPost);
        assertEquals(HttpStatus.BAD_REQUEST, responseAlert.getStatusCode());
        assertNotNull(responseAlert.getBody());
        String alertError = responseAlert.getBody();
        log.info(alertError);
        return alertError;
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

    Post createPost(Long profileId) {
        Post newPost = new Post(contentUrl, Post.PostTypeEnum.POST, profileId);

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

    Comment createComment(Long profileId, Long postId) {
        Comment newComment = new Comment(
                profileId,
                postId,
                content
        );
        HttpEntity<Comment> requestComment = new HttpEntity<>(newComment);
        ResponseEntity<Comment> responseComment = this.testRestTemplate.exchange(this.baseUrlComment,
                HttpMethod.POST,
                requestComment,
                Comment.class);

        assertEquals(HttpStatus.CREATED, responseComment.getStatusCode());
        assertNotNull(responseComment.getBody());
        Comment savedComment = responseComment.getBody();
        assertNotNull(savedComment.getId());
        assertEquals(profileId, savedComment.getProfileId());
        assertEquals(postId, savedComment.getPostId());
        assertEquals(content, savedComment.getContent());

        // visualizzo il commento salvato
        log.info(savedComment.toString());
        return savedComment;
    }

}
