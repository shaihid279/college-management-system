package com.college.cms.repository;

import com.college.cms.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    List<Notice> findAllByOrderByImportantDescCreatedAtDesc();
    List<Notice> findTop5ByOrderByImportantDescCreatedAtDesc();
}