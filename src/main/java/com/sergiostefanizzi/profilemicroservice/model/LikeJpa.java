package com.sergiostefanizzi.profilemicroservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;


import java.time.LocalDateTime;

@Entity
@Table(name = "Likes")
@Getter
@Setter
@RequiredArgsConstructor
@NoArgsConstructor(force = true)
public class LikeJpa{
    @EmbeddedId
    private final LikeId likesId;
    @Column(name = "created_at", nullable = false)
    @PastOrPresent
    @NotNull
    private LocalDateTime createdAt;
    @Column(name = "deleted_at")
    @PastOrPresent
    private LocalDateTime deletedAt;
    @Column(name = "version", nullable = false)
    @Version
    private Long version;
    @ManyToOne
    @MapsId("profileId")
    @JoinColumn(name = "profile_id", referencedColumnName = "id")
    private ProfileJpa profile;
    @ManyToOne
    @MapsId("postId")
    @JoinColumn(name = "post_id", referencedColumnName = "id")
    private PostJpa post;
}
