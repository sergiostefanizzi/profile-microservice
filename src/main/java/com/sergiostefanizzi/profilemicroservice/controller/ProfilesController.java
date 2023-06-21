package com.sergiostefanizzi.profilemicroservice.controller;

import com.sergiostefanizzi.profilemicroservice.api.ProfilesApi;
import com.sergiostefanizzi.profilemicroservice.model.FullProfile;
import com.sergiostefanizzi.profilemicroservice.model.Profile;
import com.sergiostefanizzi.profilemicroservice.model.ProfilePatch;
import com.sergiostefanizzi.profilemicroservice.service.ProfilesService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.Optional;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ProfilesController implements ProfilesApi {
    private final ProfilesService profilesService;
    @Override
    public Optional<NativeWebRequest> getRequest() {
        return ProfilesApi.super.getRequest();
    }

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
    public ResponseEntity<FullProfile> findProfileById(Long profileId) {
        return new ResponseEntity<>(HttpStatus.OK);
    }
    /*
    @Override
    public ResponseEntity<Profile> searchProfileByProfileName(@NotNull String profileName) {
        return new ResponseEntity<>(HttpStatus.OK);
    }
     */


}
