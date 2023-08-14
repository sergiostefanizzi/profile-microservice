package com.sergiostefanizzi.profilemicroservice.system;

import com.sergiostefanizzi.profilemicroservice.model.ProfileJpa;
import com.sergiostefanizzi.profilemicroservice.system.util.ProfileContext;
import com.sergiostefanizzi.profilemicroservice.system.util.ProfileInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ProfileConfiguration implements WebMvcConfigurer {

    @Bean
    @Scope(scopeName = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
    ProfileContext profileContext(){
        ProfileContext context = new ProfileContext();
        ProfileJpa p = new ProfileJpa();
        p.setId(1L);
        context.setProfileJpa(p);
        return context;
    }
    @Autowired
    ProfileInterceptor profileInterceptor;
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(profileInterceptor)
                .addPathPatterns("/profiles/**")
                .excludePathPatterns("/profiles/search")
                .excludePathPatterns("/profiles/{profileId}/f*/**");
    }
}
