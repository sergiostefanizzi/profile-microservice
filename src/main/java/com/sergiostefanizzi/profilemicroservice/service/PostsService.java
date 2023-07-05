package com.sergiostefanizzi.profilemicroservice.service;

import com.sergiostefanizzi.profilemicroservice.model.*;
import com.sergiostefanizzi.profilemicroservice.model.converter.LikeToLikeJpaConverter;
import com.sergiostefanizzi.profilemicroservice.model.converter.PostToPostJpaConverter;
import com.sergiostefanizzi.profilemicroservice.repository.LikesRepository;
import com.sergiostefanizzi.profilemicroservice.repository.PostsRepository;
import com.sergiostefanizzi.profilemicroservice.repository.ProfilesRepository;
import com.sergiostefanizzi.profilemicroservice.system.exception.PostNotFoundException;
import com.sergiostefanizzi.profilemicroservice.system.exception.ProfileNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
    private final LikeToLikeJpaConverter likeToLikeJpaConverter;

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
        if(postId == null){
            throw new PostNotFoundException("null");
        }
        // Controllo prima l'esistenza del post
        PostJpa postJpa = this.postsRepository.findById(postId)
                .filter(post -> post.getDeletedAt() == null)
                .orElseThrow(() -> new PostNotFoundException(postId));
        // TODO mi serve il JWT
        // Controllo che chi richiede la rimozione abbia l'autorizzazione per farlo
        /*
        if (postJpa.getProfile().getId().equals(Long.MIN_VALUE)){
            throw new PostNotFoundException(postId);
        }
        */

        postJpa.setDeletedAt(LocalDateTime.now());

        log.info("Post Deleted At -> "+postJpa.getDeletedAt());

        this.postsRepository.save(postJpa);
    }

    @Transactional
    public Post update(Long postId, PostPatch postPatch) {
        if(postId == null){
            throw new PostNotFoundException("null");
        }

        // Controllo prima l'esistenza del post
        PostJpa postJpa = this.postsRepository.findById(postId)
                .filter(post -> post.getDeletedAt() == null)
                .orElseThrow(() -> new PostNotFoundException(postId));
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
        if(postId == null){
            throw new PostNotFoundException("null");
        }

        return this.postToPostJpaConverter.convertBack(
                this.postsRepository.findById(postId)
                .filter(post -> post.getDeletedAt() == null)
                .orElseThrow(() -> new PostNotFoundException(postId))
        );
    }

    @Transactional
    public void addLike(Boolean removeLike, Like like) {
        // TODO mi serve il JWT
        // Controllo che chi vuole mettere il abbia l'autorizzazione per farlo
        // sia controllando il profilo
        // sia controllando che il profilo a cui appartiene il posto sia visibile da chi vuole mettere like

        // Controllo prima l'esistenza del post
        PostJpa postJpa = this.postsRepository.findById(like.getPostId())
                .filter(post -> post.getDeletedAt() == null)
                .orElseThrow(() -> new PostNotFoundException(like.getPostId()));
        // Controllo poi l'esistenza del profilo di chi vuole mettere like
        ProfileJpa profileJpa = this.profilesRepository.findById(like.getProfileId())
                .filter(profile -> profile.getDeletedAt() == null)
                .orElseThrow(() -> new ProfileNotFoundException(like.getProfileId()));

        // Controllo l'esistenza del like, se non esiste lo creo con il like ottenuto dal controller
        Optional<LikeJpa> optionalLikeJpa = this.likesRepository.findById(new LikeId(profileJpa.getId(), postJpa.getId()));
        if (optionalLikeJpa.isPresent() && removeLike){
            LikeJpa likeJpa = optionalLikeJpa.get();
            likeJpa.setDeletedAt(LocalDateTime.now());
            this.likesRepository.save(likeJpa);
        } else if (optionalLikeJpa.isEmpty() && !removeLike){
            LikeJpa likeJpa = this.likeToLikeJpaConverter.convert(like);
            assert likeJpa != null;
            likeJpa.setCreatedAt(LocalDateTime.now());
            likeJpa.setDeletedAt(null);
            likeJpa.setProfile(profileJpa);
            likeJpa.setPost(postJpa);
            this.likesRepository.save(likeJpa);
        }
                //.orElseGet(() -> this.likeToLikeJpaConverter.convert(like));


    }


}
