package com.sergiostefanizzi.profilemicroservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sergiostefanizzi.profilemicroservice.model.Post;
import com.sergiostefanizzi.profilemicroservice.model.Profile;
import com.sergiostefanizzi.profilemicroservice.model.ProfileJpa;
import com.sergiostefanizzi.profilemicroservice.repository.PostsRepository;
import com.sergiostefanizzi.profilemicroservice.repository.ProfilesRepository;
import lombok.extern.slf4j.Slf4j;
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
    String caption = "This is the post caption";
    Post.PostTypeEnum postType = Post.PostTypeEnum.POST;
    Long postId = 1L;
    Long profileId;
    private Post newPost;
    Profile newProfile = new Profile("pinco_pallino",false,111L);


    @BeforeEach
    void setUp() {
        this.baseUrlProfile = this.baseUrl + ":" + port + "/profiles";
        this.baseUrl += ":" + port + "/posts";

        this.newPost = new Post(contentUrl, postType, profileId);
        this.newPost.setCaption(caption);

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
}