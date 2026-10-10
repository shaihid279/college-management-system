package com.college.cms.controller;

import com.college.cms.config.LoginSuccessHandler;
import com.college.cms.model.ContactMessage;
import com.college.cms.repository.ContactMessageRepository;
import com.college.cms.repository.NoticeRepository;
import com.college.cms.service.AudienceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class HomeController {
    private final NoticeRepository notices;
    private final ContactMessageRepository contacts;
    private final AudienceService audience;

    @GetMapping("/")
    public String home(Authentication auth, Model m) {
        AudienceService.Viewer v = audience.viewer(auth);
        m.addAttribute("notices", notices.findAllByOrderByImportantDescCreatedAtDesc().stream()
                .filter(n -> audience.visible(v, n.getTargetBranches())).limit(5).toList());
        return "index";
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication auth) {
        return "redirect:" + LoginSuccessHandler.targetFor(auth);
    }

    @GetMapping("/contact")
    public String contact() { return "contact"; }

    @PostMapping("/contact")
    public String send(@RequestParam String name, @RequestParam String email,
                       @RequestParam(defaultValue = "") String subject, @RequestParam String message,
                       @RequestParam(defaultValue = "") String website, RedirectAttributes ra) {
        if (!website.isBlank()) return "redirect:/contact"; // spam bot (honeypot)
        name = name.trim();
        email = email.trim();
        message = message.trim();
        if (name.length() < 2 || name.length() > 100 || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
                || message.length() < 10 || message.length() > 2000) {
            ra.addFlashAttribute("error", "Naam, sahi email aur kam se kam 10 character ka message likhein.");
            return "redirect:/contact";
        }
        ContactMessage c = new ContactMessage();
        c.setName(name);
        c.setEmail(email);
        c.setSubject(subject.trim().length() > 190 ? subject.trim().substring(0, 190) : subject.trim());
        c.setMessage(message);
        contacts.save(c);
        ra.addFlashAttribute("success", "Dhanyavaad! Aapka message mil gaya, jaldi reply karenge.");
        return "redirect:/contact";
    }
}