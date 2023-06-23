package com.sergiostefanizzi.profilemicroservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sergiostefanizzi.profilemicroservice.model.Post;
import com.sergiostefanizzi.profilemicroservice.model.Profile;
import com.sergiostefanizzi.profilemicroservice.repository.PostsRepository;
import com.sergiostefanizzi.profilemicroservice.repository.ProfilesRepository;
import com.sergiostefanizzi.profilemicroservice.service.PostsService;
import com.sergiostefanizzi.profilemicroservice.service.ProfilesService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(PostsController.class)
@ActiveProfiles("test")
@Slf4j
class PostsControllerTest {
    @MockBean
    private PostsService postsService;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    String contentUrl = "https://upload.wikimedia.org/wikipedia/commons/9/9a/Cape_may.jpg";
    String caption = "This is the post caption";
    Post.PostTypeEnum postType = Post.PostTypeEnum.POST;
    Long postId = 1L;
    Long profileId = 11L;
    private Post newPost;
    private Post savedPost;
    String newPostJson;

    @BeforeEach
    void setUp() throws JsonProcessingException {
        this.newPost = new Post(contentUrl, postType, profileId);
        this.newPost.setCaption(caption);

        newPostJson = this.objectMapper.writeValueAsString(this.newPost);

        this.savedPost = new Post(contentUrl, postType, profileId);
        this.savedPost.setCaption(caption);
        this.savedPost.setId(postId);
    }

    @Test
    void testAddPost_Then_201() throws Exception {
        when(this.postsService.save(this.newPost)).thenReturn(this.savedPost);

        MvcResult result = this.mockMvc.perform(post("/posts")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newPostJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.*", hasSize(5)))
                .andExpect(jsonPath("$.id").value(this.savedPost.getId()))
                .andExpect(jsonPath("$.content_url").value(this.savedPost.getContentUrl()))
                .andExpect(jsonPath("$.caption").value(this.savedPost.getCaption()))
                .andExpect(jsonPath("$.post_type").value(this.savedPost.getPostType().toString()))
                .andExpect(jsonPath("$.profile_id").value(this.savedPost.getProfileId()))
                .andReturn();

        // salvo risposta in result per visualizzarla
        String resultAsString = result.getResponse().getContentAsString();
        Post postResult = this.objectMapper.readValue(resultAsString, Post.class);

        log.info(postResult.toString());
    }
}