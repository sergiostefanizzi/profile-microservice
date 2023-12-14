package com.sergiostefanizzi.profilemicroservice.service;

import com.sergiostefanizzi.profilemicroservice.model.*;
import com.sergiostefanizzi.profilemicroservice.model.converter.FollowsToFollowsJpaConverter;
import com.sergiostefanizzi.profilemicroservice.model.converter.ProfileToProfileJpaConverter;
import com.sergiostefanizzi.profilemicroservice.repository.FollowsRepository;
import com.sergiostefanizzi.profilemicroservice.repository.ProfilesRepository;
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

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@Slf4j
class FollowsServiceTest {

    @InjectMocks
    private FollowsService followsService;
    @Mock
    private FollowsRepository followsRepository;
    @Mock
    private ProfilesRepository profilesRepository;
    @Mock
    private FollowsToFollowsJpaConverter followsToFollowsJpaConverter;
    @Mock
    private ProfileToProfileJpaConverter profileToProfileJpaConverter;
    @Mock
    private SecurityContext securityContext;
    @Mock
    private KeycloakService keycloakService;
    private JwtAuthenticationToken jwtAuthenticationToken;
    private JwtAuthenticationToken jwtAuthenticationTokenWithProfileList;
    private JwtAuthenticationToken jwtAuthenticationTokenPrivateWithProfileList;
    private ProfileJpa publicProfileJpa;
    private ProfileJpa privateProfileJpa;
    private ProfileJpa publicProfileJpa2;
    private final String accountId1 = UUID.randomUUID().toString();
    private final String accountId2 = UUID.randomUUID().toString();
    private final String accountId3 = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
        this.publicProfileJpa = new ProfileJpa("pinco_pallino",false, this.accountId1);
        this.publicProfileJpa.setId(1L);
        this.publicProfileJpa.setCreatedAt(LocalDateTime.MIN);

        this.publicProfileJpa2 = new ProfileJpa("marioBros",false, this.accountId2);
        this.publicProfileJpa2.setId(2L);
        this.publicProfileJpa2.setCreatedAt(LocalDateTime.MIN);

        this.privateProfileJpa = new ProfileJpa("luigiBros",true, this.accountId3);
        this.privateProfileJpa.setId(3L);
        this.privateProfileJpa.setCreatedAt(LocalDateTime.MIN);

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
        claims.put("profileList", List.of(publicProfileJpa.getId()));
        this.jwtAuthenticationTokenWithProfileList = new JwtAuthenticationToken(
                new Jwt("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c",
                        Instant.now(),
                        Instant.MAX,
                        headers,
                        claims)
        );

