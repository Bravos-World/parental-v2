package com.bravos.parentalv2.websocket;

import com.bravos.parentalv2.service.WebSocketSecretService;
import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

  private static final Logger log = LoggerFactory.getLogger(WebSocketAuthInterceptor.class);
  private static final String SECRET_KEY_HEADER = "X-Secret-Key";

  private final WebSocketSecretService secretService;

  public WebSocketAuthInterceptor(WebSocketSecretService secretService) {
    this.secretService = secretService;
  }

  @Override
  public boolean beforeHandshake(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response,
                                 @NonNull WebSocketHandler wsHandler, @NonNull Map<String, Object> attributes) {
    String secretKey = null;
    if (!request.getHeaders().isEmpty()) {
      secretKey = request.getHeaders().getFirst(SECRET_KEY_HEADER);
    }
    // Fallback: read from query parameter for clients that cannot set WS headers
    if (secretKey == null && request instanceof ServletServerHttpRequest servletRequest) {
      HttpServletRequest httpRequest = servletRequest.getServletRequest();
      secretKey = httpRequest.getParameter("secretKey");
    }
    if (!secretService.validateKey(secretKey)) {
      log.warn("WebSocket connection rejected: invalid or missing secret key from {}", request.getRemoteAddress());
      response.setStatusCode(HttpStatus.UNAUTHORIZED);
      return false;
    }
    log.debug("WebSocket handshake authorized from {}", request.getRemoteAddress());
    return true;
  }

  @Override
  public void afterHandshake(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response,
                              @NonNull WebSocketHandler wsHandler, Exception exception) {
    log.debug("WebSocket handshake completed from {}", request.getRemoteAddress());
  }

}




