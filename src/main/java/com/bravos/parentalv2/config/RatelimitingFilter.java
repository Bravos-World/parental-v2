package com.bravos.parentalv2.config;

import io.github.bucket4j.BandwidthBuilder;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RatelimitingFilter extends OncePerRequestFilter {

  private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

  @Override
  protected void doFilterInternal(@NonNull HttpServletRequest request,
                                  @NonNull HttpServletResponse response,
                                  @NonNull FilterChain filterChain) {
    String ip = request.getHeader("X-Real-IP");
    if (ip == null || ip.isBlank()) {
      ip = request.getRemoteAddr();
    }
    String uri = request.getRequestURI();
    Bucket bucket = buckets.computeIfAbsent(ip + ":" + uri, k -> newBucket(uri));
    if (bucket.tryConsume(1)) {
      try {
        filterChain.doFilter(request, response);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    } else {
      response.setStatus(429);
    }
  }

  private Bucket newBucket(String uri) {
    if(uri.startsWith("/api/auth/login")) {
      return Bucket.builder()
          .addLimit(BandwidthBuilder.builder()
              .capacity(5)
              .refillGreedy(5, Duration.ofMinutes(2))
              .build()
          ).build();
    }
    return Bucket.builder()
        .addLimit(BandwidthBuilder.builder()
            .capacity(120)
            .refillGreedy(120, Duration.ofMinutes(1))
            .build()
        ).build();
  }

}
