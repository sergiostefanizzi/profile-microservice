package com.sergiostefanizzi.profilemicroservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sergiostefanizzi.profilemicroservice.model.*;
import com.sergiostefanizzi.profilemicroservice.repository.AlertsRepository;
import com.sergiostefanizzi.profilemicroservice.repository.CommentsRepository;
import com.sergiostefanizzi.profilemicroservice.repository.PostsRepository;
import com.sergiostefanizzi.profilemicroservice.repository.ProfilesRepository;
import com.sergiostefanizzi.profilemicroservice.service.KeycloakService;
import com.sergiostefanizzi.profilemicroservice.service.PostsService;
import com.sergiostefanizzi.profilemicroservice.service.ProfilesService;
import com.sergiostefanizzi.profilemicroservice.system.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
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
import org.springframework.web.bind.MissingServletRequestParameterException;
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
@WebMvcTest(PostsController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@Slf4j
class PostsControllerTest {

    @MockBean
    private PostsService postsService;
    @MockBean
    private ProfilesRepository profilesRepository;
    @MockBean
    private PostsRepository postsRepository;
    @MockBean
    private CommentsRepository commentsRepository;
    @MockBean
    private AlertsRepository alertsRepository;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private KeycloakService keycloakService;
    @MockBean
    private SecurityContext securityContext;
    private JwtAuthenticationToken jwtAuthenticationToken;
    String contentUrl = "https://upload.wikimedia.org/wikipedia/commons/9/9a/Cape_may.jpg";
    String contentUrlXSS = "http://www.example.com?d=<script type=\"javascript\" src=\"http://www.google.it\"/>"; //Cross site scripting XSS
    String caption = "This is the post caption";
    Post.PostTypeEnum postType = Post.PostTypeEnum.POST;
    Long postId = 1L;
    Long profileId = 1L;
    String accountId = UUID.randomUUID().toString();
    private Post newPost;
    private Post savedPost;
    private Like newLike;
    String newPostJson;
    List<String> errors;
    Long invalidProfileId = Long.MAX_VALUE;
    Long invalidPostId = Long.MAX_VALUE;

    @BeforeEach
    void setUp() throws JsonProcessingException {
        SecurityContextHolder.setContext(securityContext);

        this.newPost = new Post(contentUrl, postType, profileId);
        this.newPost.setCaption(caption);

        newPostJson = this.objectMapper.writeValueAsString(this.newPost);

        this.savedPost = new Post(contentUrl, postType, profileId);
        this.savedPost.setCaption(caption);
        this.savedPost.setId(postId);

        errors = new ArrayList<>();

        Map<String, Object> headers = new HashMap<>();
        headers.put("alg","HS256");
        headers.put("typ","JWT");
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", this.accountId);
        claims.put("profileList", List.of(profileId));
        this.jwtAuthenticationToken = new JwtAuthenticationToken(new Jwt("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c",
                Instant.now(),
                Instant.MAX,
                headers,
                claims));
    }


    @AfterEach
    void tearDown() {
        errors.clear();
    }

    @Test
    void testAddPost_Then_201() throws Exception {
        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationToken);
        when(this.keycloakService.checkActiveById(anyString())).thenReturn(true);
        when(this.keycloakService.checksEmailValidated(anyString())).thenReturn(true);
        when(this.postsService.save(ArgumentMatchers.any(Post.class))).thenReturn(this.savedPost);

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

        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationToken);
        when(this.keycloakService.checkActiveById(anyString())).thenReturn(true);
        when(this.keycloakService.checksEmailValidated(anyString())).thenReturn(true);
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
    void testAddPost_MissingRequired_Then_400() throws Exception {
        // arrayList contenente i messaggi di errore
        errors.add("contentUrl must not be null");
        errors.add("postType must not be null");
        errors.add("profileId must not be null");

        // Imposto a null tutti i campi richiesti
        this.newPost.setContentUrl(null);
        this.newPost.setPostType(null);
        this.newPost.setProfileId(null);

        newPostJson = this.objectMapper.writeValueAsString(this.newPost);

        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationToken);
        when(this.keycloakService.checkActiveById(anyString())).thenReturn(true);
        when(this.keycloakService.checksEmailValidated(anyString())).thenReturn(true);

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
        log.info("Errors\n" + resultAsString);
    }

    @Test
    void testAddPost_CaptionLength_Then_400() throws Exception {
        errors.add("caption size must be between 0 and 2200");
        // genero una caption di 2210 caratteri, superando di 10 il limite
        this.newPost.setCaption(RandomStringUtils.randomAlphabetic(2210));

        newPostJson = this.objectMapper.writeValueAsString(this.newPost);
        log.info("JSON\n" + newPostJson);

        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationToken);
        when(this.keycloakService.checkActiveById(anyString())).thenReturn(true);
        when(this.keycloakService.checksEmailValidated(anyString())).thenReturn(true);

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
        log.info("Errors\n" + resultAsString);
    }

    @Test
    void testAddPost_InvalidContentUrl_Then_400() throws Exception {
        errors.add("contentUrl must be a valid URL");
        errors.add("contentUrl size must be between 3 and 2048");
        // Url non valido
        this.newPost.setContentUrl("https://upload.wikimedia.o/ ra-%%$^&& iuyi" + RandomStringUtils.randomAlphabetic(2048));
        newPostJson = this.objectMapper.writeValueAsString(this.newPost);

        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationToken);
        when(this.keycloakService.checkActiveById(anyString())).thenReturn(true);
        when(this.keycloakService.checksEmailValidated(anyString())).thenReturn(true);

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
        log.info("Errors\n" + resultAsString);
    }

    @Test
    void testAddPost_InvalidContentUrlXSS_Then_400() throws Exception {
        this.newPost.setContentUrl(contentUrlXSS);
        newPostJson = this.objectMapper.writeValueAsString(this.newPost);

        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationToken);
        when(this.keycloakService.checkActiveById(anyString())).thenReturn(true);
        when(this.keycloakService.checksEmailValidated(anyString())).thenReturn(true);

        MvcResult result = this.mockMvc.perform(post("/posts")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newPostJson))
                .andExpect(status().isBadRequest())
                .andExpect(res -> assertTrue(res.getResolvedException() instanceof MethodArgumentNotValidException))
                .andExpect(jsonPath("$.error").isArray())
                .andExpect(jsonPath("$.error", hasSize(1)))
                .andExpect(jsonPath("$.error[0]").value("contentUrl must be a valid URL"))
                .andReturn();
        // salvo risposta in result solo per visualizzarla
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n" + resultAsString);
    }

    @Test
    void testAddPost_InvalidPostType_Then_400() throws Exception {
        JsonNode jsonNode = this.objectMapper.readTree(newPostJson);

        ((ObjectNode) jsonNode).put("post_type", "NOTVALID");
        newPostJson = this.objectMapper.writeValueAsString(jsonNode);

        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationToken);
        when(this.keycloakService.checkActiveById(anyString())).thenReturn(true);
        when(this.keycloakService.checksEmailValidated(anyString())).thenReturn(true);

        MvcResult result = this.mockMvc.perform(post("/posts")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newPostJson))
                .andExpect(status().isBadRequest())
                .andExpect(res -> assertTrue(
                        res.getResolvedException() instanceof HttpMessageNotReadableException
                ))
                .andExpect(jsonPath("$.error").value("Message is not readable"))
                .andReturn();
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n" + resultAsString);
        log.info("Resolved Error ---> " + result.getResolvedException());
    }

    @Test
    void testAddPost_InvalidProfileId_Then_400() throws Exception {
        JsonNode jsonNode = this.objectMapper.readTree(newPostJson);
        ((ObjectNode) jsonNode).put("profile_id", "IdNotLong");
        newPostJson = this.objectMapper.writeValueAsString(jsonNode);

        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationToken);
        when(this.keycloakService.checkActiveById(anyString())).thenReturn(true);
        when(this.keycloakService.checksEmailValidated(anyString())).thenReturn(true);

        MvcResult result = this.mockMvc.perform(post("/posts")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newPostJson))
                .andExpect(status().isBadRequest())
                .andExpect(res -> assertTrue(
                        res.getResolvedException() instanceof HttpMessageNotReadableException
                ))
                .andExpect(jsonPath("$.error").value("Message is not readable")).andReturn();
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n" + resultAsString);
        log.info("Resolved Error ---> " + result.getResolvedException());
    }

    @Test
    void testAddPost_Then_403() throws Exception {
        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationToken);
        when(this.keycloakService.checkActiveById(anyString())).thenReturn(true);
        when(this.keycloakService.checksEmailValidated(anyString())).thenReturn(true);
        doThrow(new NotInProfileListException(invalidProfileId)).when(this.postsService).save(this.newPost);

        MvcResult result = this.mockMvc.perform(post("/posts")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newPostJson))
                .andExpect(status().isForbidden())
                .andExpect(res -> assertTrue(
                        res.getResolvedException() instanceof NotInProfileListException
                ))
                .andExpect(jsonPath("$.error").value("Profile " + invalidProfileId + " is not inside the profile list!"))
                .andReturn();
        // Visualizzo l'errore
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n" + resultAsString);
        log.info("Resolved Error ---> " + result.getResolvedException());
    }

    @Test
    void testDeletePostById_Then_204() throws Exception {
        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationToken);
        when(this.keycloakService.checkActiveById(anyString())).thenReturn(true);
        when(this.keycloakService.checksEmailValidated(anyString())).thenReturn(true);
        when(this.postsRepository.checkActiveForDeleteById(anyLong())).thenReturn(Optional.of(postId));
        when(this.profilesRepository.checkActiveByPostId(anyLong())).thenReturn(Optional.of(profileId));

        doNothing().when(this.postsService).remove(anyLong(), anyLong());

        this.mockMvc.perform(delete("/posts/{postId}", postId)
                        .queryParam("selectedUserProfileId", String.valueOf(profileId)))
                .andExpect(status().isNoContent());
    }

    @Test
    void testDeletePostById_Then_400() throws Exception {
        MvcResult result = this.mockMvc.perform(delete("/posts/IdNotLong")
                        .queryParam("selectedUserProfileId", String.valueOf(profileId)))
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
    void testDeletePostById_QueryParamNotValid_Then_400() throws Exception {
        when(this.postsRepository.checkActiveForDeleteById(anyLong())).thenReturn(Optional.of(postId));
        when(this.profilesRepository.checkActiveByPostId(anyLong())).thenReturn(Optional.of(profileId));
        MvcResult result = this.mockMvc.perform(delete("/posts/{postId}", postId)
                        .queryParam("selectedUserProfileId", "IdNotLong"))
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
    void testDeletePostById_MissingQueryParam_Then_400() throws Exception {
        when(this.postsRepository.checkActiveForDeleteById(anyLong())).thenReturn(Optional.of(postId));
        when(this.profilesRepository.checkActiveByPostId(anyLong())).thenReturn(Optional.of(profileId));
        MvcResult result = this.mockMvc.perform(delete("/posts/{postId}", postId))
                .andExpect(status().isBadRequest())
                .andExpect(res -> assertTrue(
                        res.getResolvedException() instanceof MissingServletRequestParameterException
                ))
                .andExpect(jsonPath("$.error").value("Required request parameter 'selectedUserProfileId' for method parameter type Long is not present")).andReturn();
        // Visualizzo l'errore
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n" + resultAsString);
        log.info("Resolved Error ---> " + result.getResolvedException());
    }


    @Test
    void testDeletePostById_Then_403() throws Exception {
        when(this.postsRepository.checkActiveForDeleteById(anyLong())).thenReturn(Optional.of(postId));
        when(this.profilesRepository.checkActiveByPostId(anyLong())).thenReturn(Optional.of(profileId));
        doThrow(new NotInProfileListException(profileId)).when(this.postsService).remove(anyLong(), anyLong());

        MvcResult result = this.mockMvc.perform(delete("/posts/{postId}",postId)
                .queryParam("selectedUserProfileId", String.valueOf(profileId)))
                .andExpect(status().isForbidden())
                .andExpect(res -> assertTrue(
                        res.getResolvedException() instanceof NotInProfileListException
                ))
                .andExpect(jsonPath("$.error").value("Profile " + profileId + " is not inside the profile list!"))
                .andReturn();
        // Visualizzo l'errore
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n"+resultAsString);
        log.info("Resolved Error ---> "+result.getResolvedException());
    }

    @Test
    void testDeletePostById_IdsMismatch_Then_403() throws Exception {
        when(this.postsRepository.checkActiveForDeleteById(anyLong())).thenReturn(Optional.of(postId));
        when(this.profilesRepository.checkActiveByPostId(anyLong())).thenReturn(Optional.of(profileId));
        doThrow(new IdsMismatchException()).when(this.postsService).remove(anyLong(), anyLong());
        MvcResult result = this.mockMvc.perform(delete("/posts/{postId}",postId)
                        .queryParam("selectedUserProfileId", String.valueOf(Long.MAX_VALUE)))
                .andExpect(status().isForbidden())
                .andExpect(res -> assertTrue(
                        res.getResolvedException() instanceof IdsMismatchException
                ))
                .andExpect(jsonPath("$.error").value("Ids mismatch"))
                .andReturn();
        // Visualizzo l'errore
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n"+resultAsString);
        log.info("Resolved Error ---> "+result.getResolvedException());
    }

    @Test
    void testDeletePostById_Then_404() throws Exception {
        when(this.postsRepository.checkActiveForDeleteById(anyLong())).thenReturn(Optional.empty());

        MvcResult result = this.mockMvc.perform(delete("/posts/{postId}",postId)
                        .queryParam("selectedUserProfileId", String.valueOf(profileId)))
                .andExpect(status().isNotFound())
                .andExpect(res -> assertTrue(
                        res.getResolvedException() instanceof PostNotFoundException
                ))
                .andExpect(jsonPath("$.error").value("Post " + profileId + " not found!"))
                .andReturn();
        // Visualizzo l'errore
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n"+resultAsString);
        log.info("Resolved Error ---> "+result.getResolvedException());
    }

    @Test
    void testDeletePostById_DeletedProfile_Then_404() throws Exception {
        when(this.profilesRepository.checkActiveByPostId(anyLong())).thenReturn(Optional.empty());

        MvcResult result = this.mockMvc.perform(delete("/posts/{postId}",postId)
                        .queryParam("selectedUserProfileId", String.valueOf(profileId)))
                .andExpect(status().isNotFound())
                .andExpect(res -> assertTrue(
                        res.getResolvedException() instanceof PostNotFoundException
                ))
                .andExpect(jsonPath("$.error").value("Post " + profileId + " not found!"))
                .andReturn();
        // Visualizzo l'errore
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n"+resultAsString);
        log.info("Resolved Error ---> "+result.getResolvedException());
    }

    @Test
    void testUpdatePostById_Then_200() throws Exception{
        // Definisco la caption da aggiornare tramite l'oggetto PostPatch
        String newCaption = "Nuova caption del post";
        PostPatch postPatch = new PostPatch(newCaption);

        String postPatchJson = this.objectMapper.writeValueAsString(postPatch);


        // Aggiorno il post che verra' restituito dal service con il nuovo valore
        this.savedPost.setCaption(newCaption);

        when(this.postsRepository.checkActiveById(anyLong())).thenReturn(Optional.of(postId));
        when(this.profilesRepository.checkActiveByPostId(anyLong())).thenReturn(Optional.of(profileId));
        when(this.postsService.update(anyLong(), anyLong(), ArgumentMatchers.any(PostPatch.class))).thenReturn(this.savedPost);

        MvcResult result = this.mockMvc.perform(patch("/posts/{postId}",postId)
                        .queryParam("selectedUserProfileId", String.valueOf(profileId))
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postPatchJson))
                .andExpect(status().isOk())
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
    void testUpdatePost_InvalidId_Then_400() throws Exception{
        // Definisco la caption da aggiornare tramite l'oggetto PostPatch
        String newCaption = "Nuova caption del post";
        PostPatch postPatch = new PostPatch(newCaption);

        String postPatchJson = this.objectMapper.writeValueAsString(postPatch);

        // Aggiorno il post che verra' restituito dal service con il nuovo valore
        this.savedPost.setCaption(newCaption);


        MvcResult result = this.mockMvc.perform(patch("/posts/IdNotLong")
                .queryParam("selectedUserProfileId", String.valueOf(profileId))
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postPatchJson))
                .andExpect(status().isBadRequest())
                .andExpect(res -> assertTrue(
                        res.getResolvedException() instanceof NumberFormatException
                ))
                .andExpect(jsonPath("$.error").value("ID is not valid!")).andReturn();
        // Visualizzo l'errore
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n"+resultAsString);
        log.info("Resolved Error ---> "+result.getResolvedException());
    }



    @Test
    void testUpdatePost_MissingQueryParam_Then_400() throws Exception{
        // Definisco la caption da aggiornare tramite l'oggetto PostPatch
        String newCaption = "Nuova caption del post";
        PostPatch postPatch = new PostPatch(newCaption);

        String postPatchJson = this.objectMapper.writeValueAsString(postPatch);

        // Aggiorno il post che verra' restituito dal service con il nuovo valore
        this.savedPost.setCaption(newCaption);

        when(this.postsRepository.checkActiveById(anyLong())).thenReturn(Optional.of(postId));
        when(this.profilesRepository.checkActiveByPostId(anyLong())).thenReturn(Optional.of(profileId));

        MvcResult result = this.mockMvc.perform(patch("/posts/{postId}",postId)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postPatchJson))
                .andExpect(status().isBadRequest())
                .andExpect(res -> assertTrue(
                        res.getResolvedException() instanceof MissingServletRequestParameterException
                ))
                .andExpect(jsonPath("$.error").value("Required request parameter 'selectedUserProfileId' for method parameter type Long is not present")).andReturn();
        // Visualizzo l'errore
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n"+resultAsString);
        log.info("Resolved Error ---> "+result.getResolvedException());
    }

    @Test
    void testUpdatePost_QueryParamNotValid_Then_400() throws Exception{
        // Definisco la caption da aggiornare tramite l'oggetto PostPatch
        String newCaption = "Nuova caption del post";
        PostPatch postPatch = new PostPatch(newCaption);

        String postPatchJson = this.objectMapper.writeValueAsString(postPatch);

        // Aggiorno il post che verra' restituito dal service con il nuovo valore
        this.savedPost.setCaption(newCaption);

        when(this.postsRepository.checkActiveById(anyLong())).thenReturn(Optional.of(postId));
        when(this.profilesRepository.checkActiveByPostId(anyLong())).thenReturn(Optional.of(profileId));

        MvcResult result = this.mockMvc.perform(patch("/posts/{postId}",postId)
                        .queryParam("selectedUserProfileId", "IdNotLong")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postPatchJson))
                .andExpect(status().isBadRequest())
                .andExpect(res -> assertTrue(
                        res.getResolvedException() instanceof MethodArgumentTypeMismatchException
                ))
                .andExpect(jsonPath("$.error").value("Type mismatch")).andReturn();
        // Visualizzo l'errore
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n"+resultAsString);
        log.info("Resolved Error ---> "+result.getResolvedException());
    }

    @Test
    void testUpdatePost_CaptionLength_Then_400() throws Exception{
        when(this.postsRepository.checkActiveById(anyLong())).thenReturn(Optional.of(postId));
        when(this.profilesRepository.checkActiveByPostId(anyLong())).thenReturn(Optional.of(profileId));

        // genero una caption di 2210 caratteri, superando di 10 il limite
        PostPatch postPatch = new PostPatch(RandomStringUtils.randomAlphabetic(2210));

        String postPatchJson = this.objectMapper.writeValueAsString(postPatch);

        MvcResult result = this.mockMvc.perform(patch("/posts/{postId}",postId)
                        .queryParam("selectedUserProfileId", String.valueOf(profileId))
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postPatchJson))
                .andExpect(status().isBadRequest())
                .andExpect(res -> assertTrue(
                        res.getResolvedException() instanceof MethodArgumentNotValidException)
                )
                .andExpect(jsonPath("$.error").isArray())
                .andExpect(jsonPath("$.error", hasSize(1)))
                .andExpect(jsonPath("$.error[0]").value("caption size must be between 0 and 2200")).andReturn();
        // salvo risposta in result solo per visualizzarla
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n"+resultAsString);
    }

    @Test
    void testUpdatePost_Then_403() throws Exception{
        // Definisco la caption da aggiornare tramite l'oggetto PostPatch
        String newCaption = "Nuova caption del post";
        PostPatch postPatch = new PostPatch(newCaption);

        String postPatchJson = this.objectMapper.writeValueAsString(postPatch);

        // Aggiorno il post che verra' restituito dal service con il nuovo valore
        this.savedPost.setCaption(newCaption);

        when(this.postsRepository.checkActiveById(anyLong())).thenReturn(Optional.of(postId));
        when(this.profilesRepository.checkActiveByPostId(anyLong())).thenReturn(Optional.of(profileId));
        when(this.postsService.update(anyLong(), anyLong(), ArgumentMatchers.any(PostPatch.class))).thenThrow(new NotInProfileListException(profileId));

        MvcResult result = this.mockMvc.perform(patch("/posts/{postId}",postId)
                        .queryParam("selectedUserProfileId", String.valueOf(profileId))
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postPatchJson))
                .andExpect(status().isForbidden())
                .andExpect(res -> assertTrue(
                        res.getResolvedException() instanceof NotInProfileListException
                ))
                .andExpect(jsonPath("$.error").value("Profile " + profileId + " is not inside the profile list!")).andReturn();
        // Visualizzo l'errore
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n"+resultAsString);
        log.info("Resolved Error ---> "+result.getResolvedException());
    }

    @Test
    void testUpdatePost_IdsMismatch_Then_403() throws Exception{
        // Definisco la caption da aggiornare tramite l'oggetto PostPatch
        String newCaption = "Nuova caption del post";
        PostPatch postPatch = new PostPatch(newCaption);

        String postPatchJson = this.objectMapper.writeValueAsString(postPatch);

        // Aggiorno il post che verra' restituito dal service con il nuovo valore
        this.savedPost.setCaption(newCaption);

        when(this.postsRepository.checkActiveById(anyLong())).thenReturn(Optional.of(postId));
        when(this.profilesRepository.checkActiveByPostId(anyLong())).thenReturn(Optional.of(profileId));
        when(this.postsService.update(anyLong(), anyLong(), ArgumentMatchers.any(PostPatch.class))).thenThrow(new NotInProfileListException(invalidProfileId));

        MvcResult result = this.mockMvc.perform(patch("/posts/{postId}",postId)
                        .queryParam("selectedUserProfileId", String.valueOf(invalidProfileId))
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postPatchJson))
                .andExpect(status().isForbidden())
                .andExpect(res -> assertTrue(
                        res.getResolvedException() instanceof NotInProfileListException
                ))
                .andExpect(jsonPath("$.error").value("Profile " + invalidProfileId + " is not inside the profile list!")).andReturn();
        // Visualizzo l'errore
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n"+resultAsString);
        log.info("Resolved Error ---> "+result.getResolvedException());
    }



    @Test
    void testUpdatePost_Then_404() throws Exception{
        // Definisco la caption da aggiornare tramite l'oggetto PostPatch
        String newCaption = "Nuova caption del post";
        PostPatch postPatch = new PostPatch(newCaption);

        String postPatchJson = this.objectMapper.writeValueAsString(postPatch);

        when(this.postsRepository.checkActiveById(anyLong())).thenReturn(Optional.empty());


        MvcResult result = this.mockMvc.perform(patch("/posts/{postId}",invalidPostId)
                        .queryParam("selectedUserProfileId", String.valueOf(profileId))
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postPatchJson))
                .andExpect(status().isNotFound())
                .andExpect(res -> assertTrue(
                        res.getResolvedException() instanceof PostNotFoundException
                ))
                .andExpect(jsonPath("$.error").value("Post "+invalidPostId+" not found!"))
                .andReturn();
        // Visualizzo l'errore
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n"+resultAsString);
        log.info("Resolved Error ---> "+result.getResolvedException());
    }

    @Test
    void testUpdatePost_DeletedProfile_Then_404() throws Exception{
        // Definisco la caption da aggiornare tramite l'oggetto PostPatch
        String newCaption = "Nuova caption del post";
        PostPatch postPatch = new PostPatch(newCaption);

        String postPatchJson = this.objectMapper.writeValueAsString(postPatch);

        when(this.postsRepository.checkActiveById(anyLong())).thenReturn(Optional.of(invalidPostId));
        when(this.profilesRepository.checkActiveByPostId(anyLong())).thenReturn(Optional.empty());


        MvcResult result = this.mockMvc.perform(patch("/posts/{postId}",invalidPostId)
                        .queryParam("selectedUserProfileId", String.valueOf(profileId))
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postPatchJson))
                .andExpect(status().isNotFound())
                .andExpect(res -> assertTrue(
                        res.getResolvedException() instanceof PostNotFoundException
                ))
                .andExpect(jsonPath("$.error").value("Post "+invalidPostId+" not found!"))
                .andReturn();
        // Visualizzo l'errore
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n"+resultAsString);
        log.info("Resolved Error ---> "+result.getResolvedException());
    }

    @Test
    void testFindPostById_Then_200() throws Exception{
        when(this.postsRepository.checkActiveById(anyLong())).thenReturn(Optional.of(postId));
        when(this.profilesRepository.checkActiveByPostId(anyLong())).thenReturn(Optional.of(profileId));
        when(this.postsService.find(anyLong(), anyLong())).thenReturn(this.savedPost);

        MvcResult result = this.mockMvc.perform(get("/posts/{postId}",postId)
                        .queryParam("selectedUserProfileId", String.valueOf(profileId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
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
    void testFindPostById_Then_400() throws Exception{
        MvcResult result = this.mockMvc.perform(get("/posts/{postId}","IdNotLong")
                        .queryParam("selectedUserProfileId", String.valueOf(profileId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(res -> assertTrue(
                        res.getResolvedException() instanceof NumberFormatException
                ))
                .andExpect(jsonPath("$.error").value("ID is not valid!")).andReturn();
        // Visualizzo l'errore
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n"+resultAsString);
        log.info("Resolved Error ---> "+result.getResolvedException());
    }

    @Test
    void testFindPostById_QueryParamNotValid_Then_400() throws Exception{
        when(this.postsRepository.checkActiveById(anyLong())).thenReturn(Optional.of(postId));
        when(this.profilesRepository.checkActiveByPostId(anyLong())).thenReturn(Optional.of(profileId));
        MvcResult result = this.mockMvc.perform(get("/posts/{postId}",postId)
                        .queryParam("selectedUserProfileId", "IdNotLong")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(res -> assertTrue(
                        res.getResolvedException() instanceof MethodArgumentTypeMismatchException
                ))
                .andExpect(jsonPath("$.error").value("Type mismatch")).andReturn();
        // Visualizzo l'errore
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n"+resultAsString);
        log.info("Resolved Error ---> "+result.getResolvedException());
    }

    @Test
    void testFindPostById_MissingQueryParam_Then_400() throws Exception{
        when(this.postsRepository.checkActiveById(anyLong())).thenReturn(Optional.of(postId));
        when(this.profilesRepository.checkActiveByPostId(anyLong())).thenReturn(Optional.of(profileId));
        MvcResult result = this.mockMvc.perform(get("/posts/{postId}",postId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(res -> assertTrue(
                        res.getResolvedException() instanceof MissingServletRequestParameterException
                ))
                .andExpect(jsonPath("$.error").value("Required request parameter 'selectedUserProfileId' for method parameter type Long is not present")).andReturn();
        // Visualizzo l'errore
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n"+resultAsString);
        log.info("Resolved Error ---> "+result.getResolvedException());
    }
    @Test
    void testFindPostById_PrivateProfile_Then_403() throws Exception{
        when(this.postsRepository.checkActiveById(anyLong())).thenReturn(Optional.of(postId));
        when(this.profilesRepository.checkActiveByPostId(anyLong())).thenReturn(Optional.of(profileId));
        when(this.postsService.find(anyLong(), anyLong())).thenThrow(new PostAccessForbiddenException(postId));
        MvcResult result = this.mockMvc.perform(get("/posts/{postId}",postId)
                        .queryParam("selectedUserProfileId", String.valueOf(invalidProfileId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(res -> assertTrue(
                        res.getResolvedException() instanceof PostAccessForbiddenException
                ))
                .andExpect(jsonPath("$.error").value("Cannot access post with id "+postId))
                .andReturn();
        // Visualizzo l'errore
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n"+resultAsString);
        log.info("Resolved Error ---> "+result.getResolvedException());
    }
    @Test
    void testFindPostById_Then_403() throws Exception{
        when(this.postsRepository.checkActiveById(anyLong())).thenReturn(Optional.of(postId));
        when(this.profilesRepository.checkActiveByPostId(anyLong())).thenReturn(Optional.of(profileId));
        when(this.postsService.find(anyLong(), anyLong())).thenThrow(new NotInProfileListException(invalidProfileId));
        MvcResult result = this.mockMvc.perform(get("/posts/{postId}",postId)
                        .queryParam("selectedUserProfileId", String.valueOf(invalidProfileId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(res -> assertTrue(
                        res.getResolvedException() instanceof NotInProfileListException
                ))
                .andExpect(jsonPath("$.error").value("Profile " + invalidProfileId + " is not inside the profile list!"))
                .andReturn();
        // Visualizzo l'errore
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n"+resultAsString);
        log.info("Resolved Error ---> "+result.getResolvedException());
    }

    @Test
    void testFindPostById_Then_404() throws Exception{
        when(this.postsRepository.checkActiveById(anyLong())).thenReturn(Optional.empty());
        when(this.profilesRepository.checkActiveByPostId(anyLong())).thenReturn(Optional.of(profileId));
        MvcResult result = this.mockMvc.perform(get("/posts/{postId}",postId)
                        .queryParam("selectedUserProfileId", String.valueOf(profileId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(res -> assertTrue(
                        res.getResolvedException() instanceof PostNotFoundException
                ))
                .andExpect(jsonPath("$.error").value("Post " + postId + " not found!"))
                .andReturn();
        // Visualizzo l'errore
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n"+resultAsString);
        log.info("Resolved Error ---> "+result.getResolvedException());
    }

    @Test
    void testFindPostById_DeletedProfile_Then_404() throws Exception{
        when(this.postsRepository.checkActiveById(anyLong())).thenReturn(Optional.of(postId));
        when(this.profilesRepository.checkActiveByPostId(anyLong())).thenReturn(Optional.empty());
        MvcResult result = this.mockMvc.perform(get("/posts/{postId}",postId)
                        .queryParam("selectedUserProfileId", String.valueOf(profileId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(res -> assertTrue(
                        res.getResolvedException() instanceof PostNotFoundException
                ))
                .andExpect(jsonPath("$.error").value("Post " + postId + " not found!"))
                .andReturn();
        // Visualizzo l'errore
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n"+resultAsString);
        log.info("Resolved Error ---> "+result.getResolvedException());
    }


    @Test
    void testAddLike_Then_204() throws Exception {
        this.newLike = new Like(profileId, postId);
        String newLikeJson = this.objectMapper.writeValueAsString(this.newLike);

        doNothing().when(this.postsService).addLike(false, this.newLike);

        this.mockMvc.perform(put("/posts/likes?removeLike={removeLike}",false)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newLikeJson))
                .andExpect(status().isNoContent());
    }

    @Test
    void testAddLike_Remove_Then_204() throws Exception {
        this.newLike = new Like(profileId, postId);
        String newLikeJson = this.objectMapper.writeValueAsString(this.newLike);

        doNothing().when(this.postsService).addLike(true, this.newLike);

        this.mockMvc.perform(put("/posts/likes?removeLike={removeLike}",true)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newLikeJson))
                .andExpect(status().isNoContent());
    }

    @Test
    void testAddLike_MissingQueryParam_Then_400() throws Exception {
        this.newLike = new Like(profileId, postId);
        String newLikeJson = this.objectMapper.writeValueAsString(this.newLike);

        doNothing().when(this.postsService).addLike(false, this.newLike);


        MvcResult result = this.mockMvc.perform(put("/posts/likes")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newLikeJson))
                .andExpect(status().isBadRequest())
                .andExpect(res -> assertTrue(
                        res.getResolvedException() instanceof MissingServletRequestParameterException
                ))
                .andExpect(jsonPath("$.error").value("Required request parameter 'removeLike' for method parameter type Boolean is not present")).andReturn();
        // Visualizzo l'errore
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n"+resultAsString);
        log.info("Resolved Error ---> "+result.getResolvedException());
    }

    @Test
    void testAddLike_Then_400() throws Exception {
        this.newLike = new Like(profileId, postId);
        String newLikeJson = this.objectMapper.writeValueAsString(this.newLike);
        JsonNode jsonNode = this.objectMapper.readTree(newLikeJson);

        ((ObjectNode) jsonNode).put("profile_id", "IdNotLong");
        ((ObjectNode) jsonNode).put("post_id", "IdNotLong");
        newLikeJson = this.objectMapper.writeValueAsString(jsonNode);

        doNothing().when(this.postsService).addLike(false, this.newLike);

        MvcResult result = this.mockMvc.perform(put("/posts/likes?removeLike={removeLike}",false)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newLikeJson))
                .andExpect(status().isBadRequest())
                .andExpect(res -> assertTrue(
                        res.getResolvedException() instanceof HttpMessageNotReadableException
                ))
                .andExpect(jsonPath("$.error").value("Message is not readable")).andReturn();
        // Visualizzo l'errore
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n"+resultAsString);
        log.info("Resolved Error ---> "+result.getResolvedException());
    }

    @Test
    void testAddLike_Then_403() throws Exception {
        this.newLike = new Like(profileId, invalidPostId);
        String newLikeJson = this.objectMapper.writeValueAsString(this.newLike);

        doThrow(new NotInProfileListException(profileId)).when(this.postsService).addLike(false, this.newLike);

        MvcResult result = this.mockMvc.perform(put("/posts/likes?removeLike={removeLike}",false)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newLikeJson))
                .andExpect(status().isForbidden())
                .andExpect(res -> assertTrue(
                        res.getResolvedException() instanceof NotInProfileListException
                ))
                .andExpect(jsonPath("$.error").value("Profile " + profileId + " is not inside the profile list!")).andReturn();
        // Visualizzo l'errore
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n"+resultAsString);
        log.info("Resolved Error ---> "+result.getResolvedException());
    }

    @Test
    void testAddLike_PrivatePost_Then_403() throws Exception {
        this.newLike = new Like(profileId, invalidPostId);
        String newLikeJson = this.objectMapper.writeValueAsString(this.newLike);

        doThrow(new PostAccessForbiddenException(postId)).when(this.postsService).addLike(false, this.newLike);

        MvcResult result = this.mockMvc.perform(put("/posts/likes?removeLike={removeLike}",false)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newLikeJson))
                .andExpect(status().isForbidden())
                .andExpect(res -> assertTrue(
                        res.getResolvedException() instanceof PostAccessForbiddenException
                ))
                .andExpect(jsonPath("$.error").value("Cannot access post with id "+postId)).andReturn();
        // Visualizzo l'errore
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n"+resultAsString);
        log.info("Resolved Error ---> "+result.getResolvedException());
    }
    @Test
    void testAddLike_PostNotFound_Then_404() throws Exception {
        this.newLike = new Like(profileId, invalidPostId);
        String newLikeJson = this.objectMapper.writeValueAsString(this.newLike);

        doThrow(new PostNotFoundException(invalidPostId)).when(this.postsService).addLike(false, this.newLike);

        MvcResult result = this.mockMvc.perform(put("/posts/likes?removeLike={removeLike}",false)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newLikeJson))
                .andExpect(status().isNotFound())
                .andExpect(res -> assertTrue(
                        res.getResolvedException() instanceof PostNotFoundException
                ))
                .andExpect(jsonPath("$.error").value("Post " + invalidPostId + " not found!")).andReturn();
        // Visualizzo l'errore
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n"+resultAsString);
        log.info("Resolved Error ---> "+result.getResolvedException());
    }

    @Test
    void testAddLike_ProfileNotFound_Then_404() throws Exception {
        this.newLike = new Like(invalidProfileId, postId);
        String newLikeJson = this.objectMapper.writeValueAsString(this.newLike);

        doThrow(new ProfileNotFoundException(invalidProfileId)).when(this.postsService).addLike(false, this.newLike);

        MvcResult result = this.mockMvc.perform(put("/posts/likes?removeLike={removeLike}",false)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newLikeJson))
                .andExpect(status().isNotFound())
                .andExpect(res -> assertTrue(
                        res.getResolvedException() instanceof ProfileNotFoundException
                ))
                .andExpect(jsonPath("$.error").value("Profile " + invalidProfileId + " not found!")).andReturn();
        // Visualizzo l'errore
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n"+resultAsString);
        log.info("Resolved Error ---> "+result.getResolvedException());
    }
/*
    @Test
    void testFindAllLikesByPostId_Then_200() throws Exception {
        List<Like> likeList = asList(
                new Like(1L,postId),
                new Like(2L,postId),
                new Like(3L,postId)
        );
        when(this.postsRepository.checkActiveById(anyLong())).thenReturn(Optional.of(postId));
        when(this.profilesRepository.checkActiveByPostId(anyLong())).thenReturn(Optional.of(profileId));
        when(this.postsService.findAllLikesByPostId(postId)).thenReturn(likeList);

        MvcResult result = this.mockMvc.perform(get("/posts/likes/{postId}",postId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(likeList.size())))
                .andExpect(jsonPath("$[0].profile_id").value(likeList.get(0).getProfileId()))
                .andExpect(jsonPath("$[1].profile_id").value(likeList.get(1).getProfileId()))
                .andExpect(jsonPath("$[2].profile_id").value(likeList.get(2).getProfileId()))
                .andExpect(jsonPath("$[0].post_id").value(likeList.get(0).getPostId()))
                .andExpect(jsonPath("$[1].post_id").value(likeList.get(1).getPostId()))
                .andExpect(jsonPath("$[2].post_id").value(likeList.get(2).getPostId()))
                .andReturn();

        // salvo risposta in result per visualizzarla

        String resultAsString = result.getResponse().getContentAsString();

        log.info(resultAsString);
    }

    @Test
    void testFindAllLikesByPostId_Empty_Then_200() throws Exception {
        List<Like> likeList = new ArrayList<>();
        when(this.postsRepository.checkActiveById(anyLong())).thenReturn(Optional.of(postId));
        when(this.profilesRepository.checkActiveByPostId(anyLong())).thenReturn(Optional.of(profileId));
        when(this.postsService.findAllLikesByPostId(postId)).thenReturn(likeList);

        MvcResult result = this.mockMvc.perform(get("/posts/likes/{postId}",postId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(likeList.size())))
                .andReturn();

        // salvo risposta in result per visualizzarla

        String resultAsString = result.getResponse().getContentAsString();

        log.info(resultAsString);
    }

    @Test
    void testFindAllLikesByPostId_Then_400() throws Exception {
        errors.add("ID is not valid!");
        MvcResult result = this.mockMvc.perform(get("/posts/likes/{postId}","IdNotLong")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(res -> assertTrue(
                        res.getResolvedException() instanceof NumberFormatException
                ))
                .andExpect(jsonPath("$.error").value(errors.get(0))).andReturn();
        // Visualizzo l'errore
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n"+resultAsString);
        log.info("Resolved Error ---> "+result.getResolvedException());
    }

    // TODO 401, 403

    @Test
    void testFindAllLikesByPostId_PostNotFound_Then_404() throws Exception {
        errors.add("Post "+invalidPostId+" not found!");

        when(this.postsService.findAllLikesByPostId(invalidPostId)).thenThrow(new PostNotFoundException(invalidPostId));
        MvcResult result = this.mockMvc.perform(get("/posts/likes/{postId}",invalidPostId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(res -> assertTrue(
                        res.getResolvedException() instanceof PostNotFoundException
                ))
                .andExpect(jsonPath("$.error").value(errors.get(0))).andReturn();
        // Visualizzo l'errore
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n"+resultAsString);
        log.info("Resolved Error ---> "+result.getResolvedException());
    }

    @Test
    void testAddComment_Then_201() throws Exception {
        String content = "Commento al post";
        Long commentId = 1L;
        Comment newComment = new Comment(
                profileId,
                postId,
                content
        );
        Comment savedComment = new Comment(
                profileId,
                postId,
                content
        );
        savedComment.setId(commentId);

        String newCommentJson = this.objectMapper.writeValueAsString(newComment);


        when(this.postsService.addComment(newComment)).thenReturn(savedComment);

        MvcResult result = this.mockMvc.perform(post("/posts/comments")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newCommentJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.*", hasSize(4)))
                .andExpect(jsonPath("$.id").value(savedComment.getId()))
                .andExpect(jsonPath("$.profile_id").value(savedComment.getProfileId()))
                .andExpect(jsonPath("$.post_id").value(savedComment.getPostId()))
                .andExpect(jsonPath("$.content").value(savedComment.getContent()))
                .andReturn();

        // salvo risposta in result per visualizzarla
        String resultAsString = result.getResponse().getContentAsString();
        Comment commentResult = this.objectMapper.readValue(resultAsString, Comment.class);

        log.info(commentResult.toString());
    }

    @Test
    void testAddComment_CommentOnStory_Then_400() throws Exception {
        String content = "Commento al post";
        Long commentId = 1L;
        Comment newComment = new Comment(
                profileId,
                postId,
                content
        );


        String newCommentJson = this.objectMapper.writeValueAsString(newComment);


        when(this.postsService.addComment(newComment)).thenThrow(new CommentOnStoryException());

        MvcResult result = this.mockMvc.perform(post("/posts/comments")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newCommentJson))
                .andExpect(status().isBadRequest())
                .andExpect(res -> assertTrue(res.getResolvedException() instanceof CommentOnStoryException))
                .andExpect(jsonPath("$.error").value("Cannot comment on a story!"))
                .andReturn();
        // Visualizzo l'errore
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n" + resultAsString);
        log.info("Resolved Error ---> " + result.getResolvedException());
    }

    @Test
    void testAddComment_Content_Size_Then_400() throws Exception {
        errors.add("content size must be between 1 and 2200");
        // genero una caption di 2210 caratteri, superando di 10 il limite
        String content = RandomStringUtils.randomAlphabetic(2210);
        Comment newComment = new Comment(
                profileId,
                postId,
                content
        );

        String newCommentJson = this.objectMapper.writeValueAsString(newComment);

        MvcResult result = this.mockMvc.perform(post("/posts/comments")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newCommentJson))
                .andExpect(status().isBadRequest())
                .andExpect(res -> assertTrue(res.getResolvedException() instanceof MethodArgumentNotValidException))
                .andExpect(jsonPath("$.error").isArray())
                .andExpect(jsonPath("$.error", hasSize(1)))
                .andExpect(jsonPath("$.error[0]").value(in(errors)))
                .andReturn();
        // salvo risposta in result solo per visualizzarla
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n" + resultAsString);
    }

    @Test
    void testAddComment_Invalid_Ids_Then_400() throws Exception {
        errors.add("JSON parse error: Cannot deserialize value of type `java.lang.Long` from String \"IdNotLong\": not a valid `java.lang.Long` value");
        // genero una caption di 2210 caratteri, superando di 10 il limite
        String content = "Commento al post";
        Comment newComment = new Comment(
                profileId,
                postId,
                content
        );

        String newCommentJson = this.objectMapper.writeValueAsString(newComment);
        JsonNode jsonNode = this.objectMapper.readTree(newCommentJson);
        ((ObjectNode) jsonNode).put("profile_id", "IdNotLong");
        ((ObjectNode) jsonNode).put("post_id", "IdNotLong");
        newCommentJson = this.objectMapper.writeValueAsString(jsonNode);

        MvcResult result = this.mockMvc.perform(post("/posts/comments")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newCommentJson))
                .andExpect(status().isBadRequest())
                .andExpect(res -> assertTrue(res.getResolvedException() instanceof HttpMessageNotReadableException))
                .andExpect(jsonPath("$.error").value(in(errors)))
                .andReturn();
        // salvo risposta in result solo per visualizzarla
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n" + resultAsString);
    }

    //TODO 401, 403

    // Da sostituire con il 403
    @Test
    void testAddComment_ProfileNotFound_Then_404() throws Exception {
        String content = "Commento al post";
        Comment newComment = new Comment(
                invalidProfileId,
                postId,
                content
        );


        String newCommentJson = this.objectMapper.writeValueAsString(newComment);

        doThrow(new ProfileNotFoundException(invalidProfileId)).when(this.postsService).addComment(newComment);

        MvcResult result = this.mockMvc.perform(post("/posts/comments")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newCommentJson))
                .andExpect(status().isNotFound())
                .andExpect(res -> assertTrue(
                        res.getResolvedException() instanceof ProfileNotFoundException
                ))
                .andExpect(jsonPath("$.error").value("Profile " + invalidProfileId + " not found!"))
                .andReturn();
        // Visualizzo l'errore
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n" + resultAsString);
        log.info("Resolved Error ---> " + result.getResolvedException());
    }

    @Test
    void testAddComment_PostNotFound_Then_404() throws Exception {
        String content = "Commento al post";
        Comment newComment = new Comment(
                profileId,
                invalidPostId,
                content
        );


        String newCommentJson = this.objectMapper.writeValueAsString(newComment);

        doThrow(new PostNotFoundException(invalidPostId)).when(this.postsService).addComment(newComment);

        MvcResult result = this.mockMvc.perform(post("/posts/comments")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newCommentJson))
                .andExpect(status().isNotFound())
                .andExpect(res -> assertTrue(
                        res.getResolvedException() instanceof PostNotFoundException
                ))
                .andExpect(jsonPath("$.error").value("Post " + invalidProfileId + " not found!"))
                .andReturn();
        // Visualizzo l'errore
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n" + resultAsString);
        log.info("Resolved Error ---> " + result.getResolvedException());
    }

    @Test
    void testUpdatedCommentById_Then_200() throws Exception {
        Long commentId = 1L;
        String content = "Commento modificato";
        CommentPatch commentPatch = new CommentPatch(content);

        Comment updatedComment = new Comment(1L,1L,content);
        updatedComment.setId(commentId);
        String commentPatchJson = this.objectMapper.writeValueAsString(commentPatch);

        when(this.commentsRepository.checkActiveById(anyLong())).thenReturn(Optional.of(commentId));
        when(this.postsRepository.checkActiveByCommentId(anyLong())).thenReturn(Optional.of(postId));
        when(this.profilesRepository.checkActiveByPostId(anyLong())).thenReturn(Optional.of(profileId));
        when(this.postsService.updateCommentById(commentId, commentPatch)).thenReturn(updatedComment);

        MvcResult result = this.mockMvc.perform(patch("/posts/comments/{commentId}",commentId)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commentPatchJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.*", hasSize(4)))
                .andExpect(jsonPath("$.id").value(updatedComment.getId()))
                .andExpect(jsonPath("$.profile_id").value(updatedComment.getProfileId()))
                .andExpect(jsonPath("$.post_id").value(updatedComment.getPostId()))
                .andExpect(jsonPath("$.content").value(updatedComment.getContent()))
                .andReturn();

        // salvo risposta in result per visualizzarla
        String resultAsString = result.getResponse().getContentAsString();
        Comment commentResult = this.objectMapper.readValue(resultAsString, Comment.class);

        log.info(commentResult.toString());
    }

    @Test
    void testUpdatedCommentById_Then_400() throws Exception {
        errors.add("ID is not valid!");
        MvcResult result = this.mockMvc.perform(get("/posts/comments/{commentId}","IdNotLong")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(res -> assertTrue(
                        res.getResolvedException() instanceof NumberFormatException
                ))
                .andExpect(jsonPath("$.error").value(errors.get(0))).andReturn();
        // Visualizzo l'errore
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n"+resultAsString);
        log.info("Resolved Error ---> "+result.getResolvedException());
    }

    @Test
    void testUpdatedCommentById_Content_Size_Then_400() throws Exception {
        errors.add("content size must be between 1 and 2200");
        // genero una caption di 2210 caratteri, superando di 10 il limite
        Long commentId = 1L;
        String content = RandomStringUtils.randomAlphabetic(2210);
        CommentPatch commentPatch = new CommentPatch(content);

        String commentPatchJson = this.objectMapper.writeValueAsString(commentPatch);

        when(this.commentsRepository.checkActiveById(anyLong())).thenReturn(Optional.of(commentId));
        when(this.postsRepository.checkActiveByCommentId(anyLong())).thenReturn(Optional.of(postId));
        when(this.profilesRepository.checkActiveByPostId(anyLong())).thenReturn(Optional.of(profileId));

        MvcResult result = this.mockMvc.perform(patch("/posts/comments/{commentId}",commentId)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commentPatchJson))
                .andExpect(status().isBadRequest())
                .andExpect(res -> assertTrue(res.getResolvedException() instanceof MethodArgumentNotValidException))
                .andExpect(jsonPath("$.error").isArray())
                .andExpect(jsonPath("$.error", hasSize(1)))
                .andExpect(jsonPath("$.error[0]").value(in(errors)))
                .andReturn();
        // salvo risposta in result solo per visualizzarla
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n" + resultAsString);
    }


    //TODO 401, 403

    // Da sostituire con il 403
    @Test
    void testUpdatedCommentById_CommentNotFound_Then_404() throws Exception {
        Long commentId = Long.MIN_VALUE;
        String content = "Commento al post";
        CommentPatch commentPatch = new CommentPatch(content);

        String commentPatchJson = this.objectMapper.writeValueAsString(commentPatch);

        doThrow(new CommentNotFoundException(commentId)).when(this.postsService).updateCommentById(commentId, commentPatch);

        MvcResult result = this.mockMvc.perform(patch("/posts/comments/{commentId}",commentId)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commentPatchJson))
                .andExpect(status().isNotFound())
                .andExpect(res -> assertTrue(
                        res.getResolvedException() instanceof CommentNotFoundException
                ))
                .andExpect(jsonPath("$.error").value("Comment " + commentId + " not found!"))
                .andReturn();
        // Visualizzo l'errore
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n" + resultAsString);
        log.info("Resolved Error ---> " + result.getResolvedException());
    }

    @Test
    void testDeleteCommentById_Then_204() throws Exception {
        Long commentId = 1L;
        when(this.commentsRepository.checkActiveById(anyLong())).thenReturn(Optional.of(commentId));
        when(this.postsRepository.checkActiveByCommentId(anyLong())).thenReturn(Optional.of(postId));
        when(this.profilesRepository.checkActiveByPostId(anyLong())).thenReturn(Optional.of(profileId));
        doNothing().when(this.postsService).deleteCommentById(commentId);

        this.mockMvc.perform(delete("/posts/comments/{commentId}", commentId))
                .andExpect(status().isNoContent());
    }

    @Test
    void testDeleteCommentById_Then_400() throws Exception {
        errors.add("ID is not valid!");
        MvcResult result = this.mockMvc.perform(delete("/posts/comments/IdNotLong"))
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

    // TODO 401, 403
    @Test
    void testDeleteCommentById_Then_404() throws Exception {
        Long invalidCommentId = Long.MIN_VALUE;
        doThrow(new CommentNotFoundException(invalidCommentId)).when(this.postsService).deleteCommentById(invalidCommentId);

        MvcResult result = this.mockMvc.perform(delete("/posts/comments/{commentId}",invalidCommentId))
                .andExpect(status().isNotFound())
                .andExpect(res -> assertTrue(
                        res.getResolvedException() instanceof CommentNotFoundException
                ))
                .andExpect(jsonPath("$.error").value("Comment "+invalidCommentId+" not found!"))
                .andReturn();
        // Visualizzo l'errore
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n"+resultAsString);
        log.info("Resolved Error ---> "+result.getResolvedException());
    }

    @Test
    void testFindAllCommentsByPostId_Then_200() throws Exception {
        List<Comment> commentList = asList(
                new Comment(1L, postId, "Commento1"),
                new Comment(2L, postId, "Commento2"),
                new Comment(3L, postId, "Commento3")
        );
        commentList.get(0).setId(1L);
        commentList.get(1).setId(2L);
        commentList.get(2).setId(3L);
        when(this.postsRepository.checkActiveById(anyLong())).thenReturn(Optional.of(postId));
        when(this.profilesRepository.checkActiveByPostId(anyLong())).thenReturn(Optional.of(profileId));
        when(this.postsService.findAllCommentsByPostId(postId)).thenReturn(commentList);

        MvcResult result = this.mockMvc.perform(get("/posts/comments/{postId}",postId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(commentList.size())))
                .andExpect(jsonPath("$[0].id").value(commentList.get(0).getId()))
                .andExpect(jsonPath("$[0].profile_id").value(commentList.get(0).getProfileId()))
                .andExpect(jsonPath("$[0].post_id").value(commentList.get(0).getPostId()))
                .andExpect(jsonPath("$[0].content").value(commentList.get(0).getContent()))
                .andExpect(jsonPath("$[1].id").value(commentList.get(1).getId()))
                .andExpect(jsonPath("$[1].profile_id").value(commentList.get(1).getProfileId()))
                .andExpect(jsonPath("$[1].post_id").value(commentList.get(1).getPostId()))
                .andExpect(jsonPath("$[1].content").value(commentList.get(1).getContent()))
                .andExpect(jsonPath("$[2].id").value(commentList.get(2).getId()))
                .andExpect(jsonPath("$[2].profile_id").value(commentList.get(2).getProfileId()))
                .andExpect(jsonPath("$[2].post_id").value(commentList.get(2).getPostId()))
                .andExpect(jsonPath("$[2].content").value(commentList.get(2).getContent()))
                .andReturn();

        // salvo risposta in result per visualizzarla
        String resultAsString = result.getResponse().getContentAsString();

        log.info(resultAsString);
    }

    @Test
    void testFindAllCommentsByPostId_InvalidId_Then_400() throws Exception {
        errors.add("ID is not valid!");
        MvcResult result = this.mockMvc.perform(get("/posts/comments/IdNotLong")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(res -> assertTrue(
                        res.getResolvedException() instanceof NumberFormatException
                ))
                .andExpect(jsonPath("$.error").value(errors.get(0))).andReturn();
        // Visualizzo l'errore
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n"+resultAsString);
        log.info("Resolved Error ---> "+result.getResolvedException());
    }

    //TODO 401,403
    @Test
    void testFindAllCommentsByPostId_PostNotFound_Then_404() throws Exception {
        when(this.postsRepository.checkActiveById(invalidPostId)).thenReturn(Optional.empty());
        doThrow(new PostNotFoundException(invalidPostId)).when(this.postsService).findAllCommentsByPostId(invalidPostId);

        MvcResult result = this.mockMvc.perform(get("/posts/comments/{postId}",invalidPostId))
                .andExpect(status().isNotFound())
                .andExpect(res -> assertTrue(
                        res.getResolvedException() instanceof PostNotFoundException
                ))
                .andExpect(jsonPath("$.error").value("Post "+invalidPostId+" not found!"))
                .andReturn();
        // Visualizzo l'errore
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n"+resultAsString);
        log.info("Resolved Error ---> "+result.getResolvedException());
    }

    @Test
    void testProfileFeedByProfileId_Then_200() throws Exception{
        List<Post> postList = createPostList();
        when(this.profilesRepository.checkActiveById(anyLong())).thenReturn(Optional.of(profileId));
        when(this.postsService.profileFeedByProfileId(profileId, null)).thenReturn(postList);

        MvcResult result = this.mockMvc.perform(get("/posts/feed/{profileId}?onlyPost={onlyPost}", profileId,null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[0].id").value(postList.get(0).getId()))
                .andExpect(jsonPath("$[1].id").value(postList.get(1).getId()))
                .andExpect(jsonPath("$[2].id").value(postList.get(2).getId()))
                .andExpect(jsonPath("$[3].id").value(postList.get(3).getId()))
                .andReturn();

        // salvo risposta in result per visualizzarla
        String resultAsString = result.getResponse().getContentAsString();

        log.info(resultAsString);
    }

    @Test
    void testProfileFeedByProfileId_OnlyPost_Then_200() throws Exception{
        List<Post> postList = createPostList().stream().filter(post -> post.getPostType().equals(Post.PostTypeEnum.POST)).toList();
        when(this.profilesRepository.checkActiveById(anyLong())).thenReturn(Optional.of(profileId));
        when(this.postsService.profileFeedByProfileId(anyLong(), anyBoolean())).thenReturn(postList);

        MvcResult result = this.mockMvc.perform(get("/posts/feed/{profileId}?onlyPost={onlyPost}", profileId,  true))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(postList.get(0).getId()))
                .andExpect(jsonPath("$[0].post_type").value(postList.get(0).getPostType().getValue()))
                .andExpect(jsonPath("$[1].id").value(postList.get(1).getId()))
                .andExpect(jsonPath("$[1].post_type").value(postList.get(1).getPostType().getValue()))
                .andReturn();

        // salvo risposta in result per visualizzarla
        String resultAsString = result.getResponse().getContentAsString();

        log.info(resultAsString);
    }

    @Test
    void testProfileFeedByProfileId_OnlyStories_Then_200() throws Exception{
        List<Post> postList = createPostList().stream().filter(post -> post.getPostType().equals(Post.PostTypeEnum.STORY)).toList();
        when(this.profilesRepository.checkActiveById(anyLong())).thenReturn(Optional.of(profileId));
        when(this.postsService.profileFeedByProfileId(anyLong(), anyBoolean())).thenReturn(postList);

        MvcResult result = this.mockMvc.perform(get("/posts/feed/{profileId}?onlyPost={onlyPost}", profileId,  false))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(postList.get(0).getId()))
                .andExpect(jsonPath("$[0].post_type").value(postList.get(0).getPostType().getValue()))
                .andExpect(jsonPath("$[1].id").value(postList.get(1).getId()))
                .andExpect(jsonPath("$[1].post_type").value(postList.get(1).getPostType().getValue()))
                .andReturn();

        // salvo risposta in result per visualizzarla
        String resultAsString = result.getResponse().getContentAsString();

        log.info(resultAsString);
    }

    @Test
    void testProfileFeedByProfileId_InvalidId_Then_400() throws Exception {
        errors.add("ID is not valid!");
        MvcResult result = this.mockMvc.perform(get("/posts/feed/IdNotLong"))
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

    @Test
    void testProfileFeedByProfileId_Then_400() throws Exception {
        when(this.profilesRepository.checkActiveById(anyLong())).thenReturn(Optional.of(profileId));
        MvcResult result = this.mockMvc.perform(get("/posts/feed/{profileId}?onlyPost={onlyPost}", profileId, "NotBoolean")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(res -> assertTrue(
                        res.getResolvedException() instanceof MethodArgumentTypeMismatchException
                ))
                .andReturn();

        // salvo risposta in result solo per visualizzarla
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n" + resultAsString);
    }

    @Test
    void testProfileFeedByProfileId_Then_404() throws Exception {
        when(this.profilesRepository.checkActiveById(anyLong())).thenReturn(Optional.empty());
        MvcResult result = this.mockMvc.perform(get("/posts/feed/{profileId}?onlyPost={onlyPost}", profileId, null)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(res -> assertTrue(
                        res.getResolvedException() instanceof ProfileNotFoundException
                ))
                .andExpect(jsonPath("$.error").value("Profile " + profileId + " not found!"))
                .andReturn();

        // salvo risposta in result solo per visualizzarla
        String resultAsString = result.getResponse().getContentAsString();
        log.info("Errors\n" + resultAsString);
    }

    private List<Post> createPostList() {
        Post post1 = new Post(contentUrl, Post.PostTypeEnum.POST, 3L);
        post1.setCaption("First Post Caption");
        post1.setId(1L);

        Post post2 = new Post(contentUrl, Post.PostTypeEnum.POST, 4L);
        post2.setCaption("Second Post Caption");
        post2.setId(2L);

        Post post3 = new Post(contentUrl, Post.PostTypeEnum.STORY, 3L);
        post3.setId(3L);

        Post post4 = new Post(contentUrl, Post.PostTypeEnum.STORY, 8L);
        post4.setId(4L);

        List<Post> postList = new ArrayList<>();
        postList.add(post1);
        postList.add(post2);
        postList.add(post3);
        postList.add(post4);
        return postList;
    }

     */

}
