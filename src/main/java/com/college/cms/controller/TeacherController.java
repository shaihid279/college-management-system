package com.college.cms.controller;

import com.college.cms.model.*;
import com.college.cms.repository.*;
import com.college.cms.service.AuditService;
import com.college.cms.service.BranchService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Controller
@RequestMapping("/teacher")
@RequiredArgsConstructor
public class TeacherController {
    private static final List<String> MODES = List.of("Cash", "UPI", "Card", "Net Banking", "Cheque", "DD");

    private final StudentRepository studentRepo;
    private final MarkRepository markRepo;
    private final PaymentRepository paymentRepo;
    private final AuditLogRepository auditRepo;
    private final AuditService audit;
    private final BranchService branchService;

    @GetMapping("/dashboard")
    public String dashboard(Authentication auth, Model m) {
        List<Student> all = studentRepo.findAll();
        m.addAttribute("total", all.size());
        m.addAttribute("lowAttendance", all.stream().filter(Student::isAttendanceLow).count());
        m.addAttribute("pendingCount", all.stream().filter(s -> s.getPendingFees().signum() > 0).count());
        m.addAttribute("recent", auditRepo.findTop100ByActorOrderByCreatedAtDesc(auth.getName()).stream().limit(6).toList());
        m.addAttribute("branches", branchService.all());
        return "teacher/dashboard";
    }

