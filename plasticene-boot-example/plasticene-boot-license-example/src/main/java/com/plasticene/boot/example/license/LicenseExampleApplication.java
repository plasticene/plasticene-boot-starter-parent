package com.plasticene.boot.example.license;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author shepherd
 */
@SpringBootApplication
@EnableScheduling
public class LicenseExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(LicenseExampleApplication.class, args);
    }

}
