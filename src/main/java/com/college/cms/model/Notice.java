package com.college.cms.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Entity @Table(name = "notices") @Getter @Setter @NoArgsConstructor
public class Notice {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 200) private String title;
    @Column(length = 3000) private String content;
    private boolean important;
    private LocalDateTime createdAt = LocalDateTime.now();
    @Column(length = 60) private String postedBy;
    /** "ALL" ya "Branch1|Branch2". Purane notices (null) sabke liye maane jate hain. */
    @Column(length = 1000) private String targetBranches = "ALL";

    public boolean isForAll() { return targetBranches == null || targetBranches.isBlank() || "ALL".equals(targetBranches); }
    public List<String> getTargetList() { return isForAll() ? List.of() : Arrays.asList(targetBranches.split("\\|")); }
}