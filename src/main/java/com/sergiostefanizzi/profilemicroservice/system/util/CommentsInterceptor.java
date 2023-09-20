package com.sergiostefanizzi.profilemicroservice.system.util;

import com.sergiostefanizzi.profilemicroservice.repository.CommentsRepository;
import com.sergiostefanizzi.profilemicroservice.repository.PostsRepository;
import com.sergiostefanizzi.profilemicroservice.system.exception.CommentNotFoundException;
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
public class CommentsInterceptor implements HandlerInterceptor {
    @Autowired
    private CommentsRepository commentsRepository;
    @Autowired
    private PostsRepository postsRepository;
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        log.info("\n\tComment Interceptor -> "+request.getRequestURI());
        // Esco se e' un metodo post
        String requestMethod = request.getMethod();
        Map pathVariables = (Map) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (!requestMethod.equalsIgnoreCase("POST")) {
            Long checkId;
            if (requestMethod.equalsIgnoreCase("GET")) {
                Long postId = Long.valueOf((String) pathVariables.get("postId"));
                checkId = this.postsRepository.checkActiveById(postId, LocalDateTime.now().minusDays(1))
                        .orElseThrow(() -> new PostNotFoundException(postId));
            } else {
                Long commentId = Long.valueOf((String) pathVariables.get("commentId"));
                checkId = this.commentsRepository.checkActiveById(commentId)
                        .orElseThrow(() -> new CommentNotFoundException(commentId));
            }
            log.info("\n\tComment Interceptor: ID-> " + checkId);
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
