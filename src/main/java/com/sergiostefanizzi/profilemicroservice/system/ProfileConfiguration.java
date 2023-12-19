package com.sergiostefanizzi.profilemicroservice.system;

import com.sergiostefanizzi.profilemicroservice.system.util.*;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class ProfileConfiguration implements WebMvcConfigurer {
    private final ProfileInterceptor profileInterceptor;

    private final FollowsInterceptor followsInterceptor;

    private final PostsInterceptor postsInterceptor;

    private final CommentsInterceptor commentsInterceptor;

    private final AdminsProfileInterceptor adminsProfileInterceptor;

    private final AlertsInterceptor alertsInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(this.profileInterceptor)
                .addPathPatterns("/profiles/**")
                .addPathPatterns("/posts/feed/*")
                .excludePathPatterns("/profiles/search")
                .excludePathPatterns("/profiles/*/f*/**")
                .excludePathPatterns("/alerts");
        registry.addInterceptor(this.followsInterceptor)
                .addPathPatterns("/profiles/*/f*/**")
                .excludePathPatterns("/alerts");
        registry.addInterceptor(this.postsInterceptor)
                .addPathPatterns("/posts/**")
                .addPathPatterns("/posts/likes/*")
                .excludePathPatterns("/posts/feed/*")
                .excludePathPatterns("/posts/comments/*")
                .excludePathPatterns("/alerts");
        registry.addInterceptor(this.commentsInterceptor)
                .addPathPatterns("/posts/comments/*")
                .excludePathPatterns("/alerts");
        registry.addInterceptor(this.adminsProfileInterceptor)
                .addPathPatterns("/admins/profiles/**");
        registry.addInterceptor(this.alertsInterceptor)
                .addPathPatterns("/admins/alerts/**");
    }
}
