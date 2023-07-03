package com.sergiostefanizzi.profilemicroservice.service;

import com.sergiostefanizzi.profilemicroservice.model.Post;
import com.sergiostefanizzi.profilemicroservice.model.PostJpa;
import com.sergiostefanizzi.profilemicroservice.model.PostPatch;
import com.sergiostefanizzi.profilemicroservice.model.ProfileJpa;
import com.sergiostefanizzi.profilemicroservice.model.converter.PostToPostJpaConverter;
import com.sergiostefanizzi.profilemicroservice.repository.PostsRepository;
import com.sergiostefanizzi.profilemicroservice.repository.ProfilesRepository;
import com.sergiostefanizzi.profilemicroservice.system.exception.PostNotFoundException;
import com.sergiostefanizzi.profilemicroservice.system.exception.ProfileNotFoundException;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@Slf4j
class PostsServiceTest {
    @Mock
    private PostsRepository postsRepository;
    @Mock
    private PostToPostJpaConverter postToPostJpaConverter;
    // TODO da togliere quando usero' JWT
    @Mock
    private ProfilesRepository profilesRepository;
    @InjectMocks
    private PostsService postsService;

    String contentUrl = "https://upload.wikimedia.org/wikipedia/commons/9/9a/Cape_may.jpg";
    String caption = "This is the post caption";
    Post.PostTypeEnum postType = Post.PostTypeEnum.POST;
    Long postId = 1L;
    Long profileId = 11L;
    private Post newPost;
    private PostJpa savedPostJpa;
    ProfileJpa profileJpa = new ProfileJpa("pinco_pallino",false,111L);

    @BeforeEach
    void setUp() {
        this.newPost = new Post(contentUrl, postType, profileId);
        this.newPost.setCaption(caption);


        profileJpa.setId(profileId);

        this.savedPostJpa = new PostJpa(contentUrl, postType);
        this.savedPostJpa.setCaption(caption);
        this.savedPostJpa.setProfile(profileJpa);
        this.savedPostJpa.setId(postId);
    }

    @Test
    void testSaveSuccess() {
        // Post Jpa di Post
        PostJpa newPostJpa = new PostJpa(contentUrl, postType);
        newPostJpa.setCaption(caption);
        // Post che verra' restituito
        Post convertedPost = new Post(contentUrl, postType, profileId);
        convertedPost.setCaption(caption);
        convertedPost.setId(postId);



        when(this.profilesRepository.findById(this.newPost.getProfileId())).thenReturn(Optional.of(profileJpa));
        when(this.postToPostJpaConverter.convert(this.newPost)).thenReturn(newPostJpa);
        when(this.postsRepository.save(newPostJpa)).thenReturn(newPostJpa);
        when(this.postToPostJpaConverter.convertBack(newPostJpa)).thenReturn(convertedPost);

        Post savedPost = this.postsService.save(this.newPost);

        log.info("POST'S PROFILE-> "+newPostJpa.getProfile().getProfileName());

        assertNotNull(savedPost);
        assertEquals(postId, savedPost.getId());
        assertEquals(contentUrl, savedPost.getContentUrl());
        assertEquals(caption, savedPost.getCaption());
        assertEquals(postType, savedPost.getPostType());
        assertEquals(profileId, savedPost.getProfileId());
        verify(this.profilesRepository, times(1)).findById(this.newPost.getProfileId());
        verify(this.postToPostJpaConverter, times(1)).convert(this.newPost);
        verify(this.postsRepository, times(1)).save(newPostJpa);
        verify(this.postToPostJpaConverter, times(1)).convertBack(newPostJpa);
    }

    @Test
    void testSave_ProfileNotFound_Failed(){
        when(this.profilesRepository.findById(this.newPost.getProfileId())).thenReturn(Optional.empty());

        assertThrows(ProfileNotFoundException.class,
                () -> this.postsService.save(this.newPost));

        verify(this.profilesRepository, times(1)).findById(this.newPost.getProfileId());
        verify(this.postToPostJpaConverter, times(0)).convert(this.newPost);
        verify(this.postsRepository, times(0)).save(any(PostJpa.class));
        verify(this.postToPostJpaConverter, times(0)).convertBack(any(PostJpa.class));
    }

    @Test
    void testRemoveSuccess(){
        when(this.postsRepository.findById(postId)).thenReturn(Optional.of(this.savedPostJpa));
        when(this.postsRepository.save(this.savedPostJpa)).thenReturn(this.savedPostJpa);

        // l'istante di rimozione deve essere nullo prima della rimozione
        assertNull(this.savedPostJpa.getDeletedAt());

        this.postsService.remove(postId);

        // l'istante di rimozione deve essere NON nullo DOPO la rimozione
        assertNotNull(this.savedPostJpa.getDeletedAt());
        verify(this.postsRepository, times(1)).findById(postId);
        verify(this.postsRepository, times(1)).save(this.savedPostJpa);
    }

    @Test
    void testRemove_PostNot_Found_Failed(){
        when(this.postsRepository.findById(postId)).thenReturn(Optional.empty());
        assertThrows(PostNotFoundException.class,
                () -> this.postsService.remove(postId)
        );

        verify(this.postsRepository, times(1)).findById(postId);
        verify(this.postsRepository, times(0)).save(any(PostJpa.class));
    }

