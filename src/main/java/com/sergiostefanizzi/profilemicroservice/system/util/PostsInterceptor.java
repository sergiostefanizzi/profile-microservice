package com.sergiostefanizzi.profilemicroservice.system.util;

import com.sergiostefanizzi.profilemicroservice.repository.PostsRepository;
import com.sergiostefanizzi.profilemicroservice.repository.ProfilesRepository;
import com.sergiostefanizzi.profilemicroservice.service.KeycloakService;
import com.sergiostefanizzi.profilemicroservice.system.exception.NotInProfileListException;
import com.sergiostefanizzi.profilemicroservice.system.exception.PostNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostsInterceptor implements HandlerInterceptor {
    private final PostsRepository postsRepository;
    private final ProfilesRepository profilesRepository;
    private final KeycloakService keycloakService;
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
        // Controllo che il profilo che ha pubblicato quel post sia ancora attivo
        Long checkProfileId = this.profilesRepository.checkActiveByPostId(postId)
                .orElseThrow(() -> new PostNotFoundException(checkPostId));
        // Per le operazioni di rimozione e aggiornamento del post, controllo che
        // chi le richiede abbia l'autorizzazione per farlo. Cioe' sia l'autore del post
        /*
        if ((requestMethod.equalsIgnoreCase("DELETE") || requestMethod.equalsIgnoreCase("PATCH"))
                && (Boolean.FALSE.equals(JwtUtilityClass.isInProfileListJwt(checkProfileId))
                && (Boolean.FALSE.equals(this.keycloakService.isInProfileList(JwtUtilityClass.getJwtAccountId(),checkProfileId))))){
                    throw new NotInProfileListException(checkProfileId);
        }

         */

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


