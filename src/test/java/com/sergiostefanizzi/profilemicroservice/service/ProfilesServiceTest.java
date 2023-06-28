package com.sergiostefanizzi.profilemicroservice.service;

import com.sergiostefanizzi.profilemicroservice.model.*;
import com.sergiostefanizzi.profilemicroservice.model.converter.ProfileToProfileJpaConverter;
import com.sergiostefanizzi.profilemicroservice.repository.ProfilesRepository;
import com.sergiostefanizzi.profilemicroservice.system.exception.PostNotFoundException;
import com.sergiostefanizzi.profilemicroservice.system.exception.ProfileAlreadyCreatedException;
import com.sergiostefanizzi.profilemicroservice.system.exception.ProfileNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.validator.routines.UrlValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import javax.swing.text.html.Option;
import java.time.LocalDateTime;
import java.util.Optional;

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
    Boolean updatedIsPrivate = true;
    Long accountId = 1L;
    String bio = "This is Giuseppe's profile!";
    String updatedBio = "New Giuseppe's bio";
    String pictureUrl = "https://upload.wikimedia.org/wikipedia/commons/7/7e/Circle-icons-profile.svg";
    String updatedPictureUrl = "https://icons-for-free.com/iconfiles/png/512/avatar+person+profile+user+icon-1320086059654790795.png";
    Long profileId = 12L;
    private Profile newProfile;
    private ProfileJpa savedProfileJpa;
    private UrlValidator validator;
    @BeforeEach
    void setUp() {
        this.newProfile = new Profile(profileName,isPrivate,accountId);
        this.newProfile.setBio(bio);
        this.newProfile.setPictureUrl(pictureUrl);

        this.savedProfileJpa = new ProfileJpa(profileName, isPrivate, accountId);
        this.savedProfileJpa.setBio(bio);
        this.savedProfileJpa.setPictureUrl(pictureUrl);
        this.savedProfileJpa.setId(profileId);



        this.validator = new UrlValidator();
    }

    @AfterEach
    void tearDown() {
    }

    // SAVE A PROFILE
    @Test
    void testSaveSuccess() {
        // Jpa del nuovo profilo nel db
        ProfileJpa newProfileJpa = new ProfileJpa(profileName, isPrivate, accountId);
        newProfileJpa.setBio(bio);
        newProfileJpa.setPictureUrl(pictureUrl);

        // Profilo che verra' restituito service
        Profile convertedProfile = new Profile(profileName, isPrivate, accountId);
        convertedProfile.setBio(bio);
        convertedProfile.setPictureUrl(pictureUrl);
        convertedProfile.setId(profileId);

        when(this.profilesRepository.findByProfileName(this.newProfile.getProfileName())).thenReturn(Optional.empty());
        when(this.profileToProfileJpaConverter.convert(this.newProfile)).thenReturn(newProfileJpa);
        when(this.profilesRepository.save(newProfileJpa)).thenReturn(newProfileJpa);
        when(this.profileToProfileJpaConverter.convertBack(newProfileJpa)).thenReturn(convertedProfile);

        log.info("CREATED_AT before ---> "+newProfileJpa.getCreatedAt());

        Profile savedProfile = this.profilesService.save(this.newProfile);

        assertNotNull(savedProfile);
        assertEquals(profileId, savedProfile.getId());
        assertEquals(profileName, savedProfile.getProfileName());
        assertEquals(isPrivate, savedProfile.getIsPrivate());
        assertEquals(accountId, savedProfile.getAccountId());
        assertEquals(bio, savedProfile.getBio());
        assertEquals(pictureUrl, savedProfile.getPictureUrl());
        assertTrue(validator.isValid(savedProfile.getPictureUrl()));
        verify(this.profileToProfileJpaConverter, times(1)).convert(this.newProfile);
        verify(this.profilesRepository, times(1)).save(newProfileJpa);
        verify(this.profileToProfileJpaConverter, times(1)).convertBack(newProfileJpa);

        log.info("CREATED_AT after ---> "+newProfileJpa.getCreatedAt());
        log.info("PROFILE_ID after ---> "+savedProfile.getId());
        log.info(String.valueOf(savedProfile));
    }

    @Test
    void testSaveFailed_ProfileNameExists() {
        when(this.profilesRepository.findByProfileName(this.newProfile.getProfileName())).thenReturn(Optional.of(this.savedProfileJpa));
        log.info("Profile with name "+this.savedProfileJpa.getProfileName()+" exists");
        assertThrows(ProfileAlreadyCreatedException.class, () -> {
            this.profilesService.save(this.newProfile);
        });
        verify(this.profilesRepository, times(1)).findByProfileName(this.newProfile.getProfileName());
        verify(this.profileToProfileJpaConverter, times(0)).convert(this.newProfile);
        verify(this.profilesRepository, times(0)).save(any(ProfileJpa.class));
        verify(this.profileToProfileJpaConverter, times(0)).convertBack(any(ProfileJpa.class));
    }

    // REMOVE A PROFILE
    @Test
    void testRemoveSuccess(){
        when(this.profilesRepository.findById(profileId)).thenReturn(Optional.of(this.savedProfileJpa));
        when(this.profilesRepository.save(this.savedProfileJpa)).thenReturn(this.savedProfileJpa);

        // l'istante di rimozione deve essere nullo prima della rimozione
        assertNull(this.savedProfileJpa.getDeletedAt());

        this.profilesService.remove(profileId);

        // l'istante di rimozione deve essere NON nullo DOPO la rimozione
        assertNotNull(this.savedProfileJpa.getDeletedAt());
        verify(this.profilesRepository, times(1)).findById(profileId);
        verify(this.profilesRepository, times(1)).save(this.savedProfileJpa);
    }

    @Test
    void testRemove_ProfileNot_Found_Failed(){
        when(this.profilesRepository.findById(profileId)).thenReturn(Optional.empty());
        assertThrows(ProfileNotFoundException.class,
                () -> this.profilesService.remove(profileId)
        );

        verify(this.profilesRepository, times(1)).findById(profileId);
        verify(this.profilesRepository, times(0)).save(any(ProfileJpa.class));
    }

    @Test
    void testRemove_ProfileAlreadyRemoved_Failed(){
        // Imposto una data passata
        this.savedProfileJpa.setDeletedAt(LocalDateTime.MIN);
        log.info("Deleted At"+this.savedProfileJpa.getDeletedAt());

        when(this.profilesRepository.findById(profileId)).thenReturn(Optional.of(this.savedProfileJpa));

        assertThrows(ProfileNotFoundException.class,
                () -> this.profilesService.remove(profileId)
        );

        verify(this.profilesRepository, times(1)).findById(profileId);
        verify(this.profilesRepository, times(0)).save(any(ProfileJpa.class));
    }

    // UPDATE A PROFILE

    @Test
    void testUpdateSuccess(){
        // Definisco un o piu' campi del profilo da aggiornare tramite l'oggetto ProfilePatch
        ProfilePatch profilePatch = new ProfilePatch();
        profilePatch.setBio(updatedBio);
        profilePatch.setPictureUrl(updatedPictureUrl);
        profilePatch.setIsPrivate(updatedIsPrivate);

        // Aggiorno il profilo che verra' restituito dal service con i nuovi valori
        Profile convertedProfile = new Profile(profileName, isPrivate, accountId);
        convertedProfile.setId(profileId);
        convertedProfile.setBio(profilePatch.getBio() != null ? profilePatch.getBio() : bio);
        convertedProfile.setPictureUrl(profilePatch.getPictureUrl() != null ? profilePatch.getPictureUrl() : pictureUrl);
        convertedProfile.setIsPrivate(profilePatch.getIsPrivate() != null ? profilePatch.getIsPrivate() : isPrivate);

        when(this.profilesRepository.findById(profileId)).thenReturn(Optional.of(this.savedProfileJpa));
        when(this.profilesRepository.save(this.savedProfileJpa)).thenReturn(this.savedProfileJpa);
        when(this.profileToProfileJpaConverter.convertBack(this.savedProfileJpa)).thenReturn(convertedProfile);

        Profile updatedProfile = this.profilesService.update(profileId, profilePatch);

        assertNotNull(updatedProfile);
        assertEquals(profileId, updatedProfile.getId());
        assertEquals(profileName, updatedProfile.getProfileName());
        assertEquals(updatedBio, updatedProfile.getBio());
        assertEquals(updatedPictureUrl, updatedProfile.getPictureUrl());
        assertEquals(updatedIsPrivate, updatedProfile.getIsPrivate());
        //assertEquals(bio, updatedProfile.getBio());
        //assertEquals(pictureUrl, updatedProfile.getPictureUrl());
        //assertEquals(isPrivate, updatedProfile.getIsPrivate());

        verify(this.profilesRepository, times(1)).findById(profileId);
        verify(this.profilesRepository, times(1)).save(this.savedProfileJpa);
        verify(this.profileToProfileJpaConverter, times(1)).convertBack(this.savedProfileJpa);
    }

    @Test
    void testUpdate_ProfileNotFound_Failed(){
        when(this.profilesRepository.findById(profileId)).thenReturn(Optional.empty());

        assertThrows(ProfileNotFoundException.class,
                () -> this.profilesService.update(profileId, any(ProfilePatch.class))
        );


        verify(this.profilesRepository, times(1)).findById(profileId);
        verify(this.profilesRepository, times(0)).save(this.savedProfileJpa);
        verify(this.profileToProfileJpaConverter, times(0)).convertBack(this.savedProfileJpa);
    }

    @Test
    void testUpdate_ProfileAlreadyRemoved_Failed(){
        // Imposto una data passata
        this.savedProfileJpa.setDeletedAt(LocalDateTime.MIN);
        log.info("Deleted At"+this.savedProfileJpa.getDeletedAt());

        when(this.profilesRepository.findById(profileId)).thenReturn(Optional.empty());

        assertThrows(ProfileNotFoundException.class,
                () -> this.profilesService.update(profileId, any(ProfilePatch.class))
        );


        verify(this.profilesRepository, times(1)).findById(profileId);
        verify(this.profilesRepository, times(0)).save(this.savedProfileJpa);
        verify(this.profileToProfileJpaConverter, times(0)).convertBack(this.savedProfileJpa);
    }

    @Test
    void testFindByNameSuccess(){
        // Post che verra' restituito
        Profile convertedProfile = new Profile(profileName, isPrivate, accountId);
        convertedProfile.setBio(bio);
        convertedProfile.setPictureUrl(pictureUrl);
        convertedProfile.setId(profileId);

        when(this.profilesRepository.findByProfileName(profileName)).thenReturn(Optional.of(this.savedProfileJpa));
        when(this.profileToProfileJpaConverter.convertBack((this.savedProfileJpa))).thenReturn(convertedProfile);

        Profile profile = this.profilesService.findByProfileName(profileName);

        assertNotNull(profile);
        assertEquals(profileId, profile.getId());
        assertEquals(profileName, profile.getProfileName());
        assertEquals(isPrivate, profile.getIsPrivate());
        assertEquals(accountId, profile.getAccountId());
        assertEquals(bio, profile.getBio());
        assertEquals(pictureUrl, profile.getPictureUrl());
        verify(this.profilesRepository, times(1)).findByProfileName(profileName);
        verify(this.profileToProfileJpaConverter, times(1)).convertBack(this.savedProfileJpa);
    }

    @Test
    void testFindByName_NotFound_Failed(){
        when(this.profilesRepository.findByProfileName(profileName)).thenReturn(Optional.empty());
        assertThrows(ProfileNotFoundException.class,
                () -> this.profilesService.findByProfileName(profileName)
        );

        verify(this.profilesRepository, times(1)).findByProfileName(profileName);
        verify(this.profileToProfileJpaConverter, times(0)).convertBack(any(ProfileJpa.class));
    }

    @Test
    void testFindByName_PostAlreadyRemoved_Failed(){
        // Imposto una data passata
        this.savedProfileJpa.setDeletedAt(LocalDateTime.MIN);
        log.info("Deleted At"+this.savedProfileJpa.getDeletedAt());

        when(this.profilesRepository.findByProfileName(profileName)).thenReturn(Optional.of(this.savedProfileJpa));

        assertThrows(ProfileNotFoundException.class,
                () -> this.profilesService.findByProfileName(profileName)
        );

        verify(this.profilesRepository, times(1)).findByProfileName(profileName);
        verify(this.profileToProfileJpaConverter, times(0)).convertBack(any(ProfileJpa.class));
    }

}