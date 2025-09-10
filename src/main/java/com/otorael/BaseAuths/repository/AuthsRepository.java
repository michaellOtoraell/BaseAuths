package com.otorael.BaseAuths.repository;

import com.otorael.BaseAuths.model.Auths;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthsRepository extends JpaRepository<Auths, Long> {

    Optional<Auths> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<Auths> findByResetToken(String resetToken);
}

