package com.sergiostefanizzi.profilemicroservice.service;

import com.sergiostefanizzi.profilemicroservice.model.Post;
import com.sergiostefanizzi.profilemicroservice.model.PostJpa;
import com.sergiostefanizzi.profilemicroservice.model.ProfileJpa;
import com.sergiostefanizzi.profilemicroservice.model.converter.PostToPostJpaConverter;
import com.sergiostefanizzi.profilemicroservice.repository.PostsRepository;
import com.sergiostefanizzi.profilemicroservice.repository.ProfilesRepository;
import com.sergiostefanizzi.profilemicroservice.system.exception.ProfileNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

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

    String contentUrl = "";
    String caption = "This is the post caption";
    Post.PostTypeEnum postType = Post.PostTypeEnum.POST;
    Long postId = 1L;
    Long profileId = 11L;
    private Post newPost;
    ProfileJpa profileJpa = new ProfileJpa("pinco_pallino",false,111L);

    @BeforeEach
    void setUp() {
        profileJpa.setId(profileId);
        this.newPost = new Post(contentUrl, postType, profileId);
        this.newPost.setCaption(caption);
        this.newPost.setProfileId(profileId);
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


}