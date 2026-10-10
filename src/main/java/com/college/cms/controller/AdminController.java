package com.college.cms.controller;

import com.college.cms.model.*;
import com.college.cms.repository.*;
import com.college.cms.service.AuditService;
import com.college.cms.service.BranchService;
import com.college.cms.service.FileStorageService;
import com.college.cms.service.PlacementService;
import com.college.cms.service.AudienceService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
    private final UserRepository users;
    private final EventRepository eventRepo;
    private final PasswordResetTokenRepository resetTokens;
    private final ApprovedEmailRepository approvedEmails;
    private final StudentRepository students;
    private final NoticeRepository notices;
    private final PlacementRepository placements;
    private final TopperRepository toppers;
    private final FeeStructureRepository fees;
    private final ExamScheduleRepository exams;
    private final TimetableRepository timetable;
    private final AuditLogRepository auditRepo;
    private final FeedbackRepository feedbackRepo;
    private final ContactMessageRepository contacts;
    private final PasswordEncoder encoder;
    private final FileStorageService storage;
    private final PlacementService placementService;
    private final BranchService branchService;
    private final AudienceService audience;
    private final AuditService audit;

    // ---------------- Dashboard ----------------
    @GetMapping("/dashboard")
    public String dashboard(Model m) {
        m.addAttribute("students", users.countByRole(Role.STUDENT));
        m.addAttribute("teachers", users.countByRole(Role.TEACHER));
        m.addAttribute("organizers", users.countByRole(Role.ORGANIZER));
        m.addAttribute("noticeCount", notices.count());
        m.addAttribute("openFeedback", feedbackRepo.countByStatus("OPEN"));
        m.addAttribute("unreadContacts", contacts.countBySeenFalse());
        m.addAttribute("placementStatus", placementService.getLastStatus());
        m.addAttribute("recent", auditRepo.findTop300ByOrderByCreatedAtDesc().stream().limit(6).toList());
        return "admin/dashboard";
    }

    // ---------------- Users ----------------
    @GetMapping("/users")
    public String usersPage(@RequestParam(required = false) Role role, Model m) {
        Map<Long, Student> byUser = new HashMap<>();
        students.findAll().forEach(s -> byUser.put(s.getUser().getId(), s));
        m.addAttribute("users", role == null ? users.findAllByOrderByFullNameAsc() : users.findByRoleOrderByFullNameAsc(role));
        m.addAttribute("studentByUser", byUser);
        m.addAttribute("branches", branchService.all());
        m.addAttribute("fRole", role == null ? "" : role.name());
        return "admin/users";
    }

    @PostMapping("/users")
    @Transactional
    public String createUser(@RequestParam String username, @RequestParam String fullName,
                             @RequestParam(defaultValue = "") String email, @RequestParam(defaultValue = "") String phone,
                             @RequestParam Role role, @RequestParam String password,
                             @RequestParam(defaultValue = "") String idCardNo, @RequestParam(defaultValue = "") String branch,
                             @RequestParam(required = false) Integer year,
                             @RequestParam(defaultValue = "") String department,
                             @RequestParam(required = false) BigDecimal totalFees,
                             Authentication auth, RedirectAttributes ra) {
        username = username.trim();
        fullName = fullName.trim();
        idCardNo = idCardNo.trim();
        branch = branch.trim();
        if (role == Role.ADMIN) return fail(ra, "Admin account yahan se nahi banta.");
        if (!username.matches("^[A-Za-z0-9._-]{3,40}$")) return fail(ra, "Username 3-40 character ka ho (letters, numbers, . _ -).");
        if (fullName.length() < 2) return fail(ra, "Poora naam likhein.");
        if (password.length() < 8) return fail(ra, "Password kam se kam 8 character ka ho.");
        if (users.existsByUsername(username)) return fail(ra, "Ye username pehle se hai.");
        if (role == Role.STUDENT) {
            if (idCardNo.isEmpty() || branch.isEmpty() || year == null) return fail(ra, "Student ke liye ID card no, branch aur year zaruri hai.");
            if (students.existsByIdCardNo(idCardNo)) return fail(ra, "Ye ID card number pehle se hai.");
        }
        User u = new User();
        u.setUsername(username);
        u.setPassword(encoder.encode(password));
        u.setRole(role);
        u.setFullName(fullName);
        u.setEmail(email.isBlank() ? null : email.trim());
        u.setPhone(phone.isBlank() ? null : phone.trim());
        if (role == Role.TEACHER && !department.isBlank()) u.setDepartment(department.trim());
        u = users.save(u);
        if (role == Role.STUDENT) {
            Student s = new Student();
            s.setUser(u);
            s.setIdCardNo(idCardNo);
            s.setBranch(branch);
            s.setYear(year);
            s.setTotalFees(totalFees == null || totalFees.signum() < 0 ? BigDecimal.ZERO : totalFees);
            students.save(s);
        }
        audit.log(auth.getName(), "USER_CREATED", role == Role.STUDENT ? idCardNo : null, username + " (" + role + ")");
        ra.addFlashAttribute("success", "Account ban gaya: " + username);
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/toggle")
    public String toggle(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        User u = users.findById(id).orElse(null);
        if (u == null) return fail(ra, "User nahi mila.");
        if (u.getUsername().equals(auth.getName())) return fail(ra, "Aap khud ko disable nahi kar sakte.");
        u.setEnabled(!u.isEnabled());
        users.save(u);
        audit.log(auth.getName(), u.isEnabled() ? "USER_ENABLED" : "USER_DISABLED", null, u.getUsername());
        ra.addFlashAttribute("success", u.getUsername() + (u.isEnabled() ? " enable" : " disable") + " ho gaya.");
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/password")
    public String resetPassword(@PathVariable Long id, @RequestParam String newPassword, Authentication auth, RedirectAttributes ra) {
        User u = users.findById(id).orElse(null);
        if (u == null) return fail(ra, "User nahi mila.");
        if (newPassword.length() < 8) return fail(ra, "Password kam se kam 8 character ka ho.");
        u.setPassword(encoder.encode(newPassword));
        users.save(u);
        audit.log(auth.getName(), "PASSWORD_RESET", null, u.getUsername());
        ra.addFlashAttribute("success", u.getUsername() + " ka password badal diya.");
        return "redirect:/admin/users";
    }

    // ---------------- Notices ----------------
    @PostMapping("/notices")
    public String addNotice(@RequestParam String title, @RequestParam(defaultValue = "") String content,
                            @RequestParam(defaultValue = "false") boolean important,
                            @RequestParam(defaultValue = "false") boolean allDepartments,
                            @RequestParam(required = false) List<String> branches,
                            Authentication auth, RedirectAttributes ra) {
        if (title.isBlank() || title.length() > 190) {
            ra.addFlashAttribute("error", "Notice ka title likhein.");
            return "redirect:/notices";
        }
        String target = audience.buildTarget(allDepartments, branches, branchService.all());
        if (target == null) {
            ra.addFlashAttribute("error", "Kam se kam ek department chunein ya 'Sabhi departments' tick karein.");
            return "redirect:/notices";
        }
        Notice n = new Notice();
        n.setTitle(title.trim());
        n.setContent(content.trim().length() > 2900 ? content.trim().substring(0, 2900) : content.trim());
        n.setImportant(important);
        n.setTargetBranches(target);
        n.setPostedBy(auth.getName());
        notices.save(n);
        ra.addFlashAttribute("success", "Notice post ho gaya.");
        return "redirect:/notices";
    }
    @PostMapping("/notices/{id}/delete")
    public String deleteNotice(@PathVariable Long id, RedirectAttributes ra) {
        notices.deleteById(id);
        ra.addFlashAttribute("success", "Notice delete ho gaya.");
        return "redirect:/notices";
    }

    // ---------------- Placements ----------------
    @PostMapping("/placements")
    public String addPlacement(@RequestParam String company, @RequestParam(defaultValue = "") String jobRole,
                               @RequestParam(defaultValue = "0") double packageLpa,
                               @RequestParam(defaultValue = "0") int studentsPlaced,
                               @RequestParam(defaultValue = "") String academicYear, RedirectAttributes ra) {
        if (company.isBlank()) {
            ra.addFlashAttribute("error", "Company ka naam likhein.");
            return "redirect:/placements";
        }
        Placement p = new Placement();
        p.setCompany(company.trim());
        p.setJobRole(jobRole.trim());
        p.setPackageLpa(Math.max(0, packageLpa));
        p.setStudentsPlaced(Math.max(0, studentsPlaced));
        p.setAcademicYear(academicYear.trim());
        p.setSource("MANUAL");
        placements.save(p);
        ra.addFlashAttribute("success", "Placement record add ho gaya.");
        return "redirect:/placements";
    }

    @PostMapping("/placements/sync")
    public String syncPlacements(RedirectAttributes ra) {
        String msg = placementService.syncFromWebsite();
        ra.addFlashAttribute(placementService.isSourceOnline() ? "success" : "error", msg);
        return "redirect:/placements";
    }

    @PostMapping("/placements/import")
    public String importPlacements(@RequestParam MultipartFile file, RedirectAttributes ra) {
        try {
            int n = placementService.importFile(file);
            ra.addFlashAttribute("success", n + " placement records import ho gaye.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("error", "File padhne me error aaya. Format check karein.");
        }
        return "redirect:/placements";
    }

    @PostMapping("/placements/{id}/delete")
    public String deletePlacement(@PathVariable Long id, RedirectAttributes ra) {
        placements.deleteById(id);
        ra.addFlashAttribute("success", "Record delete ho gaya.");
        return "redirect:/placements";
    }

    // ---------------- Toppers ----------------
    @PostMapping("/toppers")
    public String addTopper(@RequestParam String studentName, @RequestParam String branch,
                            @RequestParam(defaultValue = "") String academicYear,
                            @RequestParam(defaultValue = "1") int rankNo, @RequestParam(defaultValue = "0") double percentage,
                            @RequestParam(defaultValue = "") String achievement,
                            @RequestParam(required = false) MultipartFile photo, RedirectAttributes ra) {
        if (studentName.isBlank() || branch.isBlank()) {
            ra.addFlashAttribute("error", "Naam aur branch zaruri hai.");
            return "redirect:/toppers";
        }
        Topper t = new Topper();
        try {
            t.setPhoto(storage.saveImage(photo, "toppers"));
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/toppers";
        } catch (IOException e) {
            ra.addFlashAttribute("error", "Photo save nahi ho payi.");
            return "redirect:/toppers";
        }
        t.setStudentName(studentName.trim());
        t.setBranch(branch.trim());
        t.setAcademicYear(academicYear.trim());
        t.setRankNo(Math.max(1, rankNo));
        t.setPercentage(percentage);
        t.setAchievement(achievement.trim().length() > 190 ? achievement.trim().substring(0, 190) : achievement.trim());
        toppers.save(t);
        ra.addFlashAttribute("success", "Topper add ho gaya.");
        return "redirect:/toppers";
    }

    @PostMapping("/toppers/{id}/delete")
    public String deleteTopper(@PathVariable Long id, RedirectAttributes ra) {
        toppers.findById(id).ifPresent(t -> {
            storage.delete(t.getPhoto());
            toppers.delete(t);
        });
        ra.addFlashAttribute("success", "Topper delete ho gaya.");
        return "redirect:/toppers";
    }

    // ---------------- Fee structure ----------------
    @PostMapping("/fees")
    public String addFee(@RequestParam String branch, @RequestParam Integer year, @RequestParam BigDecimal tuitionFee,
                         @RequestParam(defaultValue = "0") BigDecimal otherFee,
                         @RequestParam(defaultValue = "") String notes, RedirectAttributes ra) {
        if (branch.isBlank() || tuitionFee.signum() < 0 || otherFee.signum() < 0) {
            ra.addFlashAttribute("error", "Branch aur fees sahi bharein.");
            return "redirect:/fees";
        }
        FeeStructure f = new FeeStructure();
        f.setBranch(branch.trim());
        f.setYear(year);
        f.setTuitionFee(tuitionFee);
        f.setOtherFee(otherFee);
        f.setNotes(notes.trim().length() > 190 ? notes.trim().substring(0, 190) : notes.trim());
        fees.save(f);
        ra.addFlashAttribute("success", "Fee structure add ho gaya.");
        return "redirect:/fees";
    }

    @PostMapping("/fees/{id}/delete")
    public String deleteFee(@PathVariable Long id, RedirectAttributes ra) {
        fees.deleteById(id);
        ra.addFlashAttribute("success", "Fee structure delete ho gaya.");
        return "redirect:/fees";
    }

    // ---------------- Exam schedule ----------------
    @PostMapping("/exams")
    public String addExam(@RequestParam String subject, @RequestParam String branch, @RequestParam Integer year,
                          @RequestParam(defaultValue = "") String examType,
                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate examDate,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
                          @RequestParam(defaultValue = "") String room, RedirectAttributes ra) {
        if (subject.isBlank() || branch.isBlank()) {
            ra.addFlashAttribute("error", "Subject aur branch zaruri hai.");
            return "redirect:/exams";
        }
        ExamSchedule e = new ExamSchedule();
        e.setSubject(subject.trim());
        e.setBranch(branch.trim());
        e.setYear(year);
        e.setExamType(examType.trim());
        e.setExamDate(examDate);
        e.setStartTime(startTime);
        e.setRoom(room.trim());
        exams.save(e);
        ra.addFlashAttribute("success", "Exam add ho gaya.");
        return "redirect:/exams";
    }

    @PostMapping("/exams/{id}/delete")
    public String deleteExam(@PathVariable Long id, RedirectAttributes ra) {
        exams.deleteById(id);
        ra.addFlashAttribute("success", "Exam delete ho gaya.");
        return "redirect:/exams";
    }

    // ---------------- Timetable ----------------
    @PostMapping("/timetable")
    public String addTimetable(@RequestParam String branch, @RequestParam Integer year, @RequestParam String day,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime,
                               @RequestParam String subject, @RequestParam(defaultValue = "") String teacherName,
                               @RequestParam(defaultValue = "") String room, RedirectAttributes ra) {
        if (branch.isBlank() || subject.isBlank() || !endTime.isAfter(startTime)) {
            ra.addFlashAttribute("error", "Details sahi bharein (end time start ke baad ho).");
            return "redirect:/timetable";
        }
        TimetableEntry t = new TimetableEntry();
        t.setBranch(branch.trim());
        t.setYear(year);
        t.setDay(day);
        t.setStartTime(startTime);
        t.setEndTime(endTime);
        t.setSubject(subject.trim());
        t.setTeacherName(teacherName.trim());
        t.setRoom(room.trim());
        timetable.save(t);
        ra.addFlashAttribute("success", "Timetable entry add ho gayi.");
        return "redirect:/timetable";
    }

    @PostMapping("/timetable/{id}/delete")
    public String deleteTimetable(@PathVariable Long id, RedirectAttributes ra) {
        timetable.deleteById(id);
        ra.addFlashAttribute("success", "Entry delete ho gayi.");
        return "redirect:/timetable";
    }

    // ---------------- Audit / Contacts / Feedback ----------------
    @GetMapping("/audit")
    public String auditPage(Model m) {
        m.addAttribute("logs", auditRepo.findTop300ByOrderByCreatedAtDesc());
        return "admin/audit";
    }

    @GetMapping("/contacts")
    public String contactsPage(Model m) {
        m.addAttribute("items", contacts.findAllByOrderByCreatedAtDesc());
        return "admin/contacts";
    }

    @PostMapping("/contacts/{id}/seen")
    public String markSeen(@PathVariable Long id) {
        contacts.findById(id).ifPresent(c -> {
            c.setSeen(true);
            contacts.save(c);
        });
        return "redirect:/admin/contacts";
    }

    @PostMapping("/contacts/{id}/delete")
    public String deleteContact(@PathVariable Long id) {
        contacts.deleteById(id);
        return "redirect:/admin/contacts";
    }

    @GetMapping("/feedback")
    public String feedbackPage(Model m) {
        m.addAttribute("items", feedbackRepo.findAllByOrderByCreatedAtDesc());
        return "admin/feedback";
    }

    @PostMapping("/feedback/{id}/reply")
    public String replyFeedback(@PathVariable Long id, @RequestParam(defaultValue = "") String reply,
                                @RequestParam(defaultValue = "false") boolean resolved, RedirectAttributes ra) {
        feedbackRepo.findById(id).ifPresent(f -> {
            String r = reply.trim();
            f.setReply(r.isEmpty() ? null : (r.length() > 990 ? r.substring(0, 990) : r));
            f.setStatus(resolved ? "RESOLVED" : "OPEN");
            feedbackRepo.save(f);
        });
        ra.addFlashAttribute("success", "Feedback update ho gaya.");
        return "redirect:/admin/feedback";
    }
    // ---------------- Approvals ----------------
    @GetMapping("/approvals")
    public String approvals(Model m) {
        m.addAttribute("pending", users.findByApprovalOrderByCreatedAtAsc("PENDING"));
        m.addAttribute("trusted", approvedEmails.findAll());
        return "admin/approvals";
    }

    @PostMapping("/approvals/{id}/approve")
    @Transactional
    public String approve(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        User u = users.findById(id).orElse(null);
        if (u == null || u.isApproved()) {
            ra.addFlashAttribute("error", "Ye request ab available nahi hai.");
            return "redirect:/admin/approvals";
        }
        u.setApproval("APPROVED");
        u.setEnabled(true);
        users.save(u);
        if (u.getEmail() != null && !u.getEmail().isBlank() && !approvedEmails.existsByEmailIgnoreCase(u.getEmail())) {
            ApprovedEmail a = new ApprovedEmail();
            a.setEmail(u.getEmail().toLowerCase());
            a.setRole(u.getRole().name());
            a.setApprovedBy(auth.getName());
            approvedEmails.save(a);   // ek baar approve ho gaya to is email ko dobara approval nahi lagega
        }
        audit.log(auth.getName(), "ACCOUNT_APPROVED", null, u.getUsername() + " (" + u.getRole() + ")");
        ra.addFlashAttribute("success", u.getFullName() + " approve ho gaye. Ab wo login kar sakte hain.");
        return "redirect:/admin/approvals";
    }

    @PostMapping("/approvals/{id}/reject")
    @Transactional
    public String reject(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        User u = users.findById(id).orElse(null);
        if (u == null || u.isApproved()) {
            ra.addFlashAttribute("error", "Ye request ab available nahi hai.");
            return "redirect:/admin/approvals";
        }
        storage.delete(u.getPhoto());
        resetTokens.deleteByUser(u);
        users.delete(u);
        audit.log(auth.getName(), "ACCOUNT_REJECTED", null, u.getUsername() + " (" + u.getRole() + ")");
        ra.addFlashAttribute("success", "Request reject ho gayi.");
        return "redirect:/admin/approvals";
    }

    @PostMapping("/users/{id}/delete")
    @Transactional
    public String deleteUser(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        User u = users.findById(id).orElse(null);
        if (u == null) return fail(ra, "User nahi mila.");
        if (u.getRole() != Role.TEACHER && u.getRole() != Role.ORGANIZER)
            return fail(ra, "Sirf Teacher / Organizer delete ho sakta hai. Student ko Disable karein.");
        if (u.getRole() == Role.ORGANIZER && !eventRepo.findByCreatedByOrderByEventDateTimeDesc(u).isEmpty())
            return fail(ra, "Is organizer ke events hain. Pehle Disable karein.");
        storage.delete(u.getPhoto());
        resetTokens.deleteByUser(u);
        users.delete(u);
        audit.log(auth.getName(), "USER_DELETED", null, u.getUsername() + " (" + u.getRole() + ")");
        ra.addFlashAttribute("success", "Account delete ho gaya. Wahi email dobara register kare to approval nahi lagega.");
        return "redirect:/admin/users";
    }
    private String fail(RedirectAttributes ra, String msg) {
        ra.addFlashAttribute("error", msg);
        return "redirect:/admin/users";
    }
}