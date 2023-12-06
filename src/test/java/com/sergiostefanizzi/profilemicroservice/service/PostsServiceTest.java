package com.sergiostefanizzi.profilemicroservice.service;

import com.sergiostefanizzi.profilemicroservice.model.*;
import com.sergiostefanizzi.profilemicroservice.model.converter.CommentToCommentJpaConverter;
import com.sergiostefanizzi.profilemicroservice.model.converter.LikeToLikeJpaConverter;
import com.sergiostefanizzi.profilemicroservice.model.converter.PostToPostJpaConverter;
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
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@Slf4j
class PostsServiceTest {
    @Mock
    private SecurityContext securityContext;
    @Mock
    private KeycloakService keycloakService;

    @Mock
    private PostsRepository postsRepository;
    @Mock
    private PostToPostJpaConverter postToPostJpaConverter;

    @Mock
    private ProfilesRepository profilesRepository;
    @Mock
    private LikesRepository likesRepository;
    @Mock
    private LikeToLikeJpaConverter likeToLikeJpaConverter;
    @Mock
    private CommentsRepository commentsRepository;
    @Mock
    private CommentToCommentJpaConverter commentToCommentJpaConverter;
    @Mock
    private FollowsRepository followsRepository;
    @InjectMocks
    private PostsService postsService;
    private JwtAuthenticationToken jwtAuthenticationToken;
    private JwtAuthenticationToken jwtAuthenticationTokenWithProfileList;

    String contentUrl = "https://upload.wikimedia.org/wikipedia/commons/9/9a/Cape_may.jpg";
    String caption = "This is the post caption";
    Post.PostTypeEnum postType = Post.PostTypeEnum.POST;
    private Long postId = 1L;

    private Long profileId = 11L;
    private Post newPost;
    private PostJpa savedPostJpa;
    private PostJpa savedPostJpa2;


    private ProfileJpa profileJpa = new ProfileJpa("pinco_pallino",false, UUID.randomUUID().toString());
    private ProfileJpa profileJpa2 = new ProfileJpa("pinco_pallino2",false, UUID.randomUUID().toString());
    private final PostJpa newPostJpa = new PostJpa(contentUrl, postType);

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
        this.profileJpa.setId(profileId);
        this.profileJpa2.setId(12L);

        this.newPost = new Post(contentUrl, postType, profileId);
        this.newPost.setCaption(caption);
        this.newPostJpa.setCaption(caption);


        this.savedPostJpa = new PostJpa(contentUrl, postType);
        this.savedPostJpa.setCaption(caption);
        this.savedPostJpa.setProfile(profileJpa);
        this.savedPostJpa.setId(1L);

