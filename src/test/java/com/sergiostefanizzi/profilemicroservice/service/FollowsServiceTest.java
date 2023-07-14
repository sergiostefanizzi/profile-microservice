package com.sergiostefanizzi.profilemicroservice.service;

import com.sergiostefanizzi.profilemicroservice.model.converter.FollowsToFollowsJpaConverter;
import com.sergiostefanizzi.profilemicroservice.repository.FollowsRepository;
import com.sergiostefanizzi.profilemicroservice.repository.ProfilesRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

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

    @BeforeEach
    void setUp() {
    }

    @Test
    void testAddFollows_Success(){

    }
}