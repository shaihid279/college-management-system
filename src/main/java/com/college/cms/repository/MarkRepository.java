package com.college.cms.repository;

import com.college.cms.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface MarkRepository extends JpaRepository<Mark, Long> {
    List<Mark> findByStudentOrderByExamNameAscSubjectAsc(Student student);
}