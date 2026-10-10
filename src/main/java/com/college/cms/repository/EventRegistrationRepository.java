package com.college.cms.repository;

import com.college.cms.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface EventRegistrationRepository extends JpaRepository<EventRegistration, Long> {
    boolean existsByEventAndStudent(Event event, Student student);
    Optional<EventRegistration> findByEventAndStudent(Event event, Student student);
    List<EventRegistration> findByEventOrderByRegisteredAtAsc(Event event);
    List<EventRegistration> findByStudent(Student student);
    long countByEvent(Event event);
    void deleteByEvent(Event event);
}