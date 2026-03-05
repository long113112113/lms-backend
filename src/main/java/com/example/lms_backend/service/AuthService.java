package com.example.lms_backend.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.lms_backend.dto.auth.LoginRequest;
import com.example.lms_backend.dto.auth.RegisterRequest;
import com.example.lms_backend.entity.RefreshToken;
import com.example.lms_backend.entity.User;
import com.example.lms_backend.entity.enums.Role;
import com.example.lms_backend.exception.BadCredentialsException;
import com.example.lms_backend.exception.ResourceAlreadyExistsException;
import com.example.lms_backend.repository.RefreshTokenRepository;
import com.example.lms_backend.repository.UserRepository;

@Service
public class AuthService {
    // SECURITY: Audit logging added to track authentication events
    private static final Logger securityLog = LoggerFactory.getLogger("SECURITY_AUDIT");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final RefreshTokenRepository refreshTokenRepository;

    private static final long REFRESH_TOKEN_VALIDITY_DAYS = 7;
    private static final long ACCESS_TOKEN_VALIDITY_MINUTES = 15;

    public record AuthResult(String accessToken, String refreshToken) {
    }

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtEncoder jwtEncoder,
            RefreshTokenRepository refreshTokenRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional
    public AuthResult register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            securityLog.warn("REGISTER_FAILURE email={} reason=email_exists", request.email());
            throw new ResourceAlreadyExistsException("Email already exists");
        }
        var user = new User();
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setRole(Role.STUDENT);
        var savedUser = userRepository.save(user);

        securityLog.info("REGISTER_SUCCESS userId={} email={} role={}", savedUser.getId(), savedUser.getEmail(), savedUser.getRole());

        RefreshToken refreshToken = createRefreshToken(savedUser, request.deviceId(), request.deviceName());
        String accessToken = generateAccessToken(savedUser);
        return new AuthResult(accessToken, refreshToken.getToken());
    }

    @Transactional
    public AuthResult login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> {
                    securityLog.warn("LOGIN_FAILURE email={} deviceId={} reason=invalid_credentials", request.email(), request.deviceId());
                    return new BadCredentialsException("Invalid email or password");
                });
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            securityLog.warn("LOGIN_FAILURE email={} deviceId={} reason=invalid_credentials", request.email(), request.deviceId());
            throw new BadCredentialsException("Invalid email or password");
        }
        RefreshToken refreshToken = refreshTokenRepository
                .findByUserAndDeviceId(user, request.deviceId())
                .map(existing -> {
                    existing.setToken(UUID.randomUUID().toString());
                    existing.setExpiryDate(Instant.now().plus(REFRESH_TOKEN_VALIDITY_DAYS, ChronoUnit.DAYS));
                    existing.setDeviceName(request.deviceName());
                    return existing;
                })
                .orElseGet(() -> createRefreshToken(user, request.deviceId(), request.deviceName()));

        String accessToken = generateAccessToken(user);
        securityLog.info("LOGIN_SUCCESS userId={} email={} deviceId={}", user.getId(), user.getEmail(), request.deviceId());
        return new AuthResult(accessToken, refreshToken.getToken());
    }

    @Transactional
    public AuthResult refreshToken(String refreshTokenValue) {
        return refreshTokenRepository.findByToken(refreshTokenValue)
                .map(token -> {
                    if (token.getExpiryDate().isBefore(Instant.now())) {
                        refreshTokenRepository.delete(token);
                        securityLog.warn("TOKEN_REFRESH_FAILURE userId={} deviceId={} reason=expired_token", token.getUser().getId(), token.getDeviceId());
                        throw new BadCredentialsException("Token was expired");
                    }
                    token.setToken(UUID.randomUUID().toString());
                    token.setExpiryDate(Instant.now().plus(REFRESH_TOKEN_VALIDITY_DAYS, ChronoUnit.DAYS));

                    User user = token.getUser();
                    String accessToken = generateAccessToken(user);
                    securityLog.info("TOKEN_REFRESH_SUCCESS userId={} deviceId={}", user.getId(), token.getDeviceId());
                    return new AuthResult(accessToken, token.getToken());
                })
                .orElseThrow(() -> {
                    securityLog.warn("TOKEN_REFRESH_FAILURE reason=invalid_token");
                    return new BadCredentialsException("Not valid token");
                });
    }

    @Transactional
    public void logout(String refreshTokenValue) {
        refreshTokenRepository.findByToken(refreshTokenValue)
                .ifPresent(token -> {
                    securityLog.info("LOGOUT_SUCCESS userId={} deviceId={}", token.getUser().getId(), token.getDeviceId());
                    refreshTokenRepository.delete(token);
                });
    }

    private String generateAccessToken(User user) {
        Instant now = Instant.now();

        JwtClaimsSet accessClaims = JwtClaimsSet.builder()
                .issuer("lms-system")
                .issuedAt(now)
                .expiresAt(now.plus(ACCESS_TOKEN_VALIDITY_MINUTES, ChronoUnit.MINUTES))
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
        refreshToken.setExpiryDate(Instant.now().plus(REFRESH_TOKEN_VALIDITY_DAYS, ChronoUnit.DAYS));

        return refreshTokenRepository.save(refreshToken);
    }
}
