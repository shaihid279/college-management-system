package com.college.cms.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity @Table(name = "contact_messages") @Getter @Setter @NoArgsConstructor
public class ContactMessage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 120) private String name;
    @Column(nullable = false, length = 120) private String email;
    @Column(length = 200) private String subject;
    @Column(nullable = false, length = 2000) private String message;
    private boolean seen;
    private LocalDateTime createdAt = LocalDateTime.now();
}