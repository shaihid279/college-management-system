package com.college.cms.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Entity @Table(name = "events") @Getter @Setter @NoArgsConstructor
public class Event {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 200) private String title;
    @Column(length = 3000) private String description;
    @Column(nullable = false) private LocalDateTime eventDateTime;
    @Column(length = 200) private String venue;
    @Column(length = 200) private String banner;
    @ManyToOne private User createdBy;
    /** "ALL" ya "Branch1|Branch2". Purane events (null) sabke liye maane jate hain. */
    @Column(length = 1000) private String targetBranches = "ALL";

    public boolean isForAll() { return targetBranches == null || targetBranches.isBlank() || "ALL".equals(targetBranches); }
    public List<String> getTargetList() { return isForAll() ? List.of() : Arrays.asList(targetBranches.split("\\|")); }
}