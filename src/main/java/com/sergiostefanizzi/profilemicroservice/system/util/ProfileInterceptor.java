package com.sergiostefanizzi.profilemicroservice.system.util;

import com.sergiostefanizzi.profilemicroservice.repository.ProfilesRepository;
import com.sergiostefanizzi.profilemicroservice.service.KeycloakService;
import com.sergiostefanizzi.profilemicroservice.system.exception.NotInProfileListException;
import com.sergiostefanizzi.profilemicroservice.system.exception.ProfileNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

import static com.sergiostefanizzi.profilemicroservice.system.util.JwtUtilityClass.getJwtAccountId;
import static com.sergiostefanizzi.profilemicroservice.system.util.JwtUtilityClass.isInProfileListJwt;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProfileInterceptor implements HandlerInterceptor {
    private final ProfilesRepository profilesRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler){
        log.info("\n\tProfile Interceptor -> "+request.getRequestURI());
        // Esco se e' un metodo post
        if (request.getMethod().equalsIgnoreCase("POST")) return true;

        Map pathVariables = (Map) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);

        Long profileId = Long.valueOf((String) pathVariables.get("profileId"));
        Long checkId = this.profilesRepository.checkActiveById(profileId)
                .orElseThrow(() -> new ProfileNotFoundException(profileId));

        log.info("\n\tProfile Interceptor: Profile ID-> "+checkId);
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