        claims.put("profileList", List.of(privateProfileJpa.getId()));
        this.jwtAuthenticationTokenPrivateWithProfileList = new JwtAuthenticationToken(
                new Jwt("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c",
                        Instant.now(),
                        Instant.MAX,
                        headers,
                        claims)
        );
    }

    @Test
    void testAddFollows_PublicProfile_ACCEPTED_Success(){
        FollowsJpa followsRequestJpa = new FollowsJpa(new FollowsId(this.publicProfileJpa.getId(), this.publicProfileJpa2.getId()));
        followsRequestJpa.setRequestStatus(Follows.RequestStatusEnum.ACCEPTED);
        followsRequestJpa.setFollowedAt(LocalDateTime.now());
        followsRequestJpa.setFollower(this.publicProfileJpa);
        followsRequestJpa.setFollowed(this.publicProfileJpa2);

        Follows followsRequest = new Follows(
                this.publicProfileJpa.getId(),
                this.publicProfileJpa2.getId(),
                followsRequestJpa.getRequestStatus());

        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationTokenWithProfileList);
        when(this.followsRepository.findById(any(FollowsId.class))).thenReturn(Optional.empty());
        when(this.profilesRepository.getReferenceById(anyLong())).thenReturn(this.publicProfileJpa);
        when(this.profilesRepository.getReferenceById(anyLong())).thenReturn(this.publicProfileJpa2);
        when(this.followsRepository.save(any(FollowsJpa.class))).thenReturn(followsRequestJpa);
        when(this.followsToFollowsJpaConverter.convertBack(any(FollowsJpa.class))).thenReturn(followsRequest);

        Follows returnedFollowsRequest = this.followsService.addFollows(this.publicProfileJpa.getId(), this.publicProfileJpa2.getId(),this.publicProfileJpa.getId(), false);

        assertEquals(followsRequest, returnedFollowsRequest);
        verify(this.securityContext, times(1)).getAuthentication();
        verify(this.keycloakService, times(0)).isInProfileList(anyString(), anyLong());
        verify(this.followsRepository, times(1)).findById(any(FollowsId.class));
        verify(this.profilesRepository, times(2)).getReferenceById(anyLong());
        verify(this.followsRepository, times(1)).save(any(FollowsJpa.class));
        verify(this.followsToFollowsJpaConverter, times(1)).convertBack(any(FollowsJpa.class));

        log.info(returnedFollowsRequest.toString());
    }

    @Test
    void testAddFollows_PrivateProfile_PENDING_Success(){
        FollowsJpa followsRequestJpa = new FollowsJpa(new FollowsId(this.publicProfileJpa.getId(), this.privateProfileJpa.getId()));
        followsRequestJpa.setRequestStatus(Follows.RequestStatusEnum.PENDING);
        followsRequestJpa.setFollower(this.publicProfileJpa);
        followsRequestJpa.setFollowed(this.privateProfileJpa);

        Follows followsRequest = new Follows(
                this.publicProfileJpa.getId(),
                this.privateProfileJpa.getId(),
                followsRequestJpa.getRequestStatus());

        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationTokenWithProfileList);
        when(this.followsRepository.findById(any(FollowsId.class))).thenReturn(Optional.empty());
        when(this.profilesRepository.getReferenceById(anyLong())).thenReturn(this.publicProfileJpa);
        when(this.profilesRepository.getReferenceById(anyLong())).thenReturn(this.privateProfileJpa);
        when(this.followsRepository.save(any(FollowsJpa.class))).thenReturn(followsRequestJpa);
        when(this.followsToFollowsJpaConverter.convertBack(any(FollowsJpa.class))).thenReturn(followsRequest);

        Follows returnedFollowsRequest = this.followsService.addFollows(this.publicProfileJpa.getId(), this.privateProfileJpa.getId(), this.publicProfileJpa.getId(), false);

        assertEquals(followsRequest, returnedFollowsRequest);
        verify(this.securityContext, times(1)).getAuthentication();
        verify(this.keycloakService, times(0)).isInProfileList(anyString(), anyLong());
        verify(this.profilesRepository, times(2)).getReferenceById(anyLong());
        verify(this.followsRepository, times(1)).findById(any(FollowsId.class));
        verify(this.followsRepository, times(1)).save(any(FollowsJpa.class));
        verify(this.followsToFollowsJpaConverter, times(1)).convertBack(any(FollowsJpa.class));

        log.info(returnedFollowsRequest.toString());
    }





    @Test
    void testAddFollows_FollowsAlreadyCreated_RejectedRequest_follow_PENDING_Success(){
        FollowsJpa followsRequestJpa = new FollowsJpa(new FollowsId(this.publicProfileJpa.getId(), this.privateProfileJpa.getId()));
        followsRequestJpa.setRequestStatus(Follows.RequestStatusEnum.REJECTED);
        followsRequestJpa.setFollower(this.publicProfileJpa);
        followsRequestJpa.setFollowed(this.privateProfileJpa);

        Follows followsRequest = new Follows(
                this.publicProfileJpa.getId(),
                this.privateProfileJpa.getId(),
                Follows.RequestStatusEnum.PENDING);

        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationTokenWithProfileList);
        when(this.followsRepository.findById(any(FollowsId.class))).thenReturn(Optional.of(followsRequestJpa));
        when(this.followsRepository.save(any(FollowsJpa.class))).thenReturn(followsRequestJpa);
        when(this.followsToFollowsJpaConverter.convertBack(any(FollowsJpa.class))).thenReturn(followsRequest);

        Follows returnedFollows = this.followsService.addFollows(this.publicProfileJpa.getId(), this.privateProfileJpa.getId(), this.publicProfileJpa.getId(), false);

        assertEquals(followsRequest, returnedFollows);
        verify(this.securityContext, times(1)).getAuthentication();
        verify(this.keycloakService, times(0)).isInProfileList(anyString(), anyLong());
        verify(this.profilesRepository, times(0)).getReferenceById(anyLong());
        verify(this.followsRepository, times(1)).findById(any(FollowsId.class));
        verify(this.followsRepository, times(1)).save(any(FollowsJpa.class));
        verify(this.followsToFollowsJpaConverter, times(1)).convertBack(any(FollowsJpa.class));

        log.info(returnedFollows.toString());
    }

    @Test
    void testAddFollows_FollowsAlreadyCreated_RejectedRequest_unfollow_PENDING_Success(){
        FollowsJpa followsRequestJpa = new FollowsJpa(new FollowsId(this.publicProfileJpa.getId(), this.privateProfileJpa.getId()));
        followsRequestJpa.setRequestStatus(Follows.RequestStatusEnum.REJECTED);
        followsRequestJpa.setFollower(this.publicProfileJpa);
        followsRequestJpa.setFollowed(this.privateProfileJpa);

        Follows followsRequest = new Follows(
                this.publicProfileJpa.getId(),
                this.privateProfileJpa.getId(),
                Follows.RequestStatusEnum.REJECTED);

        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationTokenWithProfileList);
        when(this.followsRepository.findById(any(FollowsId.class))).thenReturn(Optional.of(followsRequestJpa));
        when(this.followsRepository.save(any(FollowsJpa.class))).thenReturn(followsRequestJpa);
        when(this.followsToFollowsJpaConverter.convertBack(any(FollowsJpa.class))).thenReturn(followsRequest);

        Follows returnedFollows = this.followsService.addFollows(this.publicProfileJpa.getId(), this.privateProfileJpa.getId(), this.publicProfileJpa.getId(), true);

        assertEquals(followsRequest, returnedFollows);
        verify(this.securityContext, times(1)).getAuthentication();
        verify(this.keycloakService, times(0)).isInProfileList(anyString(), anyLong());
        verify(this.profilesRepository, times(0)).getReferenceById(anyLong());
        verify(this.followsRepository, times(1)).findById(any(FollowsId.class));
        verify(this.followsRepository, times(1)).save(any(FollowsJpa.class));
        verify(this.followsToFollowsJpaConverter, times(1)).convertBack(any(FollowsJpa.class));

        log.info(returnedFollows.toString());
    }

    @Test
    void testAddFollows_FollowsAlreadyCreated_Accepted_or_Pending_Request_unfollow_REJECTED_Success(){
        FollowsJpa followsRequestJpa = new FollowsJpa(new FollowsId(this.publicProfileJpa.getId(), this.privateProfileJpa.getId()));
        followsRequestJpa.setRequestStatus(Follows.RequestStatusEnum.ACCEPTED);
        followsRequestJpa.setFollower(this.publicProfileJpa);
        followsRequestJpa.setFollowed(this.privateProfileJpa);

        Follows followsRequest = new Follows(
                this.publicProfileJpa.getId(),
                this.privateProfileJpa.getId(),
                Follows.RequestStatusEnum.REJECTED);
        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationTokenWithProfileList);
        when(this.followsRepository.findById(any(FollowsId.class))).thenReturn(Optional.of(followsRequestJpa));
        when(this.followsRepository.save(any(FollowsJpa.class))).thenReturn(followsRequestJpa);
        when(this.followsToFollowsJpaConverter.convertBack(any(FollowsJpa.class))).thenReturn(followsRequest);

        Follows returnedFollows = this.followsService.addFollows(this.publicProfileJpa.getId(), this.privateProfileJpa.getId(), this.publicProfileJpa.getId(), true);

        assertEquals(followsRequest, returnedFollows);
        verify(this.securityContext, times(1)).getAuthentication();
        verify(this.keycloakService, times(0)).isInProfileList(anyString(), anyLong());
        verify(this.profilesRepository, times(0)).getReferenceById(anyLong());
        verify(this.followsRepository, times(1)).findById(any(FollowsId.class));
        verify(this.followsRepository, times(1)).save(any(FollowsJpa.class));
        verify(this.followsToFollowsJpaConverter, times(1)).convertBack(any(FollowsJpa.class));

        log.info(returnedFollows.toString());
    }

    @Test
    void testAddFollows_FollowsAlreadyCreated_Accepted_or_Pending_Request_follow_ACCEPT_Success(){
        FollowsJpa followsRequestJpa = new FollowsJpa(new FollowsId(this.publicProfileJpa.getId(), this.privateProfileJpa.getId()));
        followsRequestJpa.setRequestStatus(Follows.RequestStatusEnum.PENDING);
        followsRequestJpa.setFollower(this.publicProfileJpa);
        followsRequestJpa.setFollowed(this.privateProfileJpa);

        Follows followsRequest = new Follows(
                this.publicProfileJpa.getId(),
                this.privateProfileJpa.getId(),
                Follows.RequestStatusEnum.ACCEPTED);
        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationTokenWithProfileList);
        when(this.followsRepository.findById(any(FollowsId.class))).thenReturn(Optional.of(followsRequestJpa));
        when(this.followsRepository.save(any(FollowsJpa.class))).thenReturn(followsRequestJpa);
        when(this.followsToFollowsJpaConverter.convertBack(any(FollowsJpa.class))).thenReturn(followsRequest);

        Follows returnedFollows = this.followsService.addFollows(this.publicProfileJpa.getId(), this.privateProfileJpa.getId(), this.publicProfileJpa.getId(), true);

        assertEquals(followsRequest, returnedFollows);
        verify(this.securityContext, times(1)).getAuthentication();
        verify(this.keycloakService, times(0)).isInProfileList(anyString(), anyLong());
        verify(this.profilesRepository, times(0)).getReferenceById(anyLong());
        verify(this.followsRepository, times(1)).findById(any(FollowsId.class));
        verify(this.followsRepository, times(1)).save(any(FollowsJpa.class));
        verify(this.followsToFollowsJpaConverter, times(1)).convertBack(any(FollowsJpa.class));

        log.info(returnedFollows.toString());
    }

    @Test
    void testAddFollows_PublicProfile_ACCEPTED_ValidatedOnKeycloak_Success(){
        FollowsJpa followsRequestJpa = new FollowsJpa(new FollowsId(this.publicProfileJpa.getId(), this.publicProfileJpa2.getId()));
        followsRequestJpa.setRequestStatus(Follows.RequestStatusEnum.ACCEPTED);
        followsRequestJpa.setFollowedAt(LocalDateTime.now());
        followsRequestJpa.setFollower(this.publicProfileJpa);
        followsRequestJpa.setFollowed(this.publicProfileJpa2);

        Follows followsRequest = new Follows(
                this.publicProfileJpa.getId(),
                this.publicProfileJpa2.getId(),
                followsRequestJpa.getRequestStatus());

        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationToken);
        when(this.keycloakService.isInProfileList(anyString(), anyLong())).thenReturn(true);
        when(this.followsRepository.findById(any(FollowsId.class))).thenReturn(Optional.empty());
        when(this.profilesRepository.getReferenceById(anyLong())).thenReturn(this.publicProfileJpa);
        when(this.profilesRepository.getReferenceById(anyLong())).thenReturn(this.publicProfileJpa2);
        when(this.followsRepository.save(any(FollowsJpa.class))).thenReturn(followsRequestJpa);
        when(this.followsToFollowsJpaConverter.convertBack(any(FollowsJpa.class))).thenReturn(followsRequest);

        Follows returnedFollowsRequest = this.followsService.addFollows(this.publicProfileJpa.getId(), this.publicProfileJpa2.getId(),this.publicProfileJpa.getId(), false);

        assertEquals(followsRequest, returnedFollowsRequest);
        verify(this.securityContext, times(2)).getAuthentication();
        verify(this.keycloakService, times(1)).isInProfileList(anyString(), anyLong());
        verify(this.followsRepository, times(1)).findById(any(FollowsId.class));
        verify(this.profilesRepository, times(2)).getReferenceById(anyLong());
        verify(this.followsRepository, times(1)).save(any(FollowsJpa.class));
        verify(this.followsToFollowsJpaConverter, times(1)).convertBack(any(FollowsJpa.class));

        log.info(returnedFollowsRequest.toString());
    }

    @Test
    void testAddFollows_PublicProfile_NotInProfileList_Failed(){
        Long profileId = this.publicProfileJpa.getId();
        Long followerId = this.publicProfileJpa2.getId();

        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationToken);
        when(this.keycloakService.isInProfileList(anyString(), anyLong())).thenReturn(false);

        assertThrows(NotInProfileListException.class, () -> this.followsService.addFollows(profileId, followerId, profileId, false));

        verify(this.securityContext, times(2)).getAuthentication();
        verify(this.keycloakService, times(1)).isInProfileList(anyString(), anyLong());
        verify(this.followsRepository, times(0)).findById(any(FollowsId.class));
        verify(this.profilesRepository, times(0)).getReferenceById(anyLong());
        verify(this.followsRepository, times(0)).save(any(FollowsJpa.class));
        verify(this.followsToFollowsJpaConverter, times(0)).convertBack(any(FollowsJpa.class));
    }

    @Test
    void testAddFollows_PublicProfile_IdsMismatch_Failed(){
        Long profileId = this.publicProfileJpa.getId();
        Long followerId = this.publicProfileJpa2.getId();

        assertThrows(IdsMismatchException.class, () -> this.followsService.addFollows(profileId, followerId, Long.MAX_VALUE, false));

        verify(this.securityContext, times(0)).getAuthentication();
        verify(this.keycloakService, times(0)).isInProfileList(anyString(), anyLong());
        verify(this.followsRepository, times(0)).findById(any(FollowsId.class));
        verify(this.profilesRepository, times(0)).getReferenceById(anyLong());
        verify(this.followsRepository, times(0)).save(any(FollowsJpa.class));
        verify(this.followsToFollowsJpaConverter, times(0)).convertBack(any(FollowsJpa.class));
    }

    @Test
    void testAddFollows_UnfollowOnCreation_Failed(){
        Long profileId = this.publicProfileJpa.getId();
        Long followerId = this.privateProfileJpa.getId();
        FollowsJpa followsRequestJpa = new FollowsJpa(new FollowsId(this.publicProfileJpa.getId(), this.privateProfileJpa.getId()));
        followsRequestJpa.setRequestStatus(Follows.RequestStatusEnum.ACCEPTED);
        //followsJpa.setRequestStatus(Follows.RequestStatusEnum.PENDING);
        followsRequestJpa.setFollower(this.publicProfileJpa);
        followsRequestJpa.setFollowed(this.privateProfileJpa);

        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationTokenWithProfileList);
        when(this.followsRepository.findById(any(FollowsId.class))).thenReturn(Optional.empty());
        when(this.profilesRepository.getReferenceById(anyLong())).thenReturn(this.publicProfileJpa);
        when(this.profilesRepository.getReferenceById(anyLong())).thenReturn(this.privateProfileJpa);

        assertThrows(UnfollowOnCreationException.class,
                () -> this.followsService.addFollows(profileId, followerId, profileId, true));
        verify(this.securityContext, times(1)).getAuthentication();
        verify(this.keycloakService, times(0)).isInProfileList(anyString(), anyLong());
        verify(this.profilesRepository, times(2)).getReferenceById(anyLong());
        verify(this.followsRepository, times(1)).findById(any(FollowsId.class));
        verify(this.followsRepository, times(0)).save(any(FollowsJpa.class));
        verify(this.followsToFollowsJpaConverter, times(0)).convertBack(any(FollowsJpa.class));
    }

    @Test
    void testAcceptFollows_Success(){
        FollowsJpa followsRequestJpa = new FollowsJpa(new FollowsId(this.publicProfileJpa.getId(), this.privateProfileJpa.getId()));
        followsRequestJpa.setRequestStatus(Follows.RequestStatusEnum.PENDING);
        followsRequestJpa.setFollower(this.publicProfileJpa);
        followsRequestJpa.setFollowed(this.privateProfileJpa);

        FollowsJpa savedFollowsJpa = new FollowsJpa(new FollowsId(this.publicProfileJpa.getId(), this.privateProfileJpa.getId()));
        savedFollowsJpa.setRequestStatus(Follows.RequestStatusEnum.ACCEPTED);
        savedFollowsJpa.setFollowedAt(LocalDateTime.now());
        savedFollowsJpa.setFollower(this.publicProfileJpa);
        savedFollowsJpa.setFollowed(this.privateProfileJpa);

        Follows followsRequest = new Follows(
                this.publicProfileJpa.getId(),
                this.privateProfileJpa.getId(),
                Follows.RequestStatusEnum.ACCEPTED);

        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationTokenPrivateWithProfileList);
        when(this.followsRepository.findActiveById(any(FollowsId.class))).thenReturn(Optional.of(followsRequestJpa));
        when(this.followsRepository.save(any(FollowsJpa.class))).thenReturn(savedFollowsJpa);
        when(this.followsToFollowsJpaConverter.convertBack(any(FollowsJpa.class))).thenReturn(followsRequest);

        Follows returnedFollows = this.followsService.acceptFollows(this.privateProfileJpa.getId(), this.publicProfileJpa.getId(), this.privateProfileJpa.getId(), false);

        assertEquals(followsRequest, returnedFollows);
        verify(this.securityContext, times(1)).getAuthentication();
        verify(this.keycloakService, times(0)).isInProfileList(anyString(), anyLong());
        verify(this.followsRepository, times(1)).findActiveById(any(FollowsId.class));
        verify(this.followsRepository, times(1)).save(any(FollowsJpa.class));
        verify(this.followsToFollowsJpaConverter, times(1)).convertBack(any(FollowsJpa.class));
        log.info(savedFollowsJpa.getRequestStatus().toString());
        log.info(returnedFollows.toString());
    }

    @Test
    void testAcceptFollows_Reject_Success(){
        FollowsJpa followsRequestJpa = new FollowsJpa(new FollowsId(this.publicProfileJpa.getId(), this.privateProfileJpa.getId()));
        followsRequestJpa.setRequestStatus(Follows.RequestStatusEnum.PENDING);
        followsRequestJpa.setFollowedAt(LocalDateTime.now());
        followsRequestJpa.setFollower(this.publicProfileJpa);
        followsRequestJpa.setFollowed(this.privateProfileJpa);

        Follows followsRequest = new Follows(
                this.publicProfileJpa.getId(),
                this.privateProfileJpa.getId(),
                Follows.RequestStatusEnum.REJECTED);

        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationTokenPrivateWithProfileList);
        when(this.followsRepository.findActiveById(any(FollowsId.class))).thenReturn(Optional.of(followsRequestJpa));
        when(this.followsRepository.save(any(FollowsJpa.class))).thenReturn(followsRequestJpa);
        when(this.followsToFollowsJpaConverter.convertBack(any(FollowsJpa.class))).thenReturn(followsRequest);

        Follows returnedFollows = this.followsService.acceptFollows(this.privateProfileJpa.getId(), this.publicProfileJpa.getId(), this.privateProfileJpa.getId(), true);

        assertEquals(followsRequest, returnedFollows);
        verify(this.securityContext, times(1)).getAuthentication();
        verify(this.keycloakService, times(0)).isInProfileList(anyString(), anyLong());
        verify(this.followsRepository, times(1)).findActiveById(any(FollowsId.class));
        verify(this.followsRepository, times(1)).save(any(FollowsJpa.class));
        verify(this.followsToFollowsJpaConverter, times(1)).convertBack(any(FollowsJpa.class));
        log.info(followsRequestJpa.getRequestStatus().toString());
        log.info(returnedFollows.toString());
    }

    @Test
    void testAcceptFollows_RemoveFollower_Success(){
        FollowsJpa followsRequestJpa = new FollowsJpa(new FollowsId(this.publicProfileJpa.getId(), this.privateProfileJpa.getId()));
        followsRequestJpa.setRequestStatus(Follows.RequestStatusEnum.ACCEPTED);
        followsRequestJpa.setUnfollowedAt(LocalDateTime.now());
        followsRequestJpa.setFollower(this.publicProfileJpa);
        followsRequestJpa.setFollowed(this.privateProfileJpa);

        Follows followsRequest = new Follows(
                this.publicProfileJpa.getId(),
                this.privateProfileJpa.getId(),
                Follows.RequestStatusEnum.REJECTED);

        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationTokenPrivateWithProfileList);
        when(this.followsRepository.findActiveById(any(FollowsId.class))).thenReturn(Optional.of(followsRequestJpa));
        when(this.followsRepository.save(any(FollowsJpa.class))).thenReturn(followsRequestJpa);
        when(this.followsToFollowsJpaConverter.convertBack(any(FollowsJpa.class))).thenReturn(followsRequest);

        Follows returnedFollows = this.followsService.acceptFollows(this.privateProfileJpa.getId(), this.publicProfileJpa.getId(), this.privateProfileJpa.getId(), true);

        assertEquals(followsRequest, returnedFollows);
        verify(this.securityContext, times(1)).getAuthentication();
        verify(this.keycloakService, times(0)).isInProfileList(anyString(), anyLong());
        verify(this.followsRepository, times(1)).findActiveById(any(FollowsId.class));
        verify(this.followsRepository, times(1)).save(any(FollowsJpa.class));
        verify(this.followsToFollowsJpaConverter, times(1)).convertBack(any(FollowsJpa.class));
        log.info(followsRequestJpa.getRequestStatus().toString());
        log.info(returnedFollows.toString());
    }

    @Test
    void testAcceptFollows_AcceptedAlready_Success(){
        FollowsJpa followsRequestJpa = new FollowsJpa(new FollowsId(this.publicProfileJpa.getId(), this.privateProfileJpa.getId()));
        followsRequestJpa.setRequestStatus(Follows.RequestStatusEnum.ACCEPTED);
        followsRequestJpa.setUnfollowedAt(LocalDateTime.now());
        followsRequestJpa.setFollower(this.publicProfileJpa);
        followsRequestJpa.setFollowed(this.privateProfileJpa);

        Follows followsRequest = new Follows(
                this.publicProfileJpa.getId(),
                this.privateProfileJpa.getId(),
                Follows.RequestStatusEnum.ACCEPTED);

        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationTokenPrivateWithProfileList);
        when(this.followsRepository.findActiveById(any(FollowsId.class))).thenReturn(Optional.of(followsRequestJpa));
        when(this.followsRepository.save(any(FollowsJpa.class))).thenReturn(followsRequestJpa);
        when(this.followsToFollowsJpaConverter.convertBack(any(FollowsJpa.class))).thenReturn(followsRequest);

        Follows returnedFollows = this.followsService.acceptFollows(this.privateProfileJpa.getId(), this.publicProfileJpa.getId(), this.privateProfileJpa.getId(), false);

        assertEquals(followsRequest, returnedFollows);
        verify(this.securityContext, times(1)).getAuthentication();
        verify(this.keycloakService, times(0)).isInProfileList(anyString(), anyLong());
        verify(this.followsRepository, times(1)).findActiveById(any(FollowsId.class));
        verify(this.followsRepository, times(1)).save(any(FollowsJpa.class));
        verify(this.followsToFollowsJpaConverter, times(1)).convertBack(any(FollowsJpa.class));
        log.info(followsRequestJpa.getRequestStatus().toString());
        log.info(returnedFollows.toString());
    }

    @Test
    void testAcceptFollows_ValidatedOnKeycloak_Success(){
        FollowsJpa followsRequestJpa = new FollowsJpa(new FollowsId(this.publicProfileJpa.getId(), this.privateProfileJpa.getId()));
        followsRequestJpa.setRequestStatus(Follows.RequestStatusEnum.PENDING);
        followsRequestJpa.setFollower(this.publicProfileJpa);
        followsRequestJpa.setFollowed(this.privateProfileJpa);

        FollowsJpa savedFollowsJpa = new FollowsJpa(new FollowsId(this.publicProfileJpa.getId(), this.privateProfileJpa.getId()));
        savedFollowsJpa.setRequestStatus(Follows.RequestStatusEnum.ACCEPTED);
        savedFollowsJpa.setFollowedAt(LocalDateTime.now());
        savedFollowsJpa.setFollower(this.publicProfileJpa);
        savedFollowsJpa.setFollowed(this.privateProfileJpa);

        Follows followsRequest = new Follows(
                this.publicProfileJpa.getId(),
                this.privateProfileJpa.getId(),
                Follows.RequestStatusEnum.ACCEPTED);

        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationToken);
        when(this.keycloakService.isInProfileList(anyString(), anyLong())).thenReturn(true);
        when(this.followsRepository.findActiveById(any(FollowsId.class))).thenReturn(Optional.of(followsRequestJpa));
        when(this.followsRepository.save(any(FollowsJpa.class))).thenReturn(savedFollowsJpa);
        when(this.followsToFollowsJpaConverter.convertBack(any(FollowsJpa.class))).thenReturn(followsRequest);

        Follows returnedFollows = this.followsService.acceptFollows(this.privateProfileJpa.getId(), this.publicProfileJpa.getId(), this.privateProfileJpa.getId(), false);

        assertEquals(followsRequest, returnedFollows);
        verify(this.securityContext, times(2)).getAuthentication();
        verify(this.keycloakService, times(1)).isInProfileList(anyString(), anyLong());
        verify(this.followsRepository, times(1)).findActiveById(any(FollowsId.class));
        verify(this.followsRepository, times(1)).save(any(FollowsJpa.class));
        verify(this.followsToFollowsJpaConverter, times(1)).convertBack(any(FollowsJpa.class));
        log.info(savedFollowsJpa.getRequestStatus().toString());
        log.info(returnedFollows.toString());
    }

    @Test
    void testAcceptFollows_FollowNotFound_Failed(){
        Long profileId = this.publicProfileJpa.getId();
        Long followedId = this.privateProfileJpa.getId();
        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationTokenPrivateWithProfileList);
        when(this.followsRepository.findActiveById(any(FollowsId.class))).thenReturn(Optional.empty());

        assertThrows(FollowNotFoundException.class,
                () -> this.followsService.acceptFollows(followedId, profileId, followedId, false));

        verify(this.securityContext, times(1)).getAuthentication();
        verify(this.keycloakService, times(0)).isInProfileList(anyString(), anyLong());
        verify(this.followsRepository, times(1)).findActiveById(any(FollowsId.class));
        verify(this.followsRepository, times(0)).save(any(FollowsJpa.class));
        verify(this.followsToFollowsJpaConverter, times(0)).convertBack(any(FollowsJpa.class));
    }



    @Test
    void testAcceptFollows_NotInProfileList_Success(){
        Long profileId = this.publicProfileJpa.getId();
        Long followedId = this.privateProfileJpa.getId();

        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationToken);
        when(this.keycloakService.isInProfileList(anyString(), anyLong())).thenReturn(false);


        assertThrows(NotInProfileListException.class, () -> this.followsService.acceptFollows(followedId, profileId, followedId, false));

        verify(this.securityContext, times(2)).getAuthentication();
        verify(this.keycloakService, times(1)).isInProfileList(anyString(), anyLong());
        verify(this.followsRepository, times(0)).findActiveById(any(FollowsId.class));
        verify(this.followsRepository, times(0)).save(any(FollowsJpa.class));
        verify(this.followsToFollowsJpaConverter, times(0)).convertBack(any(FollowsJpa.class));

    }

    @Test
    void testAcceptFollows_IdsMismatch_Success(){
        Long profileId = this.publicProfileJpa.getId();
        Long followedId = this.privateProfileJpa.getId();



        assertThrows(IdsMismatchException.class, () -> this.followsService.acceptFollows(followedId, profileId, Long.MAX_VALUE, false));

        verify(this.securityContext, times(0)).getAuthentication();
        verify(this.keycloakService, times(0)).isInProfileList(anyString(), anyLong());
        verify(this.followsRepository, times(0)).findActiveById(any(FollowsId.class));
        verify(this.followsRepository, times(0)).save(any(FollowsJpa.class));
        verify(this.followsToFollowsJpaConverter, times(0)).convertBack(any(FollowsJpa.class));

    }


    @Test
    void testFindAllFollowers_Success(){
        Profile publicProfile = new Profile(this.publicProfileJpa2.getProfileName(),this.publicProfileJpa2.getIsPrivate());
        publicProfile.setAccountId(this.publicProfileJpa2.getAccountId());
        publicProfile.setId(this.publicProfileJpa2.getId());
        Profile privateProfile = new Profile(this.privateProfileJpa.getProfileName(),this.privateProfileJpa.getIsPrivate());
        privateProfile.setAccountId(this.privateProfileJpa.getAccountId());
        privateProfile.setId(this.privateProfileJpa.getId());
        List<Profile> followerList = asList(publicProfile, privateProfile);
        ProfileFollowList returnedProfileFollowList = new ProfileFollowList(followerList, followerList.size());

        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationTokenWithProfileList);
        when(this.profilesRepository.getReferenceById(anyLong())).thenReturn(this.publicProfileJpa);
        //when(this.followsRepository.findActiveAcceptedById(any(FollowsId.class))).thenReturn(Optional.of(new FollowsId(this.publicProfileJpa.getId(), this.privateProfileJpa.getId())));
        when(this.followsRepository.findActiveFollowers(any(ProfileJpa.class))).thenReturn(asList(
                this.publicProfileJpa2,
                this.privateProfileJpa
        ));
        when(this.profileToProfileJpaConverter.convertBack(this.publicProfileJpa2)).thenReturn(publicProfile);
        when(this.profileToProfileJpaConverter.convertBack(this.privateProfileJpa)).thenReturn(privateProfile);

        ProfileFollowList profileFollowList = this.followsService.findAllFollowers(this.publicProfileJpa.getId(), publicProfileJpa.getId());

        log.info(profileFollowList.toString());

        assertEquals(returnedProfileFollowList,profileFollowList);
        verify(this.securityContext, times(1)).getAuthentication();
        verify(this.keycloakService, times(0)).isInProfileList(anyString(), anyLong());
        verify(this.profilesRepository, times(1)).getReferenceById(anyLong());
        verify(this.followsRepository, times(0)).findActiveAcceptedById(any(FollowsId.class));
        verify(this.followsRepository, times(1)).findActiveFollowers(any(ProfileJpa.class));
        verify(this.profileToProfileJpaConverter, times(2)).convertBack(any(ProfileJpa.class));
    }

    @Test
    void testFindAllFollowers_FollowedPrivateProfile_Success(){
        Profile publicProfile = new Profile(this.publicProfileJpa2.getProfileName(),this.publicProfileJpa2.getIsPrivate());
        publicProfile.setAccountId(this.publicProfileJpa2.getAccountId());
        publicProfile.setId(this.publicProfileJpa2.getId());
        Profile privateProfile = new Profile(this.privateProfileJpa.getProfileName(),this.privateProfileJpa.getIsPrivate());
        privateProfile.setAccountId(this.privateProfileJpa.getAccountId());
        privateProfile.setId(this.privateProfileJpa.getId());
        List<Profile> followerList = asList(publicProfile, privateProfile);
        ProfileFollowList returnedProfileFollowList = new ProfileFollowList(followerList, followerList.size());

        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationTokenWithProfileList);
        when(this.profilesRepository.getReferenceById(anyLong())).thenReturn(this.privateProfileJpa);
        when(this.followsRepository.findActiveAcceptedById(any(FollowsId.class))).thenReturn(Optional.of(new FollowsId(this.publicProfileJpa.getId(), this.privateProfileJpa.getId())));
        when(this.followsRepository.findActiveFollowers(any(ProfileJpa.class))).thenReturn(asList(
                this.publicProfileJpa2,
                this.privateProfileJpa
        ));
        when(this.profileToProfileJpaConverter.convertBack(this.publicProfileJpa2)).thenReturn(publicProfile);
        when(this.profileToProfileJpaConverter.convertBack(this.privateProfileJpa)).thenReturn(privateProfile);

        ProfileFollowList profileFollowList = this.followsService.findAllFollowers(this.privateProfileJpa.getId(), publicProfileJpa.getId());

        log.info(profileFollowList.toString());

        assertEquals(returnedProfileFollowList,profileFollowList);
        verify(this.securityContext, times(1)).getAuthentication();
        verify(this.keycloakService, times(0)).isInProfileList(anyString(), anyLong());
        verify(this.profilesRepository, times(1)).getReferenceById(anyLong());
        verify(this.followsRepository, times(1)).findActiveAcceptedById(any(FollowsId.class));
        verify(this.followsRepository, times(1)).findActiveFollowers(any(ProfileJpa.class));
        verify(this.profileToProfileJpaConverter, times(2)).convertBack(any(ProfileJpa.class));
    }

    @Test
    void testFindAllFollowers_NotFollowedPrivateProfile_Success(){
        Profile publicProfile = new Profile(this.publicProfileJpa2.getProfileName(),this.publicProfileJpa2.getIsPrivate());
        publicProfile.setAccountId(this.publicProfileJpa2.getAccountId());
        publicProfile.setId(this.publicProfileJpa2.getId());
        Profile privateProfile = new Profile(this.privateProfileJpa.getProfileName(),this.privateProfileJpa.getIsPrivate());
        privateProfile.setAccountId(this.privateProfileJpa.getAccountId());
        privateProfile.setId(this.privateProfileJpa.getId());
        List<Profile> followerList = asList(publicProfile, privateProfile);
        ProfileFollowList returnedProfileFollowList = new ProfileFollowList(List.of(), followerList.size());

        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationTokenWithProfileList);
        when(this.profilesRepository.getReferenceById(anyLong())).thenReturn(this.privateProfileJpa);
        when(this.followsRepository.findActiveAcceptedById(any(FollowsId.class))).thenReturn(Optional.empty());
        when(this.followsRepository.findActiveFollowers(any(ProfileJpa.class))).thenReturn(asList(
                this.publicProfileJpa2,
                this.privateProfileJpa
        ));
        when(this.profileToProfileJpaConverter.convertBack(this.publicProfileJpa2)).thenReturn(publicProfile);
        when(this.profileToProfileJpaConverter.convertBack(this.privateProfileJpa)).thenReturn(privateProfile);

        ProfileFollowList profileFollowList = this.followsService.findAllFollowers(this.privateProfileJpa.getId(), publicProfileJpa.getId());

        log.info(profileFollowList.toString());

        assertEquals(returnedProfileFollowList,profileFollowList);
        verify(this.securityContext, times(1)).getAuthentication();
        verify(this.keycloakService, times(0)).isInProfileList(anyString(), anyLong());
        verify(this.profilesRepository, times(1)).getReferenceById(anyLong());
        verify(this.followsRepository, times(1)).findActiveAcceptedById(any(FollowsId.class));
        verify(this.followsRepository, times(1)).findActiveFollowers(any(ProfileJpa.class));
        verify(this.profileToProfileJpaConverter, times(2)).convertBack(any(ProfileJpa.class));
    }
