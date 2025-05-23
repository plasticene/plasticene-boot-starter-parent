package com.plasticene.boot.example.web;

import com.plasticene.boot.example.web.provider.DatabaseApiKeySecurityProvider;
import com.plasticene.boot.web.core.advice.ApiSecurityKeyProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * @author fjzheng
 */
@SpringBootApplication
public class WebExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebExampleApplication.class, args);
    }

    @Bean
    public ApiSecurityKeyProvider apiSecurityKeyProvider() {
        return new DatabaseApiKeySecurityProvider();
    }

}
