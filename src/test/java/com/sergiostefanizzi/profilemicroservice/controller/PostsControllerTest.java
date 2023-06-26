package com.sergiostefanizzi.profilemicroservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sergiostefanizzi.profilemicroservice.model.Post;
import com.sergiostefanizzi.profilemicroservice.model.Profile;
import com.sergiostefanizzi.profilemicroservice.repository.PostsRepository;
import com.sergiostefanizzi.profilemicroservice.repository.ProfilesRepository;
import com.sergiostefanizzi.profilemicroservice.service.PostsService;
import com.sergiostefanizzi.profilemicroservice.service.ProfilesService;
import com.sergiostefanizzi.profilemicroservice.system.exception.ProfileNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.in;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
    String contentUrlXSS = "http://www.example.com?d=<script type=\"javascript\" src=\"http://www.google.it\"/>"; //Cross site scripting XSS
    String caption = "This is the post caption";
    Post.PostTypeEnum postType = Post.PostTypeEnum.POST;
    Long postId = 1L;
    Long profileId = 11L;
    private Post newPost;
    private Post savedPost;
    String newPostJson;
    List<String> errors;

    @BeforeEach
    void setUp() throws JsonProcessingException {
        this.newPost = new Post(contentUrl, postType, profileId);
        this.newPost.setCaption(caption);

        newPostJson = this.objectMapper.writeValueAsString(this.newPost);

        this.savedPost = new Post(contentUrl, postType, profileId);
        this.savedPost.setCaption(caption);
        this.savedPost.setId(postId);

        errors = new ArrayList<>();
    }


    @AfterEach
    void tearDown() {
        errors.clear();
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

    @Test
    void testAddPost_RequiredField_Then_201() throws Exception {
        this.newPost.setCaption(null);
        this.savedPost.setCaption(null);
        newPostJson = this.objectMapper.writeValueAsString(this.newPost);

        when(this.postsService.save(this.newPost)).thenReturn(this.savedPost);

        //La caption non dovrebbe essere presenze, e quindi ci saranno solo quattro campi
        MvcResult result = this.mockMvc.perform(post("/posts")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newPostJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.*", hasSize(4)))
                .andExpect(jsonPath("$.id").value(this.savedPost.getId()))
                .andExpect(jsonPath("$.content_url").value(this.savedPost.getContentUrl()))
                .andExpect(jsonPath("$.post_type").value(this.savedPost.getPostType().toString()))
                .andExpect(jsonPath("$.profile_id").value(this.savedPost.getProfileId()))
                .andReturn();

        // salvo risposta in result per visualizzarla
        String resultAsString = result.getResponse().getContentAsString();
        Post postResult = this.objectMapper.readValue(resultAsString, Post.class);

        log.info(postResult.toString());
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

        newPostJson = this.objectMapper.writeValueAsString(this.newPost);

        MvcResult result = this.mockMvc.perform(post("/posts")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newPostJson))
                .andExpect(status().isBadRequest())
                .andExpect(res -> assertTrue(res.getResolvedException() instanceof MethodArgumentNotValidException))
                .andExpect(jsonPath("$.error").isArray())
                .andExpect(jsonPath("$.error", hasSize(3)))
                .andExpect(jsonPath("$.error[0]").value(in(errors)))
                .andExpect(jsonPath("$.error[1]").value(in(errors)))
                .andExpect(jsonPath("$.error[2]").value(in(errors)))
                .andReturn();
        // salvo risposta in result solo per visualizzarla
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n"+resultAsString);
    }

    @Test
    void testAddPost_CaptionLength_Then_400() throws Exception {
        errors.add("caption size must be between 0 and 2200");
        // genero una caption di 2210 caratteri, superando di 10 il limite
        this.newPost.setCaption(RandomStringUtils.randomAlphabetic(2210));

        newPostJson = this.objectMapper.writeValueAsString(this.newPost);
        log.info("JSON\n"+newPostJson);

        MvcResult result = this.mockMvc.perform(post("/posts")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newPostJson))
                .andExpect(status().isBadRequest())
                .andExpect(res -> assertTrue(res.getResolvedException() instanceof MethodArgumentNotValidException))
                .andExpect(jsonPath("$.error").isArray())
                .andExpect(jsonPath("$.error", hasSize(1)))
                .andExpect(jsonPath("$.error[0]").value(in(errors)))
                .andReturn();
        // salvo risposta in result solo per visualizzarla
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n"+resultAsString);
    }

    @Test
    void testAddPost_InvalidContentUrl_Then_400() throws Exception {
        errors.add("contentUrl must be a valid URL");
        errors.add("contentUrl size must be between 3 and 2048");
        // Url non valido
        this.newPost.setContentUrl("https://upload.wikimedia.o/ ra-%%$^&& iuyi"+RandomStringUtils.randomAlphabetic(2048));
        //Test XSS
        //this.newPost.setContentUrl(contentUrlXSS);
        newPostJson = this.objectMapper.writeValueAsString(this.newPost);

        MvcResult result = this.mockMvc.perform(post("/posts")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newPostJson))
                .andExpect(status().isBadRequest())
                .andExpect(res -> assertTrue(res.getResolvedException() instanceof MethodArgumentNotValidException))
                .andExpect(jsonPath("$.error").isArray())
                .andExpect(jsonPath("$.error", hasSize(2)))
                .andExpect(jsonPath("$.error[0]").value(in(errors)))
                .andExpect(jsonPath("$.error[1]").value(in(errors)))
                .andReturn();
        // salvo risposta in result solo per visualizzarla
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n"+resultAsString);
    }

    @Test
    void testAddPost_InvalidPostType_Then_400() throws Exception{
        JsonNode jsonNode = this.objectMapper.readTree(newPostJson);

        ((ObjectNode) jsonNode).put("post_type", "NOTVALID");
        newPostJson = this.objectMapper.writeValueAsString(jsonNode);

        MvcResult result = this.mockMvc.perform(post("/posts")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newPostJson))
                .andExpect(status().isBadRequest())
                .andExpect(res -> assertTrue(
                        res.getResolvedException() instanceof HttpMessageNotReadableException
                ))
                .andExpect(jsonPath("$.error").value("JSON parse error: Cannot construct instance of `com.sergiostefanizzi.profilemicroservice.model.Post$PostTypeEnum`, problem: Unexpected value 'NOTVALID'"))
                .andReturn();
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n"+resultAsString);
        log.info("Resolved Error ---> "+result.getResolvedException());
    }

    @Test
    void testAddPost_InvalidProfileId_Then_400() throws Exception{
        JsonNode jsonNode = this.objectMapper.readTree(newPostJson);
        ((ObjectNode) jsonNode).put("profile_id", "IdNotLong");
        newPostJson = this.objectMapper.writeValueAsString(jsonNode);

        MvcResult result = this.mockMvc.perform(post("/posts")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newPostJson))
                .andExpect(status().isBadRequest())
                .andExpect(res -> assertTrue(
                        res.getResolvedException() instanceof HttpMessageNotReadableException
                ))
                .andExpect(jsonPath("$.error").value("JSON parse error: Cannot deserialize value of type `java.lang.Long` from String \"IdNotLong\": not a valid `java.lang.Long` value")).andReturn();
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n"+resultAsString);
        log.info("Resolved Error ---> "+result.getResolvedException());
    }

    //TODO 401, 403

    // Questo dovra' essere sostituito con il 403
    @Test
    void testAddPost_Then_404() throws Exception {
        Long invalidProfileId = Long.MIN_VALUE;
        doThrow(new ProfileNotFoundException(invalidProfileId)).when(this.postsService).save(this.newPost);

        MvcResult result = this.mockMvc.perform(post("/posts")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newPostJson))
                .andExpect(status().isNotFound())
                .andExpect(res -> assertTrue(
                        res.getResolvedException() instanceof ProfileNotFoundException
                ))
                .andExpect(jsonPath("$.error").value("Profile "+invalidProfileId+" not found!"))
                .andReturn();
        // Visualizzo l'errore
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n"+resultAsString);
        log.info("Resolved Error ---> "+result.getResolvedException());
    }
}