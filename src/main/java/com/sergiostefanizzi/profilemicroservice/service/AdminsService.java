package com.sergiostefanizzi.profilemicroservice.service;

import com.sergiostefanizzi.profilemicroservice.model.Alert;
import com.sergiostefanizzi.profilemicroservice.model.Profile;
import com.sergiostefanizzi.profilemicroservice.model.ProfileAdminPatch;
import com.sergiostefanizzi.profilemicroservice.model.ProfileJpa;
import com.sergiostefanizzi.profilemicroservice.model.converter.AlertToAlertJpaConverter;
import com.sergiostefanizzi.profilemicroservice.model.converter.ProfileToProfileJpaConverter;
import com.sergiostefanizzi.profilemicroservice.repository.AlertsRepository;
import com.sergiostefanizzi.profilemicroservice.repository.ProfilesRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminsService {
    private final ProfilesRepository profilesRepository;
    private final ProfileToProfileJpaConverter profileToProfileJpaConverter;
    private final AlertsRepository alertsRepository;
    private final AlertToAlertJpaConverter alertToAlertJpaConverter;
    @Transactional
    public Profile blockProfileById(Long profileId, ProfileAdminPatch profileAdminPatch) {
        ProfileJpa profileToUpdate = this.profilesRepository.getReferenceById(profileId);


        if(profileAdminPatch.getBlockedUntil() != null){
            //blocco
            profileToUpdate.setBlockedUntil(profileAdminPatch.getBlockedUntil().toLocalDateTime());
        }else {
            //sblocco
            profileToUpdate.setBlockedUntil(null);
        }

        return this.profileToProfileJpaConverter.convertBack(
                this.profilesRepository.save(profileToUpdate)
        );
    }

    @Transactional
    public List<Profile> findAllProfiles(Boolean removedProfile) {
        List<ProfileJpa> profileList;
        if (removedProfile){
            profileList = this.profilesRepository.findAll();
        }else {
            profileList = this.profilesRepository.findAllActiveProfiles();
        }
        return profileList.stream().map(this.profileToProfileJpaConverter::convertBack).toList();
    }

    @Transactional
    public Alert findAlertById(Long alertId) {
        return this.alertToAlertJpaConverter.convertBack(
                this.alertsRepository.getReferenceById(alertId)
        );
    }
}
