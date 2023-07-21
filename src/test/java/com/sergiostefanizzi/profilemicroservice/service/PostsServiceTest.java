package com.sergiostefanizzi.profilemicroservice.service;

import com.sergiostefanizzi.profilemicroservice.model.*;
import com.sergiostefanizzi.profilemicroservice.model.converter.CommentToCommentJpaConverter;
import com.sergiostefanizzi.profilemicroservice.model.converter.LikeToLikeJpaConverter;
import com.sergiostefanizzi.profilemicroservice.model.converter.PostToPostJpaConverter;
import com.sergiostefanizzi.profilemicroservice.repository.CommentsRepository;
import com.sergiostefanizzi.profilemicroservice.repository.LikesRepository;
import com.sergiostefanizzi.profilemicroservice.repository.PostsRepository;
import com.sergiostefanizzi.profilemicroservice.repository.ProfilesRepository;
import com.sergiostefanizzi.profilemicroservice.system.exception.CommentNotFoundException;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;
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
    @Mock
    private LikesRepository likesRepository;
    @Mock
    private LikeToLikeJpaConverter likeToLikeJpaConverter;
    @Mock
    private CommentsRepository commentsRepository;
    @Mock
    private CommentToCommentJpaConverter commentToCommentJpaConverter;
    @InjectMocks
    private PostsService postsService;

    String contentUrl = "https://upload.wikimedia.org/wikipedia/commons/9/9a/Cape_may.jpg";
    String caption = "This is the post caption";
    Post.PostTypeEnum postType = Post.PostTypeEnum.POST;
    Long postId = 1L;
    Long profileId = 11L;
    private Post newPost;
    private PostJpa savedPostJpa;
    private Like newLike;
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

        this.newLike = new Like(profileId, postId);
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

    @Test
    void testAddLikeSuccess(){
        LikeJpa likeJpa = new LikeJpa(new LikeId(this.newLike.getProfileId(), this.newLike.getPostId()));

        when(this.postsRepository.findById(any(Long.class))).thenReturn(Optional.of(this.savedPostJpa));
        when(this.profilesRepository.findById(any(Long.class))).thenReturn(Optional.of(this.profileJpa));
        when(this.likesRepository.findById(any(LikeId.class))).thenReturn(Optional.empty());
        when(this.likeToLikeJpaConverter.convert(any(Like.class))).thenReturn(likeJpa);
        when(this.likesRepository.save(any(LikeJpa.class))).thenReturn(likeJpa);

        this.postsService.addLike(false, this.newLike);

        log.info("Like Created at -> "+likeJpa.getCreatedAt());
        assertNotNull(likeJpa.getCreatedAt());
        assertNull(likeJpa.getDeletedAt());
        verify(this.postsRepository, times(1)).findById(any(Long.class));
        verify(this.profilesRepository, times(1)).findById(any(Long.class));
        verify(this.likesRepository, times(1)).findById(any(LikeId.class));
        verify(this.likeToLikeJpaConverter, times(1)).convert(any(Like.class));
        verify(this.likesRepository, times(1)).save(any(LikeJpa.class));
    }

    @Test
    void testAddLike_RemoveLike_Success(){
        LikeJpa likeJpa = new LikeJpa(new LikeId(this.newLike.getProfileId(), this.newLike.getPostId()));

        when(this.postsRepository.findById(any(Long.class))).thenReturn(Optional.of(this.savedPostJpa));
        when(this.profilesRepository.findById(any(Long.class))).thenReturn(Optional.of(this.profileJpa));
        when(this.likesRepository.findById(any(LikeId.class))).thenReturn(Optional.of(likeJpa));
        when(this.likesRepository.save(likeJpa)).thenReturn(likeJpa);

        this.postsService.addLike(true, this.newLike);

        log.info("Like Deleted at -> "+likeJpa.getDeletedAt());
        assertNotNull(likeJpa.getDeletedAt());
        verify(this.postsRepository, times(1)).findById(any(Long.class));
        verify(this.profilesRepository, times(1)).findById(any(Long.class));
        verify(this.likesRepository, times(1)).findById(any(LikeId.class));
        verify(this.likeToLikeJpaConverter, times(0)).convert(any(Like.class));
        verify(this.likesRepository, times(1)).save(any(LikeJpa.class));
    }

    @Test
    void testAddLike_PostNotFound_Success(){
        when(this.postsRepository.findById(any(Long.class))).thenReturn(Optional.empty());

        assertThrows(PostNotFoundException.class, () -> this.postsService.addLike(false, this.newLike));

        verify(this.postsRepository, times(1)).findById(any(Long.class));
        verify(this.profilesRepository, times(0)).findById(any(Long.class));
        verify(this.likesRepository, times(0)).findById(any(LikeId.class));
        verify(this.likeToLikeJpaConverter, times(0)).convert(any(Like.class));
        verify(this.likesRepository, times(0)).save(any(LikeJpa.class));

    }

    @Test
    void testAddLike_ProfileNotFound_Success(){
        when(this.postsRepository.findById(any(Long.class))).thenReturn(Optional.of(this.savedPostJpa));
        when(this.profilesRepository.findById(any(Long.class))).thenReturn(Optional.empty());

        assertThrows(ProfileNotFoundException.class, () -> this.postsService.addLike(false, this.newLike));

        verify(this.postsRepository, times(1)).findById(any(Long.class));
        verify(this.profilesRepository, times(1)).findById(any(Long.class));
        verify(this.likesRepository, times(0)).findById(any(LikeId.class));
        verify(this.likeToLikeJpaConverter, times(0)).convert(any(Like.class));
        verify(this.likesRepository, times(0)).save(any(LikeJpa.class));
    }

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

        when(this.postsRepository.findActiveById(any(Long.class))).thenReturn(Optional.of(this.savedPostJpa));
        when(this.likesRepository.findAllActiveByPost(any(PostJpa.class))).thenReturn(likeJpaList);
        when(this.likeToLikeJpaConverter.convertBack(likeJpaList.get(0))).thenReturn(likeList.get(0));
        when(this.likeToLikeJpaConverter.convertBack(likeJpaList.get(1))).thenReturn(likeList.get(1));
        when(this.likeToLikeJpaConverter.convertBack(likeJpaList.get(2))).thenReturn(likeList.get(2));

        List<Like> returnedLikeList = this.postsService.findAllLikesByPostId(1L);

        assertEquals(3,returnedLikeList.size());
        verify(this.postsRepository, times(1)).findActiveById(anyLong());
        verify(this.likesRepository, times(1)).findAllActiveByPost(any(PostJpa.class));
        verify(this.likeToLikeJpaConverter, times(3)).convertBack(any(LikeJpa.class));
    }

    @Test
    void testFindAllLikesByPostId_Empty_Success(){
        List<LikeJpa> likeJpaList = new ArrayList<>();

        when(this.postsRepository.findActiveById(any(Long.class))).thenReturn(Optional.of(this.savedPostJpa));
        when(this.likesRepository.findAllActiveByPost(any(PostJpa.class))).thenReturn(likeJpaList);

        List<Like> returnedLikeList = this.postsService.findAllLikesByPostId(1L);

        assertEquals(0,returnedLikeList.size());
        verify(this.postsRepository, times(1)).findActiveById(anyLong());
        verify(this.likesRepository, times(1)).findAllActiveByPost(any(PostJpa.class));
        verify(this.likeToLikeJpaConverter, times(0)).convertBack(any(LikeJpa.class));
    }

    @Test
    void testFindAllLikesByPostId_NotFound_Failed(){
        when(this.postsRepository.findActiveById(any(Long.class))).thenReturn(Optional.empty());

        assertThrows(PostNotFoundException.class, () -> this.postsService.findAllLikesByPostId(1L));

        verify(this.postsRepository, times(1)).findActiveById(anyLong());
        verify(this.likesRepository, times(0)).findAllActiveByPost(any(PostJpa.class));
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

        when(this.commentsRepository.findActiveById(anyLong())).thenReturn(Optional.of(commentJpa));
        when(this.commentsRepository.save(any(CommentJpa.class))).thenReturn(commentJpa);
        when(this.commentToCommentJpaConverter.convertBack(any(CommentJpa.class))).thenReturn(convertedComment);

        Comment updatedComment = this.postsService.updateCommentById(commentId, commentPatch);

        assertNotNull(updatedComment);
        assertEquals(commentId, updatedComment.getId());
        assertEquals(profileJpa.getId(), updatedComment.getProfileId());
        assertEquals(this.savedPostJpa.getId(), updatedComment.getPostId());
        assertEquals(newContent, updatedComment.getContent());
        log.info("Comment's content-> "+commentJpa.getContent());
        verify(this.commentsRepository, times(1)).findActiveById(anyLong());
        verify(this.commentsRepository, times(1)).save(any(CommentJpa.class));
        verify(this.commentToCommentJpaConverter, times(1)).convertBack(any(CommentJpa.class));
    }

    @Test
    void testUpdateCommentById_CommentNotFound_Failed(){
        String newContent = "Commento al post modificato";
        CommentPatch commentPatch = new CommentPatch(newContent);
        Long commentId = 1L;

        when(this.commentsRepository.findActiveById(anyLong())).thenReturn(Optional.empty());

        assertThrows(CommentNotFoundException.class, () -> this.postsService.updateCommentById(commentId, commentPatch));

        verify(this.commentsRepository, times(1)).findActiveById(anyLong());
        verify(this.commentsRepository, times(0)).save(any(CommentJpa.class));
        verify(this.commentToCommentJpaConverter, times(0)).convertBack(any(CommentJpa.class));
    }

    @Test
    void testDeleteCommentById_Success(){
        String content = "Commento al post";
        Long commentId = 1L;
        CommentJpa commentJpa = new CommentJpa(content);
        commentJpa.setId(commentId);
        commentJpa.setProfile(profileJpa);
        commentJpa.setPost(this.savedPostJpa);

        when(this.commentsRepository.findActiveById(anyLong())).thenReturn(Optional.of(commentJpa));

        this.postsService.deleteCommentById(commentId);

        verify(this.commentsRepository, times(1)).findActiveById(anyLong());
        verify(this.commentsRepository, times(1)).save(any(CommentJpa.class));
    }

    @Test
    void testDeleteCommentById_CommentNotFound_Failed(){
        Long commentId = 1L;
        when(this.commentsRepository.findActiveById(anyLong())).thenReturn(Optional.empty());

        assertThrows(CommentNotFoundException.class, () ->this.postsService.deleteCommentById(commentId));
        verify(this.commentsRepository, times(1)).findActiveById(anyLong());
        verify(this.commentsRepository, times(0)).save(any(CommentJpa.class));
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

        when(this.postsRepository.findActiveById(anyLong())).thenReturn(Optional.of(this.savedPostJpa));
        when(this.commentsRepository.findAllActiveByPost(any(PostJpa.class))).thenReturn(commentListJpa);
        when(this.commentToCommentJpaConverter.convertBack(commentListJpa.get(0))).thenReturn(commentList.get(0));
        when(this.commentToCommentJpaConverter.convertBack(commentListJpa.get(1))).thenReturn(commentList.get(1));
        when(this.commentToCommentJpaConverter.convertBack(commentListJpa.get(2))).thenReturn(commentList.get(2));

        List<Comment> returnedCommentList = this.postsService.findAllCommentsByPostId(this.savedPostJpa.getId());

        assertNotNull(returnedCommentList);
        assertEquals(commentList, returnedCommentList);
        verify(this.postsRepository, times(1)).findActiveById(anyLong());
        verify(this.commentsRepository, times(1)).findAllActiveByPost(any(PostJpa.class));
        verify(this.commentToCommentJpaConverter, times(3)).convertBack(any(CommentJpa.class));
    }

    @Test
    void testFindAllCommentsByPostId_PostNotFound_Failed(){
        when(this.postsRepository.findActiveById(Long.MIN_VALUE)).thenReturn(Optional.empty());

        assertThrows(PostNotFoundException.class, () -> this.postsService.findAllCommentsByPostId(Long.MIN_VALUE));

        verify(this.postsRepository, times(1)).findActiveById(anyLong());
        verify(this.commentsRepository, times(0)).findAllActiveByPost(any(PostJpa.class));
        verify(this.commentToCommentJpaConverter, times(0)).convertBack(any(CommentJpa.class));
    }

}