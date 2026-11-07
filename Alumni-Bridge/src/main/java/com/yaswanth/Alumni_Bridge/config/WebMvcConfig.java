package com.yaswanth.Alumni_Bridge.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/", "/login", "/register", "/logout", "/error", "/uploads/**", "/css/**", "/js/**", "/images/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:./uploads/");
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/", "file:./uploads/images/", "file:./Alumni-Bridge/uploads/images/");
        registry.addResourceHandler("/**")
                .addResourceLocations("file:./uploads/", "file:./Alumni-Bridge/uploads/", "classpath:/static/", "classpath:/public/");
    }
}
