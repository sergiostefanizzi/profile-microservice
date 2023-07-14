package com.sergiostefanizzi.profilemicroservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@RequiredArgsConstructor
@NoArgsConstructor(force = true)
public class FollowsId implements Serializable {
    @Column(name = "follower_id")
    private final Long followerId;
    @Column(name = "followed_id")
    private final Long followedId;
}
