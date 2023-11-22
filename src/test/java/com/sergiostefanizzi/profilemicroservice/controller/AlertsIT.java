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
    /*
    @LocalServerPort
    private int port;
    private String baseUrl = "http://localhost";

    @Autowired
    private TestRestTemplate testRestTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    private Profile alertSender;
    private Post postToAlert;
    private Comment commentToAlert;

    String contentUrl = "https://upload.wikimedia.org/wikipedia/commons/9/9a/Cape_may.jpg";
    String alertReason = "Motivo della segnalazione";

    String pictureUrl = "https://upload.wikimedia.org/wikipedia/commons/7/7e/Circle-icons-profile.svg";
    String caption = "This is the caption";
    Post.PostTypeEnum postType = Post.PostTypeEnum.POST;

    @BeforeEach
    void setUp() {
        this.baseUrl += ":" + port + "/alerts";

        this.alertSender = new Profile("pinco_pallino", false, 101L);
        this.alertSender.setId(101L);
        this.alertSender.setBio("Profilo di Pinco");
        this.alertSender.setPictureUrl(pictureUrl);

        Profile postOwner = new Profile("matt_murdock", false, 105L);
        postOwner.setId(106L);
        postOwner.setBio("Profilo di Murdock");
        postOwner.setPictureUrl(pictureUrl);

        this.postToAlert = new Post(contentUrl, postType, postOwner.getId());
        this.postToAlert.setCaption(caption);
        this.postToAlert.setId(116L);

        this.commentToAlert = new Comment(
                postOwner.getId(),
                this.postToAlert.getId(),
                "Commento al post"
        );
        commentToAlert.setId(101L);
    }

    @AfterEach
    void tearDown() {

    }

    @Test
    void testCreateAlert_PostAlert_Then_201(){
        Alert newAlert = new Alert(this.alertSender.getId(), alertReason);
        newAlert.setPostId(this.postToAlert.getId());


        HttpEntity<Alert> requestAlert = new HttpEntity<>(newAlert);
        ResponseEntity<Alert> responseAlert = this.testRestTemplate.exchange(
                this.baseUrl+"?isPost={isPost}",
                HttpMethod.POST,
                requestAlert,
                Alert.class,
                true);
        assertEquals(HttpStatus.CREATED, responseAlert.getStatusCode());
        assertNotNull(responseAlert.getBody());
        Alert savedAlert = responseAlert.getBody();
        assertNotNull(savedAlert.getId());
        newAlert.setId(savedAlert.getId());
        assertEquals(newAlert, savedAlert);

        log.info(savedAlert.toString());
    }

    @Test
    void testCreateAlert_CommentAlert_Then_201(){

        Alert newAlert = new Alert(this.alertSender.getId(), alertReason);
        newAlert.setCommentId(this.commentToAlert.getId());

        HttpEntity<Alert> requestAlert = new HttpEntity<>(newAlert);
        ResponseEntity<Alert> responseAlert = this.testRestTemplate.exchange(
                this.baseUrl+"?isPost={isPost}",
                HttpMethod.POST,
                requestAlert,
                Alert.class,
                false);
        assertEquals(HttpStatus.CREATED, responseAlert.getStatusCode());
        assertNotNull(responseAlert.getBody());
        Alert savedAlert = responseAlert.getBody();
        assertNotNull(savedAlert.getId());
        newAlert.setId(savedAlert.getId());
        assertEquals(newAlert, savedAlert);

        log.info(savedAlert.toString());
    }

    @Test
    void testCreateAlert_PostAlert_AlertTypeErrorException_isPost_False_Then_400() throws JsonProcessingException {
        Alert newAlert = new Alert(this.alertSender.getId(), alertReason);
        newAlert.setPostId(this.postToAlert.getId());

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

        Alert newAlert = new Alert(this.alertSender.getId(), alertReason);
        newAlert.setCommentId(this.commentToAlert.getId());

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
        Alert newAlert = new Alert(this.alertSender.getId(), alertReason);
        newAlert.setPostId(this.postToAlert.getId());

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
        Alert newAlert = new Alert(this.alertSender.getId(), alertReason);
        newAlert.setPostId(this.postToAlert.getId());

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

        Alert newAlert = new Alert(Long.MAX_VALUE, alertReason);
        newAlert.setPostId(this.postToAlert.getId());

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

        Alert newAlert = new Alert(this.alertSender.getId(), alertReason);
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
        Alert newAlert = new Alert(this.alertSender.getId(), alertReason);
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



     */

}
