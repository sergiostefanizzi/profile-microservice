package com.sergiostefanizzi.profilemicroservice.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sergiostefanizzi.profilemicroservice.model.*;
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

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Slf4j
class PostsIT {
    @LocalServerPort
    private int port;

    private String baseUrl = "http://localhost";
    private String baseUrlProfile;

    @Autowired
    private TestRestTemplate testRestTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ProfilesRepository profilesRepository;
    @Autowired
    private PostsRepository postsRepository;
    String contentUrl = "https://upload.wikimedia.org/wikipedia/commons/9/9a/Cape_may.jpg";
    String contentUrlXSS = "http://www.example.com?d=<script type=\"javascript\" src=\"http://www.google.it\"/>"; //Cross site scripting XSS
    String caption = "This is the post caption";
    Post.PostTypeEnum postType = Post.PostTypeEnum.POST;
    Long postId = 1L;
    Long profileId;
    Integer count = 1;
    private Post newPost;
    String profileName = "pinco_pallino";
    Profile newProfile = new Profile(profileName,false,111L);
    List<String> errors;


    @BeforeEach
    void setUp() {
        this.baseUrlProfile = this.baseUrl + ":" + port + "/profiles";
        this.baseUrl += ":" + port + "/posts";

        // imposto il profileId a 0L per i test dove vengono controllati
        // altri campi diversi da questo che deve comunque essere non null
        this.newPost = new Post(contentUrl, postType, Long.MIN_VALUE);
        this.newPost.setCaption(caption);


        errors = new ArrayList<>();
    }

    @AfterEach
    void tearDown() {
        errors.clear();
        newProfile.setProfileName(profileName);
    }

