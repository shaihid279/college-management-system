package com.college.cms.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity @Table(name = "payments") @Getter @Setter @NoArgsConstructor
public class Payment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) private Student student;
    @Column(precision = 12, scale = 2, nullable = false) private BigDecimal amount;
    private LocalDate paidOn = LocalDate.now();
    @Column(length = 30) private String payMode;
    @Column(unique = true, length = 40) private String receiptNo;
    @Column(length = 200) private String note;
    @Column(length = 60) private String recordedBy;
}