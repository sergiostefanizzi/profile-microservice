package com.sergiostefanizzi.profilemicroservice.service;

import com.sergiostefanizzi.profilemicroservice.model.*;
import com.sergiostefanizzi.profilemicroservice.model.converter.PostToPostJpaConverter;
import com.sergiostefanizzi.profilemicroservice.model.converter.ProfileToProfileJpaConverter;
import com.sergiostefanizzi.profilemicroservice.repository.PostsRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@Slf4j
class ProfilesServiceTest {
    @Mock
    private ProfilesRepository profilesRepository;
    @Mock
    private PostsRepository postsRepository;
    @Mock
    private ProfileToProfileJpaConverter profileToProfileJpaConverter;
    @Mock
    private PostToPostJpaConverter postToPostJpaConverter;
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
    private ProfileJpa savedProfileJpa2;
    private ProfileJpa savedProfileJpa3;
    private UrlValidator validator;
    String contentUrl = "https://upload.wikimedia.org/wikipedia/commons/9/9a/Cape_may.jpg";
    String caption = "This is the post caption";
    Post.PostTypeEnum postType = Post.PostTypeEnum.POST;
    Long postId = 1L;
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
        this.savedProfileJpa2 = new ProfileJpa(profileName+"_2", isPrivate, accountId);
        this.savedProfileJpa2.setBio(bio);
        this.savedProfileJpa2.setPictureUrl(pictureUrl);
        this.savedProfileJpa2.setId(2L);

        this.savedProfileJpa3 = new ProfileJpa(profileName+"_3", isPrivate, accountId);
        this.savedProfileJpa3.setBio(bio);
        this.savedProfileJpa3.setPictureUrl(pictureUrl);
        this.savedProfileJpa3.setId(3L);
        this.savedProfileJpa3.setDeletedAt(LocalDateTime.MIN);


        // Post che verra' restituito
        Profile convertedProfile1 = new Profile(profileName, isPrivate, accountId);
        convertedProfile1.setBio(bio);
        convertedProfile1.setPictureUrl(pictureUrl);
        convertedProfile1.setId(profileId);

        Profile convertedProfile2 = new Profile(profileName+"_2", isPrivate, accountId);
        convertedProfile2.setBio(bio);
        convertedProfile2.setPictureUrl(pictureUrl);
        convertedProfile2.setId(2L);

        Profile convertedProfile3 = new Profile(profileName+"_3", isPrivate, accountId);
        convertedProfile3.setBio(bio);
        convertedProfile3.setPictureUrl(pictureUrl);
        convertedProfile3.setId(3L);

        when(this.profilesRepository.findAllByProfileName(profileName)).thenReturn(asList(this.savedProfileJpa, this.savedProfileJpa2, this.savedProfileJpa3));
        when(this.profileToProfileJpaConverter.convertBack(this.savedProfileJpa)).thenReturn(convertedProfile1);
        when(this.profileToProfileJpaConverter.convertBack(this.savedProfileJpa2)).thenReturn(convertedProfile2);


        List<Profile> profileList = this.profilesService.findByProfileName(profileName);

