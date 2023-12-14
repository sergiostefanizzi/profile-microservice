package com.sergiostefanizzi.profilemicroservice.system.util;

import com.sergiostefanizzi.profilemicroservice.model.FollowsId;
import com.sergiostefanizzi.profilemicroservice.model.ProfileJpa;
import com.sergiostefanizzi.profilemicroservice.repository.FollowsRepository;
import com.sergiostefanizzi.profilemicroservice.service.KeycloakService;
import com.sergiostefanizzi.profilemicroservice.system.exception.AccessForbiddenException;
import com.sergiostefanizzi.profilemicroservice.system.exception.IdsMismatchException;
import com.sergiostefanizzi.profilemicroservice.system.exception.NotInProfileListException;
import com.sergiostefanizzi.profilemicroservice.system.exception.PostAccessForbiddenException;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;
import java.util.Objects;

@Slf4j
public final class JwtUtilityClass {
    private JwtUtilityClass(){}
    public static Boolean isInProfileListJwt(Long profileId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        JwtAuthenticationToken oauthToken = (JwtAuthenticationToken) authentication;
        List<Long> profileList = oauthToken.getToken().getClaim("profileList");
        log.info("Jwt ProfileList --> "+profileList);
        if (profileList != null){
            return profileList.contains(profileId);
        }
        return false;
    }

    public static String getJwtAccountId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        JwtAuthenticationToken oauthToken = (JwtAuthenticationToken) authentication;
        String jwtAccountId = oauthToken.getToken().getClaim("sub");
        log.info("TOKEN ACCOUNT ID --> "+jwtAccountId);
        return jwtAccountId;
    }

    public static void checkProfileListAndIds(Long profileId, Long selectedUserProfileId, KeycloakService keycloakService) {
        if(!Objects.equals(profileId, selectedUserProfileId)){
            throw new IdsMismatchException();
        }

        checkProfileList(selectedUserProfileId, keycloakService);
    }

    public static void checkProfileList(Long selectedUserProfileId, KeycloakService keycloakService) {
        if (Boolean.FALSE.equals(isInProfileListJwt(selectedUserProfileId)) && Boolean.FALSE.equals(keycloakService.isInProfileList(getJwtAccountId(), selectedUserProfileId))){
            throw new NotInProfileListException(selectedUserProfileId);
        }
    }

    public static Boolean checkAccess(ProfileJpa profileJpa, Long selectedUserProfileId, FollowsRepository followsRepository) {
        // se sono id uguali oppure se è pubblico
        /*
        if (!Objects.equals(profileJpa.getId(), selectedUserProfileId)){
            log.info("Diversi");
            if (profileJpa.getIsPrivate()){
                log.info("Privato");
                if (followsRepository.findActiveAcceptedById(new FollowsId(selectedUserProfileId, profileJpa.getId())).isEmpty()){
                    log.info("Non follower");
                    return false;
                }
            }

            log.info("Pubblico ");
        }

        return true;

         */
        return (Objects.equals(profileJpa.getId(), selectedUserProfileId)) || !profileJpa.getIsPrivate() || (followsRepository.findActiveAcceptedById(new FollowsId(selectedUserProfileId, profileJpa.getId())).isPresent());
    }
}
