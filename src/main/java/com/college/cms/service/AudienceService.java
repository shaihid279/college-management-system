package com.college.cms.service;

import com.college.cms.model.User;
import com.college.cms.repository.StudentRepository;
import com.college.cms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Notice / event kis ko dikhna chahiye.
 * Admin aur Organizer sab dekhte hain. Student apni branch ke, Teacher apne department ke.
 * Target "ALL" ho to sabko dikhta hai (login ke bina bhi).
 */
@Service
@RequiredArgsConstructor
public class AudienceService {
    public record Viewer(boolean seesAll, String branch) { }

    private final UserRepository users;
    private final StudentRepository students;

    public Viewer viewer(Authentication auth) {
        if (auth == null || auth instanceof AnonymousAuthenticationToken || !auth.isAuthenticated()) {
            return new Viewer(false, null);
        }
        User u = users.findByUsername(auth.getName()).orElse(null);
        if (u == null) return new Viewer(false, null);
        if (u.isAdmin() || u.isOrganizer()) return new Viewer(true, null);
        if (u.isStudent()) {
            return new Viewer(false, students.findByUserUsername(u.getUsername()).map(s -> s.getBranch()).orElse(null));
        }
        return new Viewer(false, u.getDepartment());
    }

    public boolean visible(Viewer v, String target) {
        if (v.seesAll()) return true;
        if (target == null || target.isBlank() || "ALL".equals(target)) return true;
        if (v.branch() == null || v.branch().isBlank()) return false;
        for (String b : target.split("\\|")) {
            if (b.equalsIgnoreCase(v.branch())) return true;
        }
        return false;
    }

    /** Form se target string banata hai. "Sabhi" tick ho to "ALL"; kuch nahi chuna to null (error). */
    public String buildTarget(boolean allDepartments, List<String> picked, Collection<String> valid) {
        if (allDepartments) return "ALL";
        if (picked == null) return null;
        List<String> ok = new ArrayList<>();
        for (String p : picked) {
            for (String v : valid) {
                if (v.equalsIgnoreCase(p.trim()) && !ok.contains(v)) ok.add(v);
            }
        }
        return ok.isEmpty() ? null : String.join("|", ok);
    }
}