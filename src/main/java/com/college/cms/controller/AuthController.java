package com.college.cms.controller;

import com.college.cms.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class AuthController {
    private static final Map<String, String[]> PORTALS = Map.of(
            "student", new String[]{"Student", "bi-mortarboard-fill"},
            "teacher", new String[]{"Teacher", "bi-person-workspace"},
            "organizer", new String[]{"Event Organizer", "bi-calendar-event"},
            "admin", new String[]{"Admin", "bi-shield-lock-fill"});

    private final PasswordResetService resetService;

    private boolean loggedIn(Authentication auth) {
        return auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
    }

    /** Role chunne ka page */
    @GetMapping("/login")
    public String login(Authentication auth) {
        return loggedIn(auth) ? "redirect:/dashboard" : "login";
    }

    /** Har role ka alag login page */
    @GetMapping("/login/{portal}")
    public String portal(@PathVariable String portal, Authentication auth, Model m) {
        if (!PORTALS.containsKey(portal)) return "redirect:/login";
        if (loggedIn(auth)) return "redirect:/dashboard";
        m.addAttribute("portal", portal);
        m.addAttribute("label", PORTALS.get(portal)[0]);
        m.addAttribute("icon", PORTALS.get(portal)[1]);
        return "login-form";
    }

    @GetMapping("/forgot-password")
    public String forgot() { return "forgot"; }

    @PostMapping("/forgot-password")
    public String forgotPost(@RequestParam String identifier, RedirectAttributes ra) {
        resetService.start(identifier);
        ra.addFlashAttribute("success", "If the account is found, a password reset link has been sent (valid for 30 minutes)\n.");
        return "redirect:/forgot-password";
    }

    @GetMapping("/reset-password")
    public String reset(@RequestParam(defaultValue = "") String token, Model m) {
        m.addAttribute("valid", resetService.validate(token).isPresent());
        m.addAttribute("token", token);
        return "reset";
    }

    @PostMapping("/reset-password")
    public String resetPost(@RequestParam String token, @RequestParam String password,
                            @RequestParam String confirm, RedirectAttributes ra) {
        if (password.length() < 8 || !password.equals(confirm)) {
            ra.addFlashAttribute("error", "Password must be at least 8 characters long and must match.");
            ra.addAttribute("token", token);
            return "redirect:/reset-password";
        }
        if (resetService.reset(token, password)) {
            ra.addFlashAttribute("success", "Password changed. Login now.");
            return "redirect:/login";
        }
        ra.addFlashAttribute("error", "The link is incorrect or expired.");
        return "redirect:/forgot-password";
    }
}