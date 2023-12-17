package com.sergiostefanizzi.profilemicroservice.service;

import com.sergiostefanizzi.profilemicroservice.model.*;
import com.sergiostefanizzi.profilemicroservice.model.converter.AlertToAlertJpaConverter;
import com.sergiostefanizzi.profilemicroservice.model.converter.ProfileToProfileJpaConverter;
import com.sergiostefanizzi.profilemicroservice.repository.AlertsRepository;
import com.sergiostefanizzi.profilemicroservice.repository.ProfilesRepository;
import com.sergiostefanizzi.profilemicroservice.system.exception.AlertStatusNotValidException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
        if (Boolean.TRUE.equals(removedProfile)){
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

    @Transactional
    public Alert updateAlertById(Long alertId, AlertPatch alertPatch) {
        AlertJpa alertJpa = this.alertsRepository.getReferenceById(alertId);
        alertJpa.setManagedByAccount(alertPatch.getManagedBy());
        alertJpa.setClosedAt(LocalDateTime.now());
        return this.alertToAlertJpaConverter.convertBack(
                this.alertsRepository.save(alertJpa)
        );
    }

    @Transactional
    public List<Alert> findAllAlerts(String alertStatus) {
        return getAlertListByStatus(alertStatus).stream().map(this.alertToAlertJpaConverter::convertBack).toList();
    }

    private List<AlertJpa> getAlertListByStatus(String alertStatus) {
        List<AlertJpa> alertList;
        if (alertStatus != null){
            if(alertStatus.equalsIgnoreCase("O")){
                alertList = this.alertsRepository.findAllOpenAlerts();
                log.info("Read all the open alerts");
            } else if (alertStatus.equalsIgnoreCase("C")) {
                alertList = this.alertsRepository.findAllClosedAlerts();
                log.info("Read all the closed alerts");
            }else{
                throw new AlertStatusNotValidException();
            }
        }else {
            // return open and closed alerts
            alertList = this.alertsRepository.findAll();
            log.info("Read all the alerts");
        }
        return alertList;
    }
}
