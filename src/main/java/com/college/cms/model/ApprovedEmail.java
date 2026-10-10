package com.college.cms.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity @Table(name = "approved_emails") @Getter @Setter @NoArgsConstructor
public class ApprovedEmail {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(unique = true, nullable = false, length = 120) private String email;
    @Column(length = 20) private String role;
    private LocalDateTime approvedAt = LocalDateTime.now();
    @Column(length = 60) private String approvedBy;
}