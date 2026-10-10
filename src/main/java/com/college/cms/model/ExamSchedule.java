package com.college.cms.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity @Table(name = "exam_schedules") @Getter @Setter @NoArgsConstructor
public class ExamSchedule {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 120) private String subject;
    @Column(nullable = false, length = 60) private String branch;
    @Column(name = "study_year") private Integer year;
    @Column(length = 40) private String examType;
    private LocalDate examDate;
    private LocalTime startTime;
    @Column(length = 60) private String room;
}