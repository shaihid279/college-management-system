package com.college.cms.repository;

import com.college.cms.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByUserUsername(String username);
    boolean existsByIdCardNo(String idCardNo);
}