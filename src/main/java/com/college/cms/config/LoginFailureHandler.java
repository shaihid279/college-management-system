package com.college.cms.config;

import com.college.cms.model.User;
import com.college.cms.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class LoginFailureHandler implements AuthenticationFailureHandler {
    private static final Set<String> PORTALS = Set.of("student", "teacher", "organizer", "admin");
    private final UserRepository users;
    private final PasswordEncoder encoder;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        String portal = request.getParameter("portal");
        if (portal == null || !PORTALS.contains(portal)) portal = "student";
        String code = "bad";
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        // Pending/disabled tabhi bataate hain jab password sahi ho (info leak se bachne ke liye)
        if (username != null && password != null) {
            User u = users.findByUsername(username.trim())
                    .or(() -> users.findFirstByEmailIgnoreCase(username.trim())).orElse(null);
            if (u != null && encoder.matches(password, u.getPassword())) {
                if (!u.isApproved()) code = "pending";
                else if (!u.isEnabled()) code = "disabled";
            }
        }
        response.sendRedirect(request.getContextPath() + "/login/" + portal + "?error=" + code);
    }
}