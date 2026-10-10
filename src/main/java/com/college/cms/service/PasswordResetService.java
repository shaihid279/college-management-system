package com.college.cms.service;

import com.college.cms.model.PasswordResetToken;
import com.college.cms.model.User;
import com.college.cms.repository.PasswordResetTokenRepository;
import com.college.cms.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class PasswordResetService {
    private final UserRepository users;
    private final PasswordResetTokenRepository tokens;
    private final PasswordEncoder encoder;
    private final ObjectProvider<JavaMailSender> mailSender;
    private final String baseUrl;
    private final String from;

    public PasswordResetService(UserRepository users, PasswordResetTokenRepository tokens, PasswordEncoder encoder,
                                ObjectProvider<JavaMailSender> mailSender,
                                @Value("${app.base-url}") String baseUrl,
                                @Value("${spring.mail.username:noreply@CampusCore.local}") String from) {
        this.users = users;
        this.tokens = tokens;
        this.encoder = encoder;
        this.mailSender = mailSender;
        this.baseUrl = baseUrl;
        this.from = from;
    }

    @Transactional
    public void start(String identifier) {
        if (identifier == null || identifier.isBlank()) return;
        String id = identifier.trim();
        Optional<User> found = users.findByUsername(id);
        if (found.isEmpty()) found = users.findFirstByEmailIgnoreCase(id);
        if (found.isEmpty()) return;
        User user = found.get();
        tokens.deleteByUser(user);
        PasswordResetToken t = new PasswordResetToken();
        t.setToken(UUID.randomUUID().toString().replace("-", ""));
        t.setUser(user);
        t.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        tokens.save(t);
        String link = baseUrl + "/reset-password?token=" + t.getToken();
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender != null && user.getEmail() != null && !user.getEmail().isBlank()) {
            try {
                SimpleMailMessage m = new SimpleMailMessage();
                m.setFrom(from);
                m.setTo(user.getEmail());
                m.setSubject("CampusCore - Password reset");
                m.setText("Namaste " + user.getFullName() + ",\n\nOpen this link to reset your password (valid for 30 minutes:\n" + link
                        + "\n\nIf you didn't request this, please ignore it.");
                sender.send(m);
                return;
            } catch (Exception e) {
                log.warn("Mail send fail: {}", e.getMessage());
            }
        }
        log.warn("PASSWORD RESET LINK for {} : {}", user.getUsername(), link);
    }

    public Optional<PasswordResetToken> validate(String token) {
        if (token == null) return Optional.empty();
        return tokens.findByToken(token).filter(t -> t.getExpiresAt().isAfter(LocalDateTime.now()));
    }

    @Transactional
    public boolean reset(String token, String newPassword) {
        Optional<PasswordResetToken> t = validate(token);
        if (t.isEmpty()) return false;
        User u = t.get().getUser();
        u.setPassword(encoder.encode(newPassword));
        users.save(u);
        tokens.delete(t.get());
        return true;
    }
}