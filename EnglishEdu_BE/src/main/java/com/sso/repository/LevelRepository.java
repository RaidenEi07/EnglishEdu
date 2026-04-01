package com.sso.repository;

import com.sso.entity.Level;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LevelRepository extends JpaRepository<Level, Long> {
    Optional<Level> findBySlug(String slug);
    Optional<Level> findByNameIgnoreCase(String name);
}
