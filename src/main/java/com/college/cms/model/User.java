package com.college.cms.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity @Table(name = "users") @Getter @Setter @NoArgsConstructor
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(unique = true, nullable = false, length = 60) private String username;
    @Column(nullable = false) private String password;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Role role;
    @Column(nullable = false, length = 120) private String fullName;
    @Column(length = 120) private String email;
    @Column(length = 20) private String phone;
    @Column(length = 300) private String address;
    @Column(length = 200) private String photo;
    @Column(length = 100) private String department;
    private boolean enabled = true;
    @Column(length = 20) private String approval = "APPROVED";
    private LocalDateTime createdAt = LocalDateTime.now();

    public String getInitial() { return fullName == null || fullName.isBlank() ? "?" : fullName.substring(0, 1).toUpperCase(); }
    public boolean isAdmin() { return role == Role.ADMIN; }
    public boolean isTeacher() { return role == Role.TEACHER; }
    public boolean isStudent() { return role == Role.STUDENT; }
    public boolean isOrganizer() { return role == Role.ORGANIZER; }
    public boolean isApproved() { return approval == null || "APPROVED".equals(approval); }
    public boolean isPending() { return "PENDING".equals(approval); }
}