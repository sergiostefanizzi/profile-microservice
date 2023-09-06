package com.sergiostefanizzi.profilemicroservice.system;

import com.sergiostefanizzi.profilemicroservice.system.util.CommentsInterceptor;
import com.sergiostefanizzi.profilemicroservice.system.util.FollowsInterceptor;

import com.sergiostefanizzi.profilemicroservice.system.util.PostsInterceptor;
import com.sergiostefanizzi.profilemicroservice.system.util.ProfileInterceptor;
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

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(this.profileInterceptor)
                .addPathPatterns("/profiles/**")
                .excludePathPatterns("/profiles/search")
                .excludePathPatterns("/profiles/*/f*/**");
        registry.addInterceptor(this.followsInterceptor)
                .addPathPatterns("/profiles/*/f*/**");
        registry.addInterceptor(this.postsInterceptor)
                .addPathPatterns("/posts/*")
                .addPathPatterns("/posts/likes/*")
                .excludePathPatterns("/posts/comments/*");
        registry.addInterceptor(this.commentsInterceptor)
                .addPathPatterns("/posts/comments/*");
    }
}
