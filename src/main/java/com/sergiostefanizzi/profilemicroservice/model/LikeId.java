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
public class LikeId implements Serializable {
    @Column(name = "profile_id")
    private final Long profileId;
    @Column(name = "post_id")
    private final Long postId;
}
