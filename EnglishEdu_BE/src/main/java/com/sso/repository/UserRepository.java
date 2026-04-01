package com.sso.repository;

import com.sso.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Page<User> findByRole(String role, Pageable pageable);

    @Query("SELECT u FROM User u WHERE LOWER(u.username) LIKE %:kw% OR LOWER(u.email) LIKE %:kw% OR LOWER(u.firstName) LIKE %:kw% OR LOWER(u.lastName) LIKE %:kw%")
    Page<User> findByKeyword(@Param("kw") String keyword, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.role = :role AND (LOWER(u.username) LIKE %:kw% OR LOWER(u.email) LIKE %:kw% OR LOWER(u.firstName) LIKE %:kw% OR LOWER(u.lastName) LIKE %:kw%)")
    Page<User> findByRoleAndKeyword(@Param("role") String role, @Param("kw") String keyword, Pageable pageable);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsernameOrEmail(String username, String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
