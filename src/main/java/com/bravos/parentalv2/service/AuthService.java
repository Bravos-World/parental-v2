package com.bravos.parentalv2.service;

import com.bravos.parentalv2.config.AdminProperties;
import com.bravos.parentalv2.model.Admin;
import com.bravos.parentalv2.repository.AdminRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  private final AdminRepository adminRepository;
  private final AdminProperties adminProperties;
  private final PasswordEncoder passwordEncoder;

  public AuthService(AdminRepository adminRepository,
      AdminProperties adminProperties,
      PasswordEncoder passwordEncoder) {
    this.adminRepository = adminRepository;
    this.adminProperties = adminProperties;
    this.passwordEncoder = passwordEncoder;
  }

  @PostConstruct
  public void init() {
    if (adminRepository.count() == 0) {
      Admin defaultAdmin = Admin.builder()
          .username(adminProperties.getUsername())
          .password(passwordEncoder.encode(adminProperties.getPassword()))
          .build();
      adminRepository.save(defaultAdmin);
    }
  }

  public void authenticate(String username, String password) {
    Admin admin = adminRepository.findByUsername(username).orElseThrow(() -> new BadCredentialsException("Invalid username or password"));
    if (!passwordEncoder.matches(password, admin.getPassword())) {
      throw new BadCredentialsException("Invalid username or password");
    }
  }

  @Transactional
  public void register(String username, String password) {
    if (adminRepository.existsByUsername(username)) {
      throw new IllegalArgumentException("Username already exists");
    }
    Admin admin = Admin.builder()
        .username(username)
        .password(passwordEncoder.encode(password))
        .build();
    adminRepository.save(admin);
  }

  @Transactional
  public void changePassword(String username, String oldPassword, String newPassword) {
    Admin admin = adminRepository.findByUsername(username)
        .orElseThrow(() -> new BadCredentialsException("User not found"));
    if (!passwordEncoder.matches(oldPassword, admin.getPassword())) {
      throw new BadCredentialsException("Old password is incorrect");
    }
    admin.setPassword(passwordEncoder.encode(newPassword));
    adminRepository.save(admin);
  }

}
