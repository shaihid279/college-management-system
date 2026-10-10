package com.college.cms.repository;

import com.college.cms.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface ExamScheduleRepository extends JpaRepository<ExamSchedule, Long> {
    List<ExamSchedule> findAllByOrderByExamDateAscStartTimeAsc();
}