        assertNotNull(profileList);
        assertEquals(2, profileList.size());
        assertEquals(profileId, profileList.get(0).getId());
        assertEquals(profileName, profileList.get(0).getProfileName());
        assertEquals(bio, profileList.get(0).getBio());
        assertEquals(pictureUrl, profileList.get(0).getPictureUrl());
        assertEquals(isPrivate, profileList.get(0).getIsPrivate());
        assertEquals(2L, profileList.get(1).getId());
        assertEquals(profileName+"_2", profileList.get(1).getProfileName());
        assertEquals(bio, profileList.get(1).getBio());
        assertEquals(pictureUrl, profileList.get(1).getPictureUrl());
        assertEquals(isPrivate, profileList.get(1).getIsPrivate());
        verify(this.profilesRepository, times(1)).findAllByProfileName(profileName);
        verify(this.profileToProfileJpaConverter, times(1)).convertBack(this.savedProfileJpa);
        verify(this.profileToProfileJpaConverter, times(1)).convertBack(this.savedProfileJpa2);
        verify(this.profileToProfileJpaConverter, times(0)).convertBack(this.savedProfileJpa3);
    }

    @Test
    void testFindFullSuccess(){
        // Profile che verra' restituito
        Profile convertedProfile = new Profile(profileName, isPrivate, accountId);
        convertedProfile.setBio(bio);
        convertedProfile.setPictureUrl(pictureUrl);
        convertedProfile.setId(profileId);

        PostJpa newPostJpa1 = new PostJpa(contentUrl, postType);
        newPostJpa1.setCaption(caption);
        newPostJpa1.setProfile(this.savedProfileJpa);

        PostJpa newPostJpa2 = new PostJpa(contentUrl, postType);
        newPostJpa2.setCaption(caption);
        newPostJpa2.setProfile(this.savedProfileJpa);

        List<PostJpa> postJpaList = new ArrayList<>();
        postJpaList.add(newPostJpa1);
        postJpaList.add(newPostJpa2);

        Post newPost1 = new Post(contentUrl, postType, profileId);
        newPost1.setCaption(caption);

        Post newPost2 = new Post(contentUrl, postType, profileId);
        newPost2.setCaption(caption);


        List<Post> postList = new ArrayList<>();
        postList.add(newPost1);
        postList.add(newPost2);

        FullProfile convertedFullProfile = new FullProfile(
                convertedProfile,
                postList,
                postList.size(),
                true
        );

        when(this.profilesRepository.findById(profileId)).thenReturn(Optional.of(this.savedProfileJpa));
        when(this.postsRepository.findAllByProfileId(profileId)).thenReturn(Optional.of(postJpaList));
        when(this.postToPostJpaConverter.convertBack(postJpaList.get(0))).thenReturn(newPost1);
        when(this.postToPostJpaConverter.convertBack(postJpaList.get(1))).thenReturn(newPost2);
        when(this.profileToProfileJpaConverter.convertBack(this.savedProfileJpa)).thenReturn(convertedProfile);

        FullProfile fullProfile = this.profilesService.findFull(profileId);

        assertNotNull(fullProfile);
        assertEquals(convertedProfile, fullProfile.getProfile());
        assertEquals(postList, fullProfile.getPostList());
        assertEquals(postList.size(), fullProfile.getPostCount());
        assertTrue(fullProfile.getProfileGranted());
        verify(this.profilesRepository, times(1)).findById(profileId);
        verify(this.postsRepository, times(1)).findAllByProfileId(profileId);
        verify(this.postToPostJpaConverter, times(2)).convertBack(any(PostJpa.class));
        verify(this.profileToProfileJpaConverter, times(1)).convertBack(this.savedProfileJpa);

        log.info(fullProfile.toString());
    }

    @Test
    void testFindFull_NoPost_Success(){
        // Post che verra' restituito
        Profile convertedProfile = new Profile(profileName, isPrivate, accountId);
        convertedProfile.setBio(bio);
        convertedProfile.setPictureUrl(pictureUrl);
        convertedProfile.setId(profileId);


        List<Post> postList = new ArrayList<>();

        FullProfile convertedFullProfile = new FullProfile(
                convertedProfile,
                postList,
                0,
                true
        );

        when(this.profilesRepository.findById(profileId)).thenReturn(Optional.of(this.savedProfileJpa));
        when(this.postsRepository.findAllByProfileId(profileId)).thenReturn(Optional.of(new ArrayList<>()));
        when(this.profileToProfileJpaConverter.convertBack(this.savedProfileJpa)).thenReturn(convertedProfile);

        FullProfile fullProfile = this.profilesService.findFull(profileId);

        assertNotNull(fullProfile);
        assertEquals(convertedProfile, fullProfile.getProfile());
        assertEquals(postList, fullProfile.getPostList());
        assertEquals(postList.size(), fullProfile.getPostCount());
        assertTrue(fullProfile.getProfileGranted());
        verify(this.profilesRepository, times(1)).findById(profileId);
        verify(this.postsRepository, times(1)).findAllByProfileId(profileId);
        verify(this.postToPostJpaConverter, times(0)).convertBack(any(PostJpa.class));
        verify(this.profileToProfileJpaConverter, times(1)).convertBack(this.savedProfileJpa);

        log.info(fullProfile.toString());
    }

    @Test
    void testFindFull_NotFound_Failed(){
        when(this.profilesRepository.findById(profileId)).thenReturn(Optional.empty());
        assertThrows(ProfileNotFoundException.class,
                () -> this.profilesService.findFull(profileId)
        );

        verify(this.profilesRepository, times(1)).findById(profileId);
        verify(this.postsRepository, times(0)).findAllByProfileId(profileId);
        verify(this.postToPostJpaConverter, times(0)).convertBack(any(PostJpa.class));
        verify(this.profileToProfileJpaConverter, times(0)).convertBack(any(ProfileJpa.class));
    }

    @Test
    void testFindFull_PostAlreadyRemoved_Failed(){
        // Imposto una data passata
        this.savedProfileJpa.setDeletedAt(LocalDateTime.MIN);
        log.info("Deleted At"+this.savedProfileJpa.getDeletedAt());

        when(this.profilesRepository.findById(profileId)).thenReturn(Optional.of(this.savedProfileJpa));

        assertThrows(ProfileNotFoundException.class,
                () -> this.profilesService.findFull(profileId)
        );

        verify(this.profilesRepository, times(1)).findById(profileId);
        verify(this.postsRepository, times(0)).findAllByProfileId(profileId);
        verify(this.postToPostJpaConverter, times(0)).convertBack(any(PostJpa.class));
        verify(this.profileToProfileJpaConverter, times(0)).convertBack(any(ProfileJpa.class));
    }

}