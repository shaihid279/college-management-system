package com.college.cms.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity @Table(name = "students") @Getter @Setter @NoArgsConstructor
public class Student {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @OneToOne(optional = false) @JoinColumn(name = "user_id", unique = true) private User user;
    @Column(unique = true, nullable = false, length = 40) private String idCardNo;
    @Column(nullable = false, length = 60) private String branch;
    @Column(name = "study_year") private Integer year = 1;
    @Column(precision = 12, scale = 2) private BigDecimal totalFees = BigDecimal.ZERO;
    @Column(precision = 12, scale = 2) private BigDecimal paidFees = BigDecimal.ZERO;
    private int attendedClasses;
    private int totalClasses;

    public BigDecimal getPendingFees() {
        BigDecimal p = totalFees.subtract(paidFees);
        return p.signum() < 0 ? BigDecimal.ZERO : p;
    }
    public double getAttendancePercent() {
        return totalClasses == 0 ? 0 : Math.round(attendedClasses * 1000.0 / totalClasses) / 10.0;
    }
    public boolean isAttendanceLow() { return totalClasses > 0 && getAttendancePercent() < 75; }
    public int getAbsentClasses() { return Math.max(0, totalClasses - attendedClasses); }
}