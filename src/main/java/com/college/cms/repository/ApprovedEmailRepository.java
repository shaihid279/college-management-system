package com.college.cms.repository;

import com.college.cms.model.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovedEmailRepository extends JpaRepository<ApprovedEmail, Long> {
    boolean existsByEmailIgnoreCase(String email);
}