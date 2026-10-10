package com.college.cms.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

@Component
public class LoginSuccessHandler implements AuthenticationSuccessHandler {
    private static final Map<String, String> PORTAL_ROLE = Map.of(
            "student", "ROLE_STUDENT", "teacher", "ROLE_TEACHER",
            "organizer", "ROLE_ORGANIZER", "admin", "ROLE_ADMIN");
    private final RequestCache cache = new HttpSessionRequestCache();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication auth) throws IOException {
        // Galat portal se login: student ID se teacher portal wagairah allowed nahi
        String portal = request.getParameter("portal");
        if (portal != null && PORTAL_ROLE.containsKey(portal) && !has(auth, PORTAL_ROLE.get(portal))) {
            new SecurityContextLogoutHandler().logout(request, response, auth);
            response.sendRedirect(request.getContextPath() + "/login/" + portal + "?error=role");
            return;
        }
        // Agar user kisi page par click karke login par aaya tha, to wahin wapas bhejo
        SavedRequest saved = cache.getRequest(request, response);
        if (saved != null) {
            String url = saved.getRedirectUrl();
            cache.removeRequest(request, response);
            try {
                String path = URI.create(url).getPath();
                if (path != null && allowed(path, auth)) {
                    response.sendRedirect(url);
                    return;
                }
            } catch (IllegalArgumentException ignored) { }
        }
        response.sendRedirect(request.getContextPath() + targetFor(auth));
    }

    private boolean allowed(String path, Authentication auth) {
        if (path.startsWith("/student")) return has(auth, "ROLE_STUDENT");
        if (path.startsWith("/teacher")) return has(auth, "ROLE_TEACHER") || has(auth, "ROLE_ADMIN");
        if (path.startsWith("/organizer")) return has(auth, "ROLE_ORGANIZER");
        if (path.startsWith("/admin")) return has(auth, "ROLE_ADMIN");
        return true;
    }

    private static boolean has(Authentication auth, String role) {
        for (GrantedAuthority a : auth.getAuthorities()) if (a.getAuthority().equals(role)) return true;
        return false;
    }

    public static String targetFor(Authentication auth) {
        for (GrantedAuthority a : auth.getAuthorities()) {
            switch (a.getAuthority()) {
                case "ROLE_ADMIN": return "/admin/dashboard";
                case "ROLE_TEACHER": return "/teacher/dashboard";
                case "ROLE_ORGANIZER": return "/organizer/dashboard";
                case "ROLE_STUDENT": return "/student/dashboard";
                default: break;
            }
        }
        return "/";
    }
}