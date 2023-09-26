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
import com.sergiostefanizzi.profilemicroservice.system.exception.CommentOnStoryException;
import com.sergiostefanizzi.profilemicroservice.system.exception.PostNotFoundException;
import com.sergiostefanizzi.profilemicroservice.system.exception.ProfileNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


@Service
@RequiredArgsConstructor
@Slf4j
public class PostsService {
    private final PostsRepository postsRepository;
    private final PostToPostJpaConverter postToPostJpaConverter;
    // TODO Per adesso uso il profileRepository per il controllo del profileId, ma in seguito serve il JWT
    private final ProfilesRepository profilesRepository;
    private final LikesRepository likesRepository;
    private final CommentsRepository commentsRepository;
    private final LikeToLikeJpaConverter likeToLikeJpaConverter;
    private final CommentToCommentJpaConverter commentToCommentJpaConverter;

    @Transactional
    public Post save(Post post) {
        //TODO controllo l'id del profilo all'interno del JWT
        // controllo l'esistenza del profileId , altrimenti 403 Forbidden e non 404
        ProfileJpa profileJpa = this.profilesRepository.findById(post.getProfileId()).orElseThrow(
                () -> new ProfileNotFoundException(post.getProfileId())
        );
        // il profileId e' valido quindi posso fare la conversione in postJpa
        PostJpa postJpa = this.postToPostJpaConverter.convert(post);

        assert postJpa != null;
        postJpa.setProfile(profileJpa);
        postJpa.setCreatedAt(LocalDateTime.now());
        return this.postToPostJpaConverter.convertBack(
                this.postsRepository.save(postJpa)
        );
    }

    @Transactional
    public void remove(Long postId) {
        // TODO mi serve il JWT
        // Controllo che chi richiede la rimozione abbia l'autorizzazione per farlo

        PostJpa postJpaToRemove = this.postsRepository.getReferenceById(postId);
        postJpaToRemove.setDeletedAt(LocalDateTime.now());
        this.postsRepository.save(postJpaToRemove);
    }

    @Transactional
    public Post update(Long postId, PostPatch postPatch) {
        // Controllo prima l'esistenza del post
        PostJpa postJpa = this.postsRepository.getReferenceById(postId);
        // TODO mi serve il JWT
        // Controllo che chi richiede l'aggiornamento abbia l'autorizzazione per farlo
        /*
        if (postJpa.getProfile().getId().equals(Long.MIN_VALUE)){
            throw new PostNotFoundException(postId);
        }
        */
        postJpa.setCaption(postPatch.getCaption());
        postJpa.setUpdatedAt(LocalDateTime.now());
        return this.postToPostJpaConverter.convertBack(
                this.postsRepository.save(postJpa)
        );
    }

    @Transactional
    public Post find(Long postId) {
        return this.postToPostJpaConverter.convertBack(
                this.postsRepository.getReferenceById(postId)
        );
    }

    @Transactional
    public void addLike(Boolean removeLike, Like like) {
        // TODO mi serve il JWT
        // Controllo che chi vuole mettere il abbia l'autorizzazione per farlo
        // sia controllando il profilo
        // sia controllando che il profilo a cui appartiene il posto sia visibile da chi vuole mettere like

        // Controllo prima l'esistenza del post
        PostJpa postJpa = this.postsRepository.findActiveById(like.getPostId(), LocalDateTime.now().minusDays(1))
                .orElseThrow(() -> new PostNotFoundException(like.getPostId()));
        // Controllo poi l'esistenza del profilo di chi vuole mettere like
        ProfileJpa profileJpa = this.profilesRepository.findActiveById(like.getProfileId())
                .orElseThrow(() -> new ProfileNotFoundException(like.getProfileId()));

        // Controllo l'esistenza del like, se non esiste lo creo con il like ottenuto dal controller
        Optional<LikeJpa> optionalLikeJpa = this.likesRepository.findActiveById(new LikeId(profileJpa.getId(), postJpa.getId()));
        if (optionalLikeJpa.isPresent() && removeLike){
            removeLike(optionalLikeJpa.get());
        } else if (optionalLikeJpa.isEmpty() && !removeLike){
            createLike(like, profileJpa, postJpa);
        }
    }

