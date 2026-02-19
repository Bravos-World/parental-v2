package com.bravos.parentalv2.repository;

import com.bravos.parentalv2.model.WebSocketSecret;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WebSocketSecretRepository extends JpaRepository<WebSocketSecret, Long> {

  Optional<WebSocketSecret> findFirstByOrderByIdDesc();

  boolean existsBySecretKey(String secretKey);

}

