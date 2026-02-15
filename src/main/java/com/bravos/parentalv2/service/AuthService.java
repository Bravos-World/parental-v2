package com.bravos.parentalv2.service;

import com.bravos.parentalv2.config.AdminProperties;
import jakarta.annotation.PostConstruct;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicReference;

@Service
public class AuthService {

  private final AdminProperties adminProperties;
  private final PasswordEncoder passwordEncoder;
  private final AtomicReference<String> currentUsername = new AtomicReference<>();
  private final AtomicReference<String> currentPasswordHash = new AtomicReference<>();

  public AuthService(AdminProperties adminProperties, PasswordEncoder passwordEncoder) {
    this.adminProperties = adminProperties;
    this.passwordEncoder = passwordEncoder;
  }

  @PostConstruct
  public void init() {
    currentUsername.set(adminProperties.getUsername());
    currentPasswordHash.set(passwordEncoder.encode(adminProperties.getPassword()));
  }

  public void authenticate(String username, String password) {
    if (!currentUsername.get().equals(username)) {
      throw new BadCredentialsException("Invalid username or password");
    }
    if (!passwordEncoder.matches(password, currentPasswordHash.get())) {
      throw new BadCredentialsException("Invalid username or password");
    }
  }

  public void changePassword(String oldPassword, String newPassword) {
    if (!passwordEncoder.matches(oldPassword, currentPasswordHash.get())) {
      throw new BadCredentialsException("Old password is incorrect");
    }
    currentPasswordHash.set(passwordEncoder.encode(newPassword));
  }

}
