package com.college.cms.repository;

import com.college.cms.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findTop300ByOrderByCreatedAtDesc();
    List<AuditLog> findTop100ByActorOrderByCreatedAtDesc(String actor);
}