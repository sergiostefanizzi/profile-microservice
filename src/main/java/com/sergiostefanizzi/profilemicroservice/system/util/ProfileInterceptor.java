package com.sergiostefanizzi.profilemicroservice.system.util;

import com.sergiostefanizzi.profilemicroservice.model.ProfileJpa;
import com.sergiostefanizzi.profilemicroservice.repository.ProfilesRepository;
import com.sergiostefanizzi.profilemicroservice.system.exception.ProfileNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
@Component
@RequiredArgsConstructor
@NoArgsConstructor(force = true)
public class ProfileInterceptor implements HandlerInterceptor {
    @Autowired
    private final ProfilesRepository profilesRepository;
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        log.info("\n\tInterceptor -> "+request.getRequestURI());
        if (request.getMethod().equalsIgnoreCase("POST")) return true;
        //Long profileId = Long.valueOf(request.getParameter("profileId"));


        Map pathVariables = (Map) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        Long profileId = Long.valueOf((String) pathVariables.get("profileId"));
        //String profileName = (String) pathVariables.get("profileName");

        ProfileJpa profileJpa = this.profilesRepository.findActiveById(profileId)
                .orElseThrow(() -> new ProfileNotFoundException(profileId));
        log.info("\nInterceptor Profile id-> "+profileJpa.getId());
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