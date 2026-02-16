package com.bravos.parentalv2.controller;

import com.bravos.parentalv2.dto.ApiResponse;
import com.bravos.parentalv2.dto.ChangePasswordRequest;
import com.bravos.parentalv2.dto.LoginRequest;
import com.bravos.parentalv2.dto.RegisterRequest;
import com.bravos.parentalv2.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Login, logout, registration, and password management")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/login")
  @Operation(summary = "Login with username and password")
  public ResponseEntity<ApiResponse<Void>> login(@Valid @RequestBody LoginRequest request,
      HttpServletRequest httpRequest) {
    authService.authenticate(request.getUsername(), request.getPassword());

    HttpSession session = httpRequest.getSession(true);
    session.setAttribute("authenticated", true);
    session.setAttribute("username", request.getUsername());

    return ResponseEntity.ok(ApiResponse.success("Login successful"));
  }

  @PostMapping("/register")
  @Operation(summary = "Create a new admin account")
  public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest request) {
    authService.register(request.getUsername(), request.getPassword());
    return ResponseEntity.ok(ApiResponse.success("Account created successfully"));
  }

  @PostMapping("/logout")
  @Operation(summary = "Logout and invalidate session")
  public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    if (session != null) {
      session.invalidate();
    }
    return ResponseEntity.ok(ApiResponse.success("Logout successful"));
  }

  @PostMapping("/change-password")
  @Operation(summary = "Change admin password")
  public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody ChangePasswordRequest request,
      HttpServletRequest httpRequest) {
    HttpSession session = httpRequest.getSession(false);
    String username = (String) session.getAttribute("username");
    authService.changePassword(username, request.getOldPassword(), request.getNewPassword());
    return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
  }

}
