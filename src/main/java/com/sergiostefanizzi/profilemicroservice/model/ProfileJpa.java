package com.sergiostefanizzi.profilemicroservice.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "Profile")
@Getter
@Setter
@RequiredArgsConstructor
@NoArgsConstructor(force = true)
public class ProfileJpa {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private Long id;

    @NotBlank
    @NotNull
    @Column(name = "profile_name", nullable = false, unique = true, length = 30)
    @Size(min = 8, max = 20)
    @Pattern(regexp = "^(?=.{8,20}$)(?![_.])(?!.*[_.]{2})[a-zA-Z0-9._]+(?<![_.])$")
    private final String profileName;

    @Column(name = "bio", length = 150)
    @Size(max = 150)
    private String bio;

    @Column(name = "picture_url")
    @URL
    private String pictureUrl;

    @Column(name = "is_private", nullable = false)
    @NotNull
    @NonNull
    private Boolean isPrivate;

    @Column(name = "created_at", nullable = false)
    @PastOrPresent
    @NotNull
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @PastOrPresent
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    @PastOrPresent
    private LocalDateTime deletedAt;

    @Column(name = "blocked_until")
    @Future
    private LocalDateTime blockedUntil;

    @Column(name = "account_id", nullable = false)
    @NotNull
    private final Long accountId;

    @OneToMany(mappedBy = "profile")
    @JsonBackReference
    private List<PostJpa> postList;

    @OneToMany(mappedBy = "createdBy")
    @JsonBackReference
    private List<AlertJpa> alertList;

    @OneToMany(mappedBy = "managedBy")
    @JsonBackReference
    private List<AlertJpa> managedAlertList;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