    @GetMapping("/students")
    public String students(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "") String branch,
                           @RequestParam(required = false) Integer year,
                           @RequestParam(defaultValue = "false") boolean pendingOnly, Model m) {
        String qq = q.trim().toLowerCase();
        List<Student> list = studentRepo.findAll().stream()
                .filter(s -> qq.isEmpty() || s.getIdCardNo().equalsIgnoreCase(qq)
                        || s.getUser().getFullName().toLowerCase().contains(qq))
                .filter(s -> branch.isBlank() || s.getBranch().equalsIgnoreCase(branch))
                .filter(s -> year == null || year.equals(s.getYear()))
                .filter(s -> !pendingOnly || s.getPendingFees().signum() > 0)
                .sorted(Comparator.comparing((Student s) -> s.getUser().getFullName().toLowerCase()))
                .toList();
        m.addAttribute("students", list);
        m.addAttribute("branches", branchService.all());
        m.addAttribute("q", q);
        m.addAttribute("fBranch", branch);
        m.addAttribute("fYear", year);
        m.addAttribute("pendingOnly", pendingOnly);
        return "teacher/students";
    }

    @GetMapping("/students/{id}")
    public String detail(@PathVariable Long id, Model m, RedirectAttributes ra) {
        Student s = studentRepo.findById(id).orElse(null);
        if (s == null) {
            ra.addFlashAttribute("error", "Student nahi mila.");
            return "redirect:/teacher/students";
        }
        m.addAttribute("s", s);
        m.addAttribute("marks", markRepo.findByStudentOrderByExamNameAscSubjectAsc(s));
        m.addAttribute("payments", paymentRepo.findByStudentOrderByPaidOnDescIdDesc(s));
        m.addAttribute("modes", MODES);
        return "teacher/student-detail";
    }

    @PostMapping("/students/{id}/attendance")
    @Transactional
    public String attendance(@PathVariable Long id, @RequestParam int attended, @RequestParam int total,
                             Authentication auth, RedirectAttributes ra) {
        Student s = studentRepo.findById(id).orElse(null);
        if (s == null) return notFound(ra);
        if (attended < 0 || total < 0 || attended > total) {
            ra.addFlashAttribute("error", "Attended classes total se zyada nahi ho sakti.");
            return back(id);
        }
        String old = s.getAttendedClasses() + "/" + s.getTotalClasses();
        s.setAttendedClasses(attended);
        s.setTotalClasses(total);
        studentRepo.save(s);
        audit.log(auth.getName(), "ATTENDANCE_UPDATE", s.getIdCardNo(), old + " -> " + attended + "/" + total);
        ra.addFlashAttribute("success", "Attendance update ho gayi.");
        return back(id);
    }

    @PostMapping("/students/{id}/marks")
    @Transactional
    public String addMark(@PathVariable Long id, @RequestParam String subject, @RequestParam String examName,
                          @RequestParam double obtained, @RequestParam(defaultValue = "100") double max,
                          Authentication auth, RedirectAttributes ra) {
        Student s = studentRepo.findById(id).orElse(null);
        if (s == null) return notFound(ra);
        if (subject.isBlank() || examName.isBlank() || obtained < 0 || max <= 0 || obtained > max) {
            ra.addFlashAttribute("error", "Marks details sahi bharein (obtained, max se zyada nahi).");
            return back(id);
        }
        Mark mk = new Mark();
        mk.setStudent(s);
        mk.setSubject(subject.trim());
        mk.setExamName(examName.trim());
        mk.setMarksObtained(obtained);
        mk.setMaxMarks(max);
        markRepo.save(mk);
        audit.log(auth.getName(), "MARKS_ADDED", s.getIdCardNo(), subject.trim() + " (" + examName.trim() + "): " + obtained + "/" + max);
        ra.addFlashAttribute("success", "Marks add ho gaye.");
        return back(id);
    }

    @PostMapping("/students/{id}/marks/{markId}/delete")
    @Transactional
    public String deleteMark(@PathVariable Long id, @PathVariable Long markId, Authentication auth, RedirectAttributes ra) {
        Student s = studentRepo.findById(id).orElse(null);
        if (s == null) return notFound(ra);
        markRepo.findById(markId).filter(mk -> mk.getStudent().getId().equals(id)).ifPresent(mk -> {
            markRepo.delete(mk);
            audit.log(auth.getName(), "MARKS_DELETED", s.getIdCardNo(), mk.getSubject() + " (" + mk.getExamName() + ")");
        });
        ra.addFlashAttribute("success", "Marks delete ho gaye.");
        return back(id);
    }

    @PostMapping("/students/{id}/payment")
    @Transactional
    public String payment(@PathVariable Long id, @RequestParam BigDecimal amount, @RequestParam String mode,
                          @RequestParam(defaultValue = "") String note, Authentication auth, RedirectAttributes ra) {
        Student s = studentRepo.findById(id).orElse(null);
        if (s == null) return notFound(ra);
        if (amount.signum() <= 0 || amount.compareTo(s.getPendingFees()) > 0 || !MODES.contains(mode)) {
            ra.addFlashAttribute("error", "Amount 0 se zyada aur pending fees se kam/barabar hona chahiye.");
            return back(id);
        }
        Payment p = new Payment();
        p.setStudent(s);
        p.setAmount(amount);
        p.setPayMode(mode);
        p.setNote(note.trim().length() > 190 ? note.trim().substring(0, 190) : note.trim());
        p.setReceiptNo(newReceiptNo());
        p.setRecordedBy(auth.getName());
        s.setPaidFees(s.getPaidFees().add(amount));
        studentRepo.save(s);
        paymentRepo.save(p);
        audit.log(auth.getName(), "FEES_PAYMENT", s.getIdCardNo(), "Rs. " + amount + " via " + mode + " (" + p.getReceiptNo() + ")");
        ra.addFlashAttribute("success", "Payment record ho gaya. Receipt: " + p.getReceiptNo());
        return back(id);
    }

    @PostMapping("/students/{id}/total-fees")
    @Transactional
    public String totalFees(@PathVariable Long id, @RequestParam BigDecimal totalFees, Authentication auth, RedirectAttributes ra) {
        Student s = studentRepo.findById(id).orElse(null);
        if (s == null) return notFound(ra);
        if (totalFees.signum() < 0) {
            ra.addFlashAttribute("error", "Total fees negative nahi ho sakti.");
            return back(id);
        }
        String old = s.getTotalFees().toPlainString();
        s.setTotalFees(totalFees);
        studentRepo.save(s);
        audit.log(auth.getName(), "TOTAL_FEES_UPDATE", s.getIdCardNo(), old + " -> " + totalFees.toPlainString());
        ra.addFlashAttribute("success", "Total fees update ho gayi.");
        return back(id);
    }

    @GetMapping("/audit")
    public String auditPage(Authentication auth, Model m) {
        m.addAttribute("logs", auditRepo.findTop100ByActorOrderByCreatedAtDesc(auth.getName()));
        return "teacher/audit";
    }

    private String back(Long id) { return "redirect:/teacher/students/" + id; }

    private String notFound(RedirectAttributes ra) {
        ra.addFlashAttribute("error", "Student nahi mila.");
        return "redirect:/teacher/students";
    }

    private String newReceiptNo() {
        String no;
        do {
            no = "RC" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                    + String.format("%05d", ThreadLocalRandom.current().nextInt(100000));
        } while (paymentRepo.existsByReceiptNo(no));
        return no;
    }
}