package com.sergiostefanizzi.profilemicroservice.service;

import com.sergiostefanizzi.profilemicroservice.model.*;
import com.sergiostefanizzi.profilemicroservice.model.converter.AlertToAlertJpaConverter;
import com.sergiostefanizzi.profilemicroservice.repository.AlertsRepository;
import com.sergiostefanizzi.profilemicroservice.repository.CommentsRepository;
import com.sergiostefanizzi.profilemicroservice.repository.PostsRepository;
import com.sergiostefanizzi.profilemicroservice.repository.ProfilesRepository;
import com.sergiostefanizzi.profilemicroservice.system.exception.AlertTypeErrorException;
import com.sergiostefanizzi.profilemicroservice.system.exception.ProfileNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@Slf4j
public class AlertsServiceTest {
    /*
    @InjectMocks
    private AlertsService alertsService;
    @Mock
    private AlertsRepository alertsRepository;
    @Mock
    private ProfilesRepository profilesRepository;
    @Mock
    private PostsRepository postsRepository;
    @Mock
    private CommentsRepository commentsRepository;
    @Mock
    private AlertToAlertJpaConverter alertToAlertJpaConverter;
    String profileName = "pinco_pallino";
    String profileNamePostOwner = "giuseppeVerdi";
    Boolean profileIsPrivate = false;
    Long profileId = 1L;
    Long profileIdPostOwner = 2L;
    Long accountId = 1L;
    Long accountIdPostOwner = 2L;
    Long postId = 1L;
    Long commentId = 1L;

    private ProfileJpa alertOwner;
    private Alert postAlertToCreate;
    private Alert commentAlertToCreate;
    String alertReason = "Motivo della segnalazione";
    private PostJpa postJpa;
    private CommentJpa commentJpa;
    String contentUrl = "https://upload.wikimedia.org/wikipedia/commons/9/9a/Cape_may.jpg";
    Post.PostTypeEnum postType = Post.PostTypeEnum.POST;
    private AlertJpa alertJpa;
    Long alertId = 1L;


    @BeforeEach
    void setUp() {
        this.alertOwner = new ProfileJpa(profileName, profileIsPrivate,accountId);
        this.alertOwner.setId(profileId);
        this.alertOwner.setCreatedAt(LocalDateTime.MIN);

        ProfileJpa postOwner = new ProfileJpa(profileNamePostOwner, profileIsPrivate, accountIdPostOwner);
        postOwner.setId(profileIdPostOwner);
        postOwner.setCreatedAt(LocalDateTime.MIN);

        this.postAlertToCreate = new Alert(profileId, alertReason);
        this.postAlertToCreate.setPostId(postId);

        this.commentAlertToCreate = new Alert(profileId, alertReason);
        this.commentAlertToCreate.setCommentId(commentId);

        this.postJpa = new PostJpa(contentUrl, postType);
        this.postJpa.setId(postId);
        this.postJpa.setCreatedAt(LocalDateTime.MIN);
        this.postJpa.setProfile(postOwner);

        this.commentJpa = new CommentJpa("Commento");
        this.commentJpa.setId(commentId);
        this.commentJpa.setPost(this.postJpa);
        //per comodita' inserisco lo stesi autore del post
        this.commentJpa.setProfile(postOwner);

        this.alertJpa = new AlertJpa(alertReason);
    }

    @Test
    void testCreate_PostAlert_Success(){
        LocalDateTime creationTime = LocalDateTime.now();

        AlertJpa savedAlertJpa = new AlertJpa(alertReason);
        savedAlertJpa.setPost(this.postJpa);
        savedAlertJpa.setCreatedBy(this.alertOwner);
        savedAlertJpa.setCreatedAt(creationTime);

        Alert convertedAlert = new Alert(this.alertOwner.getId(), this.postAlertToCreate.getReason());
        convertedAlert.setId(alertId);
        convertedAlert.setPostId(this.postId);

        when(this.profilesRepository.findActiveById(anyLong())).thenReturn(Optional.of(this.alertOwner));
        when(this.postsRepository.findActiveById(anyLong())).thenReturn(Optional.of(this.postJpa));
        when(this.alertToAlertJpaConverter.convert(any(Alert.class))).thenReturn(this.alertJpa);
        when(this.alertsRepository.save(any(AlertJpa.class))).thenReturn(savedAlertJpa);
        when(this.alertToAlertJpaConverter.convertBack(any(AlertJpa.class))).thenReturn(convertedAlert);

        Alert savedAlert = this.alertsService.createAlert(true, this.postAlertToCreate);

        assertEquals(convertedAlert, savedAlert);
        verify(this.profilesRepository, times(1)).findActiveById(anyLong());
        verify(this.postsRepository, times(1)).findActiveById(anyLong());
        verify(this.commentsRepository, times(0)).findActiveById(anyLong());
        verify(this.alertToAlertJpaConverter, times(1)).convert(any(Alert.class));
        verify(this.alertsRepository, times(1)).save(any(AlertJpa.class));
        verify(this.alertToAlertJpaConverter, times(1)).convertBack(any(AlertJpa.class));
        log.info("\nPost Alert:\n"+savedAlert.toString());
    }

    @Test
    void testCreate_CommentAlert_Success(){
        LocalDateTime creationTime = LocalDateTime.now();

        AlertJpa savedAlertJpa = new AlertJpa(alertReason);
        savedAlertJpa.setComment(this.commentJpa);
        savedAlertJpa.setCreatedBy(this.alertOwner);
        savedAlertJpa.setCreatedAt(creationTime);

        Alert convertedAlert = new Alert(this.alertOwner.getId(), this.commentAlertToCreate.getReason());
        convertedAlert.setId(alertId);
        convertedAlert.setCommentId(this.commentId);

        when(this.profilesRepository.findActiveById(anyLong())).thenReturn(Optional.of(this.alertOwner));
        when(this.commentsRepository.findActiveById(anyLong())).thenReturn(Optional.of(this.commentJpa));
        when(this.alertToAlertJpaConverter.convert(any(Alert.class))).thenReturn(this.alertJpa);
        when(this.alertsRepository.save(any(AlertJpa.class))).thenReturn(savedAlertJpa);
        when(this.alertToAlertJpaConverter.convertBack(any(AlertJpa.class))).thenReturn(convertedAlert);

        Alert savedAlert = this.alertsService.createAlert(false, this.commentAlertToCreate);

        assertEquals(convertedAlert, savedAlert);
        verify(this.profilesRepository, times(1)).findActiveById(anyLong());
        verify(this.postsRepository, times(0)).findActiveById(anyLong());
        verify(this.commentsRepository, times(1)).findActiveById(anyLong());
        verify(this.alertToAlertJpaConverter, times(1)).convert(any(Alert.class));
        verify(this.alertsRepository, times(1)).save(any(AlertJpa.class));
        verify(this.alertToAlertJpaConverter, times(1)).convertBack(any(AlertJpa.class));
        log.info("\nComment Alert:\n"+savedAlert.toString());
    }

    @Test
    void testCreate_PostAlert_AlertTypeErrorException_Failed(){
        when(this.profilesRepository.findActiveById(anyLong())).thenReturn(Optional.of(this.alertOwner));

        assertThrows(AlertTypeErrorException.class, () -> this.alertsService.createAlert(false, this.postAlertToCreate));

        verify(this.profilesRepository, times(1)).findActiveById(anyLong());
        verify(this.postsRepository, times(0)).findActiveById(anyLong());
        verify(this.commentsRepository, times(0)).findActiveById(anyLong());
        verify(this.alertToAlertJpaConverter, times(0)).convert(any(Alert.class));
        verify(this.alertsRepository, times(0)).save(any(AlertJpa.class));
        verify(this.alertToAlertJpaConverter, times(0)).convertBack(any(AlertJpa.class));
    }

    @Test
    void testCreate_CommentAlert_AlertTypeNotSpecifiedException_Failed(){
        when(this.profilesRepository.findActiveById(anyLong())).thenReturn(Optional.of(this.alertOwner));

        assertThrows(AlertTypeErrorException.class, () -> this.alertsService.createAlert(true, this.commentAlertToCreate));

        verify(this.profilesRepository, times(1)).findActiveById(anyLong());
        verify(this.postsRepository, times(0)).findActiveById(anyLong());
        verify(this.commentsRepository, times(0)).findActiveById(anyLong());
        verify(this.alertToAlertJpaConverter, times(0)).convert(any(Alert.class));
        verify(this.alertsRepository, times(0)).save(any(AlertJpa.class));
        verify(this.alertToAlertJpaConverter, times(0)).convertBack(any(AlertJpa.class));
    }

    @Test
    void testCreate_Alert_ProfileNotFoundException_Failed(){
        when(this.profilesRepository.findActiveById(anyLong())).thenReturn(Optional.empty());

        assertThrows(ProfileNotFoundException.class, () -> this.alertsService.createAlert(true, this.postAlertToCreate));

        verify(this.profilesRepository, times(1)).findActiveById(anyLong());
        verify(this.postsRepository, times(0)).findActiveById(anyLong());
        verify(this.commentsRepository, times(0)).findActiveById(anyLong());
        verify(this.alertToAlertJpaConverter, times(0)).convert(any(Alert.class));
        verify(this.alertsRepository, times(0)).save(any(AlertJpa.class));
        verify(this.alertToAlertJpaConverter, times(0)).convertBack(any(AlertJpa.class));
    }

     */
}
