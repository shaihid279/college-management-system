package com.college.cms.repository;

import com.college.cms.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface FeeStructureRepository extends JpaRepository<FeeStructure, Long> {
    List<FeeStructure> findAllByOrderByBranchAscYearAsc();
}