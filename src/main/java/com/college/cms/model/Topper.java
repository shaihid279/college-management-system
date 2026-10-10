package com.college.cms.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "toppers") @Getter @Setter @NoArgsConstructor
public class Topper {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 120) private String studentName;
    @Column(nullable = false, length = 60) private String branch;
    @Column(length = 20) private String academicYear;
    private int rankNo = 1;
    private double percentage;
    @Column(length = 200) private String achievement;
    @Column(length = 200) private String photo;
}