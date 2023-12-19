package com.sergiostefanizzi.profilemicroservice.system.util;

import com.sergiostefanizzi.profilemicroservice.repository.CommentsRepository;
import com.sergiostefanizzi.profilemicroservice.repository.PostsRepository;
import com.sergiostefanizzi.profilemicroservice.repository.ProfilesRepository;
import com.sergiostefanizzi.profilemicroservice.service.KeycloakService;
import com.sergiostefanizzi.profilemicroservice.system.exception.AccountNotFoundException;
import com.sergiostefanizzi.profilemicroservice.system.exception.CommentNotFoundException;
import com.sergiostefanizzi.profilemicroservice.system.exception.EmailNotValidatedException;
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

import static com.sergiostefanizzi.profilemicroservice.system.util.JwtUtilityClass.getJwtAccountId;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommentsInterceptor implements HandlerInterceptor {
    private final CommentsRepository commentsRepository;
    private final PostsRepository postsRepository;
    private final ProfilesRepository profilesRepository;
    private final KeycloakService keycloakService;
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        log.info("\n\tComment Interceptor -> "+request.getRequestURI());

        String accountId = getJwtAccountId();
        if (Boolean.TRUE.equals(this.keycloakService.checkActiveById(accountId))){
            if (Boolean.FALSE.equals(this.keycloakService.checksEmailValidated(accountId))){
                throw new EmailNotValidatedException(accountId);
            }
        }else {
            throw new AccountNotFoundException(accountId);
        }

        // Esco se e' un metodo post
        String requestMethod = request.getMethod();
        Map pathVariables = (Map) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (!requestMethod.equalsIgnoreCase("POST")) {
            Long checkPostId;
            if (requestMethod.equalsIgnoreCase("GET")) {
                Long postId = Long.valueOf((String) pathVariables.get("postId"));
                // Controllo che il post sia attivo
                checkPostId = this.postsRepository.checkActiveById(postId)
                        .orElseThrow(() -> new PostNotFoundException(postId));
                // Controllo che il profilo del post sia attivo
                this.profilesRepository.checkActiveByPostId(postId)
                        .orElseThrow(() -> new PostNotFoundException(postId));
                log.info("\n\tComment Interceptor: Post ID-> " + checkPostId);
            } else {
                Long commentId = Long.valueOf((String) pathVariables.get("commentId"));
                // Controllo che il commento sia attivo
                Long checkCommentId = this.commentsRepository.checkActiveById(commentId)
                        .orElseThrow(() -> new CommentNotFoundException(commentId));
                // Controllo che il post del commento sia attivo
                checkPostId = this.postsRepository.checkActiveByCommentId(commentId)
                        .orElseThrow(() -> new CommentNotFoundException(commentId));
                // Controllo che il profilo del post del commento sia attivo
                this.profilesRepository.checkActiveByPostId(checkPostId)
                        .orElseThrow(() -> new CommentNotFoundException(commentId));
                log.info("\n\tComment Interceptor: Comment ID-> " + checkCommentId);
            }

        }
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
