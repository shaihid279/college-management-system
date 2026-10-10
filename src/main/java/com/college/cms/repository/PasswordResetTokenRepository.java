package com.college.cms.repository;

import com.college.cms.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);
    void deleteByUser(User user);
}