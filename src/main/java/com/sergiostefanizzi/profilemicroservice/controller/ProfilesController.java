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

import java.util.List;



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
    public ResponseEntity<Void> deleteProfileById(Long profileId, Long selectedUserProfileId) {
        this.profilesService.remove(profileId, selectedUserProfileId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Override
    public ResponseEntity<Profile> updateProfileById(Long profileId,  Long selectedUserProfileId, ProfilePatch profilePatch) {
        Profile updatedProfile = this.profilesService.update(profileId, selectedUserProfileId, profilePatch);
        return new ResponseEntity<>(updatedProfile, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<Profile>> searchProfileByProfileName(String profileName) {
        List<Profile> profileList = this.profilesService.findByProfileName(profileName);
        return new ResponseEntity<>(profileList, HttpStatus.OK);
    }


    @Override
    public ResponseEntity<FullProfile> findProfileById(Long profileId, Long selectedUserProfileId) {
        FullProfile fullProfile = this.profilesService.findFull(profileId, selectedUserProfileId);
        return new ResponseEntity<>(fullProfile, HttpStatus.OK);
    }
}
