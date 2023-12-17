package com.sergiostefanizzi.profilemicroservice.service;

import com.sergiostefanizzi.profilemicroservice.model.*;
import com.sergiostefanizzi.profilemicroservice.model.converter.AlertToAlertJpaConverter;
import com.sergiostefanizzi.profilemicroservice.repository.*;
import com.sergiostefanizzi.profilemicroservice.system.exception.*;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static com.sergiostefanizzi.profilemicroservice.system.util.JwtUtilityClass.checkAccess;
import static com.sergiostefanizzi.profilemicroservice.system.util.JwtUtilityClass.checkProfileList;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertsService {
    private final AlertsRepository alertsRepository;
    private final ProfilesRepository profilesRepository;
    private final PostsRepository postsRepository;
    private final CommentsRepository commentsRepository;
    private final FollowsRepository followsRepository;
    private final AlertToAlertJpaConverter alertToAlertJpaConverter;
    private final KeycloakService keycloakService;

    @Transactional
    public Alert createAlert(Boolean isPost, @NotNull Alert alert) {
        checkProfileList(alert.getCreatedBy(), this.keycloakService);

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
        if (Boolean.TRUE.equals(isPost)) {
            alertJpa = createPostAlert(alert);
        } else {
            alertJpa = createCommentAlert(alert);
        }
        return alertJpa;
    }

    private AlertJpa createPostAlert(Alert alert) {
        PostJpa postJpa;
        AlertJpa alertJpa;
        if(alert.getPostId() == null){
            throw new AlertTypeErrorException("Alert type error");
        }
        postJpa = this.postsRepository.findActiveById(alert.getPostId())
                .orElseThrow(() -> new PostNotFoundException(alert.getPostId()));
        if (Boolean.FALSE.equals(checkAccess(postJpa.getProfile(), alert.getCreatedBy(), this.followsRepository))){
            throw new AccessForbiddenException("Post");
        }
        alertJpa = this.alertToAlertJpaConverter.convert(alert);
        assert alertJpa != null;
        alertJpa.setPost(postJpa);
        return alertJpa;
    }

    private AlertJpa createCommentAlert(Alert alert) {
        CommentJpa commentJpa;
        AlertJpa alertJpa;
        if(alert.getCommentId() == null){
            throw new AlertTypeErrorException("Alert type error");
        }
        commentJpa = this.commentsRepository.findActiveById(alert.getCommentId())
                .orElseThrow(() -> new CommentNotFoundException(alert.getCommentId()));
        if (Boolean.FALSE.equals(checkAccess(commentJpa.getPost().getProfile(), alert.getCreatedBy(), this.followsRepository))){
            throw new AccessForbiddenException("Post");
        }

        alertJpa = this.alertToAlertJpaConverter.convert(alert);
        assert alertJpa != null;
        alertJpa.setComment(commentJpa);
        return alertJpa;
    }
}
