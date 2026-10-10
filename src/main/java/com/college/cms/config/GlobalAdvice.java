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

    /** "me" (the logged-in user) is available on every page. */
    @ModelAttribute("me")
    public User me(Authentication auth) {
        if (auth == null || auth instanceof AnonymousAuthenticationToken || !auth.isAuthenticated()) return null;
        return users.findByUsername(auth.getName()).orElse(null);
    }

    /** Pending approvals badge shown in the admin navbar. */
    @ModelAttribute("pendingApprovals")
    public long pendingApprovals(Authentication auth) {
        if (auth == null || auth.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) return 0;
        return users.countByApproval("PENDING");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String tooLarge(RedirectAttributes ra) {
        ra.addFlashAttribute("error", "The file is too large (max 2 MB).");
        return "redirect:/dashboard";
    }
}