package com.college.cms.controller;

import com.college.cms.model.*;
import com.college.cms.repository.*;
import com.college.cms.service.PdfService;
import com.college.cms.service.AudienceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;

/** Student sirf apna data dekhta hai: ID hamesha login session se aati hai, URL se nahi. */
@Controller
@RequestMapping("/student")
@RequiredArgsConstructor
public class StudentController {
    private final StudentRepository students;
    private final MarkRepository marks;
    private final PaymentRepository payments;
    private final NoticeRepository notices;
    private final FeedbackRepository feedbacks;
    private final EventRepository events;
    private final EventRegistrationRepository regs;
    private final PdfService pdfService;
    private final AudienceService audience;

    private Student me(Authentication auth) {
        return students.findByUserUsername(auth.getName())
                .orElseThrow(() -> new IllegalStateException("Student profile nahi mili"));
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication auth, Model m) {
        Student s = me(auth);
        List<Mark> ms = marks.findByStudentOrderByExamNameAscSubjectAsc(s);
        m.addAttribute("s", s);
        m.addAttribute("avgMarks", ms.stream().mapToDouble(Mark::getPercentage).average().orElse(0));
        m.addAttribute("marksCount", ms.size());
        AudienceService.Viewer viewer = new AudienceService.Viewer(false, s.getBranch());
        m.addAttribute("notices", notices.findAllByOrderByImportantDescCreatedAtDesc().stream()
                .filter(n -> audience.visible(viewer, n.getTargetBranches())).limit(5).toList());
        return "student/dashboard";
    }

    @GetMapping("/marks")
    public String marks(Authentication auth, Model m) {
        Student s = me(auth);
        m.addAttribute("s", s);
        m.addAttribute("marks", marks.findByStudentOrderByExamNameAscSubjectAsc(s));
        return "student/marks";
    }

    @GetMapping("/fees")
    public String fees(Authentication auth, Model m) {
        Student s = me(auth);
        m.addAttribute("s", s);
        m.addAttribute("payments", payments.findByStudentOrderByPaidOnDescIdDesc(s));
        return "student/fees";
    }

    @GetMapping("/receipt/{id}")
    public ResponseEntity<byte[]> receipt(@PathVariable Long id, Authentication auth) {
        Student s = me(auth);
        Payment p = payments.findById(id).filter(x -> x.getStudent().getId().equals(s.getId())).orElse(null);
        if (p == null) return ResponseEntity.notFound().build();
        byte[] pdf = pdfService.feeReceipt(p);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + p.getReceiptNo() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/feedback")
    public String feedbackPage(Authentication auth, Model m) {
        m.addAttribute("items", feedbacks.findByStudentOrderByCreatedAtDesc(me(auth)));
        return "student/feedback";
    }

    @PostMapping("/feedback")
    public String feedbackSave(Authentication auth, @RequestParam String type, @RequestParam String subject,
                               @RequestParam String message, RedirectAttributes ra) {
        subject = subject.trim();
        message = message.trim();
        if (subject.length() < 3 || subject.length() > 190 || message.length() < 10 || message.length() > 1900) {
            ra.addFlashAttribute("error", "Subject (3+ character) aur message (10+ character) likhein.");
            return "redirect:/student/feedback";
        }
        Feedback f = new Feedback();
        f.setStudent(me(auth));
        f.setFeedbackType("GRIEVANCE".equals(type) ? "GRIEVANCE" : "FEEDBACK");
        f.setSubject(subject);
        f.setMessage(message);
        feedbacks.save(f);
        ra.addFlashAttribute("success", "Aapka message admin tak pahunch gaya.");
        return "redirect:/student/feedback";
    }

    @PostMapping("/events/{id}/register")
    public String register(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        Student s = me(auth);
        Event e = events.findById(id).orElse(null);
        if (e == null) {
            ra.addFlashAttribute("error", "Event nahi mila.");
        } else if (e.getEventDateTime().isBefore(LocalDateTime.now())) {
            ra.addFlashAttribute("error", "Ye event ho chuka hai.");
        } else if (!audience.visible(new AudienceService.Viewer(false, s.getBranch()), e.getTargetBranches())) {
            ra.addFlashAttribute("error", "Ye event aapke department ke liye nahi hai.");
        } else if (!regs.existsByEventAndStudent(e, s)) {
            EventRegistration r = new EventRegistration();
            r.setEvent(e);
            r.setStudent(s);
            regs.save(r);
            ra.addFlashAttribute("success", "Event me register ho gaye: " + e.getTitle());
        }
        return "redirect:/events";
    }
    @PostMapping("/events/{id}/unregister")
    @Transactional
    public String unregister(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        Student s = me(auth);
        events.findById(id).flatMap(e -> regs.findByEventAndStudent(e, s)).ifPresent(regs::delete);
        ra.addFlashAttribute("success", "Registration cancel ho gaya.");
        return "redirect:/events";
    }
}