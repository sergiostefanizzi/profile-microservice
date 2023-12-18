package com.sergiostefanizzi.profilemicroservice.service;

import com.sergiostefanizzi.profilemicroservice.model.*;
import com.sergiostefanizzi.profilemicroservice.model.converter.AlertToAlertJpaConverter;
import com.sergiostefanizzi.profilemicroservice.repository.*;
import com.sergiostefanizzi.profilemicroservice.system.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@Slf4j
class AlertsServiceTest {

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
    private FollowsRepository followsRepository;
    @Mock
    private AlertToAlertJpaConverter alertToAlertJpaConverter;
    @Mock
    private SecurityContext securityContext;
    @Mock
    private KeycloakService keycloakService;
    private final String accountId1 = UUID.randomUUID().toString();
    private final String accountIdPostOwner = UUID.randomUUID().toString();
    private final Long postId = 1L;
    private final Long commentId = 1L;

    private ProfileJpa alertOwner;
    private Alert postAlertToCreate;
    private Alert commentAlertToCreate;
    private final String alertReason = "Motivo della segnalazione";
    private PostJpa postJpa;
    private PostJpa privatePostJpa;
    private CommentJpa commentJpa;
    private final Post.PostTypeEnum postType = Post.PostTypeEnum.POST;
    private AlertJpa alertJpa;
    private final Long alertId = 1L;
    private JwtAuthenticationToken jwtAuthenticationToken;
    private JwtAuthenticationToken jwtAuthenticationTokenWithProfileList;


    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);

        Boolean profileIsPrivate = false;
        String profileName = "pinco_pallino";
        this.alertOwner = new ProfileJpa(profileName, profileIsPrivate, this.accountId1);
        Long profileId = 1L;
        this.alertOwner.setId(profileId);
        this.alertOwner.setCreatedAt(LocalDateTime.MIN);

        String profileNamePostOwner = "giuseppeVerdi";
        ProfileJpa postOwner = new ProfileJpa(profileNamePostOwner, profileIsPrivate, accountIdPostOwner);
        Long profileIdPostOwner = 2L;
        postOwner.setId(profileIdPostOwner);
        postOwner.setCreatedAt(LocalDateTime.MIN);

        ProfileJpa privatePostOwner = new ProfileJpa(profileNamePostOwner, true, accountIdPostOwner);
        privatePostOwner.setId(profileIdPostOwner);
        privatePostOwner.setCreatedAt(LocalDateTime.MIN);

        this.postAlertToCreate = new Alert(profileId, alertReason);
        this.postAlertToCreate.setPostId(postId);


        this.commentAlertToCreate = new Alert(profileId, alertReason);
        this.commentAlertToCreate.setCommentId(commentId);

        String contentUrl = "https://upload.wikimedia.org/wikipedia/commons/9/9a/Cape_may.jpg";
        this.postJpa = new PostJpa(contentUrl, postType);
        this.postJpa.setId(postId);
        this.postJpa.setCreatedAt(LocalDateTime.MIN);
        this.postJpa.setProfile(postOwner);

        this.privatePostJpa = new PostJpa(contentUrl, postType);
        this.privatePostJpa.setId(postId);
        this.privatePostJpa.setCreatedAt(LocalDateTime.MIN);
        this.privatePostJpa.setProfile(privatePostOwner);

        this.commentJpa = new CommentJpa("Commento");
        this.commentJpa.setId(commentId);
        this.commentJpa.setPost(this.postJpa);
        //per comodita' inserisco lo stesi autore del post
        this.commentJpa.setProfile(postOwner);

        this.alertJpa = new AlertJpa(alertReason);

        Map<String, Object> headers = new HashMap<>();
        headers.put("alg","HS256");
        headers.put("typ","JWT");
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", UUID.randomUUID().toString());

        this.jwtAuthenticationToken = new JwtAuthenticationToken(
                new Jwt("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c",
                        Instant.now(),
                        Instant.MAX,
                        headers,
                        claims)
        );
        claims.put("profileList", List.of(this.alertOwner.getId()));
        this.jwtAuthenticationTokenWithProfileList = new JwtAuthenticationToken(
                new Jwt("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c",
                        Instant.now(),
                        Instant.MAX,
                        headers,
                        claims)
        );

    }

    @Test
    void testCreate_PublicPostAlert_Success(){
        LocalDateTime creationTime = LocalDateTime.now();

        AlertJpa savedAlertJpa = new AlertJpa(alertReason);
        savedAlertJpa.setPost(this.postJpa);
        savedAlertJpa.setCreatedBy(this.alertOwner);
        savedAlertJpa.setCreatedAt(creationTime);

        Alert convertedAlert = new Alert(this.alertOwner.getId(), this.postAlertToCreate.getReason());
        convertedAlert.setId(alertId);
        convertedAlert.setPostId(this.postId);

        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationTokenWithProfileList);
        when(this.profilesRepository.findActiveById(anyLong())).thenReturn(Optional.of(this.alertOwner));
        when(this.postsRepository.findActiveById(anyLong())).thenReturn(Optional.of(this.postJpa));
        when(this.alertToAlertJpaConverter.convert(any(Alert.class))).thenReturn(this.alertJpa);
        when(this.alertsRepository.save(any(AlertJpa.class))).thenReturn(savedAlertJpa);
        when(this.alertToAlertJpaConverter.convertBack(any(AlertJpa.class))).thenReturn(convertedAlert);

        Alert savedAlert = this.alertsService.createAlert(true, this.postAlertToCreate);

        assertEquals(convertedAlert, savedAlert);
        verify(this.securityContext, times(1)).getAuthentication();
        verify(this.keycloakService, times(0)).isInProfileList(anyString(), anyLong());
        verify(this.profilesRepository, times(1)).findActiveById(anyLong());
        verify(this.postsRepository, times(1)).findActiveById(anyLong());
        verify(this.commentsRepository, times(0)).findActiveById(anyLong());
        verify(this.followsRepository, times(0)).findActiveAcceptedById(any(FollowsId.class));
        verify(this.alertToAlertJpaConverter, times(1)).convert(any(Alert.class));
        verify(this.alertsRepository, times(1)).save(any(AlertJpa.class));
        verify(this.alertToAlertJpaConverter, times(1)).convertBack(any(AlertJpa.class));
        log.info("\nPost Alert:\n"+savedAlert.toString());
    }

    @Test
    void testCreate_PublicCommentAlert_Success(){
        LocalDateTime creationTime = LocalDateTime.now();

        AlertJpa savedAlertJpa = new AlertJpa(alertReason);
        savedAlertJpa.setComment(this.commentJpa);
        savedAlertJpa.setCreatedBy(this.alertOwner);
        savedAlertJpa.setCreatedAt(creationTime);

        Alert convertedAlert = new Alert(this.alertOwner.getId(), this.commentAlertToCreate.getReason());
        convertedAlert.setId(alertId);
        convertedAlert.setCommentId(this.commentId);

        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationTokenWithProfileList);
        when(this.profilesRepository.findActiveById(anyLong())).thenReturn(Optional.of(this.alertOwner));
        when(this.commentsRepository.findActiveById(anyLong())).thenReturn(Optional.of(this.commentJpa));
        when(this.alertToAlertJpaConverter.convert(any(Alert.class))).thenReturn(this.alertJpa);
        when(this.alertsRepository.save(any(AlertJpa.class))).thenReturn(savedAlertJpa);
        when(this.alertToAlertJpaConverter.convertBack(any(AlertJpa.class))).thenReturn(convertedAlert);

        Alert savedAlert = this.alertsService.createAlert(false, this.commentAlertToCreate);

        assertEquals(convertedAlert, savedAlert);
        verify(this.securityContext, times(1)).getAuthentication();
        verify(this.keycloakService, times(0)).isInProfileList(anyString(), anyLong());
        verify(this.profilesRepository, times(1)).findActiveById(anyLong());
        verify(this.postsRepository, times(0)).findActiveById(anyLong());
        verify(this.commentsRepository, times(1)).findActiveById(anyLong());
        verify(this.followsRepository, times(0)).findActiveAcceptedById(any(FollowsId.class));
        verify(this.alertToAlertJpaConverter, times(1)).convert(any(Alert.class));
        verify(this.alertsRepository, times(1)).save(any(AlertJpa.class));
        verify(this.alertToAlertJpaConverter, times(1)).convertBack(any(AlertJpa.class));
        log.info("\nComment Alert:\n"+savedAlert.toString());
    }

    @Test
    void testCreate_PrivatePostAlert_Success(){
        LocalDateTime creationTime = LocalDateTime.now();

        AlertJpa savedAlertJpa = new AlertJpa(alertReason);
        savedAlertJpa.setPost(this.privatePostJpa);
        savedAlertJpa.setCreatedBy(this.alertOwner);
        savedAlertJpa.setCreatedAt(creationTime);

        Alert convertedAlert = new Alert(this.alertOwner.getId(), this.postAlertToCreate.getReason());
        convertedAlert.setId(alertId);
        convertedAlert.setPostId(this.postId);

        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationTokenWithProfileList);
        when(this.profilesRepository.findActiveById(anyLong())).thenReturn(Optional.of(this.alertOwner));
        when(this.postsRepository.findActiveById(anyLong())).thenReturn(Optional.of(this.privatePostJpa));
        when(this.followsRepository.findActiveAcceptedById(any(FollowsId.class))).thenReturn(Optional.of(new FollowsId(this.alertOwner.getId(), this.privatePostJpa.getProfile().getId())));
        when(this.alertToAlertJpaConverter.convert(any(Alert.class))).thenReturn(this.alertJpa);
        when(this.alertsRepository.save(any(AlertJpa.class))).thenReturn(savedAlertJpa);
        when(this.alertToAlertJpaConverter.convertBack(any(AlertJpa.class))).thenReturn(convertedAlert);

        Alert savedAlert = this.alertsService.createAlert(true, this.postAlertToCreate);

        assertEquals(convertedAlert, savedAlert);
        verify(this.securityContext, times(1)).getAuthentication();
        verify(this.keycloakService, times(0)).isInProfileList(anyString(), anyLong());
        verify(this.profilesRepository, times(1)).findActiveById(anyLong());
        verify(this.postsRepository, times(1)).findActiveById(anyLong());
        verify(this.commentsRepository, times(0)).findActiveById(anyLong());
        verify(this.followsRepository, times(1)).findActiveAcceptedById(any(FollowsId.class));
        verify(this.alertToAlertJpaConverter, times(1)).convert(any(Alert.class));
        verify(this.alertsRepository, times(1)).save(any(AlertJpa.class));
        verify(this.alertToAlertJpaConverter, times(1)).convertBack(any(AlertJpa.class));
        log.info("\nPost Alert:\n"+savedAlert.toString());
    }

    @Test
    void testCreate_PrivateCommentAlert_Success(){
        this.commentJpa.setPost(this.privatePostJpa);
        LocalDateTime creationTime = LocalDateTime.now();

        AlertJpa savedAlertJpa = new AlertJpa(alertReason);
        savedAlertJpa.setComment(this.commentJpa);
        savedAlertJpa.setCreatedBy(this.alertOwner);
        savedAlertJpa.setCreatedAt(creationTime);

        Alert convertedAlert = new Alert(this.alertOwner.getId(), this.commentAlertToCreate.getReason());
        convertedAlert.setId(alertId);
        convertedAlert.setCommentId(this.commentId);

        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationTokenWithProfileList);
        when(this.profilesRepository.findActiveById(anyLong())).thenReturn(Optional.of(this.alertOwner));
        when(this.commentsRepository.findActiveById(anyLong())).thenReturn(Optional.of(this.commentJpa));
        when(this.followsRepository.findActiveAcceptedById(any(FollowsId.class))).thenReturn(Optional.of(new FollowsId(this.alertOwner.getId(), this.commentJpa.getPost().getProfile().getId())));
        when(this.alertToAlertJpaConverter.convert(any(Alert.class))).thenReturn(this.alertJpa);
        when(this.alertsRepository.save(any(AlertJpa.class))).thenReturn(savedAlertJpa);
        when(this.alertToAlertJpaConverter.convertBack(any(AlertJpa.class))).thenReturn(convertedAlert);

        Alert savedAlert = this.alertsService.createAlert(false, this.commentAlertToCreate);

        assertEquals(convertedAlert, savedAlert);
        verify(this.securityContext, times(1)).getAuthentication();
        verify(this.keycloakService, times(0)).isInProfileList(anyString(), anyLong());
        verify(this.profilesRepository, times(1)).findActiveById(anyLong());
        verify(this.postsRepository, times(0)).findActiveById(anyLong());
        verify(this.commentsRepository, times(1)).findActiveById(anyLong());
        verify(this.followsRepository, times(1)).findActiveAcceptedById(any(FollowsId.class));
        verify(this.alertToAlertJpaConverter, times(1)).convert(any(Alert.class));
        verify(this.alertsRepository, times(1)).save(any(AlertJpa.class));
        verify(this.alertToAlertJpaConverter, times(1)).convertBack(any(AlertJpa.class));
        log.info("\nComment Alert:\n"+savedAlert.toString());
    }

    @Test
    void testCreate_PostAlert_AlertTypeErrorException_Failed(){
        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationTokenWithProfileList);
        when(this.profilesRepository.findActiveById(anyLong())).thenReturn(Optional.of(this.alertOwner));


        assertThrows(AlertTypeErrorException.class, () -> this.alertsService.createAlert(false, this.postAlertToCreate));


        verify(this.securityContext, times(1)).getAuthentication();
        verify(this.keycloakService, times(0)).isInProfileList(anyString(), anyLong());
        verify(this.profilesRepository, times(1)).findActiveById(anyLong());
        verify(this.postsRepository, times(0)).findActiveById(anyLong());
        verify(this.commentsRepository, times(0)).findActiveById(anyLong());
        verify(this.followsRepository, times(0)).findActiveAcceptedById(any(FollowsId.class));
        verify(this.alertToAlertJpaConverter, times(0)).convert(any(Alert.class));
        verify(this.alertsRepository, times(0)).save(any(AlertJpa.class));
        verify(this.alertToAlertJpaConverter, times(0)).convertBack(any(AlertJpa.class));
    }

    @Test
    void testCreate_CommentAlert_AlertTypeNotSpecifiedException_Failed(){
        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationTokenWithProfileList);
        when(this.profilesRepository.findActiveById(anyLong())).thenReturn(Optional.of(this.alertOwner));

        assertThrows(AlertTypeErrorException.class, () -> this.alertsService.createAlert(true, this.commentAlertToCreate));

        verify(this.securityContext, times(1)).getAuthentication();
        verify(this.keycloakService, times(0)).isInProfileList(anyString(), anyLong());
        verify(this.profilesRepository, times(1)).findActiveById(anyLong());
        verify(this.postsRepository, times(0)).findActiveById(anyLong());
        verify(this.commentsRepository, times(0)).findActiveById(anyLong());
        verify(this.followsRepository, times(0)).findActiveAcceptedById(any(FollowsId.class));
        verify(this.alertToAlertJpaConverter, times(0)).convert(any(Alert.class));
        verify(this.alertsRepository, times(0)).save(any(AlertJpa.class));
        verify(this.alertToAlertJpaConverter, times(0)).convertBack(any(AlertJpa.class));
    }

    @Test
    void testCreate_PublicPostAlert_ValidatedOnKeycloak_Success(){
        LocalDateTime creationTime = LocalDateTime.now();

        AlertJpa savedAlertJpa = new AlertJpa(alertReason);
        savedAlertJpa.setPost(this.postJpa);
        savedAlertJpa.setCreatedBy(this.alertOwner);
        savedAlertJpa.setCreatedAt(creationTime);

        Alert convertedAlert = new Alert(this.alertOwner.getId(), this.postAlertToCreate.getReason());
        convertedAlert.setId(alertId);
        convertedAlert.setPostId(this.postId);

        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationToken);
        when(this.keycloakService.isInProfileList(anyString(), anyLong())).thenReturn(true);
        when(this.profilesRepository.findActiveById(anyLong())).thenReturn(Optional.of(this.alertOwner));
        when(this.postsRepository.findActiveById(anyLong())).thenReturn(Optional.of(this.postJpa));
        when(this.alertToAlertJpaConverter.convert(any(Alert.class))).thenReturn(this.alertJpa);
        when(this.alertsRepository.save(any(AlertJpa.class))).thenReturn(savedAlertJpa);
        when(this.alertToAlertJpaConverter.convertBack(any(AlertJpa.class))).thenReturn(convertedAlert);

        Alert savedAlert = this.alertsService.createAlert(true, this.postAlertToCreate);

        assertEquals(convertedAlert, savedAlert);
        verify(this.securityContext, times(2)).getAuthentication();
        verify(this.keycloakService, times(1)).isInProfileList(anyString(), anyLong());
        verify(this.profilesRepository, times(1)).findActiveById(anyLong());
        verify(this.postsRepository, times(1)).findActiveById(anyLong());
        verify(this.commentsRepository, times(0)).findActiveById(anyLong());
        verify(this.followsRepository, times(0)).findActiveAcceptedById(any(FollowsId.class));
        verify(this.alertToAlertJpaConverter, times(1)).convert(any(Alert.class));
        verify(this.alertsRepository, times(1)).save(any(AlertJpa.class));
        verify(this.alertToAlertJpaConverter, times(1)).convertBack(any(AlertJpa.class));
        log.info("\nPost Alert:\n"+savedAlert.toString());
    }

    @Test
    void testCreate_PublicPostAlert_NotInProfileList_Failed(){
        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationToken);
        when(this.keycloakService.isInProfileList(anyString(), anyLong())).thenReturn(false);

        assertThrows(NotInProfileListException.class, () -> this.alertsService.createAlert(true, this.postAlertToCreate));

        verify(this.securityContext, times(2)).getAuthentication();
        verify(this.keycloakService, times(1)).isInProfileList(anyString(), anyLong());
        verify(this.profilesRepository, times(0)).findActiveById(anyLong());
        verify(this.postsRepository, times(0)).findActiveById(anyLong());
        verify(this.commentsRepository, times(0)).findActiveById(anyLong());
        verify(this.followsRepository, times(0)).findActiveAcceptedById(any(FollowsId.class));
        verify(this.alertToAlertJpaConverter, times(0)).convert(any(Alert.class));
        verify(this.alertsRepository, times(0)).save(any(AlertJpa.class));
        verify(this.alertToAlertJpaConverter, times(0)).convertBack(any(AlertJpa.class));
    }

    @Test
    void testCreate_PrivatePostAlert_Failed(){
        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationTokenWithProfileList);
        when(this.profilesRepository.findActiveById(anyLong())).thenReturn(Optional.of(this.alertOwner));
        when(this.postsRepository.findActiveById(anyLong())).thenReturn(Optional.of(this.privatePostJpa));
        when(this.followsRepository.findActiveAcceptedById(any(FollowsId.class))).thenReturn(Optional.empty());

        assertThrows(AccessForbiddenException.class, () -> this.alertsService.createAlert(true, this.postAlertToCreate));

        verify(this.securityContext, times(1)).getAuthentication();
        verify(this.keycloakService, times(0)).isInProfileList(anyString(), anyLong());
        verify(this.profilesRepository, times(1)).findActiveById(anyLong());
        verify(this.postsRepository, times(1)).findActiveById(anyLong());
        verify(this.commentsRepository, times(0)).findActiveById(anyLong());
        verify(this.followsRepository, times(1)).findActiveAcceptedById(any(FollowsId.class));
        verify(this.alertToAlertJpaConverter, times(0)).convert(any(Alert.class));
        verify(this.alertsRepository, times(0)).save(any(AlertJpa.class));
        verify(this.alertToAlertJpaConverter, times(0)).convertBack(any(AlertJpa.class));
    }




}
