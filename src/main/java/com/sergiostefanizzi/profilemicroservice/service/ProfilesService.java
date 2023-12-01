package com.sergiostefanizzi.profilemicroservice.service;

import com.sergiostefanizzi.profilemicroservice.model.*;
import com.sergiostefanizzi.profilemicroservice.model.converter.PostToPostJpaConverter;
import com.sergiostefanizzi.profilemicroservice.model.converter.ProfileToProfileJpaConverter;
import com.sergiostefanizzi.profilemicroservice.repository.FollowsRepository;
import com.sergiostefanizzi.profilemicroservice.repository.PostsRepository;
import com.sergiostefanizzi.profilemicroservice.repository.ProfilesRepository;
import com.sergiostefanizzi.profilemicroservice.system.exception.NotInProfileListException;
import com.sergiostefanizzi.profilemicroservice.system.exception.ProfileAlreadyCreatedException;
import com.sergiostefanizzi.profilemicroservice.system.exception.ProfileNotFoundException;
import com.sergiostefanizzi.profilemicroservice.system.util.JwtUtilityClass;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static com.sergiostefanizzi.profilemicroservice.system.util.JwtUtilityClass.getJwtAccountId;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfilesService {
    private final ProfilesRepository profilesRepository;
    private final PostsRepository postsRepository;
    private final FollowsRepository followsRepository;
    private final ProfileToProfileJpaConverter profileToProfileJpaConverter;
    private final PostToPostJpaConverter postToPostJpaConverter;
    private final KeycloakService keycloakService;

    private void checkProfileListAndIds(Long profileId, Long selectedUserProfileId) {
        if (!Objects.equals(profileId, selectedUserProfileId) || (Boolean.FALSE.equals(JwtUtilityClass.isInProfileListJwt(selectedUserProfileId)) && (Boolean.FALSE.equals(this.keycloakService.isInProfileList(getJwtAccountId(), selectedUserProfileId))))){
            throw new NotInProfileListException(selectedUserProfileId);
        }
    }

    private void checkProfileList(Long selectedUserProfileId) {
        if (Boolean.FALSE.equals(JwtUtilityClass.isInProfileListJwt(selectedUserProfileId)) && Boolean.FALSE.equals(this.keycloakService.isInProfileList(getJwtAccountId(), selectedUserProfileId))){
            throw new NotInProfileListException(selectedUserProfileId);
        }
    }

    @Transactional
    public Profile save(@NotNull Profile profile){
        if(this.profilesRepository.checkActiveByProfileName(profile.getProfileName()).isPresent()){
            throw new ProfileAlreadyCreatedException(profile.getProfileName());
        }
        String accountId = getJwtAccountId();
        profile.setAccountId(accountId);

        ProfileJpa newProfileJpa = this.profileToProfileJpaConverter.convert(profile);
        Objects.requireNonNull(newProfileJpa).setCreatedAt(LocalDateTime.now());

        ProfileJpa savedProfile = this.profilesRepository.save(newProfileJpa);

        this.keycloakService.updateProfileList(accountId, savedProfile.getId());

        return this.profileToProfileJpaConverter.convertBack(savedProfile);
    }

    @Transactional
    public void remove(Long profileId, Long selectedUserProfileId) {
        checkProfileListAndIds(profileId, selectedUserProfileId);

        ProfileJpa profileJpa = this.profilesRepository.getReferenceById(profileId);
        profileJpa.setDeletedAt(LocalDateTime.now());

        this.keycloakService.removeFromProfileList(getJwtAccountId(), profileId);

        this.profilesRepository.save(profileJpa);
    }



    @Transactional
    public Profile update(Long profileId, Long selectedUserProfileId, @NotNull ProfilePatch profilePatch) {
        checkProfileListAndIds(profileId, selectedUserProfileId);


        ProfileJpa profileJpa = this.profilesRepository.getReferenceById(profileId);
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
        if (!StringUtils.hasText(profileName)){
            throw new ProfileNotFoundException("Missing input parameter");
        }

        return this.profilesRepository.findAllActiveByProfileName(profileName)
                .stream().map(this.profileToProfileJpaConverter::convertBack).toList();

    }


    @Transactional
    public FullProfile findFull(Long profileId, Long selectedUserProfileId) {
        checkProfileList(selectedUserProfileId);

        ProfileJpa profileJpa = this.profilesRepository.getReferenceById(profileId);


        // cerco i post pubblicati dal profilo
        List<PostJpa> postJpaList = this.postsRepository.findAllActiveByProfileId(profileId);

        List<Post> postList = postJpaList.stream().map(this.postToPostJpaConverter::convertBack).toList();
        if(!Objects.equals(profileId, selectedUserProfileId) && (profileJpa.getIsPrivate() && (this.followsRepository.findActiveAcceptedById(new FollowsId(selectedUserProfileId, profileId)).isEmpty()))){ // selectedUserProfileId non segue profileId privato
                return new FullProfile(
                        this.profileToProfileJpaConverter.convertBack(profileJpa),
                        List.of(),
                        postList.size(),
                        false
                );


        }

        return new FullProfile(
                this.profileToProfileJpaConverter.convertBack(profileJpa),
                postList,
                postList.size(),
                true
        );

    }



}
