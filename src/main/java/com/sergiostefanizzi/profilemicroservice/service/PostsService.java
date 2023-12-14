package com.sergiostefanizzi.profilemicroservice.service;

import com.sergiostefanizzi.profilemicroservice.model.*;
import com.sergiostefanizzi.profilemicroservice.model.converter.CommentToCommentJpaConverter;
import com.sergiostefanizzi.profilemicroservice.model.converter.LikeToLikeJpaConverter;
import com.sergiostefanizzi.profilemicroservice.model.converter.PostToPostJpaConverter;
import com.sergiostefanizzi.profilemicroservice.repository.*;
import com.sergiostefanizzi.profilemicroservice.system.exception.*;
import com.sergiostefanizzi.profilemicroservice.system.util.JwtUtilityClass;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.sergiostefanizzi.profilemicroservice.system.util.JwtUtilityClass.*;


@Service
@RequiredArgsConstructor
@Slf4j
public class PostsService {
    private final PostsRepository postsRepository;
    private final PostToPostJpaConverter postToPostJpaConverter;
    private final ProfilesRepository profilesRepository;
    private final LikesRepository likesRepository;
    private final CommentsRepository commentsRepository;
    private final FollowsRepository followsRepository;
    private final LikeToLikeJpaConverter likeToLikeJpaConverter;
    private final CommentToCommentJpaConverter commentToCommentJpaConverter;
    private final KeycloakService keycloakService;

    private void checkPostAccess(ProfileJpa postOwnerProfileJpa, Long selectedUserProfileId, Long postId) {
        if (postOwnerProfileJpa.getIsPrivate() && (!Objects.equals(postOwnerProfileJpa.getId(), selectedUserProfileId))){
            FollowsId checkId = this.followsRepository.findActiveAcceptedById(new FollowsId(selectedUserProfileId, postOwnerProfileJpa.getId()))
                    .orElseThrow(() ->  new PostAccessForbiddenException(postId));
            log.info("Id "+checkId.getFollowedId()+" e' seguito da ID "+ selectedUserProfileId);
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
    public Post save(Post post) {
        checkProfileList(post.getProfileId(), this.keycloakService);


        // il profileId e' valido quindi posso fare la conversione in postJpa
        PostJpa postJpa = this.postToPostJpaConverter.convert(post);

        assert postJpa != null;
        postJpa.setProfile(this.profilesRepository.getReferenceById(post.getProfileId()));
        postJpa.setCreatedAt(LocalDateTime.now());
        return this.postToPostJpaConverter.convertBack(
                this.postsRepository.save(postJpa)
        );
    }

    @Transactional
    public void remove(Long postId, Long selectedUserProfileId) {
        PostJpa postJpaToRemove = this.postsRepository.getReferenceById(postId);
        checkProfileListAndIds(postJpaToRemove.getProfile().getId(), selectedUserProfileId, this.keycloakService);

        postJpaToRemove.setDeletedAt(LocalDateTime.now());
        this.postsRepository.save(postJpaToRemove);
    }

    @Transactional
    public Post update(Long postId, Long selectedUserProfileId, PostPatch postPatch) {
        PostJpa postJpa = this.postsRepository.getReferenceById(postId);
        checkProfileListAndIds(postJpa.getProfile().getId(), selectedUserProfileId, this.keycloakService);

        postJpa.setCaption(postPatch.getCaption());
        postJpa.setUpdatedAt(LocalDateTime.now());
        return this.postToPostJpaConverter.convertBack(
                this.postsRepository.save(postJpa)
        );
    }

    @Transactional
    public Post find(Long postId, Long selectedUserProfileId) {
        checkProfileList(selectedUserProfileId, this.keycloakService);

        PostJpa postJpa = this.postsRepository.getReferenceById(postId);
        ProfileJpa profileJpa = postJpa.getProfile();
        // Se il profilo del post e' pubblico, il post puo' essere visto liberamente
        // Se è privato controllo prima che il profile che ha pubblicato il post appartiene a chi ha inviato la richiesta
        // Se non appartiene, controllo infine se chi ha inviato la richiesta segue il profilo privato che ha pubblicato il post
        checkPostAccess(profileJpa, selectedUserProfileId, postId);


        return this.postToPostJpaConverter.convertBack(
                postJpa
        );
    }

    @Transactional
    public void addLike(Boolean removeLike, Like like) {
        Long postId = like.getPostId();
        Long selectedUserProfileId = like.getProfileId();
        // Controllo poi l'esistenza del profilo di chi vuole mettere like

        ProfileJpa selectedProfileJpa = this.profilesRepository.findActiveById(selectedUserProfileId)
                .orElseThrow(() -> new ProfileNotFoundException(selectedUserProfileId));



        // Controllo che chi vuole mettere il like sia presente all'interno del jwt
        checkProfileList(like.getProfileId(), this.keycloakService);

        // Controllo prima l'esistenza del post
        PostJpa postJpa = this.postsRepository.findActiveById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));
        // Controllo che l'autore del post non sia stato eliminato o bloccato
        ProfileJpa postOwnerProfileJpa = this.profilesRepository.findActiveByPostId(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));


