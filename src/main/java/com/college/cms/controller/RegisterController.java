package com.college.cms.controller;

import com.college.cms.model.*;
import com.college.cms.repository.*;
import com.college.cms.service.BranchService;
import com.college.cms.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class RegisterController {
    private static final Map<String, Role> ROLES =
            Map.of("student", Role.STUDENT, "teacher", Role.TEACHER, "organizer", Role.ORGANIZER);

    private final UserRepository users;
    private final StudentRepository students;
    private final ApprovedEmailRepository approvedEmails;
    private final PasswordEncoder encoder;
    private final FileStorageService storage;
    private final BranchService branchService;

    @GetMapping("/register")
    public String root() { return "redirect:/register/student"; }

    @GetMapping("/register/{role}")
    public String form(@PathVariable String role, Authentication auth, Model m) {
        if (!ROLES.containsKey(role)) return "redirect:/register/student";
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) return "redirect:/dashboard";
        m.addAttribute("role", role);
        m.addAttribute("needsApproval", ROLES.get(role) != Role.STUDENT);
        m.addAttribute("branches", branchService.all());
        return "register";
    }

    @PostMapping("/register/{role}")
    @Transactional
    public String submit(@PathVariable String role, @RequestParam String fullName, @RequestParam String username,
                         @RequestParam String email, @RequestParam(defaultValue = "") String phone,
                         @RequestParam String password, @RequestParam String confirm,
                         @RequestParam(defaultValue = "") String idCardNo, @RequestParam(defaultValue = "") String branch,
                         @RequestParam(required = false) Integer year,
                         @RequestParam(defaultValue = "") String department,
                         @RequestParam(required = false) MultipartFile photo,
                         @RequestParam(defaultValue = "") String website, RedirectAttributes ra) {
        Role r = ROLES.get(role);
        if (r == null) return "redirect:/register/student";
        if (!website.isBlank()) return "redirect:/"; // spam bot (honeypot)

        fullName = fullName.trim();
        username = username.trim();
        email = email.trim().toLowerCase();
        phone = phone.trim();
        idCardNo = idCardNo.trim();
        branch = branch.trim();
        department = department.trim();

        Map<String, String> keep = new HashMap<>();
        keep.put("fullName", fullName);
        keep.put("username", username);
        keep.put("email", email);
        keep.put("phone", phone);
        keep.put("idCardNo", idCardNo);
        keep.put("branch", branch);
        keep.put("department", department);
        keep.put("year", year == null ? "" : year.toString());

        String err = null;
        if (fullName.length() < 2 || fullName.length() > 100) err = "The name should be between 2 and 100 characters.";
        else if (!username.matches("^[A-Za-z0-9._-]{3,40}$")) err = "Username 3-40 character only (letters, numbers, . _ -).";
        else if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$") || email.length() > 120) err = "Write the correct email.";
        else if (!phone.matches("^[0-9+\\- ]{0,15}$")) err = "Phone number is invalid.";
        else if (password.length() < 8) err = "Password must be at least 8 characters long.";
        else if (!password.equals(confirm)) err = "Both passwords do not match.";
        else if (users.existsByUsername(username)) err = "This username is already taken.";
        else if (users.findFirstByEmailIgnoreCase(email).isPresent()) err = "There is already an account with this email address.";
        else if (r == Role.TEACHER && department.isEmpty()) err = "Choose your department.";
        else if (r == Role.STUDENT) {
            if (!idCardNo.matches("^[A-Za-z0-9/_-]{3,40}$")) err = "Please enter the correct ID card number.";
            else if (branch.isEmpty()) err = "Choose branch.";
            else if (year == null || year < 1 || year > 4) err = "Choose between Year 1 to Year 4.";
            else if (students.existsByIdCardNo(idCardNo)) err = "This ID card number is already registered.";
        }
        if (err != null) return fail(ra, role, err, keep);

        String photoPath;
        try {
            photoPath = storage.saveImage(photo, "profiles");
        } catch (IllegalArgumentException e) {
            return fail(ra, role, e.getMessage(), keep);
        } catch (IOException e) {
            return fail(ra, role, "The photo could not be saved, please try again.", keep);
        }

        User u = new User();
        u.setUsername(username);
        u.setPassword(encoder.encode(password));
        u.setRole(r);
        u.setFullName(fullName);
        u.setEmail(email);
        u.setPhone(phone.isEmpty() ? null : phone);
        u.setPhoto(photoPath);
        u.setDepartment(r == Role.TEACHER ? department : null);
        boolean preApproved = r == Role.STUDENT || approvedEmails.existsByEmailIgnoreCase(email);
        u.setApproval(preApproved ? "APPROVED" : "PENDING");
        u = users.save(u);

        if (r == Role.STUDENT) {
            Student s = new Student();
            s.setUser(u);
            s.setIdCardNo(idCardNo);
            s.setBranch(branch);
            s.setYear(year);
            students.save(s);
            ra.addFlashAttribute("success", "Registration done! Login now.");
        } else if (preApproved) {
            ra.addFlashAttribute("success", "Registration ho gaya. Aapka email pehle se approved hai, ab login kar sakte hain.");
        } else {
            ra.addFlashAttribute("success", "Registration is complete. You can log in after admin approval.");
        }
        return "redirect:/login/" + role;
    }

    private String fail(RedirectAttributes ra, String role, String msg, Map<String, String> keep) {
        ra.addFlashAttribute("error", msg);
        ra.addFlashAttribute("regForm", keep);
        return "redirect:/register/" + role;
    }
}