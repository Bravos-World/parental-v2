package com.bravos.parentalv2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.ManagementWebSecurityAutoConfiguration;

@SpringBootApplication(exclude = {
    UserDetailsServiceAutoConfiguration.class,
    ManagementWebSecurityAutoConfiguration.class
})
public class Parentalv2Application {

  static void main(String[] args) {
    SpringApplication.run(Parentalv2Application.class, args);
  }

}