        // se il post appartiene a un profilo privato oppure non appartiene a chi ha messo il like
        // controllo che chi mette like segua regolarmente l'autore del post
        checkPostAccess(postOwnerProfileJpa, selectedUserProfileId, postId);

        // Controllo l'esistenza del like, se non esiste lo creo con il like ottenuto dal controller
        Optional<LikeJpa> optionalLikeJpa = this.likesRepository.findActiveById(new LikeId(selectedUserProfileId, postId));
        if (optionalLikeJpa.isPresent() && Boolean.TRUE.equals(removeLike)){
            removeLike(optionalLikeJpa.get());
        } else if (optionalLikeJpa.isEmpty() && Boolean.TRUE.equals(!removeLike)){
            createLike(like, selectedProfileJpa, postJpa);
        }
    }



    @Transactional
    public List<Like> findAllLikesByPostId(Long postId, Long selectedUserProfileId) {
        // Controllo che questo profilo sia all'interno del jwt
        checkProfileList(selectedUserProfileId, this.keycloakService);

        ProfileJpa postOwnerProfileJpa = this.profilesRepository.findActiveByPostId(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));

        checkPostAccess(postOwnerProfileJpa, selectedUserProfileId, postId);


        return this.likesRepository.findAllActiveByPostId(postId)
                .stream().map(this.likeToLikeJpaConverter::convertBack).toList();
    }

    @Transactional
    public Comment addComment(Comment comment) {
        Long selectedUserProfileId = comment.getProfileId();
        Long postId = comment.getPostId();
        //controllo l'esistenza di chi vuole commentare

        ProfileJpa selectedProfileJpa = this.profilesRepository.findActiveById(selectedUserProfileId)
                .orElseThrow(() -> new ProfileNotFoundException(selectedUserProfileId));


        // Controllo che questo profilo sia all'interno del jwt
        checkProfileList(selectedUserProfileId, this.keycloakService);

        // Controllo prima l'esistenza del post
        PostJpa postJpa = this.postsRepository.findActiveById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));

        if (postJpa.getPostType().equals(Post.PostTypeEnum.POST)){

            // Controllo l'esistenza del profilo che vuole commentare il post
            ProfileJpa postOwnerProfileJpa = this.profilesRepository.findActiveByPostId(postId)
                    .orElseThrow(() -> new PostNotFoundException(postId));

            checkPostAccess(postOwnerProfileJpa, selectedUserProfileId, postId);

            CommentJpa commentJpa = this.commentToCommentJpaConverter.convert(comment);
            assert commentJpa != null;
            commentJpa.setProfile(selectedProfileJpa);
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
    public Comment updateCommentById(Long commentId, Long selectedUserProfileId, CommentPatch commentPatch) {
        CommentJpa commentJpa = this.commentsRepository.getReferenceById(commentId);

        checkProfileListAndIds(commentJpa.getProfile().getId(), selectedUserProfileId, this.keycloakService);


        commentJpa.setContent(commentPatch.getContent());
        commentJpa.setUpdatedAt(LocalDateTime.now());

        return this.commentToCommentJpaConverter.convertBack(
                this.commentsRepository.save(commentJpa)
        );
    }

    @Transactional
    public void deleteCommentById(Long commentId, Long selectedUserProfileId) {
        CommentJpa commentJpa = this.commentsRepository.getReferenceById(commentId);

        checkProfileListAndIds(commentJpa.getProfile().getId(), selectedUserProfileId, this.keycloakService);

        commentJpa.setDeletedAt(LocalDateTime.now());

        this.commentsRepository.save(commentJpa);
    }
    @Transactional
    public List<Comment> findAllCommentsByPostId(Long postId, Long selectedUserProfileId) {
        checkProfileList(selectedUserProfileId, this.keycloakService);

        ProfileJpa postOwnerProfileJpa = this.profilesRepository.findActiveByPostId(postId)
                        .orElseThrow(() -> new PostNotFoundException(postId));

        checkPostAccess(postOwnerProfileJpa, selectedUserProfileId, postId);

        return this.commentsRepository.findAllActiveByPostId(postId)
                .stream().map(this.commentToCommentJpaConverter::convertBack).toList();
    }

    @Transactional
    public List<Post> profileFeedByProfileId(Long profileId, Long selectedUserProfileId, Boolean onlyPost) {
        checkProfileListAndIds(profileId, selectedUserProfileId, this.keycloakService);

        if (onlyPost == null){
            return this.postsRepository.getFeedByProfileId(profileId)
                    .stream().map(this.postToPostJpaConverter::convertBack).toList();
        } else {
            if (onlyPost) {
                return this.postsRepository.getPostFeedByProfileId(profileId)
                        .stream().map(this.postToPostJpaConverter::convertBack).toList();
            }else {
                return this.postsRepository.getStoryFeedByProfileId(profileId)
                        .stream().map(this.postToPostJpaConverter::convertBack).toList();
            }
        }
    }
}
