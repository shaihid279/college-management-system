package com.college.cms.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "marks") @Getter @Setter @NoArgsConstructor
public class Mark {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) private Student student;
    @Column(nullable = false, length = 100) private String subject;
    @Column(nullable = false, length = 60) private String examName;
    private double marksObtained;
    private double maxMarks = 100;

    public double getPercentage() { return maxMarks == 0 ? 0 : Math.round(marksObtained * 1000.0 / maxMarks) / 10.0; }
    public String getGrade() {
        double p = getPercentage();
        if (p >= 90) return "A+";
        if (p >= 80) return "A";
        if (p >= 70) return "B+";
        if (p >= 60) return "B";
        if (p >= 50) return "C";
        if (p >= 40) return "D";
        return "F";
    }
}