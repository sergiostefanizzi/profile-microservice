package com.sergiostefanizzi.profilemicroservice.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sergiostefanizzi.profilemicroservice.model.*;
import com.sergiostefanizzi.profilemicroservice.repository.AlertsRepository;
import com.sergiostefanizzi.profilemicroservice.repository.CommentsRepository;
import com.sergiostefanizzi.profilemicroservice.repository.PostsRepository;
import com.sergiostefanizzi.profilemicroservice.repository.ProfilesRepository;
import com.sergiostefanizzi.profilemicroservice.service.KeycloakService;
import com.sergiostefanizzi.profilemicroservice.service.ProfilesService;
import com.sergiostefanizzi.profilemicroservice.system.exception.NotInProfileListException;
import com.sergiostefanizzi.profilemicroservice.system.exception.ProfileAlreadyCreatedException;
import com.sergiostefanizzi.profilemicroservice.system.exception.ProfileNotFoundException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(ProfilesController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@Slf4j
class ProfilesControllerTest {

    @MockBean
    private ProfilesService profilesService;
    @MockBean
    private ProfilesRepository profilesRepository;
    @MockBean
    private PostsRepository postsRepository;
    @MockBean
    private CommentsRepository commentsRepository;
    @MockBean
    private AlertsRepository alertsRepository;
    @MockBean
    private KeycloakService keycloakService;
    @MockBean
    private SecurityContext securityContext;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    private Profile newProfile;
    private Profile savedProfile;
    String profileName = "giuseppe_verdi";
    Boolean isPrivate = false;
    Boolean updatedIsPrivate = true;
    String accountId = UUID.randomUUID().toString();
    String bio = "This is Giuseppe's profile!";
    String updatedBio = "New Giuseppe's bio";
    String pictureUrl = "https://upload.wikimedia.org/wikipedia/commons/7/7e/Circle-icons-profile.svg";
    String pictureUrlXSS = "http://www.example.com?d=<script type=\"javascript\" src=\"http://www.google.it\"/>"; //Cross site scripting XSS
    String updatedPictureUrl = "https://icons-for-free.com/iconfiles/png/512/avatar+person+profile+user+icon-1320086059654790795.png";
    Long profileId = 12L;
    String newProfileJson;
    String contentUrl = "https://upload.wikimedia.org/wikipedia/commons/9/9a/Cape_may.jpg";
    String caption = "This is the post caption";
    Post.PostTypeEnum postType = Post.PostTypeEnum.POST;
    private JwtAuthenticationToken jwtAuthenticationToken;
    private JwtAuthenticationToken jwtTokenError;

    ProfilesControllerTest() {
    }


    @BeforeEach
    void setUp() throws Exception{
        this.newProfile = new Profile(profileName,isPrivate);
        this.newProfile.setAccountId(accountId);
        this.newProfile.setBio(bio);
        this.newProfile.setPictureUrl(pictureUrl);

        newProfileJson = this.objectMapper.writeValueAsString(this.newProfile);

        this.savedProfile = new Profile(profileName,isPrivate);
        this.savedProfile.setAccountId(accountId);
        this.savedProfile.setBio(bio);
        this.savedProfile.setPictureUrl(pictureUrl);
        this.savedProfile.setId(profileId);

        SecurityContextHolder.setContext(this.securityContext);

        Map<String, Object> headers = new HashMap<>();
        headers.put("alg","HS256");
        headers.put("typ","JWT");
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", this.accountId);
        claims.put("profileList", List.of(profileId, 1L , 2L));
        Jwt jwt = new Jwt("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c",
                Instant.now(),
                Instant.MAX,
                headers,
                claims);

        Map<String, Object> errorClaims = new HashMap<>();
        errorClaims.put("sub", this.accountId);
        errorClaims.put("profileList", List.of(1L , 2L));
        Jwt jwtError = new Jwt("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c",
                Instant.now(),
                Instant.MAX,
                headers,
                errorClaims);

        this.jwtAuthenticationToken = new JwtAuthenticationToken(jwt);
        this.jwtTokenError = new JwtAuthenticationToken(jwtError);
    }


    @AfterEach
    void tearDown(){
    }

    private Profile getUpdatedProfile(ProfilePatch profilePatch) {
        Profile updatedProfile = new Profile(profileName, isPrivate);
        updatedProfile.setAccountId(accountId);
        updatedProfile.setId(profileId);
        updatedProfile.setBio(profilePatch.getBio() != null ? profilePatch.getBio() : bio);
        updatedProfile.setPictureUrl(profilePatch.getPictureUrl() != null ? profilePatch.getPictureUrl() : pictureUrl);
        updatedProfile.setIsPrivate(profilePatch.getIsPrivate() != null ? profilePatch.getIsPrivate() : isPrivate);
        return updatedProfile;
    }

    private FullProfile getFullProfile(Long targetProfileId, Profile convertedProfile) {
        Post newPost1 = new Post(contentUrl, postType, targetProfileId);
        newPost1.setCaption(caption);
        newPost1.setId(1L);

        Post newPost2 = new Post(contentUrl, postType, targetProfileId);
        newPost2.setCaption(caption);
        newPost1.setId(2L);


        List<Post> postList = new ArrayList<>();
        postList.add(newPost1);
        postList.add(newPost2);

        return new FullProfile(
                convertedProfile,
                postList,
                postList.size(),
                true
        );
    }

    @Test
    void testAddProfile_Then_201() throws Exception {
        when(this.profilesService.save(this.newProfile)).thenReturn(this.savedProfile);

        MvcResult result = this.mockMvc.perform(post("/profiles")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(newProfileJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.*", hasSize(6)))
                .andExpect(jsonPath("$.id").value(this.savedProfile.getId()))
                .andExpect(jsonPath("$.profile_name").value(this.savedProfile.getProfileName()))
                .andExpect(jsonPath("$.bio").value(this.savedProfile.getBio()))
                .andExpect(jsonPath("$.picture_url").value(this.savedProfile.getPictureUrl()))
                .andExpect(jsonPath("$.is_private").value(this.savedProfile.getIsPrivate()))
                .andExpect(jsonPath("$.account_id").value(this.savedProfile.getAccountId())).andReturn();
        // salvo risposta in result per visualizzarla
        String resultAsString = result.getResponse().getContentAsString();
        Profile profileResult = this.objectMapper.readValue(resultAsString, Profile.class);

        log.info(profileResult.toString());
    }


    @Test
    void testAddProfile_RequiredFields_Then_201() throws Exception {
        // imposto a null i campi non richiesti
        this.newProfile.setBio(null);
        this.newProfile.setPictureUrl(null);
        this.savedProfile.setBio(null);
        this.savedProfile.setPictureUrl(null);

        newProfileJson = this.objectMapper.writeValueAsString(this.newProfile);

        when(this.profilesService.save(this.newProfile)).thenReturn(this.savedProfile);

        MvcResult result = this.mockMvc.perform(post("/profiles")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(newProfileJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.*", hasSize(4)))
                .andExpect(jsonPath("$.id").value(this.savedProfile.getId()))
                .andExpect(jsonPath("$.profile_name").value(this.savedProfile.getProfileName()))
                .andExpect(jsonPath("$.is_private").value(this.savedProfile.getIsPrivate()))
                .andExpect(jsonPath("$.account_id").value(this.savedProfile.getAccountId())).andReturn();
        // salvo risposta in result solo per visualizzarla
        String resultAsString = result.getResponse().getContentAsString();
        Profile profileResult = this.objectMapper.readValue(resultAsString, Profile.class);

        log.info(profileResult.toString());
    }

    @Test
    void testAddProfile_MissingRequired_Then_400() throws Exception{
        // arrayList contenente i messaggi di errore
        List<String> errors = new ArrayList<>();
        errors.add("profileName must not be null");
        errors.add("isPrivate must not be null");

        // Imposto a null i campi richiesti
        this.newProfile.setProfileName(null);
        this.newProfile.setIsPrivate(null);
        this.newProfile.setAccountId(null);

        newProfileJson = this.objectMapper.writeValueAsString(this.newProfile);
        log.info("New Account Json "+newProfileJson);

        MvcResult result = this.mockMvc.perform(post("/profiles")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(newProfileJson))
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
    void testAddProfile_ProfileNameFormat_Then_400() throws Exception{
        List<String> errors = new ArrayList<>();
        errors.add("profileName size must be between 8 and 20");
        errors.add("profileName must match \"^(?=.{8,20}$)(?![_.])(?!.*[_.]{2})[a-zA-Z0-9._]+(?<![_.])$\"");

        this.newProfile.setProfileName("g_v");

        newProfileJson = this.objectMapper.writeValueAsString(this.newProfile);
        log.info("New Account Json "+newProfileJson);

        MvcResult result = this.mockMvc.perform(post("/profiles")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newProfileJson))
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

        this.newProfile.setBio(RandomStringUtils.randomAlphabetic(160));

        newProfileJson = this.objectMapper.writeValueAsString(this.newProfile);
        log.info("New Account Json "+newProfileJson);

        MvcResult result = this.mockMvc.perform(post("/profiles")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newProfileJson))
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

    @Test
    void testAddProfile_InvalidPictureUrl_Then_400() throws Exception{
        String error = "pictureUrl must be a valid URL";
        String invalidUrl = "https://upload.wikimedia.o/ "+RandomStringUtils.randomAscii(15);
        log.info("Invalid URL --> "+invalidUrl);
        this.newProfile.setPictureUrl(invalidUrl);

        newProfileJson = this.objectMapper.writeValueAsString(this.newProfile);

        MvcResult result = this.mockMvc.perform(post("/profiles")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newProfileJson))
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

    @Test
    void testAddProfile_InvalidPictureUrlXSS_Then_400() throws Exception{
        String error = "pictureUrl must be a valid URL";
        this.newProfile.setPictureUrl(pictureUrlXSS);
        newProfileJson = this.objectMapper.writeValueAsString(this.newProfile);

        MvcResult result = this.mockMvc.perform(post("/profiles")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newProfileJson))
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

    @Test
    void testAddProfile_InvalidIsPrivate_Then_400() throws Exception{
        JsonNode jsonNode = this.objectMapper.readTree(newProfileJson);

        ((ObjectNode) jsonNode).put("is_private", "FalseString");
        newProfileJson = this.objectMapper.writeValueAsString(jsonNode);

        MvcResult result = this.mockMvc.perform(post("/profiles")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(newProfileJson))
                .andExpect(status().isBadRequest())
                .andExpect(res -> assertTrue(
                        res.getResolvedException() instanceof HttpMessageNotReadableException
                ))
                .andExpect(jsonPath("$.error").value("Message is not readable")).andReturn();
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n"+resultAsString);
        log.info("Resolved Error ---> "+result.getResolvedException());
    }

    @Test
    void testAddProfile_ProfileNameExists_Then_409() throws Exception {
        when(this.profilesService.save(this.newProfile)).thenThrow(
                new ProfileAlreadyCreatedException(this.newProfile.getProfileName())
        );
        log.info("New Account Json "+newProfileJson);
        this.mockMvc.perform(post("/profiles")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(newProfileJson))
                .andExpect(status().isConflict())
                .andExpect(result -> assertTrue(
                        result.getResolvedException() instanceof ProfileAlreadyCreatedException))
                .andExpect(jsonPath("$.error").value("Conflict! Profile with name "+profileName+" already created!"));
    }

    @Test
    void testDeleteProfileById_Then_204() throws Exception{
        when(this.profilesRepository.checkActiveById(profileId)).thenReturn(Optional.of(profileId));
        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationToken);
        doNothing().when(this.profilesService).remove(profileId, profileId);

        this.mockMvc.perform(delete("/profiles/{profileId}",profileId))
                .andExpect(status().isNoContent());
   }

    @Test
    void testDeleteProfileById_Then_403() throws Exception{
        when(this.profilesRepository.checkActiveById(profileId)).thenReturn(Optional.of(profileId));
        when(this.securityContext.getAuthentication()).thenReturn(this.jwtTokenError);
        doNothing().when(this.profilesService).remove(profileId, profileId);

        MvcResult result = this.mockMvc.perform(delete("/profiles/{profileId}",profileId))
                .andExpect(status().isForbidden())
                .andExpect(res -> assertTrue(
                        res.getResolvedException() instanceof NotInProfileListException
                ))
                .andExpect(jsonPath("$.error").value("Profile "+profileId+" is not inside the profile list!"))
                .andReturn();
        // Visualizzo l'errore
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n"+resultAsString);
        log.info("Resolved Error ---> "+result.getResolvedException());
    }


    @Test
    void testDeleteProfileById_Then_404() throws Exception {
        Long invalidProfileId = Long.MAX_VALUE;
        doThrow(new ProfileNotFoundException(invalidProfileId)).when(this.profilesService).remove(invalidProfileId, profileId);

        MvcResult result = this.mockMvc.perform(delete("/profiles/{profileId}",invalidProfileId))
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

    @Test
    void testUpdateProfile_Then_200() throws Exception{
        ProfileJpa profileJpa = new ProfileJpa(profileName, isPrivate, accountId);
        profileJpa.setBio(bio);
        profileJpa.setPictureUrl(pictureUrl);
        profileJpa.setId(profileId);
        profileJpa.setCreatedAt(LocalDateTime.MIN);

        // Definisco un o piu' campi del profilo da aggiornare tramite l'oggetto ProfilePatch
        ProfilePatch profilePatch = new ProfilePatch();
        profilePatch.setBio(updatedBio);
        profilePatch.setPictureUrl(updatedPictureUrl);
        profilePatch.setIsPrivate(updatedIsPrivate);
        
        String profilePatchJson = this.objectMapper.writeValueAsString(profilePatch);

        // Aggiorno il profilo che verra' restituito dal service con i nuovi valori
        Profile updatedProfile = getUpdatedProfile(profilePatch);


        when(this.profilesService.update(profileId, profileId, profilePatch)).thenReturn(updatedProfile);
        when(this.profilesRepository.checkActiveById(profileId)).thenReturn(Optional.of(profileId));
        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationToken);
        
        MvcResult result = this.mockMvc.perform(patch("/profiles/{profileId}",profileId)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(profilePatchJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.*", hasSize(6)))
                .andExpect(jsonPath("$.id").value(updatedProfile.getId()))
                .andExpect(jsonPath("$.profile_name").value(updatedProfile.getProfileName()))
                .andExpect(jsonPath("$.bio").value(updatedProfile.getBio()))
                .andExpect(jsonPath("$.picture_url").value(updatedProfile.getPictureUrl()))
                .andExpect(jsonPath("$.is_private").value(updatedProfile.getIsPrivate()))
                .andExpect(jsonPath("$.account_id").value(updatedProfile.getAccountId())).andReturn();

        // salvo risposta in result per visualizzarla
        String resultAsString = result.getResponse().getContentAsString();
        Profile profileResult = this.objectMapper.readValue(resultAsString, Profile.class);

        log.info(profileResult.toString());
    }




    @Test
    void testUpdateProfile_BioLength_Then_400() throws Exception{
        String error = "bio size must be between 0 and 150";

        ProfileJpa profileJpa = new ProfileJpa(profileName, isPrivate, accountId);
        profileJpa.setBio(bio);
        profileJpa.setPictureUrl(pictureUrl);
        profileJpa.setId(profileId);
        profileJpa.setCreatedAt(LocalDateTime.MIN);

        // Definisco un o piu' campi del profilo da aggiornare tramite l'oggetto ProfilePatch
        ProfilePatch profilePatch = new ProfilePatch();
        profilePatch.setBio(RandomStringUtils.randomAlphabetic(160));

        String profilePatchJson = this.objectMapper.writeValueAsString(profilePatch);

        when(this.profilesRepository.checkActiveById(profileId)).thenReturn(Optional.of(profileId));
        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationToken);

        MvcResult result = this.mockMvc.perform(patch("/profiles/{profileId}",profileId)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profilePatchJson))
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

    @Test
    void testUpdateProfile_InvalidPictureUrl_Then_400() throws Exception{
        String error = "pictureUrl must be a valid URL";

        ProfileJpa profileJpa = new ProfileJpa(profileName, isPrivate, accountId);
        profileJpa.setBio(bio);
        profileJpa.setPictureUrl(pictureUrl);
        profileJpa.setId(profileId);
        profileJpa.setCreatedAt(LocalDateTime.MIN);

        ProfilePatch profilePatch = new ProfilePatch();
        String invalidUrl = "https://upload.wikimedia.o/ "+RandomStringUtils.randomAscii(15);
        log.info("Invalid URL --> "+invalidUrl);
        profilePatch.setPictureUrl(invalidUrl);


        String profilePatchJson = this.objectMapper.writeValueAsString(profilePatch);

        when(this.profilesRepository.checkActiveById(profileId)).thenReturn(Optional.of(profileId));
        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationToken);

        MvcResult result = this.mockMvc.perform(patch("/profiles/{profileId}",profileId)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profilePatchJson))
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

    @Test
    void testUpdateProfile_InvalidIsPrivate_Then_400() throws Exception{
        ProfileJpa profileJpa = new ProfileJpa(profileName, isPrivate, accountId);
        profileJpa.setBio(bio);
        profileJpa.setPictureUrl(pictureUrl);
        profileJpa.setId(profileId);
        profileJpa.setCreatedAt(LocalDateTime.MIN);

        ProfilePatch profilePatch = new ProfilePatch();
        profilePatch.setIsPrivate(false);

        String profilePatchJson = this.objectMapper.writeValueAsString(profilePatch);

        JsonNode jsonNode = this.objectMapper.readTree(profilePatchJson);
        ((ObjectNode) jsonNode).put("is_private", "FalseString");
        profilePatchJson = this.objectMapper.writeValueAsString(jsonNode);

        when(this.profilesRepository.checkActiveById(profileId)).thenReturn(Optional.of(profileId));
        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationToken);

        MvcResult result = this.mockMvc.perform(patch("/profiles/{profileId}",profileId)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profilePatchJson))
                .andExpect(status().isBadRequest())
                .andExpect(res -> assertTrue(
                        res.getResolvedException() instanceof HttpMessageNotReadableException
                ))
                .andExpect(jsonPath("$.error").value("Message is not readable")).andReturn();
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n"+resultAsString);
        log.info("Resolved Error ---> "+result.getResolvedException());
    }


    @Test
    void testUpdateProfile_Then_403() throws Exception{
        // Definisco un o piu' campi del profilo da aggiornare tramite l'oggetto ProfilePatch
        ProfilePatch profilePatch = new ProfilePatch();
        profilePatch.setBio(updatedBio);
        profilePatch.setPictureUrl(updatedPictureUrl);
        profilePatch.setIsPrivate(updatedIsPrivate);

        String profilePatchJson = this.objectMapper.writeValueAsString(profilePatch);

        when(this.profilesRepository.checkActiveById(profileId)).thenReturn(Optional.of(profileId));
        when(this.securityContext.getAuthentication()).thenReturn(this.jwtTokenError);

        MvcResult result = this.mockMvc.perform(patch("/profiles/{profileId}",profileId)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profilePatchJson))
                .andExpect(status().isForbidden())
                .andExpect(res -> assertTrue(
                        res.getResolvedException() instanceof NotInProfileListException
                ))
                .andExpect(jsonPath("$.error").value("Profile "+profileId+" is not inside the profile list!"))
                .andReturn();
        // Visualizzo l'errore
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n"+resultAsString);
        log.info("Resolved Error ---> "+result.getResolvedException());
    }

    @Test
    void testUpdateProfile_Then_404() throws Exception{
        Long invalidProfileId = Long.MAX_VALUE;
        // Definisco un o piu' campi del profilo da aggiornare tramite l'oggetto ProfilePatch
        ProfilePatch profilePatch = new ProfilePatch();
        profilePatch.setBio(updatedBio);
        profilePatch.setPictureUrl(updatedPictureUrl);
        profilePatch.setIsPrivate(updatedIsPrivate);

        String profilePatchJson = this.objectMapper.writeValueAsString(profilePatch);

        when(this.profilesRepository.checkActiveById(profileId)).thenReturn(Optional.empty());

        MvcResult result = this.mockMvc.perform(patch("/profiles/{profileId}",invalidProfileId)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(profilePatchJson))
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

    @Test
    void testFindProfileByName_Then_200() throws Exception{
        Profile savedProfile2 = new Profile(profileName+"_2",isPrivate);
        savedProfile2.setAccountId(accountId);
        savedProfile2.setBio(bio);
        savedProfile2.setPictureUrl(pictureUrl);
        savedProfile2.setId(13L);

        when(this.profilesService.findByProfileName(profileName)).thenReturn(asList(this.savedProfile, savedProfile2));

        MvcResult result = this.mockMvc.perform(get("/profiles/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .queryParam("profileName",profileName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.*").isArray())
                .andExpect(jsonPath("$.*", hasSize(2)))
                .andExpect(jsonPath("$.[0]id").value(this.savedProfile.getId()))
                .andExpect(jsonPath("$.[0]profile_name").value(this.savedProfile.getProfileName()))
                .andExpect(jsonPath("$.[0]bio").value(this.savedProfile.getBio()))
                .andExpect(jsonPath("$.[0]picture_url").value(this.savedProfile.getPictureUrl()))
                .andExpect(jsonPath("$.[0]is_private").value(this.savedProfile.getIsPrivate()))
                .andExpect(jsonPath("$.[0]account_id").value(this.savedProfile.getAccountId())).andExpect(jsonPath("$.[0]id").value(this.savedProfile.getId()))
                .andExpect(jsonPath("$.[1]id").value(savedProfile2.getId()))
                .andExpect(jsonPath("$.[1]profile_name").value(savedProfile2.getProfileName()))
                .andExpect(jsonPath("$.[1]bio").value(savedProfile2.getBio()))
                .andExpect(jsonPath("$.[1]picture_url").value(savedProfile2.getPictureUrl()))
                .andExpect(jsonPath("$.[1]is_private").value(savedProfile2.getIsPrivate()))
                .andExpect(jsonPath("$.[1]account_id").value(savedProfile2.getAccountId()))
                .andReturn();

        // salvo risposta in result per visualizzarla
        String resultAsString = result.getResponse().getContentAsString();
        log.info(resultAsString);
    }

    @Test
    void testFindProfileByName_Then_400() throws Exception{
        MvcResult result = this.mockMvc.perform(get("/profiles/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .queryParam("profileName", RandomStringUtils.randomAlphabetic(21)))
                        //.queryParam("profileName", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(res -> assertTrue(
                        res.getResolvedException() instanceof ConstraintViolationException
                ))
                .andExpect(jsonPath("$.error").isNotEmpty())
                .andReturn();
        // Visualizzo l'errore
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n"+resultAsString);
        log.info("Resolved Error ---> "+result.getResolvedException());
    }



    @Test
    void testFindFull_Then_200() throws Exception{
        Long targetProfileId = 1L;
        ProfileJpa profileJpa = new ProfileJpa(profileName, isPrivate, accountId);
        profileJpa.setBio(bio);
        profileJpa.setPictureUrl(pictureUrl);
        profileJpa.setId(targetProfileId);
        profileJpa.setCreatedAt(LocalDateTime.MIN);


        Profile convertedProfile = new Profile(profileName, isPrivate);
        convertedProfile.setAccountId(accountId);
        convertedProfile.setBio(bio);
        convertedProfile.setPictureUrl(pictureUrl);
        convertedProfile.setId(targetProfileId);

        FullProfile convertedFullProfile = getFullProfile(targetProfileId, convertedProfile);

        when(this.profilesRepository.checkActiveById(anyLong())).thenReturn(Optional.of(targetProfileId));
        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationToken);
        when(this.profilesService.findFull(anyLong(), anyLong())).thenReturn(convertedFullProfile);

        MvcResult result = this.mockMvc.perform(get("/profiles/{profileId}",targetProfileId)
                        .queryParam("selectedUserProfileId", String.valueOf(profileId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.*", hasSize(4)))
                .andExpect(jsonPath("$.profile.id").value(convertedProfile.getId()))
                .andExpect(jsonPath("$.post_list", hasSize(2)))
                .andExpect(jsonPath("$.post_count", is(2)))
                .andExpect(jsonPath("$.profile_granted", is(true)))
                .andReturn();

        // salvo risposta in result per visualizzarla
        String resultAsString = result.getResponse().getContentAsString();
        FullProfile getResult = this.objectMapper.readValue(resultAsString, FullProfile.class);

        log.info(getResult.toString());
    }



    @Test
    void testFindFull_ProfileId_NotValid_Then_400() throws Exception{
        MvcResult result = this.mockMvc.perform(get("/profiles/{profileId}","IdNotLong")
                        .queryParam("selectedUserProfileId", String.valueOf(profileId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(res -> assertTrue(
                        res.getResolvedException() instanceof NumberFormatException
                ))
                .andExpect(jsonPath("$.error").value("ID is not valid!")).andReturn();
        // Visualizzo l'errore
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n" + resultAsString);
        log.info("Resolved Error ---> " + result.getResolvedException());
    }

    @Test
    void testFindFull_SelectedUserProfileId_NotValid_Then_400() throws Exception{
        Long targetProfileId = 1L;
        when(this.profilesRepository.checkActiveById(anyLong())).thenReturn(Optional.of(targetProfileId));
        MvcResult result = this.mockMvc.perform(get("/profiles/{profileId}",targetProfileId)
                        .queryParam("selectedUserProfileId", "IdNotLong")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(res -> assertTrue(
                        res.getResolvedException() instanceof MethodArgumentTypeMismatchException
                ))
                .andExpect(jsonPath("$.error").value("Type mismatch")).andReturn();
        // Visualizzo l'errore
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n" + resultAsString);
        log.info("Resolved Error ---> " + result.getResolvedException());
    }

    @Test
    void testFindFull_Then_403() throws Exception{
        when(this.profilesRepository.checkActiveById(anyLong())).thenReturn(Optional.of(profileId));
        when(this.securityContext.getAuthentication()).thenReturn(this.jwtTokenError);
        when(this.profilesService.findFull(anyLong(), anyLong())).thenThrow(new NotInProfileListException(Long.MAX_VALUE));

        MvcResult result = this.mockMvc.perform(get("/profiles/{profileId}",profileId)
                        .queryParam("selectedUserProfileId", String.valueOf(Long.MAX_VALUE))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(res -> assertTrue(
                        res.getResolvedException() instanceof NotInProfileListException
                ))
                .andExpect(jsonPath("$.error").value("Profile "+Long.MAX_VALUE+" is not inside the profile list!"))
                .andReturn();
        // Visualizzo l'errore
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n"+resultAsString);
        log.info("Resolved Error ---> "+result.getResolvedException());
    }

    @Test
    void testFindFull_Then_404() throws Exception{
        when(this.profilesRepository.checkActiveById(anyLong())).thenReturn(Optional.empty());


        MvcResult result = this.mockMvc.perform(get("/profiles/{profileId}",profileId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(res -> assertTrue(
                        res.getResolvedException() instanceof ProfileNotFoundException
                ))
                .andExpect(jsonPath("$.error").value("Profile "+profileId+" not found!"))
                .andReturn();
        // Visualizzo l'errore
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n"+resultAsString);
        log.info("Resolved Error ---> "+result.getResolvedException());
    }



}