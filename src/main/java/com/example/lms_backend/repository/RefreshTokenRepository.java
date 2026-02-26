package com.example.lms_backend.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import com.example.lms_backend.entity.RefreshToken;
import com.example.lms_backend.entity.User;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
    Optional<RefreshToken> findByToken(String token);

    @Modifying
    @Transactional
    int deleteByUser(User user);

    @Modifying
    @Transactional
    int deleteByUserAndDeviceId(User user, String deviceId);

    Optional<RefreshToken> findByUserAndDeviceId(User user, String deviceId);

    @Modifying
    @Transactional
    int deleteByExpiryDateBefore(Instant now);
}
