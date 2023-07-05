package com.sergiostefanizzi.profilemicroservice.service;

import com.sergiostefanizzi.profilemicroservice.model.*;
import com.sergiostefanizzi.profilemicroservice.model.converter.PostToPostJpaConverter;
import com.sergiostefanizzi.profilemicroservice.model.converter.ProfileToProfileJpaConverter;
import com.sergiostefanizzi.profilemicroservice.repository.PostsRepository;
import com.sergiostefanizzi.profilemicroservice.repository.ProfilesRepository;
import com.sergiostefanizzi.profilemicroservice.system.exception.ProfileAlreadyCreatedException;
import com.sergiostefanizzi.profilemicroservice.system.exception.ProfileNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfilesService {
    private final ProfilesRepository profilesRepository;
    private final PostsRepository postsRepository;
    private final ProfileToProfileJpaConverter profileToProfileJpaConverter;
    private final PostToPostJpaConverter postToPostJpaConverter;

    @Transactional
    public Profile save(@NotNull Profile profile){
        if(this.profilesRepository.findByProfileName(profile.getProfileName()).isPresent()){
            throw new ProfileAlreadyCreatedException(profile.getProfileName());
        }
        ProfileJpa newProfileJpa = this.profileToProfileJpaConverter.convert(profile);
        Objects.requireNonNull(newProfileJpa).setCreatedAt(LocalDateTime.now());
        ProfileJpa savedProfileJpa = this.profilesRepository.save(newProfileJpa);
        log.info("PROFILEJPA NAME ---> "+savedProfileJpa.getProfileName());
        log.info("PROFILEJPA id ---> "+savedProfileJpa.getId());
        return this.profileToProfileJpaConverter.convertBack(savedProfileJpa);
    }

    @Transactional
    public void remove(Long profileId) {
        if (profileId == null){
            throw new ProfileNotFoundException("null");
        }

        // cerco profili che non siano gia' stati eliminati o che non esistano proprio
        ProfileJpa profileJpa = this.profilesRepository.findById(profileId)
                .filter(profile-> profile.getDeletedAt() == null)
                .orElseThrow(() -> new ProfileNotFoundException(profileId));

        //imposto a questo istante la data e l'ora di rimozione del profilo
        profileJpa.setDeletedAt(LocalDateTime.now());
        this.profilesRepository.save(profileJpa);

        log.info("Profile Deleted At -> "+profileJpa.getDeletedAt());
    }

    @Transactional
    public Profile update(Long profileId,@NotNull ProfilePatch profilePatch) {
        if (profileId == null){
            throw new ProfileNotFoundException("null");
        }

        // cerco profili che non siano gia' stati eliminati o che non esistano proprio
        ProfileJpa profileJpa = this.profilesRepository.findById(profileId)
                .filter(profile-> profile.getDeletedAt() == null)
                .orElseThrow(() -> new ProfileNotFoundException(profileId));
        // modifico solo i campi che devono essere aggiornati
        if (StringUtils.hasText(profilePatch.getBio())) profileJpa.setBio(profilePatch.getBio());
        if (StringUtils.hasText(profilePatch.getPictureUrl())) profileJpa.setPictureUrl(profilePatch.getPictureUrl());
        if (profilePatch.getIsPrivate() != null) profileJpa.setIsPrivate(profilePatch.getIsPrivate());

        //imposto a questo istante la data e l'ora di aggiornamento del profilo
        profileJpa.setUpdatedAt(LocalDateTime.now());
        ProfileJpa updatedProfileJpa = this.profilesRepository.save(profileJpa);

        log.info("Profile Updated At -> "+updatedProfileJpa.getUpdatedAt());

        return this.profileToProfileJpaConverter.convertBack(updatedProfileJpa);
    }

    @Transactional
    public List<Profile> findByProfileName(String profileName) {
        return this.profilesRepository.findAllByProfileName(profileName)
                .stream().filter(profileJpa -> profileJpa.getDeletedAt() == null)
                .map(this.profileToProfileJpaConverter::convertBack).toList();

    }


    @Transactional
    public FullProfile findFull(Long profileId) {
        if (profileId == null){
            throw new ProfileNotFoundException("null");
        }
        List<Post> postList = new ArrayList<>();
        int postCount = 0;

        // controllo che il profilo non sia gia' stato eliminato o che non sia mai esistito
        ProfileJpa profileJpa = this.profilesRepository.findById(profileId)
                .filter(profile-> profile.getDeletedAt() == null)
                .orElseThrow(() -> new ProfileNotFoundException(profileId));
        // cerco i post pubblicati dal profilo
        Optional<List<PostJpa>> postJpaList = this.postsRepository.findAllByProfileId(profileId);
        if (postJpaList.isPresent()){
            postList = postJpaList.get().stream().map(this.postToPostJpaConverter::convertBack).collect(Collectors.toList());
            postCount = postList.size();
        }

        //TODO Devo controllare che il posso accedere al profilo

        return new FullProfile(
                this.profileToProfileJpaConverter.convertBack(profileJpa),
                postList,
                postCount,
                true
        );
    }
}
