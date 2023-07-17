package com.sergiostefanizzi.profilemicroservice.service;

import com.sergiostefanizzi.profilemicroservice.model.Follows;
import com.sergiostefanizzi.profilemicroservice.model.FollowsId;
import com.sergiostefanizzi.profilemicroservice.model.FollowsJpa;
import com.sergiostefanizzi.profilemicroservice.model.ProfileJpa;
import com.sergiostefanizzi.profilemicroservice.model.converter.FollowsToFollowsJpaConverter;
import com.sergiostefanizzi.profilemicroservice.repository.FollowsRepository;
import com.sergiostefanizzi.profilemicroservice.repository.ProfilesRepository;
import com.sergiostefanizzi.profilemicroservice.system.exception.FollowAlreadyCreatedException;
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
    private ProfileJpa openProfileJpa;
    private ProfileJpa privateProfileJpa;
    private ProfileJpa openProfileJpa2;

    @BeforeEach
    void setUp() {
        this.openProfileJpa = new ProfileJpa("pinco_pallino",false,1L);
        this.openProfileJpa.setId(1L);
        this.openProfileJpa.setCreatedAt(LocalDateTime.MIN);

        this.openProfileJpa2 = new ProfileJpa("pinco_pallino",false,2L);
        this.openProfileJpa2.setId(2L);
        this.openProfileJpa2.setCreatedAt(LocalDateTime.MIN);

        this.privateProfileJpa = new ProfileJpa("pinco_pallino",false,3L);
        this.privateProfileJpa.setId(3L);
        this.privateProfileJpa.setCreatedAt(LocalDateTime.MIN);
    }

    @Test
    void testAddFollows_OpenProfile_ACCEPTED_Success(){
        FollowsJpa followsJpa = new FollowsJpa(new FollowsId(this.openProfileJpa.getId(), this.openProfileJpa2.getId()));
        followsJpa.setRequestStatus(Follows.RequestStatusEnum.ACCEPTED);
        followsJpa.setFollowedAt(LocalDateTime.now());
        followsJpa.setFollower(this.openProfileJpa);
        followsJpa.setFollowed(this.openProfileJpa2);

        Follows follows = new Follows(
                this.openProfileJpa.getId(),
                this.openProfileJpa2.getId(),
                followsJpa.getRequestStatus());
        when(this.profilesRepository.findNotDeletedById(anyLong())).thenReturn(Optional.of(this.openProfileJpa));
        when(this.profilesRepository.findNotDeletedById(anyLong())).thenReturn(Optional.of(this.openProfileJpa2));
        when(this.followsRepository.findById(any(FollowsId.class))).thenReturn(Optional.empty());
        when(this.followsRepository.save(any(FollowsJpa.class))).thenReturn(followsJpa);
        when(this.followsToFollowsJpaConverter.convertBack(any(FollowsJpa.class))).thenReturn(follows);

        Follows returnedFollows = this.followsService.addFollows(this.openProfileJpa.getId(), this.openProfileJpa2.getId());

        assertEquals(follows, returnedFollows);
        verify(this.profilesRepository, times(2)).findNotDeletedById(anyLong());
        verify(this.followsRepository, times(1)).findById(any(FollowsId.class));
        verify(this.followsRepository, times(1)).save(any(FollowsJpa.class));
        verify(this.followsToFollowsJpaConverter, times(1)).convertBack(any(FollowsJpa.class));

        log.info(returnedFollows.toString());
    }

    @Test
    void testAddFollows_PrivateProfile_PENDING_Success(){
        FollowsJpa followsJpa = new FollowsJpa(new FollowsId(this.openProfileJpa.getId(), this.privateProfileJpa.getId()));
        followsJpa.setRequestStatus(Follows.RequestStatusEnum.PENDING);
        followsJpa.setFollower(this.openProfileJpa);
        followsJpa.setFollowed(this.privateProfileJpa);

        Follows follows = new Follows(
                this.openProfileJpa.getId(),
                this.privateProfileJpa.getId(),
                followsJpa.getRequestStatus());
        when(this.profilesRepository.findNotDeletedById(anyLong())).thenReturn(Optional.of(this.openProfileJpa));
        when(this.profilesRepository.findNotDeletedById(anyLong())).thenReturn(Optional.of(this.privateProfileJpa));
        when(this.followsRepository.findById(any(FollowsId.class))).thenReturn(Optional.empty());
        when(this.followsRepository.save(any(FollowsJpa.class))).thenReturn(followsJpa);
        when(this.followsToFollowsJpaConverter.convertBack(any(FollowsJpa.class))).thenReturn(follows);

        Follows returnedFollows = this.followsService.addFollows(this.openProfileJpa.getId(), this.privateProfileJpa.getId());

        assertEquals(follows, returnedFollows);
        verify(this.profilesRepository, times(2)).findNotDeletedById(anyLong());
        verify(this.followsRepository, times(1)).findById(any(FollowsId.class));
        verify(this.followsRepository, times(1)).save(any(FollowsJpa.class));
        verify(this.followsToFollowsJpaConverter, times(1)).convertBack(any(FollowsJpa.class));

        log.info(returnedFollows.toString());
    }

    @Test
    void testAddFollows_PrivateProfile_RejectedRequest_PENDING_Success(){
        FollowsJpa followsJpa = new FollowsJpa(new FollowsId(this.openProfileJpa.getId(), this.privateProfileJpa.getId()));
        followsJpa.setRequestStatus(Follows.RequestStatusEnum.REJECTED);
        followsJpa.setFollower(this.openProfileJpa);
        followsJpa.setFollowed(this.privateProfileJpa);

        Follows follows = new Follows(
                this.openProfileJpa.getId(),
                this.privateProfileJpa.getId(),
                Follows.RequestStatusEnum.PENDING);

        when(this.profilesRepository.findNotDeletedById(anyLong())).thenReturn(Optional.of(this.openProfileJpa));
        when(this.profilesRepository.findNotDeletedById(anyLong())).thenReturn(Optional.of(this.privateProfileJpa));
        when(this.followsRepository.findById(any(FollowsId.class))).thenReturn(Optional.of(followsJpa));
        when(this.followsRepository.save(any(FollowsJpa.class))).thenReturn(followsJpa);
        when(this.followsToFollowsJpaConverter.convertBack(any(FollowsJpa.class))).thenReturn(follows);

        Follows returnedFollows = this.followsService.addFollows(this.openProfileJpa.getId(), this.privateProfileJpa.getId());

        assertEquals(follows, returnedFollows);
        verify(this.profilesRepository, times(2)).findNotDeletedById(anyLong());
        verify(this.followsRepository, times(1)).findById(any(FollowsId.class));
        verify(this.followsRepository, times(1)).save(any(FollowsJpa.class));
        verify(this.followsToFollowsJpaConverter, times(1)).convertBack(any(FollowsJpa.class));

        log.info(returnedFollows.toString());
    }

    @Test
    void testAddFollows_Accepted_or_Pending_Request_PENDING_Failed(){
        FollowsJpa followsJpa = new FollowsJpa(new FollowsId(this.openProfileJpa.getId(), this.privateProfileJpa.getId()));
        followsJpa.setRequestStatus(Follows.RequestStatusEnum.ACCEPTED);
        //followsJpa.setRequestStatus(Follows.RequestStatusEnum.PENDING);
        followsJpa.setFollower(this.openProfileJpa);
        followsJpa.setFollowed(this.privateProfileJpa);

        when(this.profilesRepository.findNotDeletedById(anyLong())).thenReturn(Optional.of(this.openProfileJpa));
        when(this.profilesRepository.findNotDeletedById(anyLong())).thenReturn(Optional.of(this.privateProfileJpa));
        when(this.followsRepository.findById(any(FollowsId.class))).thenReturn(Optional.of(followsJpa));

        assertThrows(FollowAlreadyCreatedException.class,
                () -> this.followsService.addFollows(this.openProfileJpa.getId(), this.privateProfileJpa.getId()));

        verify(this.profilesRepository, times(2)).findNotDeletedById(anyLong());
        verify(this.followsRepository, times(1)).findById(any(FollowsId.class));
        verify(this.followsRepository, times(0)).save(any(FollowsJpa.class));
        verify(this.followsToFollowsJpaConverter, times(0)).convertBack(any(FollowsJpa.class));
    }
}