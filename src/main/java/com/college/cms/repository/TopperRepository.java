package com.college.cms.repository;

import com.college.cms.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface TopperRepository extends JpaRepository<Topper, Long> {
    List<Topper> findAllByOrderByAcademicYearDescBranchAscRankNoAsc();
}