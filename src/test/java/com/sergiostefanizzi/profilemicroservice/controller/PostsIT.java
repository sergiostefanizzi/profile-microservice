package com.sergiostefanizzi.profilemicroservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;


import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Slf4j
class PostsIT {
    @LocalServerPort
    private int port;

    private String baseUrl = "http://localhost";
    private String baseUrlProfile;
    private String baseUrlLike;
    private String baseUrlComment;

    @Autowired
    private TestRestTemplate testRestTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ProfilesRepository profilesRepository;
    @Autowired
    private PostsRepository postsRepository;
    private Profile savedProfile1;
    private Profile savedProfile2;
    private Post savedPostToDelete1;
    private Post savedPost1;
    private Post savedPost2;
    private Post savedStory1;
    private Comment savedComment1;
    String pictureUrl = "https://upload.wikimedia.org/wikipedia/commons/7/7e/Circle-icons-profile.svg";
    String contentUrl = "https://upload.wikimedia.org/wikipedia/commons/9/9a/Cape_may.jpg";
    String contentUrlXSS = "http://www.example.com?d=<script type=\"javascript\" src=\"http://www.google.it\"/>"; //Cross site scripting XSS
    String caption = "This is the caption";
    Post.PostTypeEnum postType = Post.PostTypeEnum.POST;
    Post.PostTypeEnum storyType = Post.PostTypeEnum.STORY;
    Long postId = 1L;
    Long profileId;
    Integer count = 1;
    private Post newPost;
    String profileName = "pinco_pallino";
    Profile newProfile = new Profile(profileName,false,111L);



    @BeforeEach
    void setUp() {
        this.baseUrlProfile = this.baseUrl + ":" + port + "/profiles";
        this.baseUrl += ":" + port + "/posts";
        this.baseUrlLike = this.baseUrl + "/likes";
        this.baseUrlComment = this.baseUrl + "/comments";


        this.savedProfile1 = new Profile("pinco_pallino", false, 101L);
        savedProfile1.setId(101L);
        savedProfile1.setBio("Profilo di Pinco");
        savedProfile1.setPictureUrl(pictureUrl);

        this.savedProfile2 = new Profile("matt_murdock", false, 105L);
        savedProfile2.setId(106L);
        savedProfile2.setBio("Profilo di Murdock");
        savedProfile2.setPictureUrl(pictureUrl);

        this.newPost = new Post(contentUrl, postType, savedProfile1.getId());
        this.newPost.setCaption(caption);

        this.savedPostToDelete1 = new Post(contentUrl, postType, savedProfile1.getId());
        this.savedPostToDelete1.setCaption(caption);
        this.savedPostToDelete1.setId(101L);

        this.savedPost1 = new Post(contentUrl, postType, savedProfile1.getId());
        this.savedPost1.setCaption(caption);
        this.savedPost1.setId(102L);

        this.savedPost2 = new Post(contentUrl, postType, savedProfile2.getId());
        this.savedPost2.setCaption(caption);
        this.savedPost2.setId(116L);

        this.savedStory1 = new Post(contentUrl, storyType, savedProfile1.getId());
        this.savedStory1.setCaption(caption);
        this.savedStory1.setId(103L);

        this.savedComment1 = new Comment(
                this.savedProfile1.getId(),
                this.savedPost2.getId(),
                "Commento al post"
        );
        savedComment1.setId(101L);
    }

    @AfterEach
    void tearDown() {

        newProfile.setProfileName(profileName);
    }

    @Test
    void testAddPost_Then_201() {
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
        assertEquals(this.newPost.getContentUrl(), savedPost.getContentUrl());
        assertEquals(this.newPost.getCaption(), savedPost.getCaption());
        assertEquals(this.newPost.getPostType(), savedPost.getPostType());
        assertEquals(this.newPost.getProfileId(), savedPost.getProfileId());

        // visualizzo il post salvato
        log.info(savedPost.toString());
    }

    @Test
    void testAddPost_RequiredField_Then_201() throws Exception {
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
        assertEquals(this.newPost.getContentUrl(), savedPost.getContentUrl());
        assertEquals(this.newPost.getCaption(), savedPost.getCaption());
        assertEquals(this.newPost.getPostType(), savedPost.getPostType());
        assertEquals(this.newPost.getProfileId(), savedPost.getProfileId());

        // visualizzo il post salvato
        log.info(savedPost.toString());
    }