        this.savedPostJpa2 = new PostJpa(contentUrl, postType);
        this.savedPostJpa2.setCaption(caption);
        this.savedPostJpa2.setProfile(profileJpa2);
        this.savedPostJpa2.setId(2L);



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
        claims.put("profileList", List.of(this.profileId));
        this.jwtAuthenticationTokenWithProfileList = new JwtAuthenticationToken(
                new Jwt("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c",
                        Instant.now(),
                        Instant.MAX,
                        headers,
                        claims)
        );
    }

    @Test
    void testSave_WithProfileList_Success() {
        // Post che verra' restituito
        Post convertedPost = new Post(contentUrl, postType, profileId);
        convertedPost.setCaption(caption);
        convertedPost.setId(postId);


        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationTokenWithProfileList);
        when(this.postToPostJpaConverter.convert(any(Post.class))).thenReturn(newPostJpa);
        when(this.profilesRepository.getReferenceById(anyLong())).thenReturn(profileJpa);
        when(this.postsRepository.save(any(PostJpa.class))).thenReturn(newPostJpa);
        when(this.postToPostJpaConverter.convertBack(any(PostJpa.class))).thenReturn(convertedPost);

        Post savedPost = this.postsService.save(this.newPost);

        log.info("POST'S PROFILE-> "+newPostJpa.getProfile().getProfileName());

        assertEquals(convertedPost, savedPost);
        log.info(savedPost.toString());
        verify(this.securityContext, times(1)).getAuthentication();
        verify(this.keycloakService, times(0)).isInProfileList(anyString(), anyLong());
        verify(this.postToPostJpaConverter, times(1)).convert(any(Post.class));
        verify(this.profilesRepository, times(1)).getReferenceById(anyLong());
        verify(this.postsRepository, times(1)).save(any(PostJpa.class));
        verify(this.postToPostJpaConverter, times(1)).convertBack(any(PostJpa.class));

    }

    @Test
    void testSave_WithoutProfileList_Success() {
        // Post che verra' restituito
        Post convertedPost = new Post(contentUrl, postType, profileId);
        convertedPost.setCaption(caption);
        convertedPost.setId(postId);


        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationToken);
        when(this.keycloakService.isInProfileList(anyString(), anyLong())).thenReturn(true);
        when(this.postToPostJpaConverter.convert(any(Post.class))).thenReturn(newPostJpa);
        when(this.profilesRepository.getReferenceById(anyLong())).thenReturn(profileJpa);
        when(this.postsRepository.save(any(PostJpa.class))).thenReturn(newPostJpa);
        when(this.postToPostJpaConverter.convertBack(any(PostJpa.class))).thenReturn(convertedPost);

        Post savedPost = this.postsService.save(this.newPost);

        log.info("POST'S PROFILE-> "+newPostJpa.getProfile().getProfileName());

        assertEquals(convertedPost, savedPost);
        log.info(savedPost.toString());
        verify(this.securityContext, times(2)).getAuthentication();
        verify(this.keycloakService, times(1)).isInProfileList(anyString(), anyLong());
        verify(this.postToPostJpaConverter, times(1)).convert(any(Post.class));
        verify(this.profilesRepository, times(1)).getReferenceById(anyLong());
        verify(this.postsRepository, times(1)).save(any(PostJpa.class));
        verify(this.postToPostJpaConverter, times(1)).convertBack(any(PostJpa.class));
    }

    @Test
    void testSave_ProfileNotInProfileList_Failed(){
        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationToken);
        when(this.keycloakService.isInProfileList(anyString(), anyLong())).thenReturn(false);

        assertThrows(NotInProfileListException.class, () -> this.postsService.save(this.newPost));


        verify(this.securityContext, times(2)).getAuthentication();
        verify(this.keycloakService, times(1)).isInProfileList(anyString(), anyLong());
        verify(this.postToPostJpaConverter, times(0)).convert(any(Post.class));
        verify(this.profilesRepository, times(0)).getReferenceById(anyLong());
        verify(this.postsRepository, times(0)).save(any(PostJpa.class));
        verify(this.postToPostJpaConverter, times(0)).convertBack(any(PostJpa.class));
    }

    @Test
    void testRemove_Success(){
        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationTokenWithProfileList);
        when(this.postsRepository.getReferenceById(anyLong())).thenReturn(savedPostJpa);

        this.postsService.remove(postId, profileId);

        verify(this.securityContext, times(1)).getAuthentication();
        verify(this.keycloakService, times(0)).isInProfileList(anyString(), anyLong());
        verify(this.postsRepository, times(1)).getReferenceById(anyLong());
        verify(this.postsRepository, times(1)).save(any(PostJpa.class));

    }

    @Test
    void testRemove_ValidatedOnKeycloak_Success(){
        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationToken);
        when(this.keycloakService.isInProfileList(anyString(), anyLong())).thenReturn(true);
        when(this.postsRepository.getReferenceById(anyLong())).thenReturn(savedPostJpa);

        this.postsService.remove(postId, profileId);

        verify(this.securityContext, times(2)).getAuthentication();
        verify(this.keycloakService, times(1)).isInProfileList(anyString(), anyLong());
        verify(this.postsRepository, times(1)).getReferenceById(anyLong());
        verify(this.postsRepository, times(1)).save(any(PostJpa.class));
    }

    @Test
    void testRemove_NotInsideProfileList_Failed(){
        when(this.postsRepository.getReferenceById(anyLong())).thenReturn(savedPostJpa);
        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationToken);
        when(this.keycloakService.isInProfileList(anyString(), anyLong())).thenReturn(false);


        assertThrows(NotInProfileListException.class, () -> this.postsService.remove(postId, profileId));

        verify(this.securityContext, times(2)).getAuthentication();
        verify(this.keycloakService, times(1)).isInProfileList(anyString(), anyLong());
        verify(this.postsRepository, times(1)).getReferenceById(anyLong());
        verify(this.postsRepository, times(0)).save(any(PostJpa.class));
    }

    @Test
    void testRemove_IdsMismatch_Failed(){
        this.profileJpa.setId(Long.MAX_VALUE);
        this.savedPostJpa.setProfile(this.profileJpa);
        when(this.postsRepository.getReferenceById(anyLong())).thenReturn(savedPostJpa);


        assertThrows(IdsMismatchException.class, () -> this.postsService.remove(Long.MAX_VALUE, profileId));

        verify(this.securityContext, times(0)).getAuthentication();
        verify(this.keycloakService, times(0)).isInProfileList(anyString(), anyLong());
        verify(this.postsRepository, times(1)).getReferenceById(anyLong());
        verify(this.postsRepository, times(0)).save(any(PostJpa.class));
    }

    @Test
    void testUpdate_Success(){
        // Aggiornamento del post
        String newCaption = "Nuova caption del post";
        PostPatch postPatch = new PostPatch(newCaption);
        // Post che verra' restituito
        Post convertedPost = new Post(contentUrl, postType, profileId);
        convertedPost.setCaption(newCaption);
        convertedPost.setId(postId);

        when(this.postsRepository.getReferenceById(anyLong())).thenReturn(this.savedPostJpa);
        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationTokenWithProfileList);
        when(this.postsRepository.save(any(PostJpa.class))).thenReturn(this.savedPostJpa);
        when(this.postToPostJpaConverter.convertBack(any(PostJpa.class))).thenReturn(convertedPost);

        Post updatedPost = this.postsService.update(postId, profileId, postPatch);

        assertNotNull(updatedPost);
        assertEquals(postId, updatedPost.getId());
        assertEquals(contentUrl, updatedPost.getContentUrl());
        assertEquals(newCaption, updatedPost.getCaption());
        assertEquals(postType, updatedPost.getPostType());
        assertEquals(profileId, updatedPost.getProfileId());
        log.info(updatedPost.toString());
        verify(this.postsRepository, times(1)).getReferenceById(anyLong());
        verify(this.securityContext, times(1)).getAuthentication();
        verify(this.keycloakService, times(0)).isInProfileList(anyString(), anyLong());
        verify(this.postsRepository, times(1)).save(any(PostJpa.class));
        verify(this.postToPostJpaConverter, times(1)).convertBack(any(PostJpa.class));
    }

    @Test
    void testUpdate_ValidatedOnKeycloak_Success(){
        // Aggiornamento del post
        String newCaption = "Nuova caption del post";
        PostPatch postPatch = new PostPatch(newCaption);
        // Post che verra' restituito
        Post convertedPost = new Post(contentUrl, postType, profileId);
        convertedPost.setCaption(newCaption);
        convertedPost.setId(postId);

        when(this.postsRepository.getReferenceById(anyLong())).thenReturn(this.savedPostJpa);
        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationToken);
        when(this.keycloakService.isInProfileList(anyString(), anyLong())).thenReturn(true);
        when(this.postsRepository.save(any(PostJpa.class))).thenReturn(this.savedPostJpa);
        when(this.postToPostJpaConverter.convertBack(any(PostJpa.class))).thenReturn(convertedPost);

        Post updatedPost = this.postsService.update(postId, profileId, postPatch);

        assertNotNull(updatedPost);
        assertEquals(postId, updatedPost.getId());
        assertEquals(contentUrl, updatedPost.getContentUrl());
        assertEquals(newCaption, updatedPost.getCaption());
        assertEquals(postType, updatedPost.getPostType());
        assertEquals(profileId, updatedPost.getProfileId());
        log.info(updatedPost.toString());
        verify(this.postsRepository, times(1)).getReferenceById(anyLong());
        verify(this.securityContext, times(2)).getAuthentication();
        verify(this.keycloakService, times(1)).isInProfileList(anyString(), anyLong());
        verify(this.postsRepository, times(1)).save(any(PostJpa.class));
        verify(this.postToPostJpaConverter, times(1)).convertBack(any(PostJpa.class));
    }

    @Test
    void testUpdate_NotInsideProfileList_Failed(){
        // Aggiornamento del post
        String newCaption = "Nuova caption del post";
        PostPatch postPatch = new PostPatch(newCaption);

        when(this.postsRepository.getReferenceById(anyLong())).thenReturn(this.savedPostJpa);
        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationToken);
        when(this.keycloakService.isInProfileList(anyString(), anyLong())).thenReturn(false);

        assertThrows(NotInProfileListException.class, () -> this.postsService.update(postId, profileId, postPatch));
        verify(this.postsRepository, times(1)).getReferenceById(anyLong());
        verify(this.securityContext, times(2)).getAuthentication();
        verify(this.keycloakService, times(1)).isInProfileList(anyString(), anyLong());
        verify(this.postsRepository, times(0)).save(any(PostJpa.class));
        verify(this.postToPostJpaConverter, times(0)).convertBack(any(PostJpa.class));
    }

    @Test
    void testUpdate_IdsMismatch_Failed(){
        // Aggiornamento del post
        String newCaption = "Nuova caption del post";
        PostPatch postPatch = new PostPatch(newCaption);

        this.profileJpa.setId(Long.MAX_VALUE);
        this.savedPostJpa.setProfile(this.profileJpa);

        when(this.postsRepository.getReferenceById(anyLong())).thenReturn(this.savedPostJpa);

        assertThrows(IdsMismatchException.class, () -> this.postsService.update(Long.MAX_VALUE, profileId, postPatch));
        verify(this.postsRepository, times(1)).getReferenceById(anyLong());
        verify(this.securityContext, times(0)).getAuthentication();
        verify(this.keycloakService, times(0)).isInProfileList(anyString(), anyLong());
        verify(this.postsRepository, times(0)).save(any(PostJpa.class));
        verify(this.postToPostJpaConverter, times(0)).convertBack(any(PostJpa.class));
    }


    @Test
    void testFind_PublicProfile_Success(){
        this.profileJpa.setId(12L);
        this.savedPostJpa.setProfile(this.profileJpa);
        // Post che verra' restituito
        Post convertedPost = new Post(contentUrl, postType, this.profileJpa.getId());
        convertedPost.setCaption(caption);
        convertedPost.setId(postId);

        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationTokenWithProfileList);
        when(this.postsRepository.getReferenceById(anyLong())).thenReturn(this.savedPostJpa);
        when(this.postToPostJpaConverter.convertBack(any(PostJpa.class))).thenReturn(convertedPost);

        Post post = this.postsService.find(postId, profileId);

        assertEquals(convertedPost, post);
        log.info(post.toString());
        verify(this.securityContext, times(1)).getAuthentication();
        verify(this.keycloakService, times(0)).isInProfileList(anyString(), anyLong());
        verify(this.postsRepository, times(1)).getReferenceById(anyLong());
        verify(this.followsRepository, times(0)).findActiveAcceptedById(any(FollowsId.class));
        verify(this.postToPostJpaConverter, times(1)).convertBack(this.savedPostJpa);
    }

    @Test
    void testFind_PublicProfile_ValidateOnKeycloak_Success(){
        this.profileJpa.setId(12L);
        this.savedPostJpa.setProfile(this.profileJpa);
        // Post che verra' restituito
        Post convertedPost = new Post(contentUrl, postType, this.profileJpa.getId());
        convertedPost.setCaption(caption);
        convertedPost.setId(postId);

        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationToken);
        when(this.keycloakService.isInProfileList(anyString(), anyLong())).thenReturn(true);
        when(this.postsRepository.getReferenceById(anyLong())).thenReturn(this.savedPostJpa);
        when(this.postToPostJpaConverter.convertBack(any(PostJpa.class))).thenReturn(convertedPost);

        Post post = this.postsService.find(postId, profileId);

        assertEquals(convertedPost, post);
        log.info(post.toString());
        verify(this.securityContext, times(2)).getAuthentication();
        verify(this.keycloakService, times(1)).isInProfileList(anyString(), anyLong());
        verify(this.postsRepository, times(1)).getReferenceById(anyLong());
        verify(this.followsRepository, times(0)).findActiveAcceptedById(any(FollowsId.class));
        verify(this.postToPostJpaConverter, times(1)).convertBack(this.savedPostJpa);
    }

    @Test
    void testFind_PrivateProfile_Follower_Success(){
        this.profileJpa.setId(12L);
        this.profileJpa.setIsPrivate(true);
        this.savedPostJpa.setProfile(this.profileJpa);
        // Post che verra' restituito
        Post convertedPost = new Post(contentUrl, postType, this.profileJpa.getId());
        convertedPost.setCaption(caption);
        convertedPost.setId(postId);

        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationTokenWithProfileList);
        when(this.postsRepository.getReferenceById(anyLong())).thenReturn(this.savedPostJpa);
        when(this.followsRepository.findActiveAcceptedById(any(FollowsId.class))).thenReturn(Optional.of(new FollowsId(profileId, this.profileJpa.getId())));
        when(this.postToPostJpaConverter.convertBack(any(PostJpa.class))).thenReturn(convertedPost);

        Post post = this.postsService.find(postId, profileId);

        assertEquals(convertedPost, post);
        log.info(post.toString());
        verify(this.securityContext, times(1)).getAuthentication();
        verify(this.keycloakService, times(0)).isInProfileList(anyString(), anyLong());
        verify(this.postsRepository, times(1)).getReferenceById(anyLong());
        verify(this.followsRepository, times(1)).findActiveAcceptedById(any(FollowsId.class));
        verify(this.postToPostJpaConverter, times(1)).convertBack(this.savedPostJpa);
    }



    @Test
    void testFind_Failed(){
        this.profileJpa.setId(12L);
        this.savedPostJpa.setProfile(this.profileJpa);
        // Post che verra' restituito


        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationToken);
        when(this.keycloakService.isInProfileList(anyString(), anyLong())).thenReturn(false);

        assertThrows(NotInProfileListException.class, () -> this.postsService.find(postId, profileId));

        verify(this.securityContext, times(2)).getAuthentication();
        verify(this.keycloakService, times(1)).isInProfileList(anyString(), anyLong());
        verify(this.postsRepository, times(0)).getReferenceById(anyLong());
        verify(this.followsRepository, times(0)).findActiveAcceptedById(any(FollowsId.class));
        verify(this.postToPostJpaConverter, times(0)).convertBack(this.savedPostJpa);
    }

    @Test
    void testFind_MatchingIds_Success(){
        // Post che verra' restituito
        Post convertedPost = new Post(contentUrl, postType, this.profileJpa.getId());
        convertedPost.setCaption(caption);
        convertedPost.setId(postId);

        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationTokenWithProfileList);
        when(this.postsRepository.getReferenceById(anyLong())).thenReturn(this.savedPostJpa);
        when(this.postToPostJpaConverter.convertBack(any(PostJpa.class))).thenReturn(convertedPost);

        Post post = this.postsService.find(postId, profileId);

        assertEquals(convertedPost, post);
        log.info(post.toString());
        verify(this.securityContext, times(1)).getAuthentication();
        verify(this.keycloakService, times(0)).isInProfileList(anyString(), anyLong());
        verify(this.postsRepository, times(1)).getReferenceById(anyLong());
        verify(this.followsRepository, times(0)).findActiveAcceptedById(any(FollowsId.class));
        verify(this.postToPostJpaConverter, times(1)).convertBack(this.savedPostJpa);
    }

    @Test
    void testFind_PrivateProfile_MatchingIds_Success(){
        this.profileJpa.setIsPrivate(true);
        this.savedPostJpa.setProfile(this.profileJpa);
        // Post che verra' restituito
        Post convertedPost = new Post(contentUrl, postType, this.profileJpa.getId());
        convertedPost.setCaption(caption);
        convertedPost.setId(postId);

        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationTokenWithProfileList);
        when(this.postsRepository.getReferenceById(anyLong())).thenReturn(this.savedPostJpa);
        when(this.postToPostJpaConverter.convertBack(any(PostJpa.class))).thenReturn(convertedPost);

        Post post = this.postsService.find(postId, profileId);

        assertEquals(convertedPost, post);
        log.info(post.toString());
        verify(this.securityContext, times(1)).getAuthentication();
        verify(this.keycloakService, times(0)).isInProfileList(anyString(), anyLong());
        verify(this.postsRepository, times(1)).getReferenceById(anyLong());
        verify(this.followsRepository, times(0)).findActiveAcceptedById(any(FollowsId.class));
        verify(this.postToPostJpaConverter, times(1)).convertBack(this.savedPostJpa);
    }

    @Test
    void testFind_PrivateProfile_NotFollower_Failed(){
        this.profileJpa.setId(12L);
        this.profileJpa.setIsPrivate(true);
        this.savedPostJpa.setProfile(this.profileJpa);

        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationTokenWithProfileList);
        when(this.postsRepository.getReferenceById(anyLong())).thenReturn(this.savedPostJpa);
        when(this.followsRepository.findActiveAcceptedById(any(FollowsId.class))).thenReturn(Optional.empty());

        assertThrows(PostAccessForbiddenException.class, () -> this.postsService.find(postId, profileId));

        verify(this.securityContext, times(1)).getAuthentication();
        verify(this.keycloakService, times(0)).isInProfileList(anyString(), anyLong());
        verify(this.postsRepository, times(1)).getReferenceById(anyLong());
        verify(this.followsRepository, times(1)).findActiveAcceptedById(any(FollowsId.class));
        verify(this.postToPostJpaConverter, times(0)).convertBack(any(PostJpa.class));
    }


    @Test
    void testAddLike_OwnPost_Success(){
        LikeJpa likeJpa = new LikeJpa(new LikeId(this.profileJpa.getId(), savedPostJpa.getId()));

        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationTokenWithProfileList);
        when(this.postsRepository.findActiveById(anyLong())).thenReturn(Optional.of(this.savedPostJpa));
        when(this.profilesRepository.findActiveByPostId(any(Long.class))).thenReturn(Optional.of(this.profileJpa));
        when(this.profilesRepository.findActiveById(any(Long.class))).thenReturn(Optional.of(this.profileJpa));
        when(this.likesRepository.findActiveById(any(LikeId.class))).thenReturn(Optional.empty());
        when(this.likeToLikeJpaConverter.convert(any(Like.class))).thenReturn(likeJpa);
        when(this.likesRepository.save(any(LikeJpa.class))).thenReturn(likeJpa);

        this.postsService.addLike(false, new Like(this.profileJpa.getId(), savedPostJpa.getId()));

        log.info("Like Created at -> "+likeJpa.getCreatedAt());
        assertNotNull(likeJpa.getCreatedAt());
        assertNull(likeJpa.getDeletedAt());
        verify(this.securityContext, times(1)).getAuthentication();
        verify(this.keycloakService, times(0)).isInProfileList(anyString(), anyLong());
        verify(this.postsRepository, times(1)).findActiveById(anyLong());
        verify(this.profilesRepository, times(1)).findActiveById(any(Long.class));
        verify(this.profilesRepository, times(1)).findActiveByPostId(any(Long.class));
        verify(this.followsRepository, times(0)).findActiveAcceptedById(any(FollowsId.class));
        verify(this.likesRepository, times(1)).findActiveById(any(LikeId.class));
        verify(this.likeToLikeJpaConverter, times(1)).convert(any(Like.class));
        verify(this.likesRepository, times(1)).save(any(LikeJpa.class));
    }

    @Test
    void testAddLike_RemoveLike_OwnPost_Success(){
        LikeJpa likeJpa = new LikeJpa(new LikeId(this.profileJpa.getId(), savedPostJpa.getId()));

        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationTokenWithProfileList);
        when(this.postsRepository.findActiveById(anyLong())).thenReturn(Optional.of(this.savedPostJpa));
        when(this.profilesRepository.findActiveByPostId(any(Long.class))).thenReturn(Optional.of(this.profileJpa));
        when(this.profilesRepository.findActiveById(any(Long.class))).thenReturn(Optional.of(this.profileJpa));
        when(this.likesRepository.findActiveById(any(LikeId.class))).thenReturn(Optional.of(likeJpa));
        when(this.likesRepository.save(any(LikeJpa.class))).thenReturn(likeJpa);

        this.postsService.addLike(true, new Like(this.profileJpa.getId(), savedPostJpa.getId()));

        log.info("Like Created at -> "+likeJpa.getCreatedAt());
        assertNotNull(likeJpa.getDeletedAt());
        verify(this.securityContext, times(1)).getAuthentication();
        verify(this.keycloakService, times(0)).isInProfileList(anyString(), anyLong());
        verify(this.postsRepository, times(1)).findActiveById(anyLong());
        verify(this.profilesRepository, times(1)).findActiveById(any(Long.class));
        verify(this.profilesRepository, times(1)).findActiveByPostId(any(Long.class));
        verify(this.followsRepository, times(0)).findActiveAcceptedById(any(FollowsId.class));
        verify(this.likesRepository, times(1)).findActiveById(any(LikeId.class));
        verify(this.likeToLikeJpaConverter, times(0)).convert(any(Like.class));
        verify(this.likesRepository, times(1)).save(any(LikeJpa.class));
    }

        @Test
        void testAddLike_PublicPost_Success(){
            LikeJpa likeJpa = new LikeJpa(new LikeId(this.profileJpa.getId(), savedPostJpa2.getId()));

            when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationTokenWithProfileList);
            when(this.postsRepository.findActiveById(anyLong())).thenReturn(Optional.of(this.savedPostJpa2));
            when(this.profilesRepository.findActiveByPostId(any(Long.class))).thenReturn(Optional.of(this.profileJpa2));
            when(this.profilesRepository.findActiveById(any(Long.class))).thenReturn(Optional.of(this.profileJpa));
            when(this.likesRepository.findActiveById(any(LikeId.class))).thenReturn(Optional.empty());
            when(this.likeToLikeJpaConverter.convert(any(Like.class))).thenReturn(likeJpa);
            when(this.likesRepository.save(any(LikeJpa.class))).thenReturn(likeJpa);

            this.postsService.addLike(false, new Like(this.profileJpa.getId(), savedPostJpa2.getId()));

            log.info("Like Created at -> "+likeJpa.getCreatedAt());
            assertNotNull(likeJpa.getCreatedAt());
            assertNull(likeJpa.getDeletedAt());
            verify(this.securityContext, times(1)).getAuthentication();
            verify(this.keycloakService, times(0)).isInProfileList(anyString(), anyLong());
            verify(this.postsRepository, times(1)).findActiveById(anyLong());
            verify(this.profilesRepository, times(1)).findActiveById(any(Long.class));
            verify(this.profilesRepository, times(1)).findActiveByPostId(any(Long.class));
            verify(this.followsRepository, times(0)).findActiveAcceptedById(any(FollowsId.class));
            verify(this.likesRepository, times(1)).findActiveById(any(LikeId.class));
            verify(this.likeToLikeJpaConverter, times(1)).convert(any(Like.class));
            verify(this.likesRepository, times(1)).save(any(LikeJpa.class));
        }

    @Test
    void testAddLike_PrivatePost_Success(){
        this.profileJpa2.setIsPrivate(true);
        LikeJpa likeJpa = new LikeJpa(new LikeId(this.profileJpa.getId(), savedPostJpa2.getId()));

        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationTokenWithProfileList);
        when(this.postsRepository.findActiveById(anyLong())).thenReturn(Optional.of(this.savedPostJpa2));
        when(this.profilesRepository.findActiveByPostId(any(Long.class))).thenReturn(Optional.of(this.profileJpa2));
        when(this.profilesRepository.findActiveById(any(Long.class))).thenReturn(Optional.of(this.profileJpa));
        when(this.followsRepository.findActiveAcceptedById(any(FollowsId.class))).thenReturn(Optional.of(new FollowsId(this.profileJpa.getId(), this.profileJpa2.getId())));
        when(this.likesRepository.findActiveById(any(LikeId.class))).thenReturn(Optional.empty());
        when(this.likeToLikeJpaConverter.convert(any(Like.class))).thenReturn(likeJpa);
        when(this.likesRepository.save(any(LikeJpa.class))).thenReturn(likeJpa);

        this.postsService.addLike(false, new Like(this.profileJpa.getId(), savedPostJpa2.getId()));

        log.info("Like Created at -> "+likeJpa.getCreatedAt());
        assertNotNull(likeJpa.getCreatedAt());
        assertNull(likeJpa.getDeletedAt());
        verify(this.securityContext, times(1)).getAuthentication();
        verify(this.keycloakService, times(0)).isInProfileList(anyString(), anyLong());
        verify(this.postsRepository, times(1)).findActiveById(anyLong());
        verify(this.profilesRepository, times(1)).findActiveById(any(Long.class));
        verify(this.profilesRepository, times(1)).findActiveByPostId(any(Long.class));
        verify(this.followsRepository, times(1)).findActiveAcceptedById(any(FollowsId.class));
        verify(this.likesRepository, times(1)).findActiveById(any(LikeId.class));
        verify(this.likeToLikeJpaConverter, times(1)).convert(any(Like.class));
        verify(this.likesRepository, times(1)).save(any(LikeJpa.class));
    }

    @Test
    void testAddLike_PrivatePost_NotFollowing_Failed(){
        this.profileJpa2.setIsPrivate(true);
        Like like = new Like(this.profileJpa.getId(), savedPostJpa2.getId());

        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationTokenWithProfileList);
        when(this.postsRepository.findActiveById(anyLong())).thenReturn(Optional.of(this.savedPostJpa2));
        when(this.profilesRepository.findActiveByPostId(any(Long.class))).thenReturn(Optional.of(this.profileJpa2));
        when(this.profilesRepository.findActiveById(any(Long.class))).thenReturn(Optional.of(this.profileJpa));
        when(this.followsRepository.findActiveAcceptedById(any(FollowsId.class))).thenReturn(Optional.empty());

        assertThrows(PostAccessForbiddenException.class, () -> this.postsService.addLike(false, like));

        verify(this.securityContext, times(1)).getAuthentication();
        verify(this.keycloakService, times(0)).isInProfileList(anyString(), anyLong());
        verify(this.postsRepository, times(1)).findActiveById(anyLong());
        verify(this.profilesRepository, times(1)).findActiveById(any(Long.class));
        verify(this.profilesRepository, times(1)).findActiveByPostId(any(Long.class));
        verify(this.followsRepository, times(1)).findActiveAcceptedById(any(FollowsId.class));
        verify(this.likesRepository, times(0)).findActiveById(any(LikeId.class));
        verify(this.likeToLikeJpaConverter, times(0)).convert(any(Like.class));
        verify(this.likesRepository, times(0)).save(any(LikeJpa.class));
    }

    @Test
    void testAddLike_PublicPost_ValidateOnKeycloak_Success(){
        LikeJpa likeJpa = new LikeJpa(new LikeId(this.profileJpa.getId(), savedPostJpa2.getId()));

        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationToken);
        when(this.keycloakService.isInProfileList(anyString(), anyLong())).thenReturn(true);
        when(this.postsRepository.findActiveById(anyLong())).thenReturn(Optional.of(this.savedPostJpa2));
        when(this.profilesRepository.findActiveByPostId(any(Long.class))).thenReturn(Optional.of(this.profileJpa2));
        when(this.profilesRepository.findActiveById(any(Long.class))).thenReturn(Optional.of(this.profileJpa));
        when(this.likesRepository.findActiveById(any(LikeId.class))).thenReturn(Optional.empty());
        when(this.likeToLikeJpaConverter.convert(any(Like.class))).thenReturn(likeJpa);
        when(this.likesRepository.save(any(LikeJpa.class))).thenReturn(likeJpa);

        this.postsService.addLike(false, new Like(this.profileJpa.getId(), savedPostJpa2.getId()));

        log.info("Like Created at -> "+likeJpa.getCreatedAt());
        assertNotNull(likeJpa.getCreatedAt());
        assertNull(likeJpa.getDeletedAt());
        verify(this.securityContext, times(2)).getAuthentication();
        verify(this.keycloakService, times(1)).isInProfileList(anyString(), anyLong());
        verify(this.postsRepository, times(1)).findActiveById(anyLong());
        verify(this.profilesRepository, times(1)).findActiveById(any(Long.class));
        verify(this.profilesRepository, times(1)).findActiveByPostId(any(Long.class));
        verify(this.followsRepository, times(0)).findActiveAcceptedById(any(FollowsId.class));
        verify(this.likesRepository, times(1)).findActiveById(any(LikeId.class));
        verify(this.likeToLikeJpaConverter, times(1)).convert(any(Like.class));
        verify(this.likesRepository, times(1)).save(any(LikeJpa.class));
    }

    @Test
    void testAddLike_PublicPost_NotInsideProfileList_Failed(){
        Like like = new Like(this.profileJpa.getId(), savedPostJpa2.getId());
        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationToken);
        when(this.keycloakService.isInProfileList(anyString(), anyLong())).thenReturn(false);

        assertThrows(NotInProfileListException.class, () -> this.postsService.addLike(false, like));

        verify(this.securityContext, times(2)).getAuthentication();
        verify(this.keycloakService, times(1)).isInProfileList(anyString(), anyLong());
        verify(this.postsRepository, times(0)).findActiveById(anyLong());
        verify(this.profilesRepository, times(0)).findActiveById(any(Long.class));
        verify(this.profilesRepository, times(0)).findActiveByPostId(any(Long.class));
        verify(this.followsRepository, times(0)).findActiveAcceptedById(any(FollowsId.class));
        verify(this.likesRepository, times(0)).findActiveById(any(LikeId.class));
        verify(this.likeToLikeJpaConverter, times(0)).convert(any(Like.class));
        verify(this.likesRepository, times(0)).save(any(LikeJpa.class));
    }

    @Test
    void testAddLike_PublicPost_postNotFound_Failed(){
        Like like = new Like(this.profileJpa.getId(), savedPostJpa2.getId());

        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationTokenWithProfileList);
        when(this.postsRepository.findActiveById(anyLong())).thenReturn(Optional.empty());

        assertThrows(PostNotFoundException.class, () -> this.postsService.addLike(false, like));

        verify(this.securityContext, times(1)).getAuthentication();
        verify(this.keycloakService, times(0)).isInProfileList(anyString(), anyLong());
        verify(this.postsRepository, times(1)).findActiveById(anyLong());
        verify(this.profilesRepository, times(0)).findActiveById(any(Long.class));
        verify(this.profilesRepository, times(0)).findActiveByPostId(any(Long.class));
        verify(this.followsRepository, times(0)).findActiveAcceptedById(any(FollowsId.class));
        verify(this.likesRepository, times(0)).findActiveById(any(LikeId.class));
        verify(this.likeToLikeJpaConverter, times(0)).convert(any(Like.class));
        verify(this.likesRepository, times(0)).save(any(LikeJpa.class));
    }

    @Test
    void testAddLike_PublicPost_PostNotFound_DeletedProfileOwner_Failed(){
        Like like = new Like(this.profileJpa.getId(), savedPostJpa2.getId());

        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationTokenWithProfileList);
        when(this.postsRepository.findActiveById(anyLong())).thenReturn(Optional.of(this.savedPostJpa2));
        when(this.profilesRepository.findActiveByPostId(any(Long.class))).thenReturn(Optional.empty());

        assertThrows(PostNotFoundException.class, () -> this.postsService.addLike(false, like));

        verify(this.securityContext, times(1)).getAuthentication();
        verify(this.keycloakService, times(0)).isInProfileList(anyString(), anyLong());
        verify(this.postsRepository, times(1)).findActiveById(anyLong());
        verify(this.profilesRepository, times(0)).findActiveById(any(Long.class));
        verify(this.profilesRepository, times(1)).findActiveByPostId(any(Long.class));
        verify(this.followsRepository, times(0)).findActiveAcceptedById(any(FollowsId.class));
        verify(this.likesRepository, times(0)).findActiveById(any(LikeId.class));
        verify(this.likeToLikeJpaConverter, times(0)).convert(any(Like.class));
        verify(this.likesRepository, times(0)).save(any(LikeJpa.class));
    }

    @Test
    void testAddLike_PublicPost_ProfileNotFound_Failed(){
        Like like = new Like(this.profileJpa.getId(), savedPostJpa2.getId());

        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationTokenWithProfileList);
        when(this.postsRepository.findActiveById(anyLong())).thenReturn(Optional.of(this.savedPostJpa2));
        when(this.profilesRepository.findActiveByPostId(any(Long.class))).thenReturn(Optional.of(this.profileJpa));
        when(this.profilesRepository.findActiveById(any(Long.class))).thenReturn(Optional.empty());

        assertThrows(ProfileNotFoundException.class, () -> this.postsService.addLike(false, like));

        verify(this.securityContext, times(1)).getAuthentication();
        verify(this.keycloakService, times(0)).isInProfileList(anyString(), anyLong());
        verify(this.postsRepository, times(1)).findActiveById(anyLong());
        verify(this.profilesRepository, times(1)).findActiveById(any(Long.class));
        verify(this.profilesRepository, times(1)).findActiveByPostId(any(Long.class));
        verify(this.followsRepository, times(0)).findActiveAcceptedById(any(FollowsId.class));
        verify(this.likesRepository, times(0)).findActiveById(any(LikeId.class));
        verify(this.likeToLikeJpaConverter, times(0)).convert(any(Like.class));
        verify(this.likesRepository, times(0)).save(any(LikeJpa.class));
    }


    @Test
    void testAddLike_PublicPost_LikeAlreadyPresent_Success(){
        LikeJpa likeJpa = new LikeJpa(new LikeId(this.profileJpa.getId(), savedPostJpa2.getId()));

        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationTokenWithProfileList);
        when(this.postsRepository.findActiveById(anyLong())).thenReturn(Optional.of(this.savedPostJpa2));
        when(this.profilesRepository.findActiveByPostId(any(Long.class))).thenReturn(Optional.of(this.profileJpa2));
        when(this.profilesRepository.findActiveById(any(Long.class))).thenReturn(Optional.of(this.profileJpa));
        when(this.likesRepository.findActiveById(any(LikeId.class))).thenReturn(Optional.of(likeJpa));

        this.postsService.addLike(false, new Like(this.profileJpa.getId(), savedPostJpa2.getId()));

        verify(this.securityContext, times(1)).getAuthentication();
        verify(this.keycloakService, times(0)).isInProfileList(anyString(), anyLong());
        verify(this.postsRepository, times(1)).findActiveById(anyLong());
        verify(this.profilesRepository, times(1)).findActiveById(any(Long.class));
        verify(this.profilesRepository, times(1)).findActiveByPostId(any(Long.class));
        verify(this.followsRepository, times(0)).findActiveAcceptedById(any(FollowsId.class));
        verify(this.likesRepository, times(1)).findActiveById(any(LikeId.class));
        verify(this.likeToLikeJpaConverter, times(0)).convert(any(Like.class));
        verify(this.likesRepository, times(0)).save(any(LikeJpa.class));
    }

    @Test
    void testAddLike_PublicPost_RemoveNotExists_Success(){
        when(this.securityContext.getAuthentication()).thenReturn(this.jwtAuthenticationTokenWithProfileList);
        when(this.postsRepository.findActiveById(anyLong())).thenReturn(Optional.of(this.savedPostJpa2));
        when(this.profilesRepository.findActiveByPostId(any(Long.class))).thenReturn(Optional.of(this.profileJpa2));
        when(this.profilesRepository.findActiveById(any(Long.class))).thenReturn(Optional.of(this.profileJpa));
        
        when(this.likesRepository.findActiveById(any(LikeId.class))).thenReturn(Optional.empty());

        this.postsService.addLike(true, new Like(this.profileJpa.getId(), savedPostJpa2.getId()));


        verify(this.securityContext, times(1)).getAuthentication();
        verify(this.keycloakService, times(0)).isInProfileList(anyString(), anyLong());
        verify(this.postsRepository, times(1)).findActiveById(anyLong());
        verify(this.profilesRepository, times(1)).findActiveById(any(Long.class));
        verify(this.profilesRepository, times(1)).findActiveByPostId(any(Long.class));
        verify(this.followsRepository, times(0)).findActiveAcceptedById(any(FollowsId.class));
        verify(this.likesRepository, times(1)).findActiveById(any(LikeId.class));
        verify(this.likeToLikeJpaConverter, times(0)).convert(any(Like.class));
        verify(this.likesRepository, times(0)).save(any(LikeJpa.class));
    }

