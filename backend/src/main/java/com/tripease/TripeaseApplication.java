package com.tripease;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

// We authenticate with JWT ourselves, so Spring's default in-memory user is not needed.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class TripeaseApplication {
    public static void main(String[] args) {
        SpringApplication.run(TripeaseApplication.class, args);
    }
}
