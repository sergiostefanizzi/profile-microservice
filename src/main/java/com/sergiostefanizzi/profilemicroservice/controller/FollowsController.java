package com.sergiostefanizzi.profilemicroservice.controller;

import com.sergiostefanizzi.profilemicroservice.api.FollowsApi;
import com.sergiostefanizzi.profilemicroservice.model.Follows;
import com.sergiostefanizzi.profilemicroservice.model.Profile;
import com.sergiostefanizzi.profilemicroservice.service.FollowsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@Slf4j
public class FollowsController implements FollowsApi {
    private final FollowsService followsService;
    @Override
    public Optional<NativeWebRequest> getRequest() {
        return FollowsApi.super.getRequest();
    }

    @Override
    public ResponseEntity<Void> acceptFollows(Long profileId, Long followerId) {
        return FollowsApi.super.acceptFollows(profileId, followerId);
    }

    @Override
    public ResponseEntity<Follows> addFollows(Long profileId, Long followsId) {
        return FollowsApi.super.addFollows(profileId, followsId);
    }

    @Override
    public ResponseEntity<Void> deleteFollower(Long profileId, Long followerId) {
        return FollowsApi.super.deleteFollower(profileId, followerId);
    }

    @Override
    public ResponseEntity<Void> deleteFollowing(Long profileId, Long followsId) {
        return FollowsApi.super.deleteFollowing(profileId, followsId);
    }

    @Override
    public ResponseEntity<List<Profile>> findAllFollowers(Long profileId) {
        return FollowsApi.super.findAllFollowers(profileId);
    }

    @Override
    public ResponseEntity<List<Profile>> findAllFollowings(Long profileId) {
        return FollowsApi.super.findAllFollowings(profileId);
    }
}
