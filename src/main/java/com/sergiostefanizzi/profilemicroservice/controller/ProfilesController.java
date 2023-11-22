package com.sergiostefanizzi.profilemicroservice.controller;

import com.sergiostefanizzi.profilemicroservice.api.ProfilesApi;
import com.sergiostefanizzi.profilemicroservice.model.FullProfile;
import com.sergiostefanizzi.profilemicroservice.model.Profile;
import com.sergiostefanizzi.profilemicroservice.model.ProfilePatch;
import com.sergiostefanizzi.profilemicroservice.service.ProfilesService;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ProfilesController implements ProfilesApi {
    private final ProfilesService profilesService;

    @Override
    public ResponseEntity<Profile> addProfile(Profile profile) {
        Profile savedProfile = this.profilesService.save(profile);
        return new ResponseEntity<>(savedProfile, HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<Void> deleteProfileById(Long profileId) {
        this.profilesService.remove(profileId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Override
    public ResponseEntity<Profile> updateProfileById(Long profileId, ProfilePatch profilePatch) {
        Profile updatedProfile = this.profilesService.update(profileId, profilePatch);
        return new ResponseEntity<>(updatedProfile, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<Profile>> searchProfileByProfileName(String profileName) {
        List<Profile> profileList = this.profilesService.findByProfileName(profileName);
        return new ResponseEntity<>(profileList, HttpStatus.OK);
    }


    @Override
    public ResponseEntity<FullProfile> findProfileById(Long profileId, @NotNull Long requestProfileId) {
        FullProfile fullProfile = this.profilesService.findFull(profileId, requestProfileId);
        return new ResponseEntity<>(fullProfile, HttpStatus.OK);
    }
}
