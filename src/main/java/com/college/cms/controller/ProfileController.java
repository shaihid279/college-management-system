package com.college.cms.controller;

import com.college.cms.model.User;
import com.college.cms.repository.StudentRepository;
import com.college.cms.repository.UserRepository;
import com.college.cms.service.BranchService;
import com.college.cms.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

@Controller
@RequiredArgsConstructor
public class ProfileController {
    private final UserRepository users;
    private final StudentRepository students;
    private final PasswordEncoder encoder;
    private final FileStorageService storage;
    private final BranchService branchService;

    @GetMapping("/profile")
    public String profile(Authentication auth, Model m) {
        m.addAttribute("user", users.findByUsername(auth.getName()).orElseThrow());
        m.addAttribute("branches", branchService.all());
        students.findByUserUsername(auth.getName()).ifPresent(s -> m.addAttribute("student", s));
        return "profile";
    }

    @PostMapping("/profile")
    public String save(Authentication auth, @RequestParam String fullName,
                       @RequestParam(defaultValue = "") String email, @RequestParam(defaultValue = "") String phone,
                       @RequestParam(defaultValue = "") String address,
                       @RequestParam(defaultValue = "") String department,
                       @RequestParam(required = false) MultipartFile photo,
                       @RequestParam(defaultValue = "false") boolean removePhoto, RedirectAttributes ra) {
        User u = users.findByUsername(auth.getName()).orElseThrow();
        fullName = fullName.trim();
        email = email.trim();
        phone = phone.trim();
        if (fullName.length() < 2 || fullName.length() > 100) return fail(ra, "Naam 2 se 100 character ka hona chahiye.");
        if (!email.isEmpty() && !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) return fail(ra, "Email sahi nahi hai.");
        if (!phone.matches("^[0-9+\\- ]{0,15}$")) return fail(ra, "Phone number sahi nahi hai.");
        try {
            String saved = storage.saveImage(photo, "profiles");
            if (saved != null) {
                storage.delete(u.getPhoto());
                u.setPhoto(saved);
            } else if (removePhoto) {
                storage.delete(u.getPhoto());
                u.setPhoto(null);
            }
        } catch (IllegalArgumentException e) {
            return fail(ra, e.getMessage());
        } catch (IOException e) {
            return fail(ra, "Photo save nahi ho payi, dobara try karein.");
        }
        u.setFullName(fullName);
        u.setEmail(email.isEmpty() ? null : email);
        u.setPhone(phone.isEmpty() ? null : phone);
        u.setAddress(address.trim().length() > 290 ? address.trim().substring(0, 290) : address.trim());
        if (u.isTeacher() && !department.isBlank()) u.setDepartment(department.trim());
        users.save(u);
        ra.addFlashAttribute("success", "Profile update ho gayi.");
        return "redirect:/profile";
    }

    @GetMapping("/change-password")
    public String changePasswordPage() { return "change-password"; }

    @PostMapping("/change-password")
    public String changePassword(Authentication auth, @RequestParam String current, @RequestParam String password,
                                 @RequestParam String confirm, RedirectAttributes ra) {
        User u = users.findByUsername(auth.getName()).orElseThrow();
        if (!encoder.matches(current, u.getPassword())) {
            ra.addFlashAttribute("error", "Purana password galat hai.");
            return "redirect:/change-password";
        }
        if (password.length() < 8 || !password.equals(confirm)) {
            ra.addFlashAttribute("error", "Naya password kam se kam 8 character ka ho aur dono match karein.");
            return "redirect:/change-password";
        }
        u.setPassword(encoder.encode(password));
        users.save(u);
        ra.addFlashAttribute("success", "Password badal gaya.");
        return "redirect:/profile";
    }

    private String fail(RedirectAttributes ra, String msg) {
        ra.addFlashAttribute("error", msg);
        return "redirect:/profile";
    }
}