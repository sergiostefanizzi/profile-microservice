package com.sergiostefanizzi.profilemicroservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sergiostefanizzi.profilemicroservice.model.Profile;
import com.sergiostefanizzi.profilemicroservice.service.ProfilesService;
import com.sergiostefanizzi.profilemicroservice.system.exception.ProfileAlreadyCreatedException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.validator.routines.UrlValidator;
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
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.in;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(ProfilesController.class)
@ActiveProfiles("test")
@Slf4j
class ProfilesControllerTest {
    @MockBean
    private ProfilesService profilesService;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    private Profile newProfile;
    private Profile savedProfile;
    String profileName = "giuseppe_verdi";
    Boolean isPrivate = false;
    Long accountId = 1L;
    String bio = "This is Giuseppe's profile!";
    String pictureUrl = "https://upload.wikimedia.org/wikipedia/commons/7/7e/Circle-icons-profile.svg";
    Long profileId = 12L;
    String newAccountJson;
    private UrlValidator urlValidator;

    @BeforeEach
    void setUp() throws Exception{
        this.newProfile = new Profile(profileName,isPrivate,accountId);
        this.newProfile.setBio(bio);
        this.newProfile.setPictureUrl(pictureUrl);

        newAccountJson = this.objectMapper.writeValueAsString(this.newProfile);

        this.savedProfile = new Profile(profileName,isPrivate,accountId);
        this.savedProfile.setBio(bio);
        this.savedProfile.setPictureUrl(pictureUrl);
        this.savedProfile.setId(profileId);

        urlValidator = new UrlValidator();
    }

    @AfterEach
    void tearDown(){
    }

