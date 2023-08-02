package com.ithit.webdav.samples.springboots3;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

@SpringBootApplication(exclude = { SecurityAutoConfiguration.class })
public class SpringBootS3Application {

    public static void main(String[] args) {
        SpringApplication.run(SpringBootS3Application.class, args);
    }

}
