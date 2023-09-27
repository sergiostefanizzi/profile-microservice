package com.sergiostefanizzi.profilemicroservice.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "Follows")
@Getter
@Setter
@RequiredArgsConstructor
@NoArgsConstructor(force = true)
public class FollowsJpa {
    @EmbeddedId
    private final FollowsId followsId;
    @Column(name = "request_status", nullable = false)
    @Enumerated(EnumType.STRING)
    @NotNull
    private Follows.RequestStatusEnum requestStatus;
    @Column(name = "followed_at")
    @PastOrPresent
    private LocalDateTime followedAt;
    @Column(name = "unfollowed_at")
    @PastOrPresent
    private LocalDateTime unfollowedAt;
    @Version
    @Column(name = "version", nullable = false)
    private Long version;
    @ManyToOne()
    @MapsId("followerId")
    @JoinColumn(name = "follower_id", referencedColumnName = "id")
    private ProfileJpa follower;
    @ManyToOne()
    @MapsId("followedId")
    @JoinColumn(name = "followed_id", referencedColumnName = "id")
    private ProfileJpa followed;
}
