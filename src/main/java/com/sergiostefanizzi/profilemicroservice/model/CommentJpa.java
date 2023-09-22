package com.sergiostefanizzi.profilemicroservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "Comment")
@Getter
@Setter
@RequiredArgsConstructor
@NoArgsConstructor(force = true)
public class CommentJpa {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @Column(name = "content", nullable = false, length = 2200)
    @Size(min= 1, max = 2200)
    @NotBlank
    @NonNull
    private String content;
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
    private Long version;
    @ManyToOne(optional = false)
    @JoinColumn(name = "profile_id", referencedColumnName = "id")
    private ProfileJpa profile;
    @ManyToOne(optional = false)
    @JoinColumn(name = "post_id", referencedColumnName = "id")
    private PostJpa post;
    @OneToMany(mappedBy = "comment")
    private List<AlertJpa> alertList;
}
