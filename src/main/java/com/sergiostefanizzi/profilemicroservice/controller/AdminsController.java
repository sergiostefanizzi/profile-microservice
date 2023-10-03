package com.sergiostefanizzi.profilemicroservice.controller;

import com.sergiostefanizzi.profilemicroservice.api.AdminsApi;
import com.sergiostefanizzi.profilemicroservice.model.*;
import com.sergiostefanizzi.profilemicroservice.service.AdminsService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class AdminsController implements AdminsApi {
    private final AdminsService adminsService;

    @Override
    public ResponseEntity<Profile> blockProfileById(Long profileId, ProfileAdminPatch profileAdminPatch) {
        Profile updatedProfile = this.adminsService.blockProfileById(profileId, profileAdminPatch);
        return new ResponseEntity<>(updatedProfile, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Alert> findAlertsById(Long alertId) {
        return AdminsApi.super.findAlertsById(alertId);
    }

    @Override
    public ResponseEntity<List<Alert>> findAllAlerts(Boolean closedAlerts, Boolean adminId) {
        return AdminsApi.super.findAllAlerts(closedAlerts, adminId);
    }

    @Override
    public ResponseEntity<List<Profile>> findAllProfiles(Boolean removedProfile) {
        List<Profile> profileList = this.adminsService.findAllProfiles(removedProfile);
        return new ResponseEntity<>(profileList, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Alert> updateAlertById(Long alertId, AlertPatch alertPatch) {
        return AdminsApi.super.updateAlertById(alertId, alertPatch);
    }
}
