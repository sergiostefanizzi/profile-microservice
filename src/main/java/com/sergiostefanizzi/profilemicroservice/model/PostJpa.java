package com.sergiostefanizzi.profilemicroservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.validator.constraints.URL;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "Post")
@Getter
@Setter
@RequiredArgsConstructor
@NoArgsConstructor(force = true)
public class PostJpa {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @Column(name = "content_url", nullable = false)
    @NotNull
    @NonNull
    @Size(min = 3, max = 2048)
    private String contentUrl;
    @Column(name = "caption", length = 2200)
    @Size(max = 2200)
    private String caption;
    @Column(name = "post_type", nullable = false)
    @Enumerated(EnumType.STRING)
    @NotNull
    private final Post.PostTypeEnum postType;
    @Column(name = "created_at", nullable = false)
    @NotNull
    @PastOrPresent
    private LocalDateTime createdAt;
    @Column(name = "updated_at")
    @PastOrPresent
    private LocalDateTime updatedAt;
    @Column(name = "deleted_at")
    @PastOrPresent
    private LocalDateTime deletedAt;
    @Column(name = "version", nullable = false)
    @Version
    private Long Version;
    @ManyToOne(optional = false)
    @JoinColumn(name = "profile_id", referencedColumnName = "id")
    private ProfileJpa profile;
    @OneToMany(mappedBy = "post")
    private List<LikeJpa> likes;
    // TODO fare like,comment e alert
    /*
    @OneToMany(mappedBy = "post")
    private List<CommentJpa> comments;

    @OneToMany(mappedBy = "post")
    private List<AlertJpa> alerts;
     */
}
