package com.sergiostefanizzi.profilemicroservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sergiostefanizzi.profilemicroservice.model.Follows;
import com.sergiostefanizzi.profilemicroservice.model.FollowsId;
import com.sergiostefanizzi.profilemicroservice.model.Post;
import com.sergiostefanizzi.profilemicroservice.model.ProfileJpa;
import com.sergiostefanizzi.profilemicroservice.service.FollowsService;
import com.sergiostefanizzi.profilemicroservice.service.PostsService;
import com.sergiostefanizzi.profilemicroservice.system.exception.FollowAlreadyCreatedException;
import com.sergiostefanizzi.profilemicroservice.system.exception.ProfileNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.in;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(FollowsController.class)
@ActiveProfiles("test")
@Slf4j
class FollowsControllerTest {
    @MockBean
    private FollowsService followsService;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    Long openProfileId1 = 1L;
    Long openProfileId2 = 2L;
    Long privateProfileId = 3L;
    Long invalidProfileId = Long.MIN_VALUE;
    List<String> errors;

    @BeforeEach
    void setUp() {
        errors = new ArrayList<>();
    }

    @Test
    void testAddFollows_OpenProfile_Return_ACCEPTED_Then_201() throws Exception {
        Follows follows = new Follows(openProfileId1, openProfileId2, Follows.RequestStatusEnum.ACCEPTED);
        when(this.followsService.addFollows(openProfileId1, openProfileId2)).thenReturn(follows);

        MvcResult result = this.mockMvc.perform(post("/profiles/{profileId}/follows/{followsId}",openProfileId1,openProfileId2)
                        .contentType(MediaType.APPLICATION_JSON)
                        )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.*", hasSize(3)))
                .andExpect(jsonPath("$.follower_id").value(follows.getFollowerId()))
                .andExpect(jsonPath("$.followed_id").value(follows.getFollowedId()))
                .andExpect(jsonPath("$.request_status").value(follows.getRequestStatus().getValue()))
                .andReturn();

        // salvo risposta in result per visualizzarla
        String resultAsString = result.getResponse().getContentAsString();
        Follows followsResult = this.objectMapper.readValue(resultAsString, Follows.class);

        log.info(followsResult.toString());
    }

    @Test
    void testAddFollows_PrivateProfile_Or_RequestAlreadyRejected_Then_201() throws Exception {
        Follows follows = new Follows(openProfileId1, openProfileId2, Follows.RequestStatusEnum.PENDING);
        when(this.followsService.addFollows(openProfileId1, openProfileId2)).thenReturn(follows);

        MvcResult result = this.mockMvc.perform(post("/profiles/{profileId}/follows/{followsId}",openProfileId1,openProfileId2)
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.*", hasSize(3)))
                .andExpect(jsonPath("$.follower_id").value(follows.getFollowerId()))
                .andExpect(jsonPath("$.followed_id").value(follows.getFollowedId()))
                .andExpect(jsonPath("$.request_status").value(follows.getRequestStatus().getValue()))
                .andReturn();

        // salvo risposta in result per visualizzarla
        String resultAsString = result.getResponse().getContentAsString();
        Follows followsResult = this.objectMapper.readValue(resultAsString, Follows.class);

        log.info(followsResult.toString());
    }

    @Test
    void testAddFollows_InvalidId_Then_400() throws Exception {
        // arrayList contenente i messaggi di errore
        errors.add("Failed to convert value of type 'java.lang.String' to required type 'java.lang.Long'; For input string: \"IdNotLong\"");
        MvcResult result = this.mockMvc.perform(post("/profiles/{profileId}/follows/{followsId}","IdNotLong","IdNotLong")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(res -> assertTrue(
                        res.getResolvedException() instanceof MethodArgumentTypeMismatchException
                ))
                .andExpect(jsonPath("$.error").value(errors.get(0))).andReturn();
        // Visualizzo l'errore
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n" + resultAsString);
        log.info("Resolved Error ---> " + result.getResolvedException());
    }

    //TODO 401

    @Test
    void testAddFollows_ProfileNotFound_Then_404() throws Exception {
        // arrayList contenente i messaggi di errore
        errors.add("Profile " + invalidProfileId + " not found!");
        doThrow(new ProfileNotFoundException(invalidProfileId)).when(this.followsService).addFollows(openProfileId1,invalidProfileId);

        MvcResult result = this.mockMvc.perform(post("/profiles/{profileId}/follows/{followsId}",openProfileId1,invalidProfileId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(res -> assertTrue(
                        res.getResolvedException() instanceof ProfileNotFoundException
                ))
                .andExpect(jsonPath("$.error").value(errors.get(0)))
                .andReturn();
        // Visualizzo l'errore
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n" + resultAsString);
        log.info("Resolved Error ---> " + result.getResolvedException());
    }

    @Test
    void testAddFollows_ProfileNotFound_Then_409() throws Exception {
        // arrayList contenente i messaggi di errore
        errors.add("Conflict! Follows already created!");
        doThrow(new FollowAlreadyCreatedException()).when(this.followsService).addFollows(openProfileId1,openProfileId2);

        MvcResult result = this.mockMvc.perform(post("/profiles/{profileId}/follows/{followsId}",openProfileId1,openProfileId2)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(res -> assertTrue(
                        res.getResolvedException() instanceof FollowAlreadyCreatedException
                ))
                .andExpect(jsonPath("$.error").value(errors.get(0)))
                .andReturn();
        // Visualizzo l'errore
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n" + resultAsString);
        log.info("Resolved Error ---> " + result.getResolvedException());
    }




}