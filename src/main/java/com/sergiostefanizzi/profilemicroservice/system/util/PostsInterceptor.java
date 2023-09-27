package com.sergiostefanizzi.profilemicroservice.system.util;

import com.sergiostefanizzi.profilemicroservice.repository.PostsRepository;
import com.sergiostefanizzi.profilemicroservice.repository.ProfilesRepository;
import com.sergiostefanizzi.profilemicroservice.system.exception.PostNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.ModelAndView;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostsInterceptor implements HandlerInterceptor {
    @Autowired
    private PostsRepository postsRepository;
    @Autowired
    private ProfilesRepository profilesRepository;
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        log.info("\n\tPost Interceptor -> "+request.getRequestURI());
        // Esco se e' un metodo post
        String requestMethod = request.getMethod();
        if (requestMethod.equalsIgnoreCase("POST") || requestMethod.equalsIgnoreCase("PUT")) return true;

        Map pathVariables = (Map) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);

        Long postId = Long.valueOf((String) pathVariables.get("postId"));

        Long checkPostId;
        if (requestMethod.equalsIgnoreCase("DELETE")){
            //CheckActiveForDeleteById e' lo stesso di checkActiveById
            //ma non fa il controllo sulle storie scadute.
            //Così facendo un utente puo' anche cancellare una storia scaduta
            checkPostId = this.postsRepository.checkActiveForDeleteById(postId)
                    .orElseThrow(() -> new PostNotFoundException(postId));
        }else {
            checkPostId = this.postsRepository.checkActiveById(postId)
                    .orElseThrow(() -> new PostNotFoundException(postId));
        }

        Long checkProfileId = this.profilesRepository.checkActiveByPostId(postId)
                .orElseThrow(() -> new PostNotFoundException(checkPostId));

        log.info("\n\tPost Interceptor: Post ID-> "+checkPostId+" Profile ID-> "+checkProfileId);
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


