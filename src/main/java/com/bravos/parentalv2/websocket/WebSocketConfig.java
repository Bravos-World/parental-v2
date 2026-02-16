package com.bravos.parentalv2.websocket;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

  private final DeviceWebSocketHandler deviceWebSocketHandler;

  public WebSocketConfig(DeviceWebSocketHandler deviceWebSocketHandler) {
    this.deviceWebSocketHandler = deviceWebSocketHandler;
  }

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry.addHandler(deviceWebSocketHandler, "/ws/device").setAllowedOrigins("*");
  }

  @Bean
  public ServletServerContainerFactoryBean createWebSocketContainer() {
    ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
    container.setMaxTextMessageBufferSize(8 * 1024);
    container.setMaxBinaryMessageBufferSize(8 * 1024);
    return container;
  }


}
