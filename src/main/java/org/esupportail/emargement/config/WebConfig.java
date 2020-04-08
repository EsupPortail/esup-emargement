package org.esupportail.emargement.config;  

import org.esupportail.emargement.web.WebInterceptor;
import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.thymeleaf.dialect.springdata.SpringDataDialect;

@Configuration  
public class WebConfig implements WebMvcConfigurer {  

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
    	registry.addResourceHandler("/resources/**")
        .addResourceLocations("classpath:/static/");
    	registry.addResourceHandler("/webjars/**")
            .addResourceLocations("/webjars/");
    }
    
    @Bean
    public WebInterceptor webInterceptor() {
        return new WebInterceptor();
    }
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(webInterceptor());
    }
    
    @Bean
    public ErrorProperties errorProperties() throws Exception {
      return new ErrorProperties();
    }
        
    @Bean
    public SpringDataDialect springDataDialect() {
        return new SpringDataDialect();
    }
    
} 