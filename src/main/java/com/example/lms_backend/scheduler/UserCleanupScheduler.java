package com.example.lms_backend.scheduler;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.lms_backend.entity.User;
import com.example.lms_backend.repository.UserRepository;

@Component
public class UserCleanupScheduler {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserCleanupScheduler(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Run every day at 2 AM
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void anonymizeDeletedUsers() {
        Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);
        List<User> usersToAnonymize = userRepository.findUsersToAnonymize(thirtyDaysAgo);

        for (User user : usersToAnonymize) {
            String pseudoId = user.getId().toString().substring(0, 8);
            user.setEmail("deleted_user_" + pseudoId + "@deleted.local");
            user.setFullName("Deleted User");
            // Set an unusable random password hash
            user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        }

        userRepository.saveAll(usersToAnonymize);
    }
}
