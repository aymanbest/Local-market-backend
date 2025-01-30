package com.localmarket.main.repository.accesstoken;

import com.localmarket.main.entity.AccessToken.AccessToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface AccessTokenRepository extends JpaRepository<AccessToken, String> {
    Optional<AccessToken> findByTokenAndExpiresAtAfter(String token, LocalDateTime now);
    Optional<AccessToken> findByEmail(String email);
    void deleteByExpiresAtBefore(LocalDateTime now);
} 