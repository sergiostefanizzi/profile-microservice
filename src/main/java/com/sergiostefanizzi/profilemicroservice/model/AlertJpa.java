package com.sergiostefanizzi.profilemicroservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "Alert")
@Getter
@Setter
@RequiredArgsConstructor
@NoArgsConstructor(force = true)
public class AlertJpa {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @Column(name = "reason", nullable = false, length = 2000)
    @Size(min = 1, max = 2000)
    @NotBlank
    @NonNull
    private String reason;
    @Column(name = "created_at", nullable = false)
    @PastOrPresent
    private LocalDateTime createdAt;
    @Column(name = "closed_at")
    @PastOrPresent
    private LocalDateTime closedAt;
    @Column(name = "version", nullable = false)
    @Version
    private Long version;
    @ManyToOne
    @JoinColumn(name = "post_id", referencedColumnName = "id")
    private PostJpa post;
    @ManyToOne
    @JoinColumn(name = "comment_id", referencedColumnName = "id")
    private CommentJpa comment;
    @ManyToOne(optional = false)
    @JoinColumn(name = "created_by", referencedColumnName = "id")
    private ProfileJpa createdBy;
    @ManyToOne
    @JoinColumn(name = "managed_by", referencedColumnName = "id")
    private ProfileJpa managedBy;

}