    @Test
    void testAddPost_MissingRequired_Then_400() throws Exception{
        // arrayList contenente i messaggi di errore
        List<String> errors = asList(
                "contentUrl must not be null",
                "postType must not be null",
                "profileId must not be null"
        );

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
        String error = "caption size must be between 0 and 2200";

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
        assertEquals(error ,node.get("error").get(0).asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error").get(0));
    }

    @Test
    void testAddPost_InvalidContentUrl_Then_400() throws Exception {
        List<String> errors = asList(
                "contentUrl must be a valid URL",
                "contentUrl size must be between 3 and 2048");
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
        String error = "JSON parse error: Cannot construct instance of `com.sergiostefanizzi.profilemicroservice.model.Post$PostTypeEnum`, problem: Unexpected value 'NotValidOption'";
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
        assertEquals(error ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }

    @Test
    void testAddPost_InvalidProfileId_Then_400() throws Exception{
        String error = "JSON parse error: Cannot deserialize value of type `java.lang.Long` from String \"IdNotLong\": not a valid `java.lang.Long` value";
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
        assertEquals(error ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }

    //TODO 401, 403

    // Questo dovra' essere sostituito con il 403
    @Test
    void testAddPost_Then_404() throws Exception {
        String error = "Profile "+Long.MAX_VALUE+" not found!";

        this.newPost.setProfileId(Long.MAX_VALUE);

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
        // elimino il profilo appena inserito
        ResponseEntity<Void> responseDelete = this.testRestTemplate.exchange(this.baseUrl+"/{postId}",
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                Void.class,
                this.savedPostToDelete1.getId());

        assertEquals(HttpStatus.NO_CONTENT, responseDelete.getStatusCode());
        assertNull(responseDelete.getBody());
    }

    @Test
    void testDeletePostById_Then_400() throws Exception {
        // messaggio d'errore che mi aspetto d'ottenere
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

    //TODO remove 401, 403

    @Test
    void testDeletePostById_Then_404() throws Exception {
        Long invalidPostId = Long.MAX_VALUE;
        // messaggio d'errore che mi aspetto d'ottenere
        String error = "Post "+invalidPostId+" not found!";
        ResponseEntity<String> response = this.testRestTemplate.exchange(this.baseUrl+"/{postId}",
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                String.class,
                invalidPostId);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode node = this.objectMapper.readTree(response.getBody());
        // In questo caso l'errore NON è un array di dimensione 1
        assertEquals(error ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }

    @Test
    void testUpdatePost_Then_200() throws Exception{
        // Definisco la caption da aggiornare tramite l'oggetto PostPatch
        String newCaption = "Nuova caption del post";
        PostPatch postPatch = new PostPatch(newCaption);

        this.savedPost2.setCaption(newCaption);

        HttpEntity<PostPatch> requestPatch = new HttpEntity<>(postPatch);
        ResponseEntity<Post> responsePatch = this.testRestTemplate.exchange(this.baseUrl+"/{postId}",
                HttpMethod.PATCH,
                requestPatch,
                Post.class,
                this.savedPost2.getId());

        assertEquals(HttpStatus.OK, responsePatch.getStatusCode());
        assertNotNull(responsePatch.getBody());
        assertInstanceOf(Post.class, responsePatch.getBody());
        Post updatedPost = responsePatch.getBody();
        assertEquals(this.savedPost2, updatedPost);

        // visualizzo il profilo aggiornato
        log.info(updatedPost.toString());
        this.savedPost1.setCaption(caption);
    }

    @Test
    void testUpdatePost_InvalidId_Then_400() throws Exception{
        String error = "ID is not valid!";
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
        assertEquals(error ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }

    @Test
    void testUpdatePost_CaptionLength_Then_400() throws Exception {
        String error = "caption size must be between 0 and 2200";

        // genero una caption di 2210 caratteri, superando di 10 il limite
        PostPatch postPatch = new PostPatch(RandomStringUtils.randomAlphabetic(2210));

        HttpEntity<PostPatch> requestPatch = new HttpEntity<>(postPatch);
        ResponseEntity<String> response = this.testRestTemplate.exchange(this.baseUrl+"/{postId}",
                HttpMethod.PATCH,
                requestPatch,
                String.class,
                this.savedPost2.getId());
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode node = this.objectMapper.readTree(response.getBody());
        // In questo caso l'errore e' un array di dimensione 1
        assertTrue(node.get("error").isArray());
        assertEquals(1, node.get("error").size());
        assertEquals(error ,node.get("error").get(0).asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error").get(0));
    }

    //:TODO 401 e 403
    @Test
    void testUpdatePost_Then_404() throws Exception{
        Long invalidPostId = Long.MAX_VALUE;
        String error = "Post "+invalidPostId+" not found!";
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
        assertEquals(error ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }

    @Test
    void testFindPostById_Then_200() throws Exception{
        ResponseEntity<Post> responseGet = this.testRestTemplate.exchange(this.baseUrl+"/{postId}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                Post.class,
                this.savedPost1.getId());

        assertEquals(HttpStatus.OK, responseGet.getStatusCode());
        assertNotNull(responseGet.getBody());
        assertInstanceOf(Post.class, responseGet.getBody());
        Post savedPost = responseGet.getBody();
        assertEquals(this.savedPost1, savedPost);

        // visualizzo il profilo aggiornato
        log.info(savedPost.toString());
    }

    @Test
    void testFindPostById_Then_400() throws Exception{
        String error = "ID is not valid!";

        ResponseEntity<String> response = this.testRestTemplate.exchange(this.baseUrl+"/{postId}",
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

    //TODO 401 e 403
    @Test
    void testFindPostById_Then_404() throws Exception{
        Long invalidPostId = Long.MIN_VALUE;
        String error = "Post "+invalidPostId+" not found!";

        ResponseEntity<String> response = this.testRestTemplate.exchange(this.baseUrl+"/{postId}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                String.class,
                invalidPostId);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode node = this.objectMapper.readTree(response.getBody());
        // In questo caso l'errore NON è un array di dimensione 1
        assertEquals(error ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }

    @Test
    void testAddLike_Then_204() throws Exception {
        Like newLike = new Like(this.savedProfile2.getId(), this.savedPost1.getId());
        HttpEntity<Like> requestLike = new HttpEntity<>(newLike);
        // elimino il profilo appena inserito
        ResponseEntity<Void> responseLike = this.testRestTemplate.exchange(this.baseUrlLike+"?removeLike={removeLike}",
                HttpMethod.PUT,
                requestLike,
                Void.class,
                false);

        assertEquals(HttpStatus.NO_CONTENT, responseLike.getStatusCode());
        assertNull(responseLike.getBody());
    }

    @Test
    void testAddLike_Remove_Then_204() throws Exception {
        Like savedLike = new Like(this.savedProfile1.getId(), this.savedPost1.getId());

        HttpEntity<Like> requestLike2 = new HttpEntity<>(savedLike);
        // elimino il profilo appena inserito
        ResponseEntity<Void> responseLike = this.testRestTemplate.exchange(this.baseUrlLike+"?removeLike={removeLike}",
                HttpMethod.PUT,
                requestLike2,
                Void.class,
                true);

        assertEquals(HttpStatus.NO_CONTENT, responseLike.getStatusCode());
        assertNull(responseLike.getBody());
    }

    @Test
    void testAddLike_Then_400() throws Exception {
        String error = "JSON parse error: Cannot deserialize value of type `java.lang.Long` from String \"IdNotLong\": not a valid `java.lang.Long` value";
        Like newLike = new Like(this.savedProfile2.getId(), this.savedPost1.getId());
        String newLikeJson = this.objectMapper.writeValueAsString(newLike);
        JsonNode jsonNode = this.objectMapper.readTree(newLikeJson);

        ((ObjectNode) jsonNode).put("profile_id", "IdNotLong");
        ((ObjectNode) jsonNode).put("post_id", "IdNotLong");
        newLikeJson = this.objectMapper.writeValueAsString(jsonNode);
        // Dato che invio direttamente il json del like, devo impostare il contentType application/json
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> request = new HttpEntity<>(newLikeJson, headers);
        ResponseEntity<String> response = this.testRestTemplate.exchange(this.baseUrlLike+"?removeLike={removeLike}",
                HttpMethod.PUT,
                request,
                String.class,
                false);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode node = this.objectMapper.readTree(response.getBody());
        // In questo caso l'errore NON è un array di dimensione 1
        assertEquals(error ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }

    //TODO 401 e 403
    @Test
    void testAddLike_PostNotFound_Then_404() throws Exception {
        String error = "Post " + Long.MAX_VALUE + " not found!";


        Like newLike = new Like(this.savedProfile1.getId(), Long.MAX_VALUE);
        HttpEntity<Like> request = new HttpEntity<>(newLike);
        ResponseEntity<String> response = this.testRestTemplate.exchange(this.baseUrlLike+"?removeLike={removeLike}",
                HttpMethod.PUT,
                request,
                String.class,
                false);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode node = this.objectMapper.readTree(response.getBody());
        // In questo caso l'errore NON è un array di dimensione 1
        assertEquals(error ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }

    @Test
    void testAddLike_ProfileNotFound_Then_404() throws Exception {
        String error = "Profile " + Long.MAX_VALUE + " not found!";


        Like newLike = new Like(Long.MAX_VALUE, this.savedPost1.getId());
        HttpEntity<Like> request = new HttpEntity<>(newLike);
        ResponseEntity<String> response = this.testRestTemplate.exchange(this.baseUrlLike+"?removeLike={removeLike}",
                HttpMethod.PUT,
                request,
                String.class,
                false);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode node = this.objectMapper.readTree(response.getBody());
        // In questo caso l'errore NON è un array di dimensione 1
        assertEquals(error ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }

    @Test
    void testFindAllLikesByPostId_Then_200() throws Exception {
        List<Like> likeList = asList(
                new Like(101L,this.savedPost2.getId()),
                new Like(103L,this.savedPost2.getId()),
                new Like(104L,this.savedPost2.getId()),
                new Like(105L,this.savedPost2.getId())
        );


        ResponseEntity<List<Like>> responseLikeList = this.testRestTemplate.exchange(this.baseUrlLike + "/{postId}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<Like>>() {},
                this.savedPost2.getId());
        assertEquals(HttpStatus.OK, responseLikeList.getStatusCode());
        assertNotNull(responseLikeList.getBody());
        List<Like> returnedLikeList = responseLikeList.getBody();
        assertEquals(likeList.size(), returnedLikeList.size());
        assertEquals(likeList, returnedLikeList);

        log.info(returnedLikeList.toString());
    }

    @Test
    void testFindAllLikesByPostId_Then_400() throws Exception {
        // messaggio d'errore che mi aspetto d'ottenere
        String error = "ID is not valid!";
        ResponseEntity<String> responseLikeList = this.testRestTemplate.exchange(this.baseUrlLike + "/{postId}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                String.class,
                "IdNotLong");
        assertEquals(HttpStatus.BAD_REQUEST, responseLikeList.getStatusCode());
        assertNotNull(responseLikeList.getBody());

        JsonNode node = this.objectMapper.readTree(responseLikeList.getBody());
        // In questo caso l'errore NON è un array di dimensione 1
        assertEquals(error ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }

    // TODO 401, 403

    @Test
    void testFindAllLikesByPostId_PostNotFound_Then_404() throws Exception {
        String error = "Post " + Long.MAX_VALUE + " not found!";

        ResponseEntity<String> responseLikeList = this.testRestTemplate.exchange(this.baseUrlLike + "/{postId}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                String.class,
                Long.MAX_VALUE);
        assertEquals(HttpStatus.NOT_FOUND, responseLikeList.getStatusCode());
        assertNotNull(responseLikeList.getBody());

        JsonNode node = this.objectMapper.readTree(responseLikeList.getBody());
        // In questo caso l'errore NON è un array di dimensione 1
        assertEquals(error ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }

    @Test
    void testAddComment_Then_201() throws Exception {
        String content = "Commento al post";
        Comment newComment = new Comment(
                this.savedProfile2.getId(),
                this.savedPost1.getId(),
                content
        );
        HttpEntity<Comment> requestComment = new HttpEntity<>(newComment);
        // elimino il profilo appena inserito
        ResponseEntity<Comment> responseComment = this.testRestTemplate.exchange(this.baseUrlComment,
                HttpMethod.POST,
                requestComment,
                Comment.class);

        assertEquals(HttpStatus.CREATED, responseComment.getStatusCode());
        assertNotNull(responseComment.getBody());
        Comment savedComment = responseComment.getBody();
        assertNotNull(savedComment.getId());
        assertEquals(this.savedProfile2.getId(), savedComment.getProfileId());
        assertEquals(this.savedPost1.getId(), savedComment.getPostId());
        assertEquals(content, savedComment.getContent());

        // visualizzo il post salvato
        log.info(savedComment.toString());
    }

    @Test
    void testAddComment_CommentOnStory_Then_400() throws Exception {
        String error = "Cannot comment on a story!";

        String content = "Commento al post";
        Comment newComment = new Comment(
                this.savedProfile2.getId(),
                this.savedStory1.getId(),
                content
        );
        HttpEntity<Comment> requestComment = new HttpEntity<>(newComment);

        ResponseEntity<String> responseComment = this.testRestTemplate.exchange(this.baseUrlComment,
                HttpMethod.POST,
                requestComment,
                String.class);

        assertEquals(HttpStatus.BAD_REQUEST, responseComment.getStatusCode());
        assertNotNull(responseComment.getBody());

        JsonNode node = this.objectMapper.readTree(responseComment.getBody());
        // In questo caso l'errore NON è un array di dimensione 1
        assertEquals(error ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }

    @Test
    void testAddComment_Content_Size_Then_400() throws Exception {
        String error = "content size must be between 1 and 2200";

        String content = RandomStringUtils.randomAlphabetic(2210);
        Comment newComment = new Comment(
                this.savedProfile2.getId(),
                this.savedPost1.getId(),
                content
        );
        HttpEntity<Comment> requestComment = new HttpEntity<>(newComment);
        // elimino il profilo appena inserito
        ResponseEntity<String> responseComment = this.testRestTemplate.exchange(this.baseUrlComment,
                HttpMethod.POST,
                requestComment,
                String.class);

        assertEquals(HttpStatus.BAD_REQUEST, responseComment.getStatusCode());
        assertNotNull(responseComment.getBody());

        JsonNode node = this.objectMapper.readTree(responseComment.getBody());
        // itero gli errori ottenuti dalla risposta per confrontarli con quelli che mi aspetto di ottenere
        // Anche se è solo un errore, ottengo sempre un array di dimensione 1
        assertTrue(node.get("error").isArray());
        assertEquals(1, node.get("error").size());
        assertEquals(error ,node.get("error").get(0).asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error").get(0));
    }

    @Test
    void testAddComment_Invalid_Ids_Then_400() throws Exception {
        String error = "JSON parse error: Cannot deserialize value of type `java.lang.Long` from String \"IdNotLong\": not a valid `java.lang.Long` value";

        String content = RandomStringUtils.randomAlphabetic(2210);
        Comment newComment = new Comment(
                this.savedProfile2.getId(),
                this.savedPost1.getId(),
                content
        );

        String newCommentJson = this.objectMapper.writeValueAsString(newComment);
        JsonNode jsonNode = this.objectMapper.readTree(newCommentJson);
        ((ObjectNode) jsonNode).put("profile_id", "IdNotLong");
        ((ObjectNode) jsonNode).put("post_id", "IdNotLong");
        newCommentJson = this.objectMapper.writeValueAsString(jsonNode);

        // Dato che invio direttamente il json del profile, devo impostare il contentType application/json
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> requestComment = new HttpEntity<>(newCommentJson, headers);
        // elimino il profilo appena inserito
        ResponseEntity<String> responseComment = this.testRestTemplate.exchange(this.baseUrlComment,
                HttpMethod.POST,
                requestComment,
                String.class);
        assertEquals(HttpStatus.BAD_REQUEST, responseComment.getStatusCode());
        assertNotNull(responseComment.getBody());

        JsonNode node = this.objectMapper.readTree(responseComment.getBody());
        // In questo caso l'errore NON è un array di dimensione 1
        assertEquals(error ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }

    //TODO 401, 403

    // Da sostituire con il 403
    @Test
    void testAddComment_ProfileNotFound_Then_404() throws Exception {
        String error = "Profile " + Long.MAX_VALUE + " not found!";

        String content = "Commento al post";
        Comment newComment = new Comment(
                Long.MAX_VALUE,
                this.savedPost1.getId(),
                content
        );
        HttpEntity<Comment> requestComment = new HttpEntity<>(newComment);
        // elimino il profilo appena inserito
        ResponseEntity<String> responseComment = this.testRestTemplate.exchange(this.baseUrlComment,
                HttpMethod.POST,
                requestComment,
                String.class);

        assertEquals(HttpStatus.NOT_FOUND, responseComment.getStatusCode());
        assertNotNull(responseComment.getBody());

        JsonNode node = this.objectMapper.readTree(responseComment.getBody());
        // In questo caso l'errore NON è un array di dimensione 1
        assertEquals(error ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }

    @Test
    void testAddComment_PostNotFound_Then_404() throws Exception {
        String error = "Post " + Long.MAX_VALUE + " not found!";

        String content = "Commento al post";
        Comment newComment = new Comment(
                this.savedProfile2.getId(),
                Long.MAX_VALUE,
                content
        );
        HttpEntity<Comment> requestComment = new HttpEntity<>(newComment);
        // elimino il profilo appena inserito
        ResponseEntity<String> responseComment = this.testRestTemplate.exchange(this.baseUrlComment,
                HttpMethod.POST,
                requestComment,
                String.class);

        assertEquals(HttpStatus.NOT_FOUND, responseComment.getStatusCode());
        assertNotNull(responseComment.getBody());

        JsonNode node = this.objectMapper.readTree(responseComment.getBody());
        // In questo caso l'errore NON è un array di dimensione 1
        assertEquals(error ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }

    @Test
    void testUpdatedCommentById_Then_200() throws Exception {
        String newContent = "Commento modificato";

        this.savedComment1.setContent(newContent);

        CommentPatch commentPatch = new CommentPatch(newContent);

        HttpEntity<CommentPatch> requestCommentPatch = new HttpEntity<>(commentPatch);
        // elimino il profilo appena inserito
        ResponseEntity<Comment> responseCommentPatch = this.testRestTemplate.exchange(this.baseUrlComment+"/{commentId}",
                HttpMethod.PATCH,
                requestCommentPatch,
                Comment.class,
                this.savedComment1.getId());

        assertEquals(HttpStatus.OK, responseCommentPatch.getStatusCode());
        assertNotNull(responseCommentPatch.getBody());
        Comment savedUpdatedComment = responseCommentPatch.getBody();
        assertEquals(this.savedComment1, savedUpdatedComment);

        // visualizzo il post salvato
        log.info(savedUpdatedComment.toString());
    }

    @Test
    void testUpdatedCommentById_Then_400() throws Exception {
        String error = "ID is not valid!";

        String newContent = RandomStringUtils.randomAlphabetic(2210);
        CommentPatch commentPatch = new CommentPatch(newContent);

        HttpEntity<CommentPatch> requestCommentPatch = new HttpEntity<>(commentPatch);

        ResponseEntity<String> response = this.testRestTemplate.exchange(this.baseUrlComment+"/{commentId}",
                HttpMethod.PATCH,
                requestCommentPatch,
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
    void testUpdatedCommentById_Content_Size_Then_400() throws Exception {
        String error = "content size must be between 1 and 2200";

        String newContent = RandomStringUtils.randomAlphabetic(2210);
        CommentPatch commentPatch = new CommentPatch(newContent);

        HttpEntity<CommentPatch> requestCommentPatch = new HttpEntity<>(commentPatch);

        ResponseEntity<String> responseCommentPatch = this.testRestTemplate.exchange(this.baseUrlComment+"/{commentId}",
                HttpMethod.PATCH,
                requestCommentPatch,
                String.class,
                this.savedComment1.getId());

        assertEquals(HttpStatus.BAD_REQUEST, responseCommentPatch.getStatusCode());
        assertNotNull(responseCommentPatch.getBody());

        JsonNode node = this.objectMapper.readTree(responseCommentPatch.getBody());
        // itero gli errori ottenuti dalla risposta per confrontarli con quelli che mi aspetto di ottenere
        // Anche se è solo un errore, ottengo sempre un array di dimensione 1
        assertTrue(node.get("error").isArray());
        assertEquals(1, node.get("error").size());
        assertEquals(error, node.get("error").get(0).asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error").get(0));
    }

    //TODO 401, 403

    // Da sostituire con il 403
    @Test
    void testUpdatedCommentById_CommentNotFound_Then_404() throws Exception {
        String error = "Comment " + Long.MAX_VALUE + " not found!";
        String newContent = "Commento al post modificato";
        CommentPatch commentPatch = new CommentPatch(newContent);

        HttpEntity<CommentPatch> requestCommentPatch = new HttpEntity<>(commentPatch);
        // elimino il profilo appena inserito
        ResponseEntity<String> responseCommentPatch = this.testRestTemplate.exchange(this.baseUrlComment+"/{commentId}",
                HttpMethod.PATCH,
                requestCommentPatch,
                String.class,
                Long.MAX_VALUE);

        assertEquals(HttpStatus.NOT_FOUND, responseCommentPatch.getStatusCode());
        assertNotNull(responseCommentPatch.getBody());

        JsonNode node = this.objectMapper.readTree(responseCommentPatch.getBody());
        // In questo caso l'errore NON è un array di dimensione 1
        assertEquals(error ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }

    @Test
    void testDeleteCommentById_Then_204() throws Exception {
        ResponseEntity<Void> responseDeleteComment = this.testRestTemplate.exchange(this.baseUrlComment+"/{commentId}",
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                Void.class,
                105L);
        assertEquals(HttpStatus.NO_CONTENT, responseDeleteComment.getStatusCode());
        assertNull(responseDeleteComment.getBody());
    }

    @Test
    void testDeleteCommentById_Then_400() throws Exception {
        // messaggio d'errore che mi aspetto d'ottenere
        String error = "ID is not valid!";
        ResponseEntity<String> response = this.testRestTemplate.exchange(this.baseUrlComment+"/IdNotLong",
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

    // TODO 401, 403
    @Test
    void testDeleteCommentById_Then_404() throws Exception {
        Long invalidCommentId = Long.MAX_VALUE;
        // messaggio d'errore che mi aspetto d'ottenere
        String error = "Comment "+invalidCommentId+" not found!";
        ResponseEntity<String> responseDeleteComment = this.testRestTemplate.exchange(this.baseUrlComment+"/{commentId}",
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                String.class,
                invalidCommentId);
        assertEquals(HttpStatus.NOT_FOUND, responseDeleteComment.getStatusCode());
        assertNotNull(responseDeleteComment.getBody());

        JsonNode node = this.objectMapper.readTree(responseDeleteComment.getBody());
        // In questo caso l'errore NON è un array di dimensione 1
        assertEquals(error,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }

    @Test
    void testFindAllCommentsByPostId_Then_200(){

        ResponseEntity<List<Comment>> responseGetAllComment = this.testRestTemplate.exchange(this.baseUrlComment + "/{postId}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<Comment>>() {},
                this.savedPost2.getId());
        assertEquals(HttpStatus.OK, responseGetAllComment.getStatusCode());
        assertNotNull(responseGetAllComment.getBody());
        List<Comment> returnedCommentList = responseGetAllComment.getBody();
        log.info(returnedCommentList.toString());
        assertTrue(returnedCommentList.size()>=3);

    }

    @Test
    void testFindAllCommentsByPostId_InvalidId_Then_400() throws Exception {
        String error = "ID is not valid!";

        ResponseEntity<String> response = this.testRestTemplate.exchange(this.baseUrlComment+"/{postId}",
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

    //TODO 401,403
    @Test
    void testFindAllCommentsByPostId_PostNotFound_Then_404() throws Exception{
        String error = "Post " + Long.MAX_VALUE + " not found!";

        ResponseEntity<String> response = this.testRestTemplate.exchange(this.baseUrlComment + "/{postId}",
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
// TODO
    @Test
    void testProfileFeedByProfileId_Then_200(){
        // Creo un primo profilo
        Profile mainProfile = createPublicProfile("pincoPallino1");
        // Creo un secondo profilo
        Profile followedProfile1 = createPublicProfile("pincoPallino2");
        Profile followedProfile2 = createPublicProfile("pincoPallino3");
        createFollow(mainProfile, followedProfile1);
        createFollow(mainProfile, followedProfile2);

        Post savedPost1 = createPost(followedProfile1.getId(), Post.PostTypeEnum.POST);
        Post savedStory1 = createPost(followedProfile1.getId(), Post.PostTypeEnum.STORY);
        Post savedPost2 = createPost(followedProfile2.getId(), Post.PostTypeEnum.POST);

        List<Post> expectedFeed = new ArrayList<>();

        expectedFeed.add(savedPost2);
        expectedFeed.add(savedStory1);
        expectedFeed.add(savedPost1);



        ResponseEntity<List<Post>> response = this.testRestTemplate.exchange(
                this.baseUrl + "/feed/{profileId}?onlyPost={onlyPost}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<Post>>() {},
                mainProfile.getId(),
                null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<Post> mainProfileFeed = response.getBody();
        assertNotNull(mainProfileFeed);
        assertEquals(expectedFeed, mainProfileFeed);

        // visualizzo il post salvato
        log.info(mainProfileFeed.toString());
    }

    @Test
    void testProfileFeedByProfileId_OnlyPost_Then_200(){
        // Creo un primo profilo
        Profile mainProfile = createPublicProfile("pincoPallino4");
        // Creo un secondo profilo
        Profile followedProfile1 = createPublicProfile("pincoPallino5");
        Profile followedProfile2 = createPublicProfile("pincoPallino6");
        createFollow(mainProfile, followedProfile1);
        createFollow(mainProfile, followedProfile2);

        Post savedPost1 = createPost(followedProfile1.getId(), Post.PostTypeEnum.POST);
        createPost(followedProfile1.getId(), Post.PostTypeEnum.STORY);
        Post savedPost2 = createPost(followedProfile2.getId(), Post.PostTypeEnum.POST);

        List<Post> expectedFeed = new ArrayList<>();

        expectedFeed.add(savedPost2);
        expectedFeed.add(savedPost1);



        ResponseEntity<List<Post>> response = this.testRestTemplate.exchange(
                this.baseUrl + "/feed/{profileId}?onlyPost={onlyPost}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<Post>>() {},
                mainProfile.getId(),
                true);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<Post> mainProfileFeed = response.getBody();
        assertNotNull(mainProfileFeed);
        assertEquals(expectedFeed, mainProfileFeed);

        // visualizzo il post salvato
        log.info(mainProfileFeed.toString());
    }

    @Test
    void testProfileFeedByProfileId_OnlyStories_Then_200(){
        // Creo un primo profilo
        Profile mainProfile = createPublicProfile("pincoPallino7");
        // Creo un secondo profilo
        Profile followedProfile1 = createPublicProfile("pincoPallino8");
        Profile followedProfile2 = createPublicProfile("pincoPallino9");
        createFollow(mainProfile, followedProfile1);
        createFollow(mainProfile, followedProfile2);

        createPost(followedProfile1.getId(), Post.PostTypeEnum.POST);
        Post savedStory1 = createPost(followedProfile1.getId(), Post.PostTypeEnum.STORY);
        createPost(followedProfile2.getId(), Post.PostTypeEnum.POST);

        List<Post> expectedFeed = new ArrayList<>();

        expectedFeed.add(savedStory1);



        ResponseEntity<List<Post>> response = this.testRestTemplate.exchange(
                this.baseUrl + "/feed/{profileId}?onlyPost={onlyPost}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<Post>>() {},
                mainProfile.getId(),
                false);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<Post> mainProfileFeed = response.getBody();
        assertNotNull(mainProfileFeed);
        assertEquals(expectedFeed, mainProfileFeed);

        // visualizzo il post salvato
        log.info(mainProfileFeed.toString());
    }

    @Test
    void testProfileFeedByProfileId_InvalidId_Then_400() throws Exception {
        // messaggio d'errore che mi aspetto d'ottenere
        String error = "ID is not valid!";
        ResponseEntity<String> response = this.testRestTemplate.exchange(
                this.baseUrl + "/feed/{profileId}?onlyPost={onlyPost}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                String.class,
                "IdNotLong",
                false);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode node = this.objectMapper.readTree(response.getBody());
        // In questo caso l'errore NON è un array di dimensione 1
        assertEquals(error ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }

    @Test
    void testProfileFeedByProfileId_Then_400() throws JsonProcessingException {
        Profile mainProfile = createPublicProfile("pincoPallino10");
        // messaggio d'errore che mi aspetto d'ottenere
        String error = "Failed to convert value of type 'java.lang.String' to required type 'java.lang.Boolean'; Invalid boolean value [NotBoolean]";
        ResponseEntity<String> response = this.testRestTemplate.exchange(
                this.baseUrl + "/feed/{profileId}?onlyPost={onlyPost}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                String.class,
                mainProfile.getId(),
                "NotBoolean");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode node = this.objectMapper.readTree(response.getBody());
        // In questo caso l'errore NON è un array di dimensione 1
        assertEquals(error ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }

    @Test
    void testProfileFeedByProfileId_Then_404() throws Exception {
        String error = "Profile "+Long.MAX_VALUE+" not found!";


        // messaggio d'errore che mi aspetto d'ottenere
        ResponseEntity<String> response = this.testRestTemplate.exchange(
                this.baseUrl + "/feed/{profileId}?onlyPost={onlyPost}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                String.class,
                Long.MAX_VALUE,
                null);

        JsonNode node = this.objectMapper.readTree(response.getBody());
        // In questo caso l'errore NON è un array di dimensione 1
        assertEquals(error ,node.get("error").asText()); // asText() perche' mi dava una stringa tra doppi apici e non riuscivo a fare il confronto
        log.info("Error -> "+node.get("error"));
    }


    Post createPost(Long profileId, Post.PostTypeEnum postType) {
        Post post = new Post(contentUrl, postType, profileId);

        HttpEntity<Post> request = new HttpEntity<>(post);
        ResponseEntity<Post> response = this.testRestTemplate.exchange(
                this.baseUrl,
                HttpMethod.POST,
                request,
                Post.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        Post savedPost = response.getBody();
        assertNotNull(savedPost);
        assertNotNull(savedPost.getId());
        assertEquals(post.getContentUrl(), savedPost.getContentUrl());
        assertEquals(post.getPostType(), savedPost.getPostType());
        assertEquals(post.getProfileId(), savedPost.getProfileId());

        // visualizzo il post salvato
        log.info(savedPost.toString());
        return savedPost;
    }


    Profile createPublicProfile(String profileName){
        Profile newProfile = new Profile(profileName, false,1L);
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

        log.info(responseProfile.toString());

        return savedProfile;
    }

    void createFollow(Profile follower, Profile followed){
        ResponseEntity<Follows> responseFollows = this.testRestTemplate.exchange(
                this.baseUrlProfile+"/{profileId}/follows/{followsId}?unfollow={unfollow}",
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