package com.sergiostefanizzi.profilemicroservice.service;

import com.sergiostefanizzi.profilemicroservice.model.Profile;
import com.sergiostefanizzi.profilemicroservice.model.ProfileJpa;
import com.sergiostefanizzi.profilemicroservice.model.ProfilePatch;
import com.sergiostefanizzi.profilemicroservice.model.converter.ProfileToProfileJpaConverter;
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
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfilesService {
    private final ProfilesRepository profilesRepository;
    private final ProfileToProfileJpaConverter profileToProfileJpaConverter;

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
    public Profile findByProfileName(String profileName) {
        if (profileName.isBlank()){
            throw new ProfileNotFoundException("null");
        }

        return this.profileToProfileJpaConverter.convertBack(
                this.profilesRepository.findByProfileName(profileName)
                .filter(profile-> profile.getDeletedAt() == null)
                .orElseThrow(() -> new ProfileNotFoundException(profileName)));
    }
}
