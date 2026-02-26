package com.example.lms_backend.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.lms_backend.dto.auth.LoginRequest;
import com.example.lms_backend.dto.auth.RefreshTokenRequest;
import com.example.lms_backend.dto.auth.TokenResponse;
import com.example.lms_backend.entity.RefreshToken;
import com.example.lms_backend.entity.User;
import com.example.lms_backend.exception.BadCredentialsException;
import com.example.lms_backend.repository.RefreshTokenRepository;
import com.example.lms_backend.repository.UserRepository;

import java.util.UUID;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;

    private final RefreshTokenRepository refreshTokenRepository;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtEncoder jwtEncoder,
            RefreshTokenRepository refreshTokenRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        refreshTokenRepository.deleteByUserAndDeviceId(user, request.deviceId());

        String refreshToken = createRefreshToken(user, request.deviceId(), request.deviceName()).getToken();
        String accessToken = generateAccessToken(user);

        return new TokenResponse(accessToken, refreshToken, "Bearer", 15 * 60);
    }

    @Transactional
    public TokenResponse refreshToken(RefreshTokenRequest request) {
        return refreshTokenRepository.findByToken(request.refreshToken())
                .map(token -> {
                    if (token.getExpiryDate().isBefore(Instant.now())) {
                        refreshTokenRepository.delete(token);
                        throw new BadCredentialsException(
                                "Token was expired");
                    }
                    return token;
                })
                .map(token -> {
                    User user = token.getUser();
                    String deviceId = token.getDeviceId();
                    String deviceName = token.getDeviceName();
                    refreshTokenRepository.delete(token); // Rotate the refresh token
                    String newRefreshToken = createRefreshToken(user, deviceId, deviceName).getToken();
                    String accessToken = generateAccessToken(user);
                    return new TokenResponse(accessToken, newRefreshToken, "Bearer", 15 * 60);
                })
                .orElseThrow(() -> new BadCredentialsException("Not valid token"));
    }

    @Transactional
    public void logout(UUID userId, String deviceId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("Invalid user"));
        refreshTokenRepository.deleteByUserAndDeviceId(user, deviceId);
    }

    private String generateAccessToken(User user) {
        Instant now = Instant.now();
        long accessTokenValidity = 15;

        JwtClaimsSet accessClaims = JwtClaimsSet.builder()
                .issuer("lms-system")
                .issuedAt(now)
                .expiresAt(now.plus(accessTokenValidity, ChronoUnit.MINUTES))
                .subject(user.getId().toString())
                .claim("role", user.getRole().name())
                .claim("type", "ACCESS")
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(accessClaims)).getTokenValue();
    }

    private RefreshToken createRefreshToken(User user, String deviceId, String deviceName) {
        RefreshToken refreshToken = new RefreshToken();

        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setDeviceId(deviceId);
        refreshToken.setDeviceName(deviceName);
        refreshToken.setExpiryDate(Instant.now().plus(7, ChronoUnit.DAYS));

        return refreshTokenRepository.save(refreshToken);
    }
}
