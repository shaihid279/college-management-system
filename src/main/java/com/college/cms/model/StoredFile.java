package com.college.cms.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity @Table(name = "stored_files") @Getter @Setter @NoArgsConstructor
public class StoredFile {
    @Id @Column(length = 200) private String path;
    @Column(nullable = false, length = 60) private String contentType;
    @Lob @Column(nullable = false, columnDefinition = "LONGBLOB") private byte[] data;
    private LocalDateTime createdAt = LocalDateTime.now();
}