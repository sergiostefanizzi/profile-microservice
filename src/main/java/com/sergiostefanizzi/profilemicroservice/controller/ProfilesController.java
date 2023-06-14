package com.sergiostefanizzi.profilemicroservice.controller;

import com.sergiostefanizzi.profilemicroservice.api.ProfilesApi;
import com.sergiostefanizzi.profilemicroservice.model.FullProfile;
import com.sergiostefanizzi.profilemicroservice.model.Profile;
import com.sergiostefanizzi.profilemicroservice.model.ProfilePatch;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.Optional;

@RestController
public class ProfilesController implements ProfilesApi {
    @Override
    public Optional<NativeWebRequest> getRequest() {
        return ProfilesApi.super.getRequest();
    }

    @Override
    public ResponseEntity<Profile> addProfile(Profile profile) {
        return ProfilesApi.super.addProfile(profile);
    }

    @Override
    public ResponseEntity<Void> deleteProfileById(Long profileId) {
        return ProfilesApi.super.deleteProfileById(profileId);
    }

    @Override
    public ResponseEntity<FullProfile> findProfileById(Long profileId) {
        return ProfilesApi.super.findProfileById(profileId);
    }

    @Override
    public ResponseEntity<Profile> searchProfileByProfileName(@NotNull String profileName) {
        return ProfilesApi.super.searchProfileByProfileName(profileName);
    }

    @Override
    public ResponseEntity<Profile> updateProfileById(Long profileId, ProfilePatch profilePatch) {
        return ProfilesApi.super.updateProfileById(profileId, profilePatch);
    }
}
