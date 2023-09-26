package com.sergiostefanizzi.profilemicroservice.system.util;

import com.sergiostefanizzi.profilemicroservice.repository.ProfilesRepository;
import com.sergiostefanizzi.profilemicroservice.system.exception.FollowItselfException;
import com.sergiostefanizzi.profilemicroservice.system.exception.ProfileNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class FollowsInterceptor implements HandlerInterceptor {
    @Autowired
    private ProfilesRepository profilesRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        log.info("\n\tFollows Interceptor -> "+request.getRequestURI());

        Map pathVariables = (Map) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        Long profileId = Long.valueOf((String) pathVariables.get("profileId"));
        Long checkProfileId = this.profilesRepository.checkActiveById(profileId)
                .orElseThrow(() -> new ProfileNotFoundException(profileId));
        if (request.getMethod().equalsIgnoreCase("GET")) return true;

        Long profileToFollowId = Long.valueOf((String) pathVariables.get("followsId"));
        Long checkProfileToFollowId = this.profilesRepository.checkActiveById(profileToFollowId)
                .orElseThrow(() -> new ProfileNotFoundException(profileToFollowId));
        if (profileId.equals(profileToFollowId)){
            throw new FollowItselfException("Profile cannot follow itself!");
        }
        log.info("\nFollow Interceptor: IDs -> "+checkProfileId+", "+checkProfileToFollowId);
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        HandlerInterceptor.super.postHandle(request, response, handler, modelAndView);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }
}
