package com.college.cms.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Entity @Table(name = "timetable") @Getter @Setter @NoArgsConstructor
public class TimetableEntry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 60) private String branch;
    @Column(name = "study_year") private Integer year;
    @Column(name = "day_name", length = 15) private String day;
    private LocalTime startTime;
    private LocalTime endTime;
    @Column(length = 120) private String subject;
    @Column(length = 120) private String teacherName;
    @Column(length = 60) private String room;
}