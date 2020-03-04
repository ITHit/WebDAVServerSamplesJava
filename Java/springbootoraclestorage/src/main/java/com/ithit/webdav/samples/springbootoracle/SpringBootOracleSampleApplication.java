package com.ithit.webdav.samples.springbootoracle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableJpaRepositories
@SpringBootApplication
public class SpringBootOracleSampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringBootOracleSampleApplication.class, args);
    }

}