    @Test
    void testRemove_PostAlreadyRemoved_Failed(){
        // Imposto una data passata
        this.savedPostJpa.setDeletedAt(LocalDateTime.MIN);
        log.info("Deleted At"+this.savedPostJpa.getDeletedAt());

        when(this.postsRepository.findById(postId)).thenReturn(Optional.of(this.savedPostJpa));

        assertThrows(PostNotFoundException.class,
                () -> this.postsService.remove(postId)
        );

        verify(this.postsRepository, times(1)).findById(postId);
        verify(this.postsRepository, times(0)).save(any(PostJpa.class));
    }

    //TODO fare test rimozione post con controllo autorizzazione

    @Test
    void testUpdateSuccess(){
        // Aggiornamento del post
        String newCaption = "Nuova caption del post";
        PostPatch postPatch = new PostPatch(newCaption);
        // Post che verra' restituito
        Post convertedPost = new Post(contentUrl, postType, profileId);
        convertedPost.setCaption(newCaption);
        convertedPost.setId(postId);

        when(this.postsRepository.findById(postId)).thenReturn(Optional.of(this.savedPostJpa));
        when(this.postsRepository.save(this.savedPostJpa)).thenReturn(this.savedPostJpa);
        when(this.postToPostJpaConverter.convertBack(this.savedPostJpa)).thenReturn(convertedPost);

        Post updatedPost = this.postsService.update(postId, postPatch);

        assertNotNull(updatedPost);
        assertEquals(postId, updatedPost.getId());
        assertEquals(contentUrl, updatedPost.getContentUrl());
        assertEquals(newCaption, updatedPost.getCaption());
        assertEquals(postType, updatedPost.getPostType());
        assertEquals(profileId, updatedPost.getProfileId());
        verify(this.postsRepository, times(1)).findById(postId);
        verify(this.postsRepository, times(1)).save(this.savedPostJpa);
        verify(this.postToPostJpaConverter, times(1)).convertBack(this.savedPostJpa);
    }

    @Test
    void testUpdate_PostNot_Found_Failed(){
        when(this.postsRepository.findById(postId)).thenReturn(Optional.empty());
        assertThrows(PostNotFoundException.class,
                () -> this.postsService.update(postId, any(PostPatch.class))
        );

        verify(this.postsRepository, times(1)).findById(postId);
        verify(this.postsRepository, times(0)).save(any(PostJpa.class));
    }

    @Test
    void testUpdate_PostAlreadyRemoved_Failed(){
        // Imposto una data passata
        this.savedPostJpa.setDeletedAt(LocalDateTime.MIN);
        log.info("Deleted At"+this.savedPostJpa.getDeletedAt());

        when(this.postsRepository.findById(postId)).thenReturn(Optional.of(this.savedPostJpa));

        assertThrows(PostNotFoundException.class,
                () -> this.postsService.update(postId, any(PostPatch.class))
        );

        verify(this.postsRepository, times(1)).findById(postId);
        verify(this.postsRepository, times(0)).save(any(PostJpa.class));
    }

    @Test
    void testFindSuccess(){
        // Post che verra' restituito
        Post convertedPost = new Post(contentUrl, postType, profileId);
        convertedPost.setCaption(caption);
        convertedPost.setId(postId);

        when(this.postsRepository.findById(postId)).thenReturn(Optional.of(this.savedPostJpa));
        when(this.postToPostJpaConverter.convertBack((this.savedPostJpa))).thenReturn(convertedPost);

        Post post = this.postsService.find(postId);

        assertNotNull(post);
        assertEquals(postId, post.getId());
        assertEquals(contentUrl, post.getContentUrl());
        assertEquals(caption, post.getCaption());
        assertEquals(postType, post.getPostType());
        assertEquals(profileId, post.getProfileId());
        verify(this.postsRepository, times(1)).findById(postId);
        verify(this.postToPostJpaConverter, times(1)).convertBack(this.savedPostJpa);
    }

    @Test
    void testFind_NotFound_Failed(){
        when(this.postsRepository.findById(postId)).thenReturn(Optional.empty());
        assertThrows(PostNotFoundException.class,
                () -> this.postsService.find(postId)
        );

        verify(this.postsRepository, times(1)).findById(postId);
        verify(this.postToPostJpaConverter, times(0)).convertBack(any(PostJpa.class));
    }

    @Test
    void testFind_PostAlreadyRemoved_Failed(){
        // Imposto una data passata
        this.savedPostJpa.setDeletedAt(LocalDateTime.MIN);
        log.info("Deleted At"+this.savedPostJpa.getDeletedAt());

        when(this.postsRepository.findById(postId)).thenReturn(Optional.of(this.savedPostJpa));

        assertThrows(PostNotFoundException.class,
                () -> this.postsService.find(postId)
        );

        verify(this.postsRepository, times(1)).findById(postId);
        verify(this.postToPostJpaConverter, times(0)).convertBack(any(PostJpa.class));
    }


}