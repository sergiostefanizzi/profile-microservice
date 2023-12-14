package com.sergiostefanizzi.profilemicroservice.controller;

import com.sergiostefanizzi.profilemicroservice.api.FollowsApi;
import com.sergiostefanizzi.profilemicroservice.model.Follows;
import com.sergiostefanizzi.profilemicroservice.model.Profile;
import com.sergiostefanizzi.profilemicroservice.model.ProfileFollowList;
import com.sergiostefanizzi.profilemicroservice.service.FollowsService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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
    public ResponseEntity<Follows> addFollows(Long profileId, Long followsId, Long selectedUserProfileId, Boolean unfollow) {
        Follows follows = this.followsService.addFollows(profileId, followsId, selectedUserProfileId, unfollow);
        return new ResponseEntity<>(follows, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Follows> acceptFollows(Long profileId, Long followerId, Long selectedUserProfileId, Boolean rejectFollow) {
        Follows follows = this.followsService.acceptFollows(profileId, followerId, selectedUserProfileId, rejectFollow);
        return new ResponseEntity<>(follows, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<ProfileFollowList> findAllFollowers(Long profileId, Long selectedUserProfileId) {
        ProfileFollowList profileFollowList = this.followsService.findAllFollowers(profileId, selectedUserProfileId);
        return new ResponseEntity<>(profileFollowList, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<ProfileFollowList> findAllFollowings(Long profileId, Long selectedUserProfileId) {
        ProfileFollowList profileFollowList = this.followsService.findAllFollowings(profileId, selectedUserProfileId);
        return new ResponseEntity<>(profileFollowList, HttpStatus.OK);
    }
}
