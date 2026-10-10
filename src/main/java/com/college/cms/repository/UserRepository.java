package com.college.cms.repository;

import com.college.cms.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findFirstByEmailIgnoreCase(String email);
    boolean existsByUsername(String username);
    long countByRole(Role role);
    long countByApproval(String approval);
    List<User> findAllByOrderByFullNameAsc();
    List<User> findByRoleOrderByFullNameAsc(Role role);
    List<User> findByApprovalOrderByCreatedAtAsc(String approval);
}