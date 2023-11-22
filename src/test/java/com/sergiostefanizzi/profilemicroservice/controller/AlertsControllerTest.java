package com.sergiostefanizzi.profilemicroservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sergiostefanizzi.profilemicroservice.model.Alert;
import com.sergiostefanizzi.profilemicroservice.repository.AlertsRepository;
import com.sergiostefanizzi.profilemicroservice.repository.CommentsRepository;
import com.sergiostefanizzi.profilemicroservice.repository.PostsRepository;
import com.sergiostefanizzi.profilemicroservice.repository.ProfilesRepository;
import com.sergiostefanizzi.profilemicroservice.service.AlertsService;
import com.sergiostefanizzi.profilemicroservice.system.exception.AlertTypeErrorException;
import com.sergiostefanizzi.profilemicroservice.system.exception.CommentNotFoundException;
import com.sergiostefanizzi.profilemicroservice.system.exception.PostNotFoundException;
import com.sergiostefanizzi.profilemicroservice.system.exception.ProfileNotFoundException;
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
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(AlertsController.class)
@ActiveProfiles("test")
@Slf4j
public class AlertsControllerTest {
    /*
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private AlertsService alertsService;
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
    private Alert postAlert;
    private Alert commentAlert;
    String alertReason = "Motivo della segnalazione";
    Long profileIdAlertOwner = 1L;
    Long postId = 1L;
    Long commentId = 1L;
    Long alertId = 1L;
    @BeforeEach
    void setUp() {
        this.postAlert = new Alert(profileIdAlertOwner, alertReason);
        this.postAlert.setPostId(postId);
        this.postAlert.setId(alertId);

        this.commentAlert = new Alert(profileIdAlertOwner, alertReason);
        this.commentAlert.setCommentId(commentId);
        this.commentAlert.setId(alertId);
    }

    @AfterEach
    void tearDown() {

    }
    @Test
    void testCreateAlert_PostAlert_Then_201() throws Exception {
        when(this.alertsService.createAlert(anyBoolean(),any(Alert.class))).thenReturn(this.postAlert);

        String postAlertJson = this.objectMapper.writeValueAsString(this.postAlert);

        MvcResult result = this.mockMvc.perform(post("/alerts?isPost={isPost}",true)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(postAlertJson)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.*", hasSize(4)))
                .andExpect(jsonPath("$.id").value(this.postAlert.getId()))
                .andExpect(jsonPath("$.created_by").value(this.postAlert.getCreatedBy()))
                .andExpect(jsonPath("$.post_id").value(this.postAlert.getPostId()))
                .andExpect(jsonPath("$.reason").value(this.postAlert.getReason()))
                .andReturn();

        // salvo risposta in result per visualizzarla
        String resultAsString = result.getResponse().getContentAsString();
        Alert alertResult = this.objectMapper.readValue(resultAsString, Alert.class);

        log.info(alertResult.toString());
    }

    @Test
    void testCreateAlert_CommentAlert_Then_201() throws Exception {
        when(this.alertsService.createAlert(anyBoolean(),any(Alert.class))).thenReturn(this.commentAlert);

        String commentAlertJson = this.objectMapper.writeValueAsString(this.commentAlert);

        MvcResult result = this.mockMvc.perform(post("/alerts?isPost={isPost}",false)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(commentAlertJson)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.*", hasSize(4)))
                .andExpect(jsonPath("$.id").value(this.commentAlert.getId()))
                .andExpect(jsonPath("$.created_by").value(this.commentAlert.getCreatedBy()))
                .andExpect(jsonPath("$.comment_id").value(this.commentAlert.getCommentId()))
                .andExpect(jsonPath("$.reason").value(this.commentAlert.getReason()))
                .andReturn();

        // salvo risposta in result per visualizzarla
        String resultAsString = result.getResponse().getContentAsString();
        Alert alertResult = this.objectMapper.readValue(resultAsString, Alert.class);

        log.info(alertResult.toString());
    }

    @Test
    void testCreateAlert_PostAlert_AlertTypeErrorException_isPost_False_Then_400() throws Exception {
        String errorMsg = "Alert type error";
        when(this.alertsService.createAlert(anyBoolean(),any(Alert.class))).thenThrow(new AlertTypeErrorException(errorMsg));

        String postAlertJson = this.objectMapper.writeValueAsString(this.postAlert);

        MvcResult result = this.mockMvc.perform(post("/alerts?isPost={isPost}",false)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(postAlertJson)
                )
                .andExpect(status().isBadRequest())
                .andExpect(res -> assertTrue(res.getResolvedException() instanceof AlertTypeErrorException))
                .andExpect(jsonPath("$.error").value(errorMsg))
                .andReturn();

        // salvo risposta in result per visualizzarla
        String resultAsString = result.getResponse().getContentAsString();
        log.info("\nErrors\n" +resultAsString);
    }

    @Test
    void testCreateAlert_PostAlert_MissingServletRequestParameterException_isPost_Missing_Then_400() throws Exception {
        String errorMsg = "Required request parameter 'isPost' for method parameter type Boolean is not present";

        String postAlertJson = this.objectMapper.writeValueAsString(this.postAlert);

        MvcResult result = this.mockMvc.perform(post("/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(postAlertJson)
                )
                .andExpect(status().isBadRequest())
                .andExpect(res -> assertTrue(res.getResolvedException() instanceof MissingServletRequestParameterException))
                .andExpect(jsonPath("$.error").value(errorMsg))
                .andReturn();

        // salvo risposta in result per visualizzarla
        String resultAsString = result.getResponse().getContentAsString();
        log.info("\nErrors\n" +resultAsString);
    }

    @Test
    void testCreateAlert_PostAlert_MethodArgumentTypeMismatchException_isPost_Missing_Then_400() throws Exception {
        String errorMsg = "Failed to convert value of type 'java.lang.String' to required type 'java.lang.Boolean'; Invalid boolean value [NotBoolean]";

        String postAlertJson = this.objectMapper.writeValueAsString(this.postAlert);

        MvcResult result = this.mockMvc.perform(post("/alerts?isPost={isPost}","NotBoolean")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(postAlertJson)
                )
                .andExpect(status().isBadRequest())
                .andExpect(res -> assertTrue(res.getResolvedException() instanceof MethodArgumentTypeMismatchException))
                .andExpect(jsonPath("$.error").value(errorMsg))
                .andReturn();

        // salvo risposta in result per visualizzarla
        String resultAsString = result.getResponse().getContentAsString();
        log.info("\nErrors\n" +resultAsString);
    }

    @Test
    void testCreateAlert_PostAlert_ProfileNotFoundException_Then_404() throws Exception {
        String errorMsg = "Profile "+this.postAlert.getCreatedBy()+" not found!";
        when(this.alertsService.createAlert(anyBoolean(),any(Alert.class))).thenThrow(new ProfileNotFoundException(this.postAlert.getCreatedBy()));


        String postAlertJson = this.objectMapper.writeValueAsString(this.postAlert);

        MvcResult result = this.mockMvc.perform(post("/alerts?isPost={isPost}",true)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(postAlertJson)
                )
                .andExpect(status().isNotFound())
                .andExpect(res -> assertTrue(res.getResolvedException() instanceof ProfileNotFoundException))
                .andExpect(jsonPath("$.error").value(errorMsg))
                .andReturn();

        // salvo risposta in result per visualizzarla
        String resultAsString = result.getResponse().getContentAsString();
        log.info("\nErrors\n" +resultAsString);
    }

    @Test
    void testCreateAlert_PostAlert_PostNotFoundException_Then_404() throws Exception {
        String errorMsg = "Post "+this.postAlert.getCreatedBy()+" not found!";
        when(this.alertsService.createAlert(anyBoolean(),any(Alert.class))).thenThrow(new PostNotFoundException(this.postAlert.getPostId()));


        String postAlertJson = this.objectMapper.writeValueAsString(this.postAlert);

        MvcResult result = this.mockMvc.perform(post("/alerts?isPost={isPost}",true)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(postAlertJson)
                )
                .andExpect(status().isNotFound())
                .andExpect(res -> assertTrue(res.getResolvedException() instanceof PostNotFoundException))
                .andExpect(jsonPath("$.error").value(errorMsg))
                .andReturn();

        // salvo risposta in result per visualizzarla
        String resultAsString = result.getResponse().getContentAsString();
        log.info("\nErrors\n" +resultAsString);
    }

    @Test
    void testCreateAlert_CommentAlert_CommentNotFoundException_Then_404() throws Exception {
        String errorMsg = "Comment "+this.postAlert.getCreatedBy()+" not found!";
        when(this.alertsService.createAlert(anyBoolean(),any(Alert.class))).thenThrow(new CommentNotFoundException(this.commentAlert.getCommentId()));


        String postAlertJson = this.objectMapper.writeValueAsString(this.postAlert);

        MvcResult result = this.mockMvc.perform(post("/alerts?isPost={isPost}",false)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(postAlertJson)
                )
                .andExpect(status().isNotFound())
                .andExpect(res -> assertTrue(res.getResolvedException() instanceof CommentNotFoundException))
                .andExpect(jsonPath("$.error").value(errorMsg))
                .andReturn();

        // salvo risposta in result per visualizzarla
        String resultAsString = result.getResponse().getContentAsString();
        log.info("\nErrors\n" +resultAsString);
    }

     */
}
