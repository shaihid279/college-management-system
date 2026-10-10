package com.college.cms.config;

import com.college.cms.model.*;
import com.college.cms.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {
    private final UserRepository users;
    private final StudentRepository students;
    private final NoticeRepository notices;
    private final MarkRepository marks;
    private final PaymentRepository payments;
    private final PasswordEncoder encoder;

    @Value("${app.admin.username}") private String adminUsername;
    @Value("${app.admin.password}") private String adminPassword;
    @Value("${app.seed-demo:false}") private boolean seedDemo;

    @Override
    public void run(String... args) {
        User admin = users.findByUsername(adminUsername).orElse(null);
        if (admin == null) {
            users.save(newUser(adminUsername, adminPassword, Role.ADMIN, "System Admin", "admin@campuscore.local"));
            log.info("Fixed admin ban gaya: {}", adminUsername);
        } else if (!encoder.matches(adminPassword, admin.getPassword()) || !admin.isEnabled() || !admin.isApproved()) {
            // Admin ki ID/password hamesha config (application.properties / ADMIN_PASSWORD) se fix rahegi
            admin.setPassword(encoder.encode(adminPassword));
            admin.setEnabled(true);
            admin.setApproval("APPROVED");
            users.save(admin);
        }
        if (seedDemo) seedDemoData();
    }

    private void seedDemoData() {
        if (!users.existsByUsername("teacher1"))
            users.save(newUser("teacher1", "Teacher@123", Role.TEACHER, "Prof. Anita Sharma", "teacher1@CampusCore.local"));
        if (!users.existsByUsername("organizer1"))
            users.save(newUser("organizer1", "Organizer@123", Role.ORGANIZER, "Rahul Verma", "organizer1@CampusCore.local"));
        if (!users.existsByUsername("student1")) {
            User u = users.save(newUser("student1", "Student@123", Role.STUDENT, "Aarav Patil", "student1@CampusCore.local"));
            Student s = new Student();
            s.setUser(u);
            s.setIdCardNo("CS2024001");
            s.setBranch("Computer Engineering");
            s.setYear(2);
            s.setTotalFees(BigDecimal.valueOf(85000));
            s.setPaidFees(BigDecimal.valueOf(40000));
            s.setAttendedClasses(62);
            s.setTotalClasses(90);
            s = students.save(s);

            addMark(s, "Mathematics", "Mid Sem", 78);
            addMark(s, "Data Structures", "Mid Sem", 85);
            addMark(s, "DBMS", "Mid Sem", 66);

            Payment p = new Payment();
            p.setStudent(s);
            p.setAmount(BigDecimal.valueOf(40000));
            p.setPayMode("UPI");
            p.setReceiptNo("RCDEMO00001");
            p.setNote("Pehli kist");
            p.setRecordedBy("system");
            payments.save(p);
        }
        if (notices.count() == 0) {
            Notice n = new Notice();
            n.setTitle("CampusCore me aapka swagat hai");
            n.setContent("Ye ek demo notice hai. Admin panel se naye notice daal sakte hain.");
            n.setImportant(true);
            n.setPostedBy("system");
            notices.save(n);
        }
    }

    private void addMark(Student s, String subject, String exam, double obtained) {
        Mark m = new Mark();
        m.setStudent(s);
        m.setSubject(subject);
        m.setExamName(exam);
        m.setMarksObtained(obtained);
        m.setMaxMarks(100);
        marks.save(m);
    }

    private User newUser(String username, String rawPassword, Role role, String fullName, String email) {
        User u = new User();
        u.setUsername(username);
        u.setPassword(encoder.encode(rawPassword));
        u.setRole(role);
        u.setFullName(fullName);
        u.setEmail(email);
        return u;
    }
}