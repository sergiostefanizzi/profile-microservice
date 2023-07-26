package com.sergiostefanizzi.profilemicroservice.service;

import com.sergiostefanizzi.profilemicroservice.model.*;
import com.sergiostefanizzi.profilemicroservice.model.converter.FollowsToFollowsJpaConverter;
import com.sergiostefanizzi.profilemicroservice.model.converter.ProfileToProfileJpaConverter;
import com.sergiostefanizzi.profilemicroservice.repository.FollowsRepository;
import com.sergiostefanizzi.profilemicroservice.repository.ProfilesRepository;
import com.sergiostefanizzi.profilemicroservice.system.exception.FollowNotFoundException;
import com.sergiostefanizzi.profilemicroservice.system.exception.ProfileNotFoundException;
import com.sergiostefanizzi.profilemicroservice.system.exception.UnfollowOnCreationException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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
    private ProfileJpa publicProfileJpa;
    private ProfileJpa privateProfileJpa;
    private ProfileJpa publicProfileJpa2;

    @BeforeEach
    void setUp() {
        this.publicProfileJpa = new ProfileJpa("pinco_pallino",false,1L);
        this.publicProfileJpa.setId(1L);
        this.publicProfileJpa.setCreatedAt(LocalDateTime.MIN);

        this.publicProfileJpa2 = new ProfileJpa("pinco_pallino",false,2L);
        this.publicProfileJpa2.setId(2L);
        this.publicProfileJpa2.setCreatedAt(LocalDateTime.MIN);

        this.privateProfileJpa = new ProfileJpa("pinco_pallino",false,3L);
        this.privateProfileJpa.setId(3L);
        this.privateProfileJpa.setCreatedAt(LocalDateTime.MIN);
    }

    @Test
    void testAddFollows_PublicProfile_ACCEPTED_Success(){
        FollowsJpa followsJpa = new FollowsJpa(new FollowsId(this.publicProfileJpa.getId(), this.publicProfileJpa2.getId()));
        followsJpa.setRequestStatus(Follows.RequestStatusEnum.ACCEPTED);
        followsJpa.setFollowedAt(LocalDateTime.now());
        followsJpa.setFollower(this.publicProfileJpa);
        followsJpa.setFollowed(this.publicProfileJpa2);

        Follows follows = new Follows(
                this.publicProfileJpa.getId(),
                this.publicProfileJpa2.getId(),
                followsJpa.getRequestStatus());
        when(this.profilesRepository.findActiveById(anyLong())).thenReturn(Optional.of(this.publicProfileJpa));
        when(this.profilesRepository.findActiveById(anyLong())).thenReturn(Optional.of(this.publicProfileJpa2));
        when(this.followsRepository.findById(any(FollowsId.class))).thenReturn(Optional.empty());
        when(this.followsRepository.save(any(FollowsJpa.class))).thenReturn(followsJpa);
        when(this.followsToFollowsJpaConverter.convertBack(any(FollowsJpa.class))).thenReturn(follows);

        Follows returnedFollows = this.followsService.addFollows(this.publicProfileJpa.getId(), this.publicProfileJpa2.getId(), false);

        assertEquals(follows, returnedFollows);
        verify(this.profilesRepository, times(2)).findActiveById(anyLong());
        verify(this.followsRepository, times(1)).findById(any(FollowsId.class));
        verify(this.followsRepository, times(1)).save(any(FollowsJpa.class));
        verify(this.followsToFollowsJpaConverter, times(1)).convertBack(any(FollowsJpa.class));

        log.info(returnedFollows.toString());
    }

    @Test
    void testAddFollows_PrivateProfile_PENDING_Success(){
        FollowsJpa followsJpa = new FollowsJpa(new FollowsId(this.publicProfileJpa.getId(), this.privateProfileJpa.getId()));
        followsJpa.setRequestStatus(Follows.RequestStatusEnum.PENDING);
        followsJpa.setFollower(this.publicProfileJpa);
        followsJpa.setFollowed(this.privateProfileJpa);

        Follows follows = new Follows(
                this.publicProfileJpa.getId(),
                this.privateProfileJpa.getId(),
                followsJpa.getRequestStatus());
        when(this.profilesRepository.findActiveById(anyLong())).thenReturn(Optional.of(this.publicProfileJpa));
        when(this.profilesRepository.findActiveById(anyLong())).thenReturn(Optional.of(this.privateProfileJpa));
        when(this.followsRepository.findById(any(FollowsId.class))).thenReturn(Optional.empty());
        when(this.followsRepository.save(any(FollowsJpa.class))).thenReturn(followsJpa);
        when(this.followsToFollowsJpaConverter.convertBack(any(FollowsJpa.class))).thenReturn(follows);

        Follows returnedFollows = this.followsService.addFollows(this.publicProfileJpa.getId(), this.privateProfileJpa.getId(), false);

        assertEquals(follows, returnedFollows);
        verify(this.profilesRepository, times(2)).findActiveById(anyLong());
        verify(this.followsRepository, times(1)).findById(any(FollowsId.class));
        verify(this.followsRepository, times(1)).save(any(FollowsJpa.class));
        verify(this.followsToFollowsJpaConverter, times(1)).convertBack(any(FollowsJpa.class));

        log.info(returnedFollows.toString());
    }


    @Test
    void testAddFollows_ProfileNotFound_Failed(){
        FollowsJpa followsJpa = new FollowsJpa(new FollowsId(this.publicProfileJpa.getId(), this.privateProfileJpa.getId()));
        followsJpa.setRequestStatus(Follows.RequestStatusEnum.ACCEPTED);
        //followsJpa.setRequestStatus(Follows.RequestStatusEnum.PENDING);
        followsJpa.setFollower(this.publicProfileJpa);
        followsJpa.setFollowed(this.privateProfileJpa);

        when(this.profilesRepository.findActiveById(anyLong())).thenReturn(Optional.of(this.publicProfileJpa));
        when(this.profilesRepository.findActiveById(anyLong())).thenReturn(Optional.empty());
        when(this.followsRepository.findById(any(FollowsId.class))).thenReturn(Optional.empty());

        assertThrows(ProfileNotFoundException.class,
                () -> this.followsService.addFollows(this.publicProfileJpa.getId(), this.privateProfileJpa.getId(), true));

        verify(this.profilesRepository, times(1)).findActiveById(anyLong());
        verify(this.followsRepository, times(1)).findById(any(FollowsId.class));
        verify(this.followsRepository, times(0)).save(any(FollowsJpa.class));
        verify(this.followsToFollowsJpaConverter, times(0)).convertBack(any(FollowsJpa.class));
    }

    @Test
    void testAddFollows_UnfollowOnCreation_Failed(){
        FollowsJpa followsJpa = new FollowsJpa(new FollowsId(this.publicProfileJpa.getId(), this.privateProfileJpa.getId()));
        followsJpa.setRequestStatus(Follows.RequestStatusEnum.ACCEPTED);
        //followsJpa.setRequestStatus(Follows.RequestStatusEnum.PENDING);
        followsJpa.setFollower(this.publicProfileJpa);
        followsJpa.setFollowed(this.privateProfileJpa);

        when(this.profilesRepository.findActiveById(anyLong())).thenReturn(Optional.of(this.publicProfileJpa));
        when(this.profilesRepository.findActiveById(anyLong())).thenReturn(Optional.of(this.privateProfileJpa));
        when(this.followsRepository.findById(any(FollowsId.class))).thenReturn(Optional.empty());

        assertThrows(UnfollowOnCreationException.class,
                () -> this.followsService.addFollows(this.publicProfileJpa.getId(), this.privateProfileJpa.getId(), true));

        verify(this.profilesRepository, times(2)).findActiveById(anyLong());
        verify(this.followsRepository, times(1)).findById(any(FollowsId.class));
        verify(this.followsRepository, times(0)).save(any(FollowsJpa.class));
        verify(this.followsToFollowsJpaConverter, times(0)).convertBack(any(FollowsJpa.class));
    }

    @Test
    void testAddFollows_FollowsAlreadyCreated_RejectedRequest_follow_PENDING_Success(){
        FollowsJpa followsJpa = new FollowsJpa(new FollowsId(this.publicProfileJpa.getId(), this.privateProfileJpa.getId()));
        followsJpa.setRequestStatus(Follows.RequestStatusEnum.REJECTED);
        followsJpa.setFollower(this.publicProfileJpa);
        followsJpa.setFollowed(this.privateProfileJpa);

        Follows follows = new Follows(
                this.publicProfileJpa.getId(),
                this.privateProfileJpa.getId(),
                Follows.RequestStatusEnum.PENDING);


        when(this.followsRepository.findById(any(FollowsId.class))).thenReturn(Optional.of(followsJpa));
        when(this.followsRepository.save(any(FollowsJpa.class))).thenReturn(followsJpa);
        when(this.followsToFollowsJpaConverter.convertBack(any(FollowsJpa.class))).thenReturn(follows);

        Follows returnedFollows = this.followsService.addFollows(this.publicProfileJpa.getId(), this.privateProfileJpa.getId(), false);

        assertEquals(follows, returnedFollows);
        verify(this.profilesRepository, times(0)).findActiveById(anyLong());
        verify(this.followsRepository, times(1)).findById(any(FollowsId.class));
        verify(this.followsRepository, times(1)).save(any(FollowsJpa.class));
        verify(this.followsToFollowsJpaConverter, times(1)).convertBack(any(FollowsJpa.class));

        log.info(returnedFollows.toString());
    }

    @Test
    void testAddFollows_FollowsAlreadyCreated_RejectedRequest_unfollow_PENDING_Success(){
        FollowsJpa followsJpa = new FollowsJpa(new FollowsId(this.publicProfileJpa.getId(), this.privateProfileJpa.getId()));
        followsJpa.setRequestStatus(Follows.RequestStatusEnum.REJECTED);
        followsJpa.setFollower(this.publicProfileJpa);
        followsJpa.setFollowed(this.privateProfileJpa);

        Follows follows = new Follows(
                this.publicProfileJpa.getId(),
                this.privateProfileJpa.getId(),
                Follows.RequestStatusEnum.REJECTED);

        when(this.followsRepository.findById(any(FollowsId.class))).thenReturn(Optional.of(followsJpa));
        when(this.followsRepository.save(any(FollowsJpa.class))).thenReturn(followsJpa);
        when(this.followsToFollowsJpaConverter.convertBack(any(FollowsJpa.class))).thenReturn(follows);

        Follows returnedFollows = this.followsService.addFollows(this.publicProfileJpa.getId(), this.privateProfileJpa.getId(), true);

        assertEquals(follows, returnedFollows);
        verify(this.profilesRepository, times(0)).findActiveById(anyLong());
        verify(this.followsRepository, times(1)).findById(any(FollowsId.class));
        verify(this.followsRepository, times(1)).save(any(FollowsJpa.class));
        verify(this.followsToFollowsJpaConverter, times(1)).convertBack(any(FollowsJpa.class));

        log.info(returnedFollows.toString());
    }

    @Test
    void testAddFollows_FollowsAlreadyCreated_Accepted_or_Pending_Request_unfollow_REJECTED_Success(){
        FollowsJpa followsJpa = new FollowsJpa(new FollowsId(this.publicProfileJpa.getId(), this.privateProfileJpa.getId()));
        followsJpa.setRequestStatus(Follows.RequestStatusEnum.ACCEPTED);
        followsJpa.setFollower(this.publicProfileJpa);
        followsJpa.setFollowed(this.privateProfileJpa);

        Follows follows = new Follows(
                this.publicProfileJpa.getId(),
                this.privateProfileJpa.getId(),
                Follows.RequestStatusEnum.REJECTED);

        when(this.followsRepository.findById(any(FollowsId.class))).thenReturn(Optional.of(followsJpa));
        when(this.followsRepository.save(any(FollowsJpa.class))).thenReturn(followsJpa);
        when(this.followsToFollowsJpaConverter.convertBack(any(FollowsJpa.class))).thenReturn(follows);

        Follows returnedFollows = this.followsService.addFollows(this.publicProfileJpa.getId(), this.privateProfileJpa.getId(), true);

        assertEquals(follows, returnedFollows);
        verify(this.profilesRepository, times(0)).findActiveById(anyLong());
        verify(this.followsRepository, times(1)).findById(any(FollowsId.class));
        verify(this.followsRepository, times(1)).save(any(FollowsJpa.class));
        verify(this.followsToFollowsJpaConverter, times(1)).convertBack(any(FollowsJpa.class));

        log.info(returnedFollows.toString());
    }

    @Test
    void testAddFollows_FollowsAlreadyCreated_Accepted_or_Pending_Request_follow_ACCEPT_Success(){
        FollowsJpa followsJpa = new FollowsJpa(new FollowsId(this.publicProfileJpa.getId(), this.privateProfileJpa.getId()));
        followsJpa.setRequestStatus(Follows.RequestStatusEnum.ACCEPTED);
        followsJpa.setFollower(this.publicProfileJpa);
        followsJpa.setFollowed(this.privateProfileJpa);

        Follows follows = new Follows(
                this.publicProfileJpa.getId(),
                this.privateProfileJpa.getId(),
                Follows.RequestStatusEnum.ACCEPTED);

        when(this.followsRepository.findById(any(FollowsId.class))).thenReturn(Optional.of(followsJpa));
        when(this.followsRepository.save(any(FollowsJpa.class))).thenReturn(followsJpa);
        when(this.followsToFollowsJpaConverter.convertBack(any(FollowsJpa.class))).thenReturn(follows);

        Follows returnedFollows = this.followsService.addFollows(this.publicProfileJpa.getId(), this.privateProfileJpa.getId(), true);

        assertEquals(follows, returnedFollows);
        verify(this.profilesRepository, times(0)).findActiveById(anyLong());
        verify(this.followsRepository, times(1)).findById(any(FollowsId.class));
        verify(this.followsRepository, times(1)).save(any(FollowsJpa.class));
        verify(this.followsToFollowsJpaConverter, times(1)).convertBack(any(FollowsJpa.class));

        log.info(returnedFollows.toString());
    }

    @Test
    void testAcceptFollows_Success(){
        FollowsJpa followsJpa = new FollowsJpa(new FollowsId(this.publicProfileJpa.getId(), this.privateProfileJpa.getId()));
        followsJpa.setRequestStatus(Follows.RequestStatusEnum.PENDING);
        followsJpa.setFollower(this.publicProfileJpa);
        followsJpa.setFollowed(this.privateProfileJpa);

        FollowsJpa savedFollowsJpa = new FollowsJpa(new FollowsId(this.publicProfileJpa.getId(), this.privateProfileJpa.getId()));
        savedFollowsJpa.setRequestStatus(Follows.RequestStatusEnum.ACCEPTED);
        savedFollowsJpa.setFollowedAt(LocalDateTime.now());
        savedFollowsJpa.setFollower(this.publicProfileJpa);
        savedFollowsJpa.setFollowed(this.privateProfileJpa);

        Follows follows = new Follows(
                this.publicProfileJpa.getId(),
                this.privateProfileJpa.getId(),
                Follows.RequestStatusEnum.ACCEPTED);

        when(this.followsRepository.findById(any(FollowsId.class))).thenReturn(Optional.of(followsJpa));
        when(this.followsRepository.save(any(FollowsJpa.class))).thenReturn(savedFollowsJpa);
        when(this.followsToFollowsJpaConverter.convertBack(any(FollowsJpa.class))).thenReturn(follows);

        Follows returnedFollows = this.followsService.addFollows(this.publicProfileJpa.getId(), this.privateProfileJpa.getId(), false);

        assertEquals(follows, returnedFollows);
        verify(this.followsRepository, times(1)).findById(any(FollowsId.class));
        verify(this.followsRepository, times(1)).save(any(FollowsJpa.class));
        verify(this.followsToFollowsJpaConverter, times(1)).convertBack(any(FollowsJpa.class));
        log.info(savedFollowsJpa.getRequestStatus().toString());
        log.info(returnedFollows.toString());
    }

    @Test
    void testAcceptFollows_Reject_Success(){
        FollowsJpa followsJpa = new FollowsJpa(new FollowsId(this.publicProfileJpa.getId(), this.privateProfileJpa.getId()));
        followsJpa.setRequestStatus(Follows.RequestStatusEnum.PENDING);
        followsJpa.setFollowedAt(LocalDateTime.now());
        followsJpa.setFollower(this.publicProfileJpa);
        followsJpa.setFollowed(this.privateProfileJpa);

        Follows follows = new Follows(
                this.publicProfileJpa.getId(),
                this.privateProfileJpa.getId(),
                Follows.RequestStatusEnum.REJECTED);

        when(this.followsRepository.findById(any(FollowsId.class))).thenReturn(Optional.of(followsJpa));
        when(this.followsRepository.save(any(FollowsJpa.class))).thenReturn(followsJpa);
        when(this.followsToFollowsJpaConverter.convertBack(any(FollowsJpa.class))).thenReturn(follows);

        Follows returnedFollows = this.followsService.addFollows(this.publicProfileJpa.getId(), this.privateProfileJpa.getId(), true);

        assertEquals(follows, returnedFollows);
        verify(this.followsRepository, times(1)).findById(any(FollowsId.class));
        verify(this.followsRepository, times(1)).save(any(FollowsJpa.class));
        verify(this.followsToFollowsJpaConverter, times(1)).convertBack(any(FollowsJpa.class));
        log.info(followsJpa.getRequestStatus().toString());
        log.info(returnedFollows.toString());
    }

    @Test
    void testAcceptFollows_RemoveFollower_Success(){
        FollowsJpa followsJpa = new FollowsJpa(new FollowsId(this.publicProfileJpa.getId(), this.privateProfileJpa.getId()));
        followsJpa.setRequestStatus(Follows.RequestStatusEnum.ACCEPTED);
        followsJpa.setUnfollowedAt(LocalDateTime.now());
        followsJpa.setFollower(this.publicProfileJpa);
        followsJpa.setFollowed(this.privateProfileJpa);

        Follows follows = new Follows(
                this.publicProfileJpa.getId(),
                this.privateProfileJpa.getId(),
                Follows.RequestStatusEnum.REJECTED);

        when(this.followsRepository.findById(any(FollowsId.class))).thenReturn(Optional.of(followsJpa));
        when(this.followsRepository.save(any(FollowsJpa.class))).thenReturn(followsJpa);
        when(this.followsToFollowsJpaConverter.convertBack(any(FollowsJpa.class))).thenReturn(follows);

        Follows returnedFollows = this.followsService.addFollows(this.publicProfileJpa.getId(), this.privateProfileJpa.getId(), true);

        assertEquals(follows, returnedFollows);
        verify(this.followsRepository, times(1)).findById(any(FollowsId.class));
        verify(this.followsRepository, times(1)).save(any(FollowsJpa.class));
        verify(this.followsToFollowsJpaConverter, times(1)).convertBack(any(FollowsJpa.class));
        log.info(followsJpa.getRequestStatus().toString());
        log.info(returnedFollows.toString());
    }

    @Test
    void testAcceptFollows_AcceptedAlready_Success(){
        FollowsJpa followsJpa = new FollowsJpa(new FollowsId(this.publicProfileJpa.getId(), this.privateProfileJpa.getId()));
        followsJpa.setRequestStatus(Follows.RequestStatusEnum.ACCEPTED);
        followsJpa.setUnfollowedAt(LocalDateTime.now());
        followsJpa.setFollower(this.publicProfileJpa);
        followsJpa.setFollowed(this.privateProfileJpa);

        Follows follows = new Follows(
                this.publicProfileJpa.getId(),
                this.privateProfileJpa.getId(),
                Follows.RequestStatusEnum.ACCEPTED);


        when(this.followsRepository.findById(any(FollowsId.class))).thenReturn(Optional.of(followsJpa));
        when(this.followsRepository.save(any(FollowsJpa.class))).thenReturn(followsJpa);
        when(this.followsToFollowsJpaConverter.convertBack(any(FollowsJpa.class))).thenReturn(follows);

        Follows returnedFollows = this.followsService.addFollows(this.publicProfileJpa.getId(), this.privateProfileJpa.getId(), false);

        assertEquals(follows, returnedFollows);
        verify(this.followsRepository, times(1)).findById(any(FollowsId.class));
        verify(this.followsRepository, times(1)).save(any(FollowsJpa.class));
        verify(this.followsToFollowsJpaConverter, times(1)).convertBack(any(FollowsJpa.class));
        log.info(followsJpa.getRequestStatus().toString());
        log.info(returnedFollows.toString());
    }

    @Test
    void testAcceptFollows_FollowNotFound_Failed(){
        FollowsJpa followsJpa = new FollowsJpa(new FollowsId(this.publicProfileJpa.getId(), this.privateProfileJpa.getId()));
        followsJpa.setRequestStatus(Follows.RequestStatusEnum.ACCEPTED);
        followsJpa.setUnfollowedAt(LocalDateTime.now());
        followsJpa.setFollower(this.publicProfileJpa);
        followsJpa.setFollowed(this.privateProfileJpa);

        when(this.profilesRepository.findActiveById(this.publicProfileJpa.getId())).thenReturn(Optional.of(this.publicProfileJpa));
        when(this.profilesRepository.findActiveById(this.privateProfileJpa.getId())).thenReturn(Optional.of(this.privateProfileJpa));
        when(this.followsRepository.findById(any(FollowsId.class))).thenReturn(Optional.empty());

        assertThrows(FollowNotFoundException.class,
                () -> this.followsService.acceptFollows(this.publicProfileJpa.getId(), this.privateProfileJpa.getId(), false));

        //verify(this.profilesRepository, times(2)).findActiveById(anyLong());
        verify(this.followsRepository, times(1)).findById(any(FollowsId.class));
        verify(this.followsRepository, times(0)).save(any(FollowsJpa.class));
        verify(this.followsToFollowsJpaConverter, times(0)).convertBack(any(FollowsJpa.class));
    }

    @Test
    void testFindAllFollowers_Success(){
        Profile publicProfile2 = new Profile("pinco_pallino",false,2L);
        publicProfile2.setId(2L);
        Profile privateProfile = new Profile("pinco_pallino2",false,3L);
        privateProfile.setId(3L);
        List<Profile> followerList = asList(publicProfile2, privateProfile);
        ProfileFollowList returnedProfileFollowList = new ProfileFollowList(followerList, followerList.size());


        when(this.profilesRepository.findActiveById(anyLong())).thenReturn(Optional.of(this.publicProfileJpa));
        when(this.followsRepository.findActiveFollowers(any(ProfileJpa.class))).thenReturn(asList(
                this.publicProfileJpa2,
                this.privateProfileJpa
        ));
        when(this.profileToProfileJpaConverter.convertBack(this.publicProfileJpa2)).thenReturn(publicProfile2);
        when(this.profileToProfileJpaConverter.convertBack(this.privateProfileJpa)).thenReturn(privateProfile);

        ProfileFollowList profileFollowList = this.followsService.findAllFollowers(this.publicProfileJpa.getId());

        log.info(profileFollowList.toString());

        assertEquals(returnedProfileFollowList,profileFollowList);
        verify(this.profilesRepository, times(1)).findActiveById(anyLong());
        verify(this.followsRepository, times(1)).findActiveFollowers(any(ProfileJpa.class));
        verify(this.profileToProfileJpaConverter, times(2)).convertBack(any(ProfileJpa.class));
    }

    @Test
    void testFindAllFollowers_NoFollowers_Success(){
        List<Profile> followerList = Collections.emptyList();
        ProfileFollowList returnedProfileFollowList = new ProfileFollowList(followerList, 0);

        when(this.profilesRepository.findActiveById(anyLong())).thenReturn(Optional.of(this.publicProfileJpa));
        when(this.followsRepository.findActiveFollowers(any(ProfileJpa.class))).thenReturn(Collections.emptyList());

        ProfileFollowList profileFollowList = this.followsService.findAllFollowers(this.publicProfileJpa.getId());

        log.info(profileFollowList.toString());

        assertEquals(returnedProfileFollowList,profileFollowList);
        verify(this.profilesRepository, times(1)).findActiveById(anyLong());
        verify(this.followsRepository, times(1)).findActiveFollowers(any(ProfileJpa.class));
        verify(this.profileToProfileJpaConverter, times(0)).convertBack(any(ProfileJpa.class));
    }

    //TODO Ritorno il numero di followers ma non la lista dei profili dati che non ho accesso a tale profilo

    @Test
    void testFindAllFollowers_Failed(){
        when(this.profilesRepository.findActiveById(anyLong())).thenReturn(Optional.empty());

        assertThrows(ProfileNotFoundException.class, () -> this.followsService.findAllFollowers(this.publicProfileJpa.getId()));
        verify(this.profilesRepository, times(1)).findActiveById(anyLong());
        verify(this.followsRepository, times(0)).findActiveFollowers(any(ProfileJpa.class));
        verify(this.profileToProfileJpaConverter, times(0)).convertBack(any(ProfileJpa.class));
    }

    @Test
    void testFindAllFollowings_Success(){
        Profile publicProfile2 = new Profile("pinco_pallino",false,2L);
        publicProfile2.setId(2L);
        Profile privateProfile = new Profile("pinco_pallino2",false,3L);
        privateProfile.setId(3L);
        List<Profile> followerList = asList(publicProfile2, privateProfile);
        ProfileFollowList returnedProfileFollowList = new ProfileFollowList(followerList, followerList.size());


        when(this.profilesRepository.findActiveById(anyLong())).thenReturn(Optional.of(this.publicProfileJpa));
        when(this.followsRepository.findActiveFollowings(any(ProfileJpa.class))).thenReturn(asList(
                this.publicProfileJpa2,
                this.privateProfileJpa
        ));
        when(this.profileToProfileJpaConverter.convertBack(this.publicProfileJpa2)).thenReturn(publicProfile2);
        when(this.profileToProfileJpaConverter.convertBack(this.privateProfileJpa)).thenReturn(privateProfile);

        ProfileFollowList profileFollowList = this.followsService.findAllFollowings(this.publicProfileJpa.getId());

        log.info(profileFollowList.toString());

        assertEquals(returnedProfileFollowList,profileFollowList);
        verify(this.profilesRepository, times(1)).findActiveById(anyLong());
        verify(this.followsRepository, times(1)).findActiveFollowings(any(ProfileJpa.class));
        verify(this.profileToProfileJpaConverter, times(2)).convertBack(any(ProfileJpa.class));
    }

    @Test
    void testFindAllFollowings_NoFollowings_Success(){
        List<Profile> followerList = Collections.emptyList();
        ProfileFollowList returnedProfileFollowList = new ProfileFollowList(followerList, 0);

        when(this.profilesRepository.findActiveById(anyLong())).thenReturn(Optional.of(this.publicProfileJpa));
        when(this.followsRepository.findActiveFollowings(any(ProfileJpa.class))).thenReturn(Collections.emptyList());

        ProfileFollowList profileFollowList = this.followsService.findAllFollowings(this.publicProfileJpa.getId());

        log.info(profileFollowList.toString());

        assertEquals(returnedProfileFollowList,profileFollowList);
        verify(this.profilesRepository, times(1)).findActiveById(anyLong());
        verify(this.followsRepository, times(1)).findActiveFollowings(any(ProfileJpa.class));
        verify(this.profileToProfileJpaConverter, times(0)).convertBack(any(ProfileJpa.class));
    }

    //TODO Ritorno il numero di followers ma non la lista dei profili dati che non ho accesso a tale profilo

    @Test
    void testFindAllFollowings_Failed(){
        when(this.profilesRepository.findActiveById(anyLong())).thenReturn(Optional.empty());

        assertThrows(ProfileNotFoundException.class, () -> this.followsService.findAllFollowings(this.publicProfileJpa.getId()));
        verify(this.profilesRepository, times(1)).findActiveById(anyLong());
        verify(this.followsRepository, times(0)).findActiveFollowings(any(ProfileJpa.class));
        verify(this.profileToProfileJpaConverter, times(0)).convertBack(any(ProfileJpa.class));
    }

}