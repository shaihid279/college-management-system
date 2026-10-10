package com.college.cms.repository;

import com.college.cms.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface PlacementRepository extends JpaRepository<Placement, Long> {
    List<Placement> findAllByOrderByAcademicYearDescPackageLpaDesc();
    List<Placement> findBySource(String source);
}