    @Test
    void testAddPost_Then_201() {
        // Creo prima un profilo
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
        profileId = savedProfile.getId();
        this.newPost.setProfileId(profileId);

        HttpEntity<Post> request = new HttpEntity<>(this.newPost);
        ResponseEntity<Post> response = this.testRestTemplate.exchange(
                this.baseUrl,
                HttpMethod.POST,
                request,
                Post.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        Post savedPost = response.getBody();
        assertNotNull(savedPost.getId());
        assertEquals(contentUrl, savedPost.getContentUrl());
        assertEquals(caption, savedPost.getCaption());
        assertEquals(postType, savedPost.getPostType());
        assertEquals(profileId, savedPost.getProfileId());

        // visualizzo il post salvato
        log.info(savedPost.toString());
    }

    @Test
    void testAddPost_RequiredField_Then_201() throws Exception {
        // Creo prima un profilo
        newProfile.setProfileName(profileName+"_1");
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
        profileId = savedProfile.getId();
        this.newPost.setProfileId(profileId);

        // Imposto la caption a null
        this.newPost.setCaption(null);

        HttpEntity<Post> request = new HttpEntity<>(this.newPost);
        ResponseEntity<Post> response = this.testRestTemplate.exchange(
                this.baseUrl,
                HttpMethod.POST,
                request,
                Post.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        Post savedPost = response.getBody();
        assertNotNull(savedPost.getId());
        assertEquals(contentUrl, savedPost.getContentUrl());
        assertNull(savedPost.getCaption());
        assertEquals(postType, savedPost.getPostType());
        assertEquals(profileId, savedPost.getProfileId());

        // visualizzo il post salvato
        log.info(savedPost.toString());
    }

    @Test
    void testAddPost_MissingRequired_Then_400() throws Exception{
        // arrayList contenente i messaggi di errore
        errors.add("contentUrl must not be null");
        errors.add("postType must not be null");
        errors.add("profileId must not be null");

        // Imposto a null tutti i campi richiesti
        this.newPost.setContentUrl(null);
        this.newPost.setPostType(null);
        this.newPost.setProfileId(null);

        HttpEntity<Post> request = new HttpEntity<>(this.newPost);
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
    void testAddPost_CaptionLength_Then_400() throws Exception {
        // arrayList contenente i messaggi di errore
        errors.add("caption size must be between 0 and 2200");

        // genero una caption di 2210 caratteri, superando di 10 il limite
        this.newPost.setCaption(RandomStringUtils.randomAlphabetic(2210));

        HttpEntity<Post> request = new HttpEntity<>(this.newPost);
        ResponseEntity<String> response = this.testRestTemplate.exchange(this.baseUrl,
                HttpMethod.POST,
                request,
                String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode node = this.objectMapper.readTree(response.getBody());
        // itero gli errori ottenuti dalla risposta per confrontarli con quelli che mi aspetto di ottenere
        // Anche se è solo un errore, ottengo sempre un array di dimensione 1
        assertTrue(node.get("error").isArray());
        assertEquals(1, node.get("error").size());
        assertEquals(errors.get(0) ,node.get("error").get(0).asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error").get(0));
    }

    @Test
    void testAddPost_InvalidContentUrl_Then_400() throws Exception {
        errors.add("contentUrl must be a valid URL");
        errors.add("contentUrl size must be between 3 and 2048");
        // Url non valido
        this.newPost.setContentUrl("https://upload.wikimedia.o/ ra-%%$^&& iuyi"+RandomStringUtils.randomAlphabetic(2048));
        //Test XSS
        //this.newPost.setContentUrl(contentUrlXSS);
        HttpEntity<Post> request = new HttpEntity<>(this.newPost);
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
    void testAddPost_InvalidPostType_Then_400() throws Exception{
        errors.add("JSON parse error: Cannot construct instance of `com.sergiostefanizzi.profilemicroservice.model.Post$PostTypeEnum`, problem: Unexpected value 'NotValidOption'");
        // per impostare una stringa non valida nel campo PostTypeEnum
        // devo convertire il Post in formato json e modificare il campo direttamente
        String newPostJson = this.objectMapper.writeValueAsString(this.newPost);
        JsonNode jsonNode = this.objectMapper.readTree(newPostJson);
        ((ObjectNode) jsonNode).put("post_type", "NotValidOption");
        newPostJson = this.objectMapper.writeValueAsString(jsonNode);

        // Dato che invio direttamente il json del profile, devo impostare il contentType application/json
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> request = new HttpEntity<>(newPostJson, headers);
        ResponseEntity<String> response = this.testRestTemplate.exchange(this.baseUrl,
                HttpMethod.POST,
                request,
                String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode node = this.objectMapper.readTree(response.getBody());
        // In questo caso l'errore NON è un array di dimensione 1
        assertEquals(errors.get(0) ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }

    @Test
    void testAddPost_InvalidProfileId_Then_400() throws Exception{
        errors.add("JSON parse error: Cannot deserialize value of type `java.lang.Long` from String \"IdNotLong\": not a valid `java.lang.Long` value");
        String newPostJson = this.objectMapper.writeValueAsString(this.newPost);
        JsonNode jsonNode = this.objectMapper.readTree(newPostJson);
        ((ObjectNode) jsonNode).put("profile_id", "IdNotLong");
        newPostJson = this.objectMapper.writeValueAsString(jsonNode);

        // Dato che invio direttamente il json del profile, devo impostare il contentType application/json
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> request = new HttpEntity<>(newPostJson, headers);
        ResponseEntity<String> response = this.testRestTemplate.exchange(this.baseUrl,
                HttpMethod.POST,
                request,
                String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode node = this.objectMapper.readTree(response.getBody());
        // In questo caso l'errore NON è un array di dimensione 1
        assertEquals(errors.get(0) ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }

    //TODO 401, 403

    // Questo dovra' essere sostituito con il 403
    @Test
    void testAddPost_Then_404() throws Exception {
        String error = "Profile "+Long.MIN_VALUE+" not found!";

        HttpEntity<Post> request = new HttpEntity<>(this.newPost);
        ResponseEntity<String> response = this.testRestTemplate.exchange(this.baseUrl,
                HttpMethod.POST,
                request,
                String.class);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode node = this.objectMapper.readTree(response.getBody());
        // In questo caso l'errore NON è un array di dimensione 1
        assertEquals(error ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }

    @Test
    void testDeletePostById_Then_204() throws Exception {
        // Creo prima un profilo
        newProfile.setProfileName(profileName+"_2");
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
        profileId = savedProfile.getId();
        this.newPost.setProfileId(profileId);

        // creo un nuovo post
        HttpEntity<Post> request = new HttpEntity<>(this.newPost);
        ResponseEntity<Post> responsePost = this.testRestTemplate.exchange(this.baseUrl,
                HttpMethod.POST,
                request,
                Post.class);
        // controllo che l'inserimento sia andato a buon fine
        assertEquals(HttpStatus.CREATED, responsePost.getStatusCode());
        assertNotNull(responsePost.getBody());
        assertInstanceOf(Post.class, responsePost.getBody());
        Post savedPost = responsePost.getBody();
        assertNotNull(savedPost.getId());
        // salvo l'id generato dal post inserito
        Long savedPostId = savedPost.getId();

        // elimino il profilo appena inserito
        ResponseEntity<Void> responseDelete = this.testRestTemplate.exchange(this.baseUrl+"/{postId}",
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                Void.class,
                savedPostId);

        assertEquals(HttpStatus.NO_CONTENT, responseDelete.getStatusCode());
        assertNull(responseDelete.getBody());

        // verifico dalla repository del post che il profilo sia stato effettivamente eliminato
        // la risorsa e' ancora presente, ma e' eliminata logicamente in quanto il campo
        // deletedAt e' non nullo
        Optional<PostJpa> deletedPostJpa = this.postsRepository.findById(savedPostId);
        if (deletedPostJpa.isPresent()){
            assertNotNull(deletedPostJpa.get().getDeletedAt());
            log.info("Post \n"+savedPostId+" deleted at -> "+deletedPostJpa.get().getDeletedAt());
        }
    }

    @Test
    void testDeletePostById_Then_400() throws Exception {
        // messaggio d'errore che mi aspetto d'ottenere
        errors.add("Failed to convert value of type 'java.lang.String' to required type 'java.lang.Long'; For input string: \"IdNotLong\"");
        ResponseEntity<String> response = this.testRestTemplate.exchange(this.baseUrl+"/IdNotLong",
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode node = this.objectMapper.readTree(response.getBody());
        // In questo caso l'errore NON è un array di dimensione 1
        assertEquals(errors.get(0) ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }

    //TODO remove 401, 403

    @Test
    void testDeletePostById_Then_404() throws Exception {
        Long invalidPostId = Long.MIN_VALUE;
        // messaggio d'errore che mi aspetto d'ottenere
        errors.add("Post "+invalidPostId+" not found!");
        ResponseEntity<String> response = this.testRestTemplate.exchange(this.baseUrl+"/{postId}",
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                String.class,
                invalidPostId);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode node = this.objectMapper.readTree(response.getBody());
        // In questo caso l'errore NON è un array di dimensione 1
        assertEquals(errors.get(0) ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }

    @Test
    void testUpdatePost_Then_200() throws Exception{
        // Creo prima un profilo
        newProfile.setProfileName(profileName+"_3");
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
        profileId = savedProfile.getId();
        this.newPost.setProfileId(profileId);

        // creo un nuovo post
        HttpEntity<Post> request = new HttpEntity<>(this.newPost);
        ResponseEntity<Post> responsePost = this.testRestTemplate.exchange(this.baseUrl,
                HttpMethod.POST,
                request,
                Post.class);
        // controllo che l'inserimento sia andato a buon fine
        assertEquals(HttpStatus.CREATED, responsePost.getStatusCode());
        assertNotNull(responsePost.getBody());
        assertInstanceOf(Post.class, responsePost.getBody());
        Post savedPost = responsePost.getBody();
        assertNotNull(savedPost.getId());
        // salvo l'id generato dal post inserito
        Long savedPostId = savedPost.getId();

        // Definisco la caption da aggiornare tramite l'oggetto PostPatch
        String newCaption = "Nuova caption del post";
        PostPatch postPatch = new PostPatch(newCaption);

        HttpEntity<PostPatch> requestPatch = new HttpEntity<>(postPatch);
        ResponseEntity<Post> responsePatch = this.testRestTemplate.exchange(this.baseUrl+"/{postId}",
                HttpMethod.PATCH,
                requestPatch,
                Post.class,
                savedPostId);

        assertEquals(HttpStatus.OK, responsePatch.getStatusCode());
        assertNotNull(responsePatch.getBody());
        assertInstanceOf(Post.class, responsePatch.getBody());
        Post updatedPost = responsePatch.getBody();
        assertEquals(savedPostId, updatedPost.getId());
        assertEquals(savedPost.getContentUrl(), updatedPost.getContentUrl());
        assertEquals(postPatch.getCaption(), updatedPost.getCaption());
        assertEquals(savedPost.getPostType(), updatedPost.getPostType());
        assertEquals(savedPost.getProfileId(), updatedPost.getProfileId());

        // visualizzo il profilo aggiornato
        log.info(updatedPost.toString());
    }

    @Test
    void testUpdatePost_InvalidId_Then_400() throws Exception{
        errors.add("Failed to convert value of type 'java.lang.String' to required type 'java.lang.Long'; For input string: \"IdNotLong\"");
        // Definisco la caption da aggiornare tramite l'oggetto PostPatch
        String newCaption = "Nuova caption del post";
        PostPatch postPatch = new PostPatch(newCaption);

        HttpEntity<PostPatch> requestPatch = new HttpEntity<>(postPatch);
        ResponseEntity<String> response = this.testRestTemplate.exchange(this.baseUrl+"/{postId}",
                HttpMethod.PATCH,
                requestPatch,
                String.class,
                "IdNotLong");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode node = this.objectMapper.readTree(response.getBody());
        // In questo caso l'errore NON è un array di dimensione 1
        assertEquals(errors.get(0) ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }

    @Test
    void testUpdatePost_CaptionLength_Then_400() throws Exception {
        errors.add("caption size must be between 0 and 2200");

        // genero una caption di 2210 caratteri, superando di 10 il limite
        PostPatch postPatch = new PostPatch(RandomStringUtils.randomAlphabetic(2210));

        HttpEntity<PostPatch> requestPatch = new HttpEntity<>(postPatch);
        ResponseEntity<String> response = this.testRestTemplate.exchange(this.baseUrl+"/{postId}",
                HttpMethod.PATCH,
                requestPatch,
                String.class,
                Long.MIN_VALUE);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode node = this.objectMapper.readTree(response.getBody());
        // In questo caso l'errore e' un array di dimensione 1
        assertTrue(node.get("error").isArray());
        assertEquals(1, node.get("error").size());
        assertEquals(errors.get(0) ,node.get("error").get(0).asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error").get(0));
    }

    //:TODO 401 e 403
    @Test
    void testUpdatePost_Then_404() throws Exception{
        Long invalidPostId = Long.MIN_VALUE;
        errors.add("Post "+invalidPostId+" not found!");
        // Definisco la caption da aggiornare tramite l'oggetto PostPatch
        String newCaption = "Nuova caption del post";
        PostPatch postPatch = new PostPatch(newCaption);

        HttpEntity<PostPatch> requestPatch = new HttpEntity<>(postPatch);
        ResponseEntity<String> response = this.testRestTemplate.exchange(this.baseUrl+"/{postId}",
                HttpMethod.PATCH,
                requestPatch,
                String.class,
                invalidPostId);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode node = this.objectMapper.readTree(response.getBody());
        // In questo caso l'errore NON è un array di dimensione 1
        assertEquals(errors.get(0) ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }

}