/*
    @Test
    void testFindAllLikesByPostId_Success(){
        List<LikeJpa> likeJpaList = asList(
                new LikeJpa(new LikeId(2L, 1L)),
                new LikeJpa(new LikeId(3L, 1L)),
                new LikeJpa(new LikeId(4L, 1L)));
        List<Like> likeList = asList(
                new Like(2L,1L),
                new Like(3L,1L),
                new Like(4L,1L));

        //when(this.postsRepository.getReferenceById(any(Long.class))).thenReturn(this.savedPostJpa);
        when(this.likesRepository.findAllActiveByPostId(anyLong())).thenReturn(likeJpaList);
        when(this.likeToLikeJpaConverter.convertBack(likeJpaList.get(0))).thenReturn(likeList.get(0));
        when(this.likeToLikeJpaConverter.convertBack(likeJpaList.get(1))).thenReturn(likeList.get(1));
        when(this.likeToLikeJpaConverter.convertBack(likeJpaList.get(2))).thenReturn(likeList.get(2));

        List<Like> returnedLikeList = this.postsService.findAllLikesByPostId(1L);

        assertEquals(3,returnedLikeList.size());

        verify(this.likesRepository, times(1)).findAllActiveByPostId(anyLong());
        verify(this.likeToLikeJpaConverter, times(3)).convertBack(any(LikeJpa.class));
    }

    @Test
    void testFindAllLikesByPostId_Empty_Success(){
        List<LikeJpa> likeJpaList = new ArrayList<>();

        //when(this.postsRepository.findActiveById(any(Long.class))).thenReturn(Optional.of(this.savedPostJpa));
        when(this.likesRepository.findAllActiveByPostId(anyLong())).thenReturn(likeJpaList);

        List<Like> returnedLikeList = this.postsService.findAllLikesByPostId(1L);

        assertEquals(0,returnedLikeList.size());

        verify(this.likesRepository, times(1)).findAllActiveByPostId(anyLong());
        verify(this.likeToLikeJpaConverter, times(0)).convertBack(any(LikeJpa.class));
    }

    @Test
    void testAddCommentSuccess(){
        String content = "Commento al post";
        CommentJpa commentJpa = new CommentJpa(content);
        Long commentId = 1L;
        Comment comment = new Comment(
                this.profileJpa.getId(),
                this.savedPostJpa.getId(),
                content
        );
        comment.setId(commentId);

        when(this.postsRepository.findActiveById(anyLong())).thenReturn(Optional.of(this.savedPostJpa));
        when(this.profilesRepository.findActiveById(anyLong())).thenReturn(Optional.of(this.profileJpa));
        when(this.commentToCommentJpaConverter.convert(any(Comment.class))).thenReturn(commentJpa);
        when(this.commentsRepository.save(any(CommentJpa.class))).thenReturn(commentJpa);
        when(this.commentToCommentJpaConverter.convertBack(any(CommentJpa.class))).thenReturn(comment);

        Comment savedComment = this.postsService.addComment(new Comment(this.profileJpa.getId(), this.savedPostJpa.getId(), content));

        assertNotNull(savedComment);
        assertEquals(commentId, savedComment.getId());
        assertEquals(this.profileJpa.getId(), savedComment.getProfileId());
        assertEquals(this.savedPostJpa.getId(), savedComment.getPostId());
        assertEquals(content, savedComment.getContent());
        log.info("Comment created at -> "+commentJpa.getCreatedAt());
        verify(this.postsRepository, times(1)).findActiveById(anyLong());
        verify(this.profilesRepository, times(1)).findActiveById(anyLong());
        verify(this.commentToCommentJpaConverter, times(1)).convert(any(Comment.class));
        verify(this.commentsRepository, times(1)).save(any(CommentJpa.class));
        verify(this.commentToCommentJpaConverter, times(1)).convertBack(any(CommentJpa.class));
    }

    @Test
    void testAddComment_PostNotFound_Failed(){
        when(this.postsRepository.findActiveById(anyLong())).thenReturn(Optional.empty());

        assertThrows(PostNotFoundException.class, () -> this.postsService.addComment(new Comment(this.profileJpa.getId(), this.savedPostJpa.getId(), "Commento al post")));

        verify(this.postsRepository, times(1)).findActiveById(anyLong());
        verify(this.profilesRepository, times(0)).findActiveById(anyLong());
        verify(this.commentToCommentJpaConverter, times(0)).convert(any(Comment.class));
        verify(this.commentsRepository, times(0)).save(any(CommentJpa.class));
        verify(this.commentToCommentJpaConverter, times(0)).convertBack(any(CommentJpa.class));
    }

    @Test
    void testAddComment_ProfileNotFound_Failed(){
        when(this.postsRepository.findActiveById(anyLong())).thenReturn(Optional.of(this.savedPostJpa));
        when(this.profilesRepository.findActiveById(anyLong())).thenReturn(Optional.empty());
        assertThrows(ProfileNotFoundException.class, () -> this.postsService.addComment(new Comment(this.profileJpa.getId(), this.savedPostJpa.getId(), "Commento al post")));

        verify(this.postsRepository, times(1)).findActiveById(anyLong());
        verify(this.profilesRepository, times(1)).findActiveById(anyLong());
        verify(this.commentToCommentJpaConverter, times(0)).convert(any(Comment.class));
        verify(this.commentsRepository, times(0)).save(any(CommentJpa.class));
        verify(this.commentToCommentJpaConverter, times(0)).convertBack(any(CommentJpa.class));

    }

    @Test
    void testAddComment_CommentOnStory_Failed(){
        PostJpa savedStoryPostJpa = new PostJpa(contentUrl, Post.PostTypeEnum.STORY);
        savedStoryPostJpa.setCaption(caption);
        savedStoryPostJpa.setProfile(profileJpa);
        savedStoryPostJpa.setId(postId);
        when(this.postsRepository.findActiveById(anyLong())).thenReturn(Optional.of(savedStoryPostJpa));
        assertThrows(CommentOnStoryException.class, () -> this.postsService.addComment(new Comment(this.profileJpa.getId(), this.savedPostJpa.getId(), "Commento al post")));

        verify(this.postsRepository, times(1)).findActiveById(anyLong());
        verify(this.profilesRepository, times(0)).findActiveById(anyLong());
        verify(this.commentToCommentJpaConverter, times(0)).convert(any(Comment.class));
        verify(this.commentsRepository, times(0)).save(any(CommentJpa.class));
        verify(this.commentToCommentJpaConverter, times(0)).convertBack(any(CommentJpa.class));

    }

    @Test
    void testUpdateCommentById_Success(){
        String content = "Commento al post";
        String newContent = "Commento al post modificato";
        CommentPatch commentPatch = new CommentPatch(newContent);
        Long commentId = 1L;
        CommentJpa commentJpa = new CommentJpa(content);
        commentJpa.setId(commentId);
        commentJpa.setProfile(profileJpa);
        commentJpa.setPost(this.savedPostJpa);

        Comment convertedComment = new Comment(profileJpa.getId(), this.savedPostJpa.getId(), newContent);
        convertedComment.setId(commentId);

        when(this.commentsRepository.getReferenceById(anyLong())).thenReturn(commentJpa);
        when(this.commentsRepository.save(any(CommentJpa.class))).thenReturn(commentJpa);
        when(this.commentToCommentJpaConverter.convertBack(any(CommentJpa.class))).thenReturn(convertedComment);

        Comment updatedComment = this.postsService.updateCommentById(commentId, commentPatch);

        assertNotNull(updatedComment);
        assertEquals(commentId, updatedComment.getId());
        assertEquals(profileJpa.getId(), updatedComment.getProfileId());
        assertEquals(this.savedPostJpa.getId(), updatedComment.getPostId());
        assertEquals(newContent, updatedComment.getContent());
        log.info("Comment's content-> "+commentJpa.getContent());
        verify(this.commentsRepository, times(1)).getReferenceById(anyLong());
        verify(this.commentsRepository, times(1)).save(any(CommentJpa.class));
        verify(this.commentToCommentJpaConverter, times(1)).convertBack(any(CommentJpa.class));
    }


    @Test
    void testDeleteCommentById_Success(){
        String content = "Commento al post";
        Long commentId = 1L;
        CommentJpa commentJpa = new CommentJpa(content);
        commentJpa.setId(commentId);
        commentJpa.setProfile(profileJpa);
        commentJpa.setPost(this.savedPostJpa);

        when(this.commentsRepository.getReferenceById(anyLong())).thenReturn(commentJpa);

        this.postsService.deleteCommentById(commentId);

        verify(this.commentsRepository, times(1)).getReferenceById(anyLong());
        verify(this.commentsRepository, times(1)).save(any(CommentJpa.class));
    }


    @Test
    void testFindAllCommentsByPostId_Success(){
        List<ProfileJpa> profileJpaList = asList(
          new ProfileJpa("pinco_pallino",false,1L),
          new ProfileJpa("giuseppe_verdi",false,2L),
          new ProfileJpa("marioRossi",false,3L)
        );

        profileJpaList.get(0).setId(1L);
        profileJpaList.get(1).setId(2L);
        profileJpaList.get(2).setId(3L);

        List<Comment> commentList = asList(
                new Comment(1L, 1L, "Commento1"),
                new Comment(2L, 1L, "Commento2"),
                new Comment(3L, 1L, "Commento3")
        );
        commentList.get(0).setId(1L);
        commentList.get(1).setId(2L);
        commentList.get(2).setId(3L);

        List<CommentJpa> commentListJpa = asList(
                new CommentJpa("Commento1"),
                new CommentJpa("Commento2"),
                new CommentJpa("Commento3")
        );
        commentListJpa.get(0).setPost(this.savedPostJpa);
        commentListJpa.get(1).setPost(this.savedPostJpa);
        commentListJpa.get(2).setPost(this.savedPostJpa);
        commentListJpa.get(0).setProfile(profileJpaList.get(0));
        commentListJpa.get(1).setProfile(profileJpaList.get(1));
        commentListJpa.get(2).setProfile(profileJpaList.get(2));
        commentListJpa.get(0).setId(1L);
        commentListJpa.get(1).setId(2L);
        commentListJpa.get(2).setId(3L);

        when(this.commentsRepository.findAllActiveByPostId(anyLong())).thenReturn(commentListJpa);
        when(this.commentToCommentJpaConverter.convertBack(commentListJpa.get(0))).thenReturn(commentList.get(0));
        when(this.commentToCommentJpaConverter.convertBack(commentListJpa.get(1))).thenReturn(commentList.get(1));
        when(this.commentToCommentJpaConverter.convertBack(commentListJpa.get(2))).thenReturn(commentList.get(2));

        List<Comment> returnedCommentList = this.postsService.findAllCommentsByPostId(this.savedPostJpa.getId());

        assertNotNull(returnedCommentList);
        assertEquals(commentList, returnedCommentList);
        verify(this.commentsRepository, times(1)).findAllActiveByPostId(anyLong());
        verify(this.commentToCommentJpaConverter, times(3)).convertBack(any(CommentJpa.class));
    }


    @Test
    void testProfileFeedByProfileId_Null_OnlyPost_Success(){
        List<PostJpa> postJpaList = createPostJpaList();

        List<Post> convertedPostList = createConvertedPostList();

        when(this.postsRepository.getFeedByProfileId(anyLong())).thenReturn(postJpaList);
        when(this.postToPostJpaConverter.convertBack(postJpaList.get(0))).thenReturn(convertedPostList.get(0));
        when(this.postToPostJpaConverter.convertBack(postJpaList.get(1))).thenReturn(convertedPostList.get(1));
        when(this.postToPostJpaConverter.convertBack(postJpaList.get(2))).thenReturn(convertedPostList.get(2));
        when(this.postToPostJpaConverter.convertBack(postJpaList.get(3))).thenReturn(convertedPostList.get(3));

        List<Post> postList = this.postsService.profileFeedByProfileId(profileId, null);

        assertEquals(convertedPostList, postList);
        verify(this.postsRepository, times(1)).getFeedByProfileId(anyLong());
        verify(this.postToPostJpaConverter, times(4)).convertBack(any(PostJpa.class));
        verify(this.postsRepository, times(0)).getPostFeedByProfileId(anyLong());
        verify(this.postsRepository, times(0)).getStoryFeedByProfileId(anyLong());

        log.info(postList.toString());
    }

    @Test
    void testProfileFeedByProfileId_True_OnlyPost_Success(){
        List<PostJpa> postJpaList = createPostJpaList().stream().filter(postJpa -> postJpa.getPostType().equals(Post.PostTypeEnum.POST)).toList();

        List<Post> convertedPostList = createConvertedPostList().stream().filter(post -> post.getPostType().equals(Post.PostTypeEnum.POST)).toList();

        when(this.postsRepository.getPostFeedByProfileId(anyLong())).thenReturn(postJpaList);
        when(this.postToPostJpaConverter.convertBack(postJpaList.get(0))).thenReturn(convertedPostList.get(0));
        when(this.postToPostJpaConverter.convertBack(postJpaList.get(1))).thenReturn(convertedPostList.get(1));

        List<Post> postList = this.postsService.profileFeedByProfileId(profileId, true);

        assertEquals(convertedPostList, postList);
        verify(this.postsRepository, times(0)).getFeedByProfileId(anyLong());
        verify(this.postToPostJpaConverter, times(2)).convertBack(any(PostJpa.class));
        verify(this.postsRepository, times(1)).getPostFeedByProfileId(anyLong());
        verify(this.postsRepository, times(0)).getStoryFeedByProfileId(anyLong());

        log.info(postList.toString());
    }

    @Test
    void testProfileFeedByProfileId_False_OnlyPost_Success(){
        List<PostJpa> postJpaList = createPostJpaList().stream().filter(postJpa -> postJpa.getPostType().equals(Post.PostTypeEnum.STORY)).toList();

        List<Post> convertedPostList = createConvertedPostList().stream().filter(post -> post.getPostType().equals(Post.PostTypeEnum.STORY)).toList();

        when(this.postsRepository.getStoryFeedByProfileId(anyLong())).thenReturn(postJpaList);
        when(this.postToPostJpaConverter.convertBack(postJpaList.get(0))).thenReturn(convertedPostList.get(0));
        when(this.postToPostJpaConverter.convertBack(postJpaList.get(1))).thenReturn(convertedPostList.get(1));

        List<Post> postList = this.postsService.profileFeedByProfileId(profileId, false);

        assertEquals(convertedPostList, postList);
        verify(this.postsRepository, times(0)).getFeedByProfileId(anyLong());
        verify(this.postToPostJpaConverter, times(2)).convertBack(any(PostJpa.class));
        verify(this.postsRepository, times(0)).getPostFeedByProfileId(anyLong());
        verify(this.postsRepository, times(1)).getStoryFeedByProfileId(anyLong());

        log.info(postList.toString());
    }


    private List<Post> createConvertedPostList() {
        Post newPost1 = new Post(contentUrl, Post.PostTypeEnum.POST, profileId);
        newPost1.setCaption("First Post Caption");
        newPost1.setId(1L);

        Post newPost2 = new Post(contentUrl, Post.PostTypeEnum.STORY, profileId);
        newPost2.setId(2L);

        Post newPost3 = new Post(contentUrl, Post.PostTypeEnum.POST, profileId);
        newPost3.setCaption("Second Post Caption");
        newPost3.setId(3L);

        Post newPost4 = new Post(contentUrl, Post.PostTypeEnum.STORY, profileId);
        newPost4.setId(4L);


        List<Post> convertedPostList = new ArrayList<>();
        convertedPostList.add(newPost1);
        convertedPostList.add(newPost2);
        convertedPostList.add(newPost3);
        convertedPostList.add(newPost4);
        return convertedPostList;
    }

    private List<PostJpa> createPostJpaList() {
        PostJpa newPostJpa1 = new PostJpa(contentUrl, Post.PostTypeEnum.POST);
        newPostJpa1.setCaption("First Post Caption");
        newPostJpa1.setProfile(profileJpa);
        newPostJpa1.setId(1L);

        PostJpa newPostJpa2 = new PostJpa(contentUrl, Post.PostTypeEnum.STORY);
        newPostJpa2.setProfile(profileJpa);
        newPostJpa2.setId(2L);

        PostJpa newPostJpa3 = new PostJpa(contentUrl, Post.PostTypeEnum.POST);
        newPostJpa3.setCaption("Second Post Caption");
        newPostJpa3.setProfile(profileJpa);
        newPostJpa3.setId(3L);

        PostJpa newPostJpa4 = new PostJpa(contentUrl, Post.PostTypeEnum.STORY);
        newPostJpa4.setProfile(profileJpa);
        newPostJpa4.setId(4L);

        List<PostJpa> postJpaList = new ArrayList<>();
        postJpaList.add(newPostJpa1);
        postJpaList.add(newPostJpa2);
        postJpaList.add(newPostJpa3);
        postJpaList.add(newPostJpa4);
        return postJpaList;
    }

     */

}