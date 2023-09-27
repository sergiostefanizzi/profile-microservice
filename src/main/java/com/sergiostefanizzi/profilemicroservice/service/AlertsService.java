package com.sergiostefanizzi.profilemicroservice.service;

import com.sergiostefanizzi.profilemicroservice.model.*;
import com.sergiostefanizzi.profilemicroservice.model.converter.AlertToAlertJpaConverter;
import com.sergiostefanizzi.profilemicroservice.repository.AlertsRepository;
import com.sergiostefanizzi.profilemicroservice.repository.CommentsRepository;
import com.sergiostefanizzi.profilemicroservice.repository.PostsRepository;
import com.sergiostefanizzi.profilemicroservice.repository.ProfilesRepository;
import com.sergiostefanizzi.profilemicroservice.system.exception.AlertTypeNotSpecifiedException;
import com.sergiostefanizzi.profilemicroservice.system.exception.CommentNotFoundException;
import com.sergiostefanizzi.profilemicroservice.system.exception.PostNotFoundException;
import com.sergiostefanizzi.profilemicroservice.system.exception.ProfileNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertsService {
    private final AlertsRepository alertsRepository;
    private final ProfilesRepository profilesRepository;
    private final PostsRepository postsRepository;
    private final CommentsRepository commentsRepository;
    private final AlertToAlertJpaConverter alertToAlertJpaConverter;

    @Transactional
    public Alert createAlert(Boolean isPost, @NotNull Alert alert) {
        ProfileJpa profileJpa = this.profilesRepository.findActiveById(alert.getCreatedBy())
                .orElseThrow(() -> new ProfileNotFoundException(alert.getCreatedBy()));

        AlertJpa alertJpa = createAlertByType(isPost, alert);

        alertJpa.setCreatedBy(profileJpa);
        alertJpa.setCreatedAt(LocalDateTime.now());

        return this.alertToAlertJpaConverter.convertBack(
                this.alertsRepository.save(alertJpa)
        );
    }

    private AlertJpa createAlertByType(Boolean isPost, Alert alert) {
        AlertJpa alertJpa;
        if (isPost && (alert.getPostId() != null && alert.getCommentId() == null)) {
            alertJpa = createPostAlert(alert);
        } else if (!isPost && (alert.getCommentId() != null && alert.getPostId() == null)) {
            alertJpa = createCommentAlert(alert);
        } else {
            throw new AlertTypeNotSpecifiedException("Alert type is not specified");
        }
        return alertJpa;
    }

    private AlertJpa createPostAlert(Alert alert) {
        PostJpa postJpa;
        AlertJpa alertJpa;
        postJpa = this.postsRepository.findActiveById(alert.getPostId())
                .orElseThrow(() -> new PostNotFoundException(alert.getPostId()));
        alertJpa = this.alertToAlertJpaConverter.convert(alert);
        assert alertJpa != null;
        alertJpa.setPost(postJpa);
        return alertJpa;
    }

    private AlertJpa createCommentAlert(Alert alert) {
        CommentJpa commentJpa;
        AlertJpa alertJpa;
        commentJpa = this.commentsRepository.findActiveById(alert.getCommentId())
                .orElseThrow(() -> new CommentNotFoundException(alert.getCommentId()));
        alertJpa = this.alertToAlertJpaConverter.convert(alert);
        assert alertJpa != null;
        alertJpa.setComment(commentJpa);
        return alertJpa;
    }
}
