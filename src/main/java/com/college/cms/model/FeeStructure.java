package com.college.cms.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity @Table(name = "fee_structure") @Getter @Setter @NoArgsConstructor
public class FeeStructure {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 60) private String branch;
    @Column(name = "study_year") private Integer year;
    @Column(precision = 12, scale = 2) private BigDecimal tuitionFee = BigDecimal.ZERO;
    @Column(precision = 12, scale = 2) private BigDecimal otherFee = BigDecimal.ZERO;
    @Column(length = 200) private String notes;

    public BigDecimal getTotal() { return tuitionFee.add(otherFee); }
}