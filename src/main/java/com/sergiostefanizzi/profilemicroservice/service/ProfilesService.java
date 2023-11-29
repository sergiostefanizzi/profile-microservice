package com.sergiostefanizzi.profilemicroservice.service;

import com.sergiostefanizzi.profilemicroservice.model.*;
import com.sergiostefanizzi.profilemicroservice.model.converter.PostToPostJpaConverter;
import com.sergiostefanizzi.profilemicroservice.model.converter.ProfileToProfileJpaConverter;
import com.sergiostefanizzi.profilemicroservice.repository.*;
import com.sergiostefanizzi.profilemicroservice.system.exception.FollowNotFoundException;
import com.sergiostefanizzi.profilemicroservice.system.exception.NotInProfileListException;
import com.sergiostefanizzi.profilemicroservice.system.exception.ProfileAlreadyCreatedException;
import com.sergiostefanizzi.profilemicroservice.system.exception.ProfileNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
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
    private final FollowsRepository followsRepository;
    private final ProfileToProfileJpaConverter profileToProfileJpaConverter;
    private final PostToPostJpaConverter postToPostJpaConverter;
    private final KeycloakService keycloakService;

    private static String getJwtAccountId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        JwtAuthenticationToken oauthToken = (JwtAuthenticationToken) authentication;
        String jwtAccountId = oauthToken.getToken().getClaim("sub");
        log.info("TOKEN ACCOUNT ID --> "+jwtAccountId);
        return jwtAccountId;
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
    public void remove(Long profileId) {
        ProfileJpa profileJpa = this.profilesRepository.getReferenceById(profileId);
        profileJpa.setDeletedAt(LocalDateTime.now());

        this.keycloakService.removeFromProfileList(getJwtAccountId(), profileId);

        this.profilesRepository.save(profileJpa);

    }

    @Transactional
    public Profile update(Long profileId,@NotNull ProfilePatch profilePatch) {
        // cerco profili che non siano gia' stati eliminati o che non esistano proprio
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
        List<Post> postList = new ArrayList<>();
        Boolean isProfileGranted = true;


        // controllo che il profilo non sia gia' stato eliminato o che non sia mai esistito
        ProfileJpa profileJpa = this.profilesRepository.getReferenceById(profileId);
        // cerco i post pubblicati dal profilo

        if (profileJpa.getIsPrivate()){
            if(this.keycloakService.isInProfileList(getJwtAccountId(), selectedUserProfileId)){
                if (this.followsRepository.findActiveAcceptedById(new FollowsId(selectedUserProfileId, profileId)).isEmpty()){
                    isProfileGranted = false;
                }
            } else {
                throw new NotInProfileListException(selectedUserProfileId);
            }
        }

        Optional<List<PostJpa>> postJpaList = this.postsRepository.findAllActiveByProfileId(profileId);
        if (postJpaList.isPresent()){
            postList = postJpaList.get().stream().map(this.postToPostJpaConverter::convertBack).collect(Collectors.toList());
        }



        return new FullProfile(
                this.profileToProfileJpaConverter.convertBack(profileJpa),
                postList,
                postList.size(),
                isProfileGranted
        );
    }
}
