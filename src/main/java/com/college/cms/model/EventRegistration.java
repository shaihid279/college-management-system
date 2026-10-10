package com.college.cms.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity @Table(name = "event_registrations", uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "student_id"}))
@Getter @Setter @NoArgsConstructor
public class EventRegistration {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) private Event event;
    @ManyToOne(optional = false) private Student student;
    private LocalDateTime registeredAt = LocalDateTime.now();
}