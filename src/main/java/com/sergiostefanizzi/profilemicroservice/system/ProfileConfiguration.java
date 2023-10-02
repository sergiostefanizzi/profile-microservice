package com.sergiostefanizzi.profilemicroservice.system;

import com.sergiostefanizzi.profilemicroservice.system.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ProfileConfiguration implements WebMvcConfigurer {
    /*
    @Bean
    @Scope(scopeName = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
    ProfileContext profileContext(){
        ProfileContext context = new ProfileContext();
        ProfileJpa p = new ProfileJpa();
        p.setId(1L);
        context.setProfileJpa(p);
        return context;
    }

     */
    @Autowired
    private ProfileInterceptor profileInterceptor;
    @Autowired
    private FollowsInterceptor followsInterceptor;
    @Autowired
    private PostsInterceptor postsInterceptor;
    @Autowired
    private CommentsInterceptor commentsInterceptor;
    @Autowired
    private AdminsProfileInterceptor adminsProfileInterceptor;

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
                .addPathPatterns("/posts/*")
                .addPathPatterns("/posts/likes/*")
                .excludePathPatterns("/posts/feed/*")
                .excludePathPatterns("/posts/comments/*")
                .excludePathPatterns("/alerts");
        registry.addInterceptor(this.commentsInterceptor)
                .addPathPatterns("/posts/comments/*")
                .excludePathPatterns("/alerts");
        registry.addInterceptor(this.adminsProfileInterceptor)
                .addPathPatterns("/admins/profiles/**");
    }
}
