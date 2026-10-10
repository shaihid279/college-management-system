package com.college.cms.service;

import com.college.cms.model.StoredFile;
import com.college.cms.repository.StoredFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageService {
    private static final Map<String, String> TYPES = Map.of(
            "jpg", "image/jpeg", "jpeg", "image/jpeg", "png", "image/png", "webp", "image/webp");
    private static final int MAX_BYTES = 2 * 1024 * 1024;

    private final StoredFileRepository files;

    /** Saves an image in the database and returns "folder/uuid.ext". Returns null for an empty file. */
    public String saveImage(MultipartFile file, String folder) throws IOException {
        if (file == null || file.isEmpty()) return null;
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        int dot = original.lastIndexOf('.');
        String ext = dot >= 0 ? original.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
        String mime = TYPES.get(ext);
        String ct = file.getContentType() == null ? "" : file.getContentType();
        if (mime == null || !ct.startsWith("image/")) {
            throw new IllegalArgumentException("Only JPG, PNG or WEBP images are allowed.");
        }
        byte[] data = file.getBytes();
        if (data.length > MAX_BYTES) throw new IllegalArgumentException("The image is larger than 2 MB.");
        if (!looksLikeImage(data)) throw new IllegalArgumentException("This does not look like a valid image file.");

        StoredFile f = new StoredFile();
        f.setPath(folder + "/" + UUID.randomUUID() + "." + ext);
        f.setContentType(mime);
        f.setData(data);
        files.save(f);
        return f.getPath();
    }

    public void delete(String path) {
        if (path == null || path.isBlank()) return;
        try {
            files.deleteById(path);
        } catch (Exception ignored) { }
    }

    private boolean looksLikeImage(byte[] d) {
        if (d.length < 12) return false;
        boolean jpg = (d[0] & 0xFF) == 0xFF && (d[1] & 0xFF) == 0xD8;
        boolean png = (d[0] & 0xFF) == 0x89 && d[1] == 'P' && d[2] == 'N' && d[3] == 'G';
        boolean webp = d[0] == 'R' && d[1] == 'I' && d[2] == 'F' && d[3] == 'F'
                && d[8] == 'W' && d[9] == 'E' && d[10] == 'B' && d[11] == 'P';
        return jpg || png || webp;
    }
}