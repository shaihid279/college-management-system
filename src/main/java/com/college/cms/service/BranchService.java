package com.college.cms.service;

import com.college.cms.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

@Service
@RequiredArgsConstructor
public class BranchService {
    private final StudentRepository students;
    private static final List<String> DEFAULTS = List.of(
            "First Year Engineering",
            "Civil Engineering",
            "Electronics and Telecommunication Engineering",
            "Mechanical Engineering",
            "Electrical Engineering",
            "Information Technology",
            "Computer Science & Design",
            "Computer Engineering",
            "Artificial Intelligence and Data Science",
            "Electronics Engineering (VLSI Design and Technology)");

    public List<String> all() {
        TreeSet<String> set = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        set.addAll(DEFAULTS);
        students.findAll().forEach(s -> set.add(s.getBranch()));
        return new ArrayList<>(set);
    }
}