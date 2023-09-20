package com.sergiostefanizzi.profilemicroservice.service;

import com.sergiostefanizzi.profilemicroservice.model.*;
import com.sergiostefanizzi.profilemicroservice.model.converter.PostToPostJpaConverter;
import com.sergiostefanizzi.profilemicroservice.model.converter.ProfileToProfileJpaConverter;
import com.sergiostefanizzi.profilemicroservice.repository.*;
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
    private LikesRepository likesRepository;
    @Mock
    private CommentsRepository commentsRepository;
    @Mock
    private FollowsRepository followsRepository;
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
    private UrlValidator validator;
    String contentUrl = "https://upload.wikimedia.org/wikipedia/commons/9/9a/Cape_may.jpg";
    String caption = "This is the post caption";
    Post.PostTypeEnum postType = Post.PostTypeEnum.POST;
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

        when(this.profilesRepository.checkActiveByProfileName(anyString())).thenReturn(Optional.empty());
        when(this.profileToProfileJpaConverter.convert(any(Profile.class))).thenReturn(newProfileJpa);
        when(this.profilesRepository.save(any(ProfileJpa.class))).thenReturn(newProfileJpa);
        when(this.profileToProfileJpaConverter.convertBack(any(ProfileJpa.class))).thenReturn(convertedProfile);

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

        verify(this.profilesRepository, times(1)).checkActiveByProfileName(anyString());
        verify(this.profileToProfileJpaConverter, times(1)).convert(any(Profile.class));
        verify(this.profilesRepository, times(1)).save(any(ProfileJpa.class));
        verify(this.profileToProfileJpaConverter, times(1)).convertBack(any(ProfileJpa.class));

        log.info("CREATED_AT after ---> "+newProfileJpa.getCreatedAt());
        log.info("PROFILE_ID after ---> "+savedProfile.getId());
        log.info(String.valueOf(savedProfile));
    }

    @Test
    void testSaveFailed_ProfileNameExists() {
        when(this.profilesRepository.checkActiveByProfileName(anyString())).thenReturn(Optional.of(this.newProfile.getProfileName()));

        log.info("Profile with name "+this.savedProfileJpa.getProfileName()+" exists");
        assertThrows(ProfileAlreadyCreatedException.class, () -> {
            this.profilesService.save(this.newProfile);
        });

        verify(this.profilesRepository, times(1)).checkActiveByProfileName(anyString());
        verify(this.profileToProfileJpaConverter, times(0)).convert(any(Profile.class));
        verify(this.profilesRepository, times(0)).save(any(ProfileJpa.class));
        verify(this.profileToProfileJpaConverter, times(0)).convertBack(any(ProfileJpa.class));
    }

    // REMOVE A PROFILE
    @Test
    void testRemoveSuccess(){
        doNothing().when(this.profilesRepository).removeProfileByProfileId(anyLong(), any(LocalDateTime.class));
        doNothing().when(this.postsRepository).removePostByProfileId(anyLong(), any(LocalDateTime.class));
        doNothing().when(this.likesRepository).removeLikeByProfileId(anyLong(), any(LocalDateTime.class));
        doNothing().when(this.commentsRepository).removeCommentByProfileId(anyLong(), any(LocalDateTime.class));
        doNothing().when(this.followsRepository).removeFollowByProfileId(anyLong(), any(LocalDateTime.class));

        this.profilesService.remove(profileId);

        // l'istante di rimozione deve essere NON nullo DOPO la rimozione

        verify(this.profilesRepository, times(1)).removeProfileByProfileId(anyLong(), any(LocalDateTime.class));
        verify(this.postsRepository, times(1)).removePostByProfileId(anyLong(), any(LocalDateTime.class));
        verify(this.likesRepository, times(1)).removeLikeByProfileId(anyLong(), any(LocalDateTime.class));
        verify(this.commentsRepository, times(1)).removeCommentByProfileId(anyLong(), any(LocalDateTime.class));
        verify(this.followsRepository, times(1)).removeFollowByProfileId(anyLong(), any(LocalDateTime.class));

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

        when(this.profilesRepository.getReferenceById(anyLong())).thenReturn(this.savedProfileJpa);
        when(this.profilesRepository.save(any(ProfileJpa.class))).thenReturn(this.savedProfileJpa);
        when(this.profileToProfileJpaConverter.convertBack(any(ProfileJpa.class))).thenReturn(convertedProfile);

        Profile updatedProfile = this.profilesService.update(profileId, profilePatch);

        assertNotNull(updatedProfile);
        assertEquals(profileId, updatedProfile.getId());
        assertEquals(profileName, updatedProfile.getProfileName());
        assertEquals(updatedBio, updatedProfile.getBio());
        assertEquals(updatedPictureUrl, updatedProfile.getPictureUrl());
        assertEquals(updatedIsPrivate, updatedProfile.getIsPrivate());


        verify(this.profilesRepository, times(1)).getReferenceById(anyLong());
        verify(this.profilesRepository, times(1)).save(any(ProfileJpa.class));
        verify(this.profileToProfileJpaConverter, times(1)).convertBack(any(ProfileJpa.class));
    }



    @Test
    void testFindByNameSuccess(){
        ProfileJpa savedProfileJpa2 = new ProfileJpa(profileName + "_2", isPrivate, accountId);
        savedProfileJpa2.setBio(bio);
        savedProfileJpa2.setPictureUrl(pictureUrl);
        savedProfileJpa2.setId(2L);

        ProfileJpa savedProfileJpa3 = new ProfileJpa(profileName + "_3", isPrivate, accountId);
        savedProfileJpa3.setBio(bio);
        savedProfileJpa3.setPictureUrl(pictureUrl);
        savedProfileJpa3.setId(3L);


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

        List<Profile> profileList = asList(convertedProfile1, convertedProfile2, convertedProfile3);

        when(this.profilesRepository.findAllActiveByProfileName(anyString())).thenReturn(asList(this.savedProfileJpa, savedProfileJpa2, savedProfileJpa3));
        when(this.profileToProfileJpaConverter.convertBack(this.savedProfileJpa)).thenReturn(convertedProfile1);
        when(this.profileToProfileJpaConverter.convertBack(savedProfileJpa2)).thenReturn(convertedProfile2);
        when(this.profileToProfileJpaConverter.convertBack(savedProfileJpa3)).thenReturn(convertedProfile3);


        List<Profile> returnedProfileList = this.profilesService.findByProfileName(profileName);

        log.info("Profile List --> "+returnedProfileList);
        assertNotNull(returnedProfileList);
        assertEquals(3, returnedProfileList.size());
        assertEquals(profileList, returnedProfileList);


        verify(this.profilesRepository, times(1)).findAllActiveByProfileName(anyString());
        verify(this.profileToProfileJpaConverter, times(3)).convertBack(any(ProfileJpa.class));
    }


    @Test
    void testFindFullSuccess(){
        // Profile che verra' restituito
        Profile convertedProfile = new Profile(profileName, isPrivate, accountId);
        convertedProfile.setBio(bio);
        convertedProfile.setPictureUrl(pictureUrl);
        convertedProfile.setId(profileId);

        PostJpa newPostJpa1 = new PostJpa(contentUrl, postType);
        newPostJpa1.setCaption("First: "+caption);
        newPostJpa1.setProfile(this.savedProfileJpa);
        newPostJpa1.setId(1L);

        PostJpa newPostJpa2 = new PostJpa(contentUrl, postType);
        newPostJpa2.setCaption("Second: "+caption);
        newPostJpa2.setProfile(this.savedProfileJpa);
        newPostJpa2.setId(2L);

        List<PostJpa> postJpaList = new ArrayList<>();
        postJpaList.add(newPostJpa1);
        postJpaList.add(newPostJpa2);

        Post newPost1 = new Post(contentUrl, postType, profileId);
        newPost1.setCaption("First: "+caption);
        newPost1.setId(1L);

        Post newPost2 = new Post(contentUrl, postType, profileId);
        newPost2.setCaption("Second: "+caption);
        newPost2.setId(2L);


        List<Post> postList = new ArrayList<>();
        postList.add(newPost1);
        postList.add(newPost2);

        FullProfile convertedFullProfile = new FullProfile(
                convertedProfile,
                postList,
                postList.size(),
                true
        );

        when(this.profilesRepository.getReferenceById(anyLong())).thenReturn(this.savedProfileJpa);
        when(this.postsRepository.findAllActiveByProfileId(anyLong(), any(LocalDateTime.class))).thenReturn(Optional.of(postJpaList));
        when(this.postToPostJpaConverter.convertBack(postJpaList.get(0))).thenReturn(newPost1);
        when(this.postToPostJpaConverter.convertBack(postJpaList.get(1))).thenReturn(newPost2);
        when(this.profileToProfileJpaConverter.convertBack(any(ProfileJpa.class))).thenReturn(convertedProfile);

        FullProfile fullProfile = this.profilesService.findFull(profileId);

        assertNotNull(fullProfile);
        assertEquals(convertedFullProfile, fullProfile);
        verify(this.profilesRepository, times(1)).getReferenceById(anyLong());
        verify(this.postsRepository, times(1)).findAllActiveByProfileId(anyLong(), any(LocalDateTime.class));
        verify(this.postToPostJpaConverter, times(2)).convertBack(any(PostJpa.class));
        verify(this.profileToProfileJpaConverter, times(1)).convertBack(any(ProfileJpa.class));

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

        when(this.profilesRepository.getReferenceById(anyLong())).thenReturn(this.savedProfileJpa);
        when(this.postsRepository.findAllActiveByProfileId(anyLong(), any(LocalDateTime.class))).thenReturn(Optional.of(new ArrayList<>()));
        when(this.profileToProfileJpaConverter.convertBack(any(ProfileJpa.class))).thenReturn(convertedProfile);

        FullProfile fullProfile = this.profilesService.findFull(profileId);

        assertNotNull(fullProfile);
        assertEquals(convertedFullProfile, fullProfile);
        assertTrue(fullProfile.getProfileGranted());
        verify(this.profilesRepository, times(1)).getReferenceById(anyLong());
        verify(this.postsRepository, times(1)).findAllActiveByProfileId(anyLong(), any(LocalDateTime.class));
        verify(this.postToPostJpaConverter, times(0)).convertBack(any(PostJpa.class));
        verify(this.profileToProfileJpaConverter, times(1)).convertBack(any(ProfileJpa.class));

        log.info(fullProfile.toString());
    }



}