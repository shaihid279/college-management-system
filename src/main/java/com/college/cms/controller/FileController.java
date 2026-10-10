package com.college.cms.controller;

import com.college.cms.repository.StoredFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.Duration;

/** Photos database se serve hoti hain (Render par disk mit jata hai, DB nahi). */
@Controller
@RequiredArgsConstructor
public class FileController {
    private final StoredFileRepository files;

    @GetMapping("/uploads/{folder}/{name:.+}")
    public ResponseEntity<byte[]> serve(@PathVariable String folder, @PathVariable String name) {
        return files.findById(folder + "/" + name)
                .map(f -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(f.getContentType()))
                        .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePrivate())
                        .body(f.getData()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}