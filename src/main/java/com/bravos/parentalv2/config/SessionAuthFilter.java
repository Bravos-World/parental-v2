package com.bravos.parentalv2.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class SessionAuthFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(HttpServletRequest request,
                                  @NonNull HttpServletResponse response,
                                  @NonNull FilterChain filterChain) throws ServletException, IOException {
    String uri = request.getRequestURI();
    if (!uri.startsWith("/ws") &&
        !uri.startsWith("/api/") &&
        !uri.startsWith("/swagger-ui/") &&
        !uri.startsWith("/v3/api-docs/")) {
      response.setStatus(444);
      return;
    }
    HttpSession session = request.getSession(false);
    if (session != null && Boolean.TRUE.equals(session.getAttribute("authenticated"))) {
      String username = (String) session.getAttribute("username");
      UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
          username, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
      SecurityContextHolder.getContext().setAuthentication(auth);
    }
    filterChain.doFilter(request, response);
  }

}