    @Test
    void testAddProfile_Then_201() throws Exception {
        when(this.profilesService.save(this.newProfile)).thenReturn(this.savedProfile);

        MvcResult result = this.mockMvc.perform(post("/profiles")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(newAccountJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.*", hasSize(6)))
                .andExpect(jsonPath("$.id").value(this.savedProfile.getId()))
                .andExpect(jsonPath("$.profile_name").value(this.savedProfile.getProfileName()))
                .andExpect(jsonPath("$.bio").value(this.savedProfile.getBio()))
                .andExpect(jsonPath("$.picture_url").value(this.savedProfile.getPictureUrl().toString()))
                .andExpect(jsonPath("$.is_private").value(this.savedProfile.getIsPrivate()))
                .andExpect(jsonPath("$.account_id").value(this.savedProfile.getAccountId())).andReturn();
        // salvo risposta in result per visualizzarla ma anche per controllare la validita' dell'url
        String resultAsString = result.getResponse().getContentAsString();
        Profile profileResult = this.objectMapper.readValue(resultAsString, Profile.class);

        assertTrue(this.urlValidator.isValid(profileResult.getPictureUrl().toString()));

        log.info("Profile Id ---> "+ profileResult.getId());
        log.info("Profile Name ---> "+ profileResult.getProfileName());
        log.info("Picture Bio ---> "+ profileResult.getBio());
        log.info("Profile Picture Url ---> "+ profileResult.getPictureUrl());
        log.info("Profile Private ---> "+ profileResult.getIsPrivate());
        log.info("Profile Account Id ---> "+ profileResult.getAccountId());
    }

    @Test
    void testAddProfile_RequiredFields_Then_201() throws Exception {
        // imposto a null i campi non richiesti
        this.newProfile.setBio(null);
        this.newProfile.setPictureUrl(null);
        this.savedProfile.setBio(null);
        this.savedProfile.setPictureUrl(null);

        newAccountJson = this.objectMapper.writeValueAsString(this.newProfile);

        when(this.profilesService.save(this.newProfile)).thenReturn(this.savedProfile);

        MvcResult result = this.mockMvc.perform(post("/profiles")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(newAccountJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.*", hasSize(4)))
                .andExpect(jsonPath("$.id").value(this.savedProfile.getId()))
                .andExpect(jsonPath("$.profile_name").value(this.savedProfile.getProfileName()))
                .andExpect(jsonPath("$.is_private").value(this.savedProfile.getIsPrivate()))
                .andExpect(jsonPath("$.account_id").value(this.savedProfile.getAccountId())).andReturn();
        // salvo risposta in result solo per visualizzarla
        String resultAsString = result.getResponse().getContentAsString();
        Profile profileResult = this.objectMapper.readValue(resultAsString, Profile.class);

        log.info("Profile Id ---> "+ profileResult.getId());
        log.info("Profile Name ---> "+ profileResult.getProfileName());
        log.info("Picture Bio ---> "+ profileResult.getBio());
        log.info("Profile Picture Url ---> "+ profileResult.getPictureUrl());
        log.info("Profile Private ---> "+ profileResult.getIsPrivate());
        log.info("Profile Account Id ---> "+ profileResult.getAccountId());
    }

    @Test
    void testAddProfile_MissingRequired_Then_400() throws Exception{
        // arrayList contenente i messaggii di errore
        List<String> errors = new ArrayList<>();
        errors.add("profileName must not be null");
        errors.add("isPrivate must not be null");
        errors.add("accountId must not be null");

        // Imposto a null i campi richiesti
        this.newProfile.setProfileName(null);
        this.newProfile.setIsPrivate(null);
        this.newProfile.setAccountId(null);

        newAccountJson = this.objectMapper.writeValueAsString(this.newProfile);
        log.info("New Account Json "+newAccountJson);

        MvcResult result = this.mockMvc.perform(post("/profiles")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(newAccountJson))
                .andExpect(status().isBadRequest())
                .andExpect(res -> assertTrue(
                        res.getResolvedException() instanceof MethodArgumentNotValidException)
                )
                .andExpect(jsonPath("$.error").isArray())
                .andExpect(jsonPath("$.error", hasSize(3)))
                .andExpect(jsonPath("$.error[0]").value(in(errors)))
                .andExpect(jsonPath("$.error[1]").value(in(errors)))
                .andExpect(jsonPath("$.error[2]").value(in(errors))).andReturn();
        // salvo risposta in result solo per visualizzarla
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n"+resultAsString);
    }

    @Test
    void testAddProfile_ProfileNameFormat_Then_400() throws Exception{
        List<String> errors = new ArrayList<>();
        errors.add("profileName size must be between 8 and 20");
        errors.add("profileName must match \"^(?=.{8,20}$)(?![_.])(?!.*[_.]{2})[a-zA-Z0-9._]+(?<![_.])$\"");

        this.newProfile.setProfileName("g_v");

        newAccountJson = this.objectMapper.writeValueAsString(this.newProfile);
        log.info("New Account Json "+newAccountJson);

        MvcResult result = this.mockMvc.perform(post("/profiles")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newAccountJson))
                .andExpect(status().isBadRequest())
                .andExpect(res -> assertTrue(
                        res.getResolvedException() instanceof MethodArgumentNotValidException)
                )
                .andExpect(jsonPath("$.error").isArray())
                .andExpect(jsonPath("$.error", hasSize(2)))
                .andExpect(jsonPath("$.error[0]").value(in(errors)))
                .andExpect(jsonPath("$.error[1]").value(in(errors))).andReturn();
        // salvo risposta in result solo per visualizzarla
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n"+resultAsString);
    }

    @Test
    void testAddProfile_BioLength_Then_400() throws Exception{
        String error = "bio size must be between 0 and 150";

        this.newProfile.setBio("Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Aenean commodo ligula eget dolor. Aenean massa. Cum sociis natoque penatibus et magnis dis parturient.");

        newAccountJson = this.objectMapper.writeValueAsString(this.newProfile);
        log.info("New Account Json "+newAccountJson);

        MvcResult result = this.mockMvc.perform(post("/profiles")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newAccountJson))
                .andExpect(status().isBadRequest())
                .andExpect(res -> assertTrue(
                        res.getResolvedException() instanceof MethodArgumentNotValidException)
                )
                .andExpect(jsonPath("$.error").isArray())
                .andExpect(jsonPath("$.error", hasSize(1)))
                .andExpect(jsonPath("$.error[0]").value(error)).andReturn();
        // salvo risposta in result solo per visualizzarla
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n"+resultAsString);
    }

    //TODO:continuare
    @Test
    void testAddProfile_InvalidPictureUrl_Then_400() throws Exception{
        this.newProfile.setPictureUrl("htt://upload.wikimedia.org/wikipedia/commons/7/7e/Circle-icons-profile.svg");
        newAccountJson = this.objectMapper.writeValueAsString(this.newProfile);
        when(this.profilesService.save(this.newProfile)).thenReturn(this.savedProfile);



        MvcResult result = this.mockMvc.perform(post("/profiles")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newAccountJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isArray()).andReturn();
        // salvo risposta in result per visualizzarla ma anche per controllare la validita' dell'url
        String resultAsString = result.getResponse().getContentAsString();

    }

    @Test
    void testAddProfile_ProfileNameExists_Then_409() throws Exception {
        when(this.profilesService.save(this.newProfile)).thenThrow(
                new ProfileAlreadyCreatedException(this.newProfile.getProfileName())
        );
        log.info("New Account Json "+newAccountJson);
        this.mockMvc.perform(post("/profiles")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(newAccountJson))
                .andExpect(status().isConflict())
                .andExpect(result -> assertTrue(
                        result.getResolvedException() instanceof ProfileAlreadyCreatedException))
                .andExpect(jsonPath("$.error").value("Conflict! Profile with name "+profileName+" already created!"));
    }


}