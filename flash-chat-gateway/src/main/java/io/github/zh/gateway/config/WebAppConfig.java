package io.github.zh.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.reactive.config.ResourceHandlerRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@Configuration
public class WebAppConfig implements WebFluxConfigurer {

    @Value("${flash-chat.web-app-path:../flash-chat-app-web}")
    private String webAppPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("file:" + webAppPath + "/")
                .setCacheControl(CacheControl.noCache());
    }
}
