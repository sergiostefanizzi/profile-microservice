package com.sergiostefanizzi.profilemicroservice.system.util;

import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;

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
}
