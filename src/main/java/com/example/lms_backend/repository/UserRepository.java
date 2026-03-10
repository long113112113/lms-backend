package com.example.lms_backend.repository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;
import java.time.Instant;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.example.lms_backend.entity.User;

public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {
    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    @Query(value = "SELECT * FROM users WHERE is_deleted = true AND deleted_at < :thirtyDaysAgo", nativeQuery = true)
    List<User> findUsersToAnonymize(Instant thirtyDaysAgo);
}