    private void createLike(Like like, ProfileJpa profileJpa, PostJpa postJpa) {
        LikeJpa likeJpa;
        likeJpa = this.likeToLikeJpaConverter.convert(like);
        assert likeJpa != null;
        likeJpa.setCreatedAt(LocalDateTime.now());
        likeJpa.setDeletedAt(null);
        likeJpa.setProfile(profileJpa);
        likeJpa.setPost(postJpa);
        this.likesRepository.save(likeJpa);
    }

    private void removeLike(LikeJpa likeJpa) {
        likeJpa.setDeletedAt(LocalDateTime.now());
        this.likesRepository.save(likeJpa);
    }

    @Transactional
    public List<Like> findAllLikesByPostId(Long postId) {
        // TODO mi serve il JWT
        // Controllo che chi vuole mettere il like abbia l'autorizzazione per farlo
        // sia controllando il profilo
        // sia controllando che il profilo a cui appartiene il post sia visibile da chi vuole mettere like

        // Controllo prima l'esistenza del post
        //PostJpa postJpa = this.postsRepository.getReferenceById(postId);

        return this.likesRepository.findAllActiveByPostId(postId)
                .stream().map(this.likeToLikeJpaConverter::convertBack).toList();
    }

    @Transactional
    public Comment addComment(Comment comment) {
        // TODO mi serve il JWT
        // Controllo che chi vuole inserire il comment abbia l'autorizzazione per farlo
        // sia controllando il profilo
        // sia controllando che il profilo a cui appartiene il post sia visibile da chi vuole mettere like
        // Controllo prima l'esistenza del post
        PostJpa postJpa = this.postsRepository.findActiveById(comment.getPostId(), LocalDateTime.now().minusDays(1))
                .orElseThrow(() -> new PostNotFoundException(comment.getPostId()));
        if (postJpa.getPostType().equals(Post.PostTypeEnum.POST)){
            // Controllo l'esistenza del profilo che vuole commentare il post
            ProfileJpa profileJpa = this.profilesRepository.findActiveById(comment.getProfileId())
                    .orElseThrow(() -> new ProfileNotFoundException(comment.getProfileId()));

            CommentJpa commentJpa = this.commentToCommentJpaConverter.convert(comment);
            assert commentJpa != null;
            commentJpa.setProfile(profileJpa);
            commentJpa.setPost(postJpa);
            commentJpa.setCreatedAt(LocalDateTime.now());

            return this.commentToCommentJpaConverter.convertBack(
                    this.commentsRepository.save(commentJpa)
            );
        }else{
            throw new CommentOnStoryException();
        }

    }

    @Transactional
    public Comment updateCommentById(Long commentId, CommentPatch commentPatch) {
        CommentJpa commentJpa = this.commentsRepository.getReferenceById(commentId);

        commentJpa.setContent(commentPatch.getContent());
        commentJpa.setUpdatedAt(LocalDateTime.now());

        return this.commentToCommentJpaConverter.convertBack(
                this.commentsRepository.save(commentJpa)
        );
    }

    @Transactional
    public void deleteCommentById(Long commentId) {
        CommentJpa commentJpa = this.commentsRepository.getReferenceById(commentId);

        commentJpa.setDeletedAt(LocalDateTime.now());

        this.commentsRepository.save(commentJpa);
    }
    @Transactional
    public List<Comment> findAllCommentsByPostId(Long postId) {
        return this.commentsRepository.findAllActiveByPostId(postId)
                .stream().map(this.commentToCommentJpaConverter::convertBack).toList();
    }

    @Transactional
    public List<Post> profileFeedByProfileId(Long profileId, Boolean onlyPost) {
        if (onlyPost == null){
            return this.postsRepository.getFeedByProfileId(profileId, LocalDateTime.now().minusDays(1))
                    .stream().map(this.postToPostJpaConverter::convertBack).toList();
        } else {
            if (onlyPost) {
                return this.postsRepository.getPostFeedByProfileId(profileId)
                        .stream().map(this.postToPostJpaConverter::convertBack).toList();
            }else {
                return this.postsRepository.getStoryFeedByProfileId(profileId, LocalDateTime.now().minusDays(1))
                        .stream().map(this.postToPostJpaConverter::convertBack).toList();
            }
        }
    }
}
