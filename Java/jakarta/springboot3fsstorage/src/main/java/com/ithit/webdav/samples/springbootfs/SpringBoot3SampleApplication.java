package com.ithit.webdav.samples.springbootfs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

@SpringBootApplication(exclude = { SecurityAutoConfiguration.class })
public class SpringBoot3SampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringBoot3SampleApplication.class, args);
    }

}
