package com.college.cms.config;

import com.college.cms.model.User;
import com.college.cms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalAdvice {
    private final UserRepository users;

    /** Har page par "me" (logged-in user) available hoga. */
    @ModelAttribute("me")
    public User me(Authentication auth) {
        if (auth == null || auth instanceof AnonymousAuthenticationToken || !auth.isAuthenticated()) return null;
        return users.findByUsername(auth.getName()).orElse(null);
    }

    /** Admin ke navbar me pending approvals ka badge. */
    @ModelAttribute("pendingApprovals")
    public long pendingApprovals(Authentication auth) {
        if (auth == null || auth.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) return 0;
        return users.countByApproval("PENDING");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String tooLarge(RedirectAttributes ra) {
        ra.addFlashAttribute("error", "File bahut badi hai (max 2 MB).");
        return "redirect:/dashboard";
    }
}