package com.sergiostefanizzi.profilemicroservice.service;

import com.sergiostefanizzi.profilemicroservice.model.Profile;
import com.sergiostefanizzi.profilemicroservice.model.ProfileJpa;
import com.sergiostefanizzi.profilemicroservice.model.converter.ProfileToProfileJpaConverter;
import com.sergiostefanizzi.profilemicroservice.repository.ProfilesRepository;
import com.sergiostefanizzi.profilemicroservice.system.exception.ProfileAlreadyCreatedException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.validator.routines.UrlValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.net.URI;
import java.util.Optional;

import static org.hamcrest.Matchers.any;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@Slf4j
class ProfilesServiceTest {
    @Mock
    private ProfilesRepository profilesRepository;
    @Mock
    private ProfileToProfileJpaConverter profileToProfileJpaConverter;
    @InjectMocks
    private ProfilesService profilesService;

    String profileName = "giuseppe_verdi";
    Boolean isPrivate = false;
    Long accountId = 1L;
    String bio = "This is Giuseppe's profile!";
    String pictureUrl = "https://upload.wikimedia.org/wikipedia/commons/7/7e/Circle-icons-profile.svg";
    private Profile newProfile;
    private ProfileJpa newProfileJpa;
    private Profile convertedProfile;
    private ProfileJpa oldProfile;
    private UrlValidator validator;
    @BeforeEach
    void setUp() {
        this.newProfile = new Profile(profileName,isPrivate,accountId);
        this.newProfile.setBio(bio);
        this.newProfile.setPictureUrl(pictureUrl);

        this.newProfileJpa = new ProfileJpa(profileName, isPrivate, accountId);
        this.newProfileJpa.setBio(bio);
        this.newProfileJpa.setPictureUrl(pictureUrl);

        this.convertedProfile = new Profile(profileName, isPrivate, accountId);
        this.convertedProfile.setBio(bio);
        this.convertedProfile.setPictureUrl(pictureUrl);
        this.convertedProfile.setId(12L);

        this.oldProfile = new ProfileJpa(profileName, isPrivate, accountId);
        this.oldProfile.setBio(bio);
        this.oldProfile.setPictureUrl(pictureUrl);
        this.oldProfile.setId(12L);

        this.validator = new UrlValidator();
    }

    @AfterEach
    void tearDown() {
    }

    // SAVE A PROFILE
    @Test
    void testSaveSuccess() {
        when(this.profilesRepository.findByProfileName(this.newProfile.getProfileName())).thenReturn(Optional.empty());
        when(this.profileToProfileJpaConverter.convert(this.newProfile)).thenReturn(this.newProfileJpa);
        when(this.profilesRepository.save(this.newProfileJpa)).thenReturn(this.newProfileJpa);
        when(this.profileToProfileJpaConverter.convertBack(this.newProfileJpa)).thenReturn(this.convertedProfile);

        log.info("CREATED_AT before ---> "+this.newProfileJpa.getCreatedAt());

        Profile savedProfile = this.profilesService.save(this.newProfile);

        log.info("CREATED_AT after ---> "+this.newProfileJpa.getCreatedAt());
        log.info("PROFILE_ID after ---> "+savedProfile.getId());
        log.info(String.valueOf(savedProfile));

        assertNotNull(savedProfile.getId());
        assertEquals(profileName, savedProfile.getProfileName());
        assertEquals(isPrivate, savedProfile.getIsPrivate());
        assertEquals(accountId, savedProfile.getAccountId());
        assertEquals(bio, savedProfile.getBio());
        assertEquals(pictureUrl, savedProfile.getPictureUrl());
        assertTrue(validator.isValid(savedProfile.getPictureUrl()));
        verify(this.profileToProfileJpaConverter, times(1)).convert(this.newProfile);
        verify(this.profilesRepository, times(1)).save(this.newProfileJpa);
        verify(this.profileToProfileJpaConverter, times(1)).convertBack(this.newProfileJpa);
    }

    @Test
    void testSaveFailed_ProfileNameExists() {
        when(this.profilesRepository.findByProfileName(this.newProfile.getProfileName())).thenReturn(Optional.of(this.oldProfile));
        log.info("Profile with id "+this.oldProfile.getId()+" exists");
        assertThrows(ProfileAlreadyCreatedException.class, () -> {
            this.profilesService.save(this.newProfile);
        });
        verify(this.profilesRepository, times(1)).findByProfileName(this.newProfile.getProfileName());
        verify(this.profileToProfileJpaConverter, times(0)).convert(this.newProfile);
        verify(this.profilesRepository, times(0)).save(this.newProfileJpa);
        verify(this.profileToProfileJpaConverter, times(0)).convertBack(this.newProfileJpa);
    }


}