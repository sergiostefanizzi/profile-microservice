package com.sergiostefanizzi.profilemicroservice.service;

import com.sergiostefanizzi.profilemicroservice.model.Post;
import com.sergiostefanizzi.profilemicroservice.model.PostJpa;
import com.sergiostefanizzi.profilemicroservice.model.ProfileJpa;
import com.sergiostefanizzi.profilemicroservice.model.converter.PostToPostJpaConverter;
import com.sergiostefanizzi.profilemicroservice.repository.PostsRepository;
import com.sergiostefanizzi.profilemicroservice.repository.ProfilesRepository;
import com.sergiostefanizzi.profilemicroservice.system.exception.ProfileAlreadyCreatedException;
import com.sergiostefanizzi.profilemicroservice.system.exception.ProfileNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostsService {
    private final PostsRepository postsRepository;
    private final PostToPostJpaConverter postToPostJpaConverter;
    // TODO Per adesso uso il profileRepository per il controllo del profileId, ma in seguito serve il JWT
    private final ProfilesRepository profilesRepository;

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
    //TODO aggiungere converte DTO JPA
}
