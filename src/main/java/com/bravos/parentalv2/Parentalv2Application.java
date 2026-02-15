package com.bravos.parentalv2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = {
    UserDetailsServiceAutoConfiguration.class
})
public class Parentalv2Application {

  static void main(String[] args) {
    SpringApplication.run(Parentalv2Application.class, args);
  }

}
