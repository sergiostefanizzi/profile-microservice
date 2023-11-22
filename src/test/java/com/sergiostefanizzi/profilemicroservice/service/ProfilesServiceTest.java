package com.sergiostefanizzi.profilemicroservice.service;

import com.sergiostefanizzi.profilemicroservice.model.*;
import com.sergiostefanizzi.profilemicroservice.model.converter.PostToPostJpaConverter;
import com.sergiostefanizzi.profilemicroservice.model.converter.ProfileToProfileJpaConverter;
import com.sergiostefanizzi.profilemicroservice.repository.*;
import com.sergiostefanizzi.profilemicroservice.system.exception.NotInProfileListException;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@Slf4j
class ProfilesServiceTest {
    @Mock
    private ProfilesRepository profilesRepository;
    @Mock
    private PostsRepository postsRepository;
    /*
    @Mock
    private LikesRepository likesRepository;
    @Mock
    private CommentsRepository commentsRepository;

     */
    @Mock
    private FollowsRepository followsRepository;
    @Mock
    private ProfileToProfileJpaConverter profileToProfileJpaConverter;
    @Mock
    private PostToPostJpaConverter postToPostJpaConverter;
    @Mock
    private SecurityContext securityContext;
    @Mock
    private KeycloakService keycloakService;
    @InjectMocks
    private ProfilesService profilesService;
    private JwtAuthenticationToken jwtAuthenticationToken;