/*
    @Test
    void testFindAllFollowers_NoFollowers_Success(){
        List<Profile> followerList = Collections.emptyList();
        ProfileFollowList returnedProfileFollowList = new ProfileFollowList(followerList, 0);

        when(this.profilesRepository.getReferenceById(anyLong())).thenReturn(this.publicProfileJpa);
        when(this.followsRepository.findActiveFollowers(any(ProfileJpa.class))).thenReturn(Collections.emptyList());

        ProfileFollowList profileFollowList = this.followsService.findAllFollowers(this.publicProfileJpa.getId());

        log.info(profileFollowList.toString());

        assertEquals(returnedProfileFollowList,profileFollowList);
        verify(this.profilesRepository, times(1)).getReferenceById(anyLong());
        verify(this.followsRepository, times(1)).findActiveFollowers(any(ProfileJpa.class));
        verify(this.profileToProfileJpaConverter, times(0)).convertBack(any(ProfileJpa.class));
    }

    //TODO Ritorno il numero di followers ma non la lista dei profili dati che non ho accesso a tale profilo


    @Test
    void testFindAllFollowings_Success(){
        Profile publicProfile2 = new Profile("pinco_pallino",false,2L);
        publicProfile2.setId(2L);
        Profile privateProfile = new Profile("pinco_pallino2",false,3L);
        privateProfile.setId(3L);
        List<Profile> followerList = asList(publicProfile2, privateProfile);
        ProfileFollowList returnedProfileFollowList = new ProfileFollowList(followerList, followerList.size());


        when(this.profilesRepository.getReferenceById(anyLong())).thenReturn(this.publicProfileJpa);
        when(this.followsRepository.findActiveFollowings(any(ProfileJpa.class))).thenReturn(asList(
                this.publicProfileJpa2,
                this.privateProfileJpa
        ));
        when(this.profileToProfileJpaConverter.convertBack(this.publicProfileJpa2)).thenReturn(publicProfile2);
        when(this.profileToProfileJpaConverter.convertBack(this.privateProfileJpa)).thenReturn(privateProfile);

        ProfileFollowList profileFollowList = this.followsService.findAllFollowings(this.publicProfileJpa.getId());

        log.info(profileFollowList.toString());

        assertEquals(returnedProfileFollowList,profileFollowList);
        verify(this.profilesRepository, times(1)).getReferenceById(anyLong());
        verify(this.followsRepository, times(1)).findActiveFollowings(any(ProfileJpa.class));
        verify(this.profileToProfileJpaConverter, times(2)).convertBack(any(ProfileJpa.class));
    }

    @Test
    void testFindAllFollowings_NoFollowings_Success(){
        List<Profile> followerList = Collections.emptyList();
        ProfileFollowList returnedProfileFollowList = new ProfileFollowList(followerList, 0);

        when(this.profilesRepository.getReferenceById(anyLong())).thenReturn(this.publicProfileJpa);
        when(this.followsRepository.findActiveFollowings(any(ProfileJpa.class))).thenReturn(Collections.emptyList());

        ProfileFollowList profileFollowList = this.followsService.findAllFollowings(this.publicProfileJpa.getId());

        log.info(profileFollowList.toString());

        assertEquals(returnedProfileFollowList,profileFollowList);
        verify(this.profilesRepository, times(1)).getReferenceById(anyLong());
        verify(this.followsRepository, times(1)).findActiveFollowings(any(ProfileJpa.class));
        verify(this.profileToProfileJpaConverter, times(0)).convertBack(any(ProfileJpa.class));
    }


     */
    //TODO Ritorno il numero di followers ma non la lista dei profili dati che non ho accesso a tale profilo


}