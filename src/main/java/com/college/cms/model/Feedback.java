package com.college.cms.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity @Table(name = "feedbacks") @Getter @Setter @NoArgsConstructor
public class Feedback {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) private Student student;
    @Column(length = 20) private String feedbackType = "FEEDBACK";
    @Column(nullable = false, length = 200) private String subject;
    @Column(nullable = false, length = 2000) private String message;
    @Column(length = 20) private String status = "OPEN";
    @Column(length = 1000) private String reply;
    private LocalDateTime createdAt = LocalDateTime.now();
}