    private String profileName = "giuseppe_verdi";
    private Boolean isPrivate = false;
    private Boolean updatedIsPrivate = true;
    private String accountId = UUID.randomUUID().toString();
    private String bio = "This is Giuseppe's profile!";
    private String updatedBio = "New Giuseppe's bio";
    private String pictureUrl = "https://upload.wikimedia.org/wikipedia/commons/7/7e/Circle-icons-profile.svg";
    private String updatedPictureUrl = "https://icons-for-free.com/iconfiles/png/512/avatar+person+profile+user+icon-1320086059654790795.png";
    private Long profileId = 12L;
    private Profile newProfile;
    private ProfileJpa savedProfileJpa;
    private UrlValidator validator;
    private String contentUrl = "https://upload.wikimedia.org/wikipedia/commons/9/9a/Cape_may.jpg";
    private String caption = "This is the post caption";
    Post.PostTypeEnum postType = Post.PostTypeEnum.POST;
    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);

        this.newProfile = new Profile(profileName,isPrivate);
        this.newProfile.setBio(bio);
        this.newProfile.setPictureUrl(pictureUrl);


        this.savedProfileJpa = new ProfileJpa(profileName, isPrivate, accountId);
        this.savedProfileJpa.setBio(bio);
        this.savedProfileJpa.setPictureUrl(pictureUrl);
        this.savedProfileJpa.setId(profileId);


        this.validator = new UrlValidator();

        Map<String, Object> headers = new HashMap<>();
        headers.put("alg","HS256");
        headers.put("typ","JWT");
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", this.accountId);
        Jwt jwt = new Jwt("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c",
                Instant.now(),
                Instant.MAX,
                headers,
                claims);

        this.jwtAuthenticationToken = new JwtAuthenticationToken(jwt);
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
        newProfileJpa.setId(profileId);

        // Profilo che verra' restituito service
        Profile convertedProfile = new Profile(profileName, isPrivate);
        convertedProfile.setAccountId(accountId);
        convertedProfile.setBio(bio);
        convertedProfile.setPictureUrl(pictureUrl);
        convertedProfile.setId(profileId);

        when(this.profilesRepository.checkActiveByProfileName(anyString())).thenReturn(Optional.empty());
        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationToken);
        when(this.profileToProfileJpaConverter.convert(any(Profile.class))).thenReturn(newProfileJpa);
        when(this.profilesRepository.save(any(ProfileJpa.class))).thenReturn(newProfileJpa);
        when(this.keycloakService.updateProfileList(anyString(), anyLong())).thenReturn(true);
        when(this.profileToProfileJpaConverter.convertBack(any(ProfileJpa.class))).thenReturn(convertedProfile);

        log.info("CREATED_AT before ---> "+newProfileJpa.getCreatedAt());

        Profile savedProfile = this.profilesService.save(this.newProfile);

        assertNotNull(savedProfile);
        this.newProfile.setAccountId(this.accountId);
        this.newProfile.setId(this.profileId);

        assertEquals(this.newProfile, savedProfile);

        assertTrue(validator.isValid(savedProfile.getPictureUrl()));

        verify(this.profilesRepository, times(1)).checkActiveByProfileName(anyString());
        verify(this.securityContext, times(1)).getAuthentication();
        verify(this.profileToProfileJpaConverter, times(1)).convert(any(Profile.class));
        verify(this.profilesRepository, times(1)).save(any(ProfileJpa.class));
        verify(this.keycloakService, times(1)).updateProfileList(anyString(), anyLong());
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
        verify(this.securityContext, times(0)).getAuthentication();
        verify(this.profileToProfileJpaConverter, times(0)).convert(any(Profile.class));
        verify(this.profilesRepository, times(0)).save(any(ProfileJpa.class));
        verify(this.keycloakService, times(0)).updateProfileList(anyString(), anyLong());
        verify(this.profileToProfileJpaConverter, times(0)).convertBack(any(ProfileJpa.class));
    }

    // REMOVE A PROFILE
    @Test
    void testRemoveSuccess(){
        when(this.profilesRepository.getReferenceById(anyLong())).thenReturn(savedProfileJpa);
        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationToken);
        when(this.keycloakService.removeFromProfileList(anyString(), anyLong())).thenReturn(true);

        this.profilesService.remove(profileId);

        // l'istante di rimozione deve essere NON nullo DOPO la rimozione

        verify(this.profilesRepository, times(1)).getReferenceById(anyLong());
        verify(this.securityContext, times(1)).getAuthentication();
        verify(this.keycloakService, times(1)).removeFromProfileList(anyString(), anyLong());
        verify(this.profilesRepository, times(1)).save(any(ProfileJpa.class));
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
        Profile convertedProfile = new Profile(profileName, isPrivate);
        convertedProfile.setAccountId(accountId);
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
        Profile convertedProfile1 = new Profile(profileName, isPrivate);
        convertedProfile1.setAccountId(accountId);
        convertedProfile1.setBio(bio);
        convertedProfile1.setPictureUrl(pictureUrl);
        convertedProfile1.setId(profileId);

        Profile convertedProfile2 = new Profile(profileName+"_2", isPrivate);
        convertedProfile2.setAccountId(accountId);
        convertedProfile2.setBio(bio);
        convertedProfile2.setPictureUrl(pictureUrl);
        convertedProfile2.setId(2L);

        Profile convertedProfile3 = new Profile(profileName+"_3", isPrivate);
        convertedProfile3.setAccountId(accountId);
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
    void testFindFull_PrivateProfile_Success(){
        this.savedProfileJpa.setIsPrivate(true);
        // Profile che verra' restituito
        Profile convertedProfile = new Profile(profileName, true);
        convertedProfile.setAccountId(accountId);
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
        when(this.postsRepository.findAllActiveByProfileId(anyLong())).thenReturn(Optional.of(postJpaList));
        when(this.postToPostJpaConverter.convertBack(postJpaList.get(0))).thenReturn(newPost1);
        when(this.postToPostJpaConverter.convertBack(postJpaList.get(1))).thenReturn(newPost2);
        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationToken);
        when(this.keycloakService.isInProfileList(anyString(), anyLong())).thenReturn(true);
        when(this.followsRepository.findActiveAcceptedById(any(FollowsId.class))).thenReturn(Optional.of(new FollowsJpa(new FollowsId(123L, profileId))));
        when(this.profileToProfileJpaConverter.convertBack(any(ProfileJpa.class))).thenReturn(convertedProfile);

        FullProfile fullProfile = this.profilesService.findFull(profileId, 123L);

        assertNotNull(fullProfile);
        assertEquals(convertedFullProfile, fullProfile);
        verify(this.profilesRepository, times(1)).getReferenceById(anyLong());
        verify(this.postsRepository, times(1)).findAllActiveByProfileId(anyLong());
        verify(this.postToPostJpaConverter, times(2)).convertBack(any(PostJpa.class));
        verify(this.securityContext, times(1)).getAuthentication();
        verify(this.keycloakService, times(1)).isInProfileList(anyString(), anyLong());
        verify(this.followsRepository, times(1)).findActiveAcceptedById(any(FollowsId.class));
        verify(this.profileToProfileJpaConverter, times(1)).convertBack(any(ProfileJpa.class));

        log.info(fullProfile.toString());
    }



    @Test
    void testFindFull_PublicProfile_Success(){
        // Profile che verra' restituito
        Profile convertedProfile = new Profile(profileName, false);
        convertedProfile.setAccountId(accountId);
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
        when(this.postsRepository.findAllActiveByProfileId(anyLong())).thenReturn(Optional.of(postJpaList));
        when(this.postToPostJpaConverter.convertBack(postJpaList.get(0))).thenReturn(newPost1);
        when(this.postToPostJpaConverter.convertBack(postJpaList.get(1))).thenReturn(newPost2);
        when(this.profileToProfileJpaConverter.convertBack(any(ProfileJpa.class))).thenReturn(convertedProfile);

        FullProfile fullProfile = this.profilesService.findFull(profileId, 123L);

        assertNotNull(fullProfile);
        assertEquals(convertedFullProfile, fullProfile);
        verify(this.profilesRepository, times(1)).getReferenceById(anyLong());
        verify(this.postsRepository, times(1)).findAllActiveByProfileId(anyLong());
        verify(this.postToPostJpaConverter, times(2)).convertBack(any(PostJpa.class));
        verify(this.securityContext, times(0)).getAuthentication();
        verify(this.keycloakService, times(0)).isInProfileList(anyString(), anyLong());
        verify(this.followsRepository, times(0)).findActiveAcceptedById(any(FollowsId.class));
        verify(this.profileToProfileJpaConverter, times(1)).convertBack(any(ProfileJpa.class));

        log.info(fullProfile.toString());
    }

    @Test
    void testFindFull_PrivateProfile_NotInProfileList_Failed(){
        this.savedProfileJpa.setIsPrivate(true);
        // Profile che verra' restituito
        Profile convertedProfile = new Profile(profileName, true);
        convertedProfile.setAccountId(accountId);
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


        when(this.profilesRepository.getReferenceById(anyLong())).thenReturn(this.savedProfileJpa);
        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationToken);
        when(this.keycloakService.isInProfileList(anyString(), anyLong())).thenReturn(false);

        assertThrows(NotInProfileListException.class, () -> this.profilesService.findFull(profileId, 123L));


        verify(this.profilesRepository, times(1)).getReferenceById(anyLong());
        verify(this.postsRepository, times(0)).findAllActiveByProfileId(anyLong());
        verify(this.postToPostJpaConverter, times(0)).convertBack(any(PostJpa.class));
        verify(this.securityContext, times(1)).getAuthentication();
        verify(this.keycloakService, times(1)).isInProfileList(anyString(), anyLong());
        verify(this.followsRepository, times(0)).findActiveAcceptedById(any(FollowsId.class));
        verify(this.profileToProfileJpaConverter, times(0)).convertBack(any(ProfileJpa.class));

    }

    @Test
    void testFindFull_PrivateProfile_NotAFollower_Failed(){
        this.savedProfileJpa.setIsPrivate(true);
        // Profile che verra' restituito
        Profile convertedProfile = new Profile(profileName, true);
        convertedProfile.setAccountId(accountId);
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
                false
        );

        when(this.profilesRepository.getReferenceById(anyLong())).thenReturn(this.savedProfileJpa);
        when(this.postsRepository.findAllActiveByProfileId(anyLong())).thenReturn(Optional.of(postJpaList));
        when(this.postToPostJpaConverter.convertBack(postJpaList.get(0))).thenReturn(newPost1);
        when(this.postToPostJpaConverter.convertBack(postJpaList.get(1))).thenReturn(newPost2);
        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationToken);
        when(this.keycloakService.isInProfileList(anyString(), anyLong())).thenReturn(true);
        when(this.followsRepository.findActiveAcceptedById(any(FollowsId.class))).thenReturn(Optional.empty());
        when(this.profileToProfileJpaConverter.convertBack(any(ProfileJpa.class))).thenReturn(convertedProfile);

        FullProfile fullProfile = this.profilesService.findFull(profileId, 123L);

        assertNotNull(fullProfile);
        assertEquals(convertedFullProfile, fullProfile);
        verify(this.profilesRepository, times(1)).getReferenceById(anyLong());
        verify(this.postsRepository, times(1)).findAllActiveByProfileId(anyLong());
        verify(this.postToPostJpaConverter, times(2)).convertBack(any(PostJpa.class));
        verify(this.securityContext, times(1)).getAuthentication();
        verify(this.keycloakService, times(1)).isInProfileList(anyString(), anyLong());
        verify(this.followsRepository, times(1)).findActiveAcceptedById(any(FollowsId.class));
        verify(this.profileToProfileJpaConverter, times(1)).convertBack(any(ProfileJpa.class));

        log.info(fullProfile.toString());
    }



}