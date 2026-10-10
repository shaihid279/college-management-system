package com.college.cms.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity @Table(name = "audit_logs") @Getter @Setter @NoArgsConstructor
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(length = 60) private String actor;
    @Column(length = 60) private String action;
    @Column(length = 60) private String targetIdCard;
    @Column(length = 500) private String details;
    private LocalDateTime createdAt = LocalDateTime.now();
}