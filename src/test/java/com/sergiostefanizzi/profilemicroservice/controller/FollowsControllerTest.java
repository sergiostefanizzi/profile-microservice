package com.sergiostefanizzi.profilemicroservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sergiostefanizzi.profilemicroservice.model.Follows;
import com.sergiostefanizzi.profilemicroservice.model.Profile;
import com.sergiostefanizzi.profilemicroservice.model.ProfileFollowList;
import com.sergiostefanizzi.profilemicroservice.repository.AlertsRepository;
import com.sergiostefanizzi.profilemicroservice.repository.CommentsRepository;
import com.sergiostefanizzi.profilemicroservice.repository.PostsRepository;
import com.sergiostefanizzi.profilemicroservice.repository.ProfilesRepository;
import com.sergiostefanizzi.profilemicroservice.service.FollowsService;
import com.sergiostefanizzi.profilemicroservice.system.exception.FollowItselfException;
import com.sergiostefanizzi.profilemicroservice.system.exception.FollowNotFoundException;
import com.sergiostefanizzi.profilemicroservice.system.exception.ProfileNotFoundException;
import com.sergiostefanizzi.profilemicroservice.system.exception.UnfollowOnCreationException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(FollowsController.class)
@ActiveProfiles("test")
@Slf4j
class FollowsControllerTest {
    /*
    @MockBean
    private FollowsService followsService;
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private ProfilesRepository profilesRepository;
    @MockBean
    private PostsRepository postsRepository;
    @MockBean
    private CommentsRepository commentsRepository;
    @MockBean
    private AlertsRepository alertsRepository;
    @Autowired
    private ObjectMapper objectMapper;
    Long publicProfileId1 = 1L;
    Long publicProfileId2 = 2L;
    Long privateProfileId = 3L;
    Long invalidProfileId = Long.MIN_VALUE;
    List<String> errors;

    @BeforeEach
    void setUp() {
        errors = new ArrayList<>();
    }

    @AfterEach
    void tearDown() {
        errors.clear();
    }

    @Test
    void testAddFollows_PublicProfile_Return_ACCEPTED_Then_200() throws Exception {
        Follows followsRequest = new Follows(publicProfileId1, publicProfileId2, Follows.RequestStatusEnum.ACCEPTED);

        when(this.profilesRepository.checkActiveById(publicProfileId1)).thenReturn(Optional.of(publicProfileId1));
        when(this.profilesRepository.checkActiveById(publicProfileId2)).thenReturn(Optional.of(publicProfileId2));
        when(this.followsService.addFollows(anyLong(), anyLong(),anyBoolean())).thenReturn(followsRequest);


        MvcResult result = this.mockMvc.perform(put("/profiles/{profileId}/follows/{followsId}?unfollow={unfollow}",publicProfileId1,publicProfileId2,false)
                        .contentType(MediaType.APPLICATION_JSON)
                        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.*", hasSize(3)))
                .andExpect(jsonPath("$.follower_id").value(followsRequest.getFollowerId()))
                .andExpect(jsonPath("$.followed_id").value(followsRequest.getFollowedId()))
                .andExpect(jsonPath("$.request_status").value(followsRequest.getRequestStatus().getValue()))
                .andReturn();

        verify(this.profilesRepository, times(2)).checkActiveById(anyLong());

        // salvo risposta in result per visualizzarla
        String resultAsString = result.getResponse().getContentAsString();
        Follows followsResult = this.objectMapper.readValue(resultAsString, Follows.class);

        log.info(followsResult.toString());
    }

    @Test
    void testAddFollows_PrivateProfile_Return_Pending_Then_200() throws Exception {
        Follows followsRequest = new Follows(publicProfileId1, privateProfileId, Follows.RequestStatusEnum.PENDING);
        when(this.profilesRepository.checkActiveById(publicProfileId1)).thenReturn(Optional.of(publicProfileId1));
        when(this.profilesRepository.checkActiveById(privateProfileId)).thenReturn(Optional.of(privateProfileId));
        when(this.followsService.addFollows(anyLong(), anyLong(), anyBoolean())).thenReturn(followsRequest);

        MvcResult result = this.mockMvc.perform(put("/profiles/{profileId}/follows/{followsId}?unfollow={unfollow}",publicProfileId1,privateProfileId,false)
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.*", hasSize(3)))
                .andExpect(jsonPath("$.follower_id").value(followsRequest.getFollowerId()))
                .andExpect(jsonPath("$.followed_id").value(followsRequest.getFollowedId()))
                .andExpect(jsonPath("$.request_status").value(followsRequest.getRequestStatus().getValue()))
                .andReturn();

        // salvo risposta in result per visualizzarla
        String resultAsString = result.getResponse().getContentAsString();
        Follows followsResult = this.objectMapper.readValue(resultAsString, Follows.class);

        log.info(followsResult.toString());
    }

    @Test
    void testAddFollows_Unfollow_Return_Rejected_Then_200() throws Exception {
        Follows followsRequest = new Follows(publicProfileId1, privateProfileId, Follows.RequestStatusEnum.REJECTED);
        when(this.profilesRepository.checkActiveById(publicProfileId1)).thenReturn(Optional.of(publicProfileId1));
        when(this.profilesRepository.checkActiveById(privateProfileId)).thenReturn(Optional.of(privateProfileId));
        when(this.followsService.addFollows(anyLong(), anyLong(), anyBoolean())).thenReturn(followsRequest);

        MvcResult result = this.mockMvc.perform(put("/profiles/{profileId}/follows/{followsId}?unfollow={unfollow}",publicProfileId1,privateProfileId,true)
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.*", hasSize(3)))
                .andExpect(jsonPath("$.follower_id").value(followsRequest.getFollowerId()))
                .andExpect(jsonPath("$.followed_id").value(followsRequest.getFollowedId()))
                .andExpect(jsonPath("$.request_status").value(followsRequest.getRequestStatus().getValue()))
                .andReturn();

        // salvo risposta in result per visualizzarla
        String resultAsString = result.getResponse().getContentAsString();
        Follows followsResult = this.objectMapper.readValue(resultAsString, Follows.class);

        log.info(followsResult.toString());
    }

    @Test
    void testAddFollows_FollowItself_Then_400() throws Exception {
        errors.add("Profile cannot follow itself!");
        Follows followsRequest = new Follows(publicProfileId1, publicProfileId1, Follows.RequestStatusEnum.ACCEPTED);

        when(this.profilesRepository.checkActiveById(publicProfileId1)).thenReturn(Optional.of(publicProfileId1));
        when(this.profilesRepository.checkActiveById(publicProfileId1)).thenReturn(Optional.of(publicProfileId1));


        MvcResult result = this.mockMvc.perform(put("/profiles/{profileId}/follows/{followsId}?unfollow={unfollow}",publicProfileId1,publicProfileId1,false)
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isBadRequest())
                .andExpect(res -> assertTrue(
                        res.getResolvedException() instanceof FollowItselfException
                ))
                .andExpect(jsonPath("$.error").value(errors.get(0)))
                .andReturn();

        verify(this.profilesRepository, times(2)).checkActiveById(anyLong());

        // Visualizzo l'errore
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n" + resultAsString);
        log.info("Resolved Error ---> " + result.getResolvedException());


    }

    @Test
    void testAddFollows_UnfollowOnCreation_Then_400() throws Exception {
        // arrayList contenente i messaggi di errore
        errors.add("Unfollows on creation is not possible!");
        when(this.profilesRepository.checkActiveById(publicProfileId1)).thenReturn(Optional.of(publicProfileId1));
        when(this.profilesRepository.checkActiveById(publicProfileId2)).thenReturn(Optional.of(publicProfileId2));
        when(this.followsService.addFollows(anyLong(),anyLong(), anyBoolean())).thenThrow(new UnfollowOnCreationException());

        MvcResult result = this.mockMvc.perform(put("/profiles/{profileId}/follows/{followsId}?unfollow={unfollow}",publicProfileId1,publicProfileId2,true)
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isBadRequest())
                .andExpect(res -> assertTrue(
                        res.getResolvedException() instanceof UnfollowOnCreationException
                ))
                .andExpect(jsonPath("$.error").value(errors.get(0)))
                .andReturn();
        // Visualizzo l'errore
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n" + resultAsString);
        log.info("Resolved Error ---> " + result.getResolvedException());
    }


    @Test
    void testAddFollows_InvalidId_Then_400() throws Exception {
        // arrayList contenente i messaggi di errore
        errors.add("ID is not valid!");

        MvcResult result = this.mockMvc.perform(put("/profiles/{profileId}/follows/{followsId}","IdNotLong","IdNotLong")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(res -> assertTrue(
                        res.getResolvedException() instanceof NumberFormatException
                ))
                .andExpect(jsonPath("$.error").value(errors.get(0))).andReturn();
        // Visualizzo l'errore
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n" + resultAsString);
        log.info("Resolved Error ---> " + result.getResolvedException());
    }

    //TODO 401


    @Test
    void testAddFollows_ProfileToFollowNotFound_Then_404() throws Exception {
        // arrayList contenente i messaggi di errore
        errors.add("Profile " + invalidProfileId + " not found!");
        when(this.profilesRepository.checkActiveById(publicProfileId1)).thenReturn(Optional.of(publicProfileId1));
        when(this.profilesRepository.checkActiveById(invalidProfileId)).thenReturn(Optional.empty());
        doThrow(new ProfileNotFoundException(invalidProfileId)).when(this.followsService).addFollows(anyLong(),anyLong(),anyBoolean());


        MvcResult result = this.mockMvc.perform(put("/profiles/{profileId}/follows/{followsId}?unfollow={unfollow}",publicProfileId1,invalidProfileId,false)
                        .contentType(MediaType.APPLICATION_JSON)
                )
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
    void testAddFollows_ProfileNotFound_Then_404() throws Exception {
        // arrayList contenente i messaggi di errore
        errors.add("Profile " + invalidProfileId + " not found!");
        when(this.profilesRepository.checkActiveById(invalidProfileId)).thenReturn(Optional.empty());
        when(this.profilesRepository.checkActiveById(publicProfileId1)).thenReturn(Optional.of(publicProfileId1));
        doThrow(new ProfileNotFoundException(invalidProfileId)).when(this.followsService).addFollows(anyLong(),anyLong(),anyBoolean());


        MvcResult result = this.mockMvc.perform(put("/profiles/{profileId}/follows/{followsId}?unfollow={unfollow}",invalidProfileId,publicProfileId1,false)
                        .contentType(MediaType.APPLICATION_JSON)
                )
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
    void testAcceptFollows_Then_200() throws Exception {
        Follows followsRequest = new Follows(publicProfileId1, privateProfileId, Follows.RequestStatusEnum.ACCEPTED);
        when(this.profilesRepository.checkActiveById(privateProfileId)).thenReturn(Optional.of(privateProfileId));
        when(this.profilesRepository.checkActiveById(publicProfileId1)).thenReturn(Optional.of(publicProfileId1));
        when(this.followsService.acceptFollows(anyLong(), anyLong(), anyBoolean())).thenReturn(followsRequest);

        MvcResult result = this.mockMvc.perform(put("/profiles/{profileId}/followedBy/{followsId}?rejectFollow={rejectFollow}",
                        privateProfileId, publicProfileId1,false)
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.*", hasSize(3)))
                .andExpect(jsonPath("$.follower_id").value(followsRequest.getFollowerId()))
                .andExpect(jsonPath("$.followed_id").value(followsRequest.getFollowedId()))
                .andExpect(jsonPath("$.request_status").value(followsRequest.getRequestStatus().getValue()))
                .andReturn();

        // salvo risposta in result per visualizzarla
        String resultAsString = result.getResponse().getContentAsString();
        Follows followsResult = this.objectMapper.readValue(resultAsString, Follows.class);

        log.info(followsResult.toString());
    }

    @Test
    void testAcceptFollows_Reject_Then_200() throws Exception {
        Follows followsRequest = new Follows(publicProfileId1, privateProfileId, Follows.RequestStatusEnum.REJECTED);
        when(this.profilesRepository.checkActiveById(privateProfileId)).thenReturn(Optional.of(privateProfileId));
        when(this.profilesRepository.checkActiveById(publicProfileId1)).thenReturn(Optional.of(publicProfileId1));
        when(this.followsService.acceptFollows(anyLong(), anyLong(), anyBoolean())).thenReturn(followsRequest);

        MvcResult result = this.mockMvc.perform(put("/profiles/{profileId}/followedBy/{followsId}?rejectFollow={rejectFollow}",
                        privateProfileId, publicProfileId1,true)
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.*", hasSize(3)))
                .andExpect(jsonPath("$.follower_id").value(followsRequest.getFollowerId()))
                .andExpect(jsonPath("$.followed_id").value(followsRequest.getFollowedId()))
                .andExpect(jsonPath("$.request_status").value(followsRequest.getRequestStatus().getValue()))
                .andReturn();

        // salvo risposta in result per visualizzarla
        String resultAsString = result.getResponse().getContentAsString();
        Follows followsResult = this.objectMapper.readValue(resultAsString, Follows.class);

        log.info(followsResult.toString());
    }

    @Test
    void testAcceptFollows_InvalidId_Then_400() throws Exception {
        // arrayList contenente i messaggi di errore
        errors.add("ID is not valid!");
        MvcResult result = this.mockMvc.perform(put("/profiles/{profileId}/followedBy/{followerId}","IdNotLong","IdNotLong")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(res -> assertTrue(
                        res.getResolvedException() instanceof NumberFormatException
                ))
                .andExpect(jsonPath("$.error").value(errors.get(0))).andReturn();
        // Visualizzo l'errore
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n" + resultAsString);
        log.info("Resolved Error ---> " + result.getResolvedException());
    }

    //TODO 401, 403


    @Test
    void testAcceptFollows_FollowNotFound_Then_404() throws Exception {
        // arrayList contenente i messaggi di errore
        errors.add("Follows not found!");
        when(this.profilesRepository.checkActiveById(privateProfileId)).thenReturn(Optional.of(privateProfileId));
        when(this.profilesRepository.checkActiveById(publicProfileId1)).thenReturn(Optional.of(publicProfileId1));
        doThrow(new FollowNotFoundException()).when(this.followsService).acceptFollows(anyLong(), anyLong(),anyBoolean());


        MvcResult result = this.mockMvc.perform(put("/profiles/{profileId}/followedBy/{followsId}?rejectFollow={rejectFollow}",privateProfileId,publicProfileId1,false)
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isNotFound())
                .andExpect(res -> assertTrue(
                        res.getResolvedException() instanceof FollowNotFoundException
                ))
                .andExpect(jsonPath("$.error").value(errors.get(0)))
                .andReturn();
        // Visualizzo l'errore
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n" + resultAsString);
        log.info("Resolved Error ---> " + result.getResolvedException());
    }

    @Test
    void testFindAllFollowers_Then_200() throws Exception {
        Profile publicProfile2 = new Profile("pinco_pallino",false,2L);
        publicProfile2.setId(2L);
        Profile privateProfile = new Profile("pinco_pallino2",false,3L);
        privateProfile.setId(3L);
        List<Profile> followerList = asList(publicProfile2, privateProfile);
        ProfileFollowList returnedProfileFollowList = new ProfileFollowList(followerList, followerList.size());

        when(this.profilesRepository.checkActiveById(publicProfileId1)).thenReturn(Optional.of(publicProfileId1));
        when(this.followsService.findAllFollowers(anyLong())).thenReturn(returnedProfileFollowList);

        MvcResult result = this.mockMvc.perform(get("/profiles/{profileId}/followedBy",publicProfileId1)
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.*", hasSize(2)))
                .andExpect(jsonPath("$.profiles").isArray())
                .andExpect(jsonPath("$.profiles", hasSize(2)))
                .andExpect(jsonPath("$.profiles[0].id").value(followerList.get(0).getId()))
                .andExpect(jsonPath("$.profiles[0].profile_name").value(followerList.get(0).getProfileName()))
                .andExpect(jsonPath("$.profiles[0].bio").doesNotExist())
                .andExpect(jsonPath("$.profiles[0].picture_url").doesNotExist())
                .andExpect(jsonPath("$.profiles[0].is_private").value(followerList.get(0).getIsPrivate()))
                .andExpect(jsonPath("$.profiles[0].account_id").value(followerList.get(0).getAccountId()))
                .andExpect(jsonPath("$.profiles[1].id").value(followerList.get(1).getId()))
                .andExpect(jsonPath("$.profiles[1].profile_name").value(followerList.get(1).getProfileName()))
                .andExpect(jsonPath("$.profiles[1].bio").doesNotExist())
                .andExpect(jsonPath("$.profiles[1].picture_url").doesNotExist())
                .andExpect(jsonPath("$.profiles[1].is_private").value(followerList.get(1).getIsPrivate()))
                .andExpect(jsonPath("$.profiles[1].account_id").value(followerList.get(1).getAccountId()))
                .andExpect(jsonPath("$.profile_count").value(followerList.size()))
                .andReturn();

        // salvo risposta in result per visualizzarla
        String resultAsString = result.getResponse().getContentAsString();
        ProfileFollowList followerListResult = this.objectMapper.readValue(resultAsString, ProfileFollowList.class);

        log.info(followerListResult.toString());
    }

    @Test
    void testFindAllFollowers_NoFollowers_Then_200() throws Exception {
        List<Profile> followerList = Collections.emptyList();
        ProfileFollowList returnedProfileFollowList = new ProfileFollowList(followerList, 0);

        when(this.profilesRepository.checkActiveById(publicProfileId1)).thenReturn(Optional.of(publicProfileId1));
        when(this.followsService.findAllFollowers(anyLong())).thenReturn(returnedProfileFollowList);

        MvcResult result = this.mockMvc.perform(get("/profiles/{profileId}/followedBy",publicProfileId1)
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.*", hasSize(2)))
                .andExpect(jsonPath("$.profiles").isArray())
                .andExpect(jsonPath("$.profiles").isEmpty())
                .andExpect(jsonPath("$.profile_count").value(0))
                .andReturn();

        // salvo risposta in result per visualizzarla
        String resultAsString = result.getResponse().getContentAsString();
        ProfileFollowList followerListResult = this.objectMapper.readValue(resultAsString, ProfileFollowList.class);

        log.info(followerListResult.toString());
    }

    @Test
    void testFindAllFollowers_ProfileNotGranted_Then_200() throws Exception {
        List<Profile> followerList = Collections.emptyList();
        ProfileFollowList returnedProfileFollowList = new ProfileFollowList(followerList, 50);

        when(this.profilesRepository.checkActiveById(publicProfileId1)).thenReturn(Optional.of(publicProfileId1));
        when(this.followsService.findAllFollowers(anyLong())).thenReturn(returnedProfileFollowList);

        MvcResult result = this.mockMvc.perform(get("/profiles/{profileId}/followedBy",publicProfileId1)
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.*", hasSize(2)))
                .andExpect(jsonPath("$.profiles").isArray())
                .andExpect(jsonPath("$.profiles").isEmpty())
                .andExpect(jsonPath("$.profile_count").value(50))
                .andReturn();

        // salvo risposta in result per visualizzarla
        String resultAsString = result.getResponse().getContentAsString();
        ProfileFollowList followerListResult = this.objectMapper.readValue(resultAsString, ProfileFollowList.class);

        log.info(followerListResult.toString());
    }

    @Test
    void testFindAllFollowers_Then_400() throws Exception {
        // arrayList contenente i messaggi di errore
        errors.add("ID is not valid!");
        MvcResult result = this.mockMvc.perform(get("/profiles/{profileId}/followedBy","IdNotLong")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(res -> assertTrue(
                        res.getResolvedException() instanceof NumberFormatException
                ))
                .andExpect(jsonPath("$.error").value(errors.get(0))).andReturn();
        // Visualizzo l'errore
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n" + resultAsString);
        log.info("Resolved Error ---> " + result.getResolvedException());
    }

    //TODO 401


    @Test
    void testFindAllFollowers_ProfileNotFound_Then_404() throws Exception {
        // arrayList contenente i messaggi di errore
        errors.add("Profile " + publicProfileId1 + " not found!");
        when(this.profilesRepository.checkActiveById(publicProfileId1)).thenReturn(Optional.empty());
        doThrow(new ProfileNotFoundException(publicProfileId1)).when(this.followsService).findAllFollowers(anyLong());


        MvcResult result = this.mockMvc.perform(get("/profiles/{profileId}/followedBy",publicProfileId1)
                        .contentType(MediaType.APPLICATION_JSON)
                )
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
    void testFindAllFollowings_Then_200() throws Exception {
        Profile publicProfile2 = new Profile("pinco_pallino",false,2L);
        publicProfile2.setId(2L);
        Profile privateProfile = new Profile("pinco_pallino2",false,3L);
        privateProfile.setId(3L);
        List<Profile> followingsList = asList(publicProfile2, privateProfile);
        ProfileFollowList returnedProfileFollowList = new ProfileFollowList(followingsList, followingsList.size());

        when(this.profilesRepository.checkActiveById(publicProfileId1)).thenReturn(Optional.of(publicProfileId1));
        when(this.followsService.findAllFollowings(anyLong())).thenReturn(returnedProfileFollowList);

        MvcResult result = this.mockMvc.perform(get("/profiles/{profileId}/follows",publicProfileId1)
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.*", hasSize(2)))
                .andExpect(jsonPath("$.profiles").isArray())
                .andExpect(jsonPath("$.profiles", hasSize(2)))
                .andExpect(jsonPath("$.profiles[0].id").value(followingsList.get(0).getId()))
                .andExpect(jsonPath("$.profiles[0].profile_name").value(followingsList.get(0).getProfileName()))
                .andExpect(jsonPath("$.profiles[0].bio").doesNotExist())
                .andExpect(jsonPath("$.profiles[0].picture_url").doesNotExist())
                .andExpect(jsonPath("$.profiles[0].is_private").value(followingsList.get(0).getIsPrivate()))
                .andExpect(jsonPath("$.profiles[0].account_id").value(followingsList.get(0).getAccountId()))
                .andExpect(jsonPath("$.profiles[1].id").value(followingsList.get(1).getId()))
                .andExpect(jsonPath("$.profiles[1].profile_name").value(followingsList.get(1).getProfileName()))
                .andExpect(jsonPath("$.profiles[1].bio").doesNotExist())
                .andExpect(jsonPath("$.profiles[1].picture_url").doesNotExist())
                .andExpect(jsonPath("$.profiles[1].is_private").value(followingsList.get(1).getIsPrivate()))
                .andExpect(jsonPath("$.profiles[1].account_id").value(followingsList.get(1).getAccountId()))
                .andExpect(jsonPath("$.profile_count").value(followingsList.size()))
                .andReturn();

        // salvo risposta in result per visualizzarla
        String resultAsString = result.getResponse().getContentAsString();
        ProfileFollowList followerListResult = this.objectMapper.readValue(resultAsString, ProfileFollowList.class);

        log.info(followerListResult.toString());
    }

    @Test
    void testFindAllFollowings_NoFollowings_Then_200() throws Exception {
        List<Profile> followingList = Collections.emptyList();
        ProfileFollowList returnedProfileFollowList = new ProfileFollowList(followingList, 0);

        when(this.profilesRepository.checkActiveById(publicProfileId1)).thenReturn(Optional.of(publicProfileId1));
        when(this.followsService.findAllFollowings(anyLong())).thenReturn(returnedProfileFollowList);

        MvcResult result = this.mockMvc.perform(get("/profiles/{profileId}/follows",publicProfileId1)
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.*", hasSize(2)))
                .andExpect(jsonPath("$.profiles").isArray())
                .andExpect(jsonPath("$.profiles").isEmpty())
                .andExpect(jsonPath("$.profile_count").value(0))
                .andReturn();

        // salvo risposta in result per visualizzarla
        String resultAsString = result.getResponse().getContentAsString();
        ProfileFollowList followerListResult = this.objectMapper.readValue(resultAsString, ProfileFollowList.class);

        log.info(followerListResult.toString());
    }

    @Test
    void testFindAllFollowings_ProfileNotGranted_Then_200() throws Exception {
        List<Profile> followingList = Collections.emptyList();
        ProfileFollowList returnedProfileFollowList = new ProfileFollowList(followingList, 50);

        when(this.profilesRepository.checkActiveById(publicProfileId1)).thenReturn(Optional.of(publicProfileId1));
        when(this.followsService.findAllFollowings(anyLong())).thenReturn(returnedProfileFollowList);

        MvcResult result = this.mockMvc.perform(get("/profiles/{profileId}/follows",publicProfileId1)
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.*", hasSize(2)))
                .andExpect(jsonPath("$.profiles").isArray())
                .andExpect(jsonPath("$.profiles").isEmpty())
                .andExpect(jsonPath("$.profile_count").value(50))
                .andReturn();

        // salvo risposta in result per visualizzarla
        String resultAsString = result.getResponse().getContentAsString();
        ProfileFollowList followerListResult = this.objectMapper.readValue(resultAsString, ProfileFollowList.class);

        log.info(followerListResult.toString());
    }

    @Test
    void testFindAllFollowings_Then_400() throws Exception {
        // arrayList contenente i messaggi di errore
        errors.add("ID is not valid!");
        MvcResult result = this.mockMvc.perform(get("/profiles/{profileId}/follows","IdNotLong")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(res -> assertTrue(
                        res.getResolvedException() instanceof NumberFormatException
                ))
                .andExpect(jsonPath("$.error").value(errors.get(0))).andReturn();
        // Visualizzo l'errore
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n" + resultAsString);
        log.info("Resolved Error ---> " + result.getResolvedException());
    }

    //TODO 401


    @Test
    void testFindAllFollowings_ProfileNotFound_Then_404() throws Exception {
        // arrayList contenente i messaggi di errore
        errors.add("Profile " + publicProfileId1 + " not found!");

        when(this.profilesRepository.checkActiveById(publicProfileId1)).thenReturn(Optional.empty());
        doThrow(new ProfileNotFoundException(publicProfileId1)).when(this.followsService).findAllFollowings(anyLong());


        MvcResult result = this.mockMvc.perform(get("/profiles/{profileId}/follows",publicProfileId1)
                        .contentType(MediaType.APPLICATION_JSON)
                )
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

     */


}