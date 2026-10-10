package com.college.cms.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "placements") @Getter @Setter @NoArgsConstructor
public class Placement {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 150) private String company;
    @Column(length = 120) private String jobRole;
    private double packageLpa;
    private int studentsPlaced;
    @Column(length = 20) private String academicYear;
    @Column(length = 10) private String source = "MANUAL";
}