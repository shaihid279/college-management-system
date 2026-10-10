package com.college.cms.controller;

import com.college.cms.model.*;
import com.college.cms.repository.*;
import com.college.cms.service.AudienceService;
import com.college.cms.service.BranchService;
import com.college.cms.service.PlacementService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.*;

@Controller
@RequiredArgsConstructor
public class InfoController {
    private static final List<String> DAYS = List.of("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday");

    private final NoticeRepository noticeRepo;
    private final EventRepository eventRepo;
    private final EventRegistrationRepository regRepo;
    private final StudentRepository studentRepo;
    private final ExamScheduleRepository examRepo;
    private final TimetableRepository ttRepo;
    private final PlacementRepository placementRepo;
    private final TopperRepository topperRepo;
    private final FeeStructureRepository feeRepo;
    private final PlacementService placementService;
    private final BranchService branchService;
    private final AudienceService audience;

    @GetMapping("/notices")
    public String notices(Authentication auth, Model m) {
        AudienceService.Viewer v = audience.viewer(auth);
        m.addAttribute("notices", noticeRepo.findAllByOrderByImportantDescCreatedAtDesc().stream()
                .filter(n -> audience.visible(v, n.getTargetBranches())).toList());
        m.addAttribute("branches", branchService.all());
        return "info/notices";
    }

    @GetMapping("/events")
    public String events(Authentication auth, Model m) {
        AudienceService.Viewer v = audience.viewer(auth);
        List<Event> events = eventRepo.findAllByOrderByEventDateTimeAsc().stream()
                .filter(e -> audience.visible(v, e.getTargetBranches())).toList();
        Map<Long, Long> counts = new HashMap<>();
        for (Event e : events) counts.put(e.getId(), regRepo.countByEvent(e));
        Set<Long> registered = new HashSet<>();
        studentRepo.findByUserUsername(auth.getName())
                .ifPresent(s -> regRepo.findByStudent(s).forEach(r -> registered.add(r.getEvent().getId())));
        m.addAttribute("events", events);
        m.addAttribute("counts", counts);
        m.addAttribute("registered", registered);
        m.addAttribute("now", LocalDateTime.now());
        return "info/events";
    }

    @GetMapping("/exams")
    public String exams(@RequestParam(required = false) String branch, @RequestParam(required = false) Integer year,
                        Authentication auth, Model m) {
        Student me = studentRepo.findByUserUsername(auth.getName()).orElse(null);
        if (me != null && branch == null) { branch = me.getBranch(); year = me.getYear(); }
        final String b = branch;
        final Integer y = year;
        m.addAttribute("exams", examRepo.findAllByOrderByExamDateAscStartTimeAsc().stream()
                .filter(x -> match(b, y, x.getBranch(), x.getYear())).toList());
        m.addAttribute("branches", branchService.all());
        m.addAttribute("fBranch", b == null ? "" : b);
        m.addAttribute("fYear", y);
        return "info/exams";
    }

    @GetMapping("/timetable")
    public String timetable(@RequestParam(required = false) String branch, @RequestParam(required = false) Integer year,
                            Authentication auth, Model m) {
        Student me = studentRepo.findByUserUsername(auth.getName()).orElse(null);
        if (me != null && branch == null) { branch = me.getBranch(); year = me.getYear(); }
        final String b = branch;
        final Integer y = year;
        List<TimetableEntry> list = ttRepo.findAllByOrderByStartTimeAsc().stream()
                .filter(x -> match(b, y, x.getBranch(), x.getYear())).toList();
        Map<String, List<TimetableEntry>> byDay = new LinkedHashMap<>();
        for (String d : DAYS) {
            List<TimetableEntry> l = list.stream().filter(t -> d.equals(t.getDay())).toList();
            if (!l.isEmpty()) byDay.put(d, l);
        }
        m.addAttribute("byDay", byDay);
        m.addAttribute("days", DAYS);
        m.addAttribute("branches", branchService.all());
        m.addAttribute("fBranch", b == null ? "" : b);
        m.addAttribute("fYear", y);
        return "info/timetable";
    }

    @GetMapping("/placements")
    public String placements(Model m) {
        List<Placement> list = placementRepo.findAllByOrderByAcademicYearDescPackageLpaDesc();
        m.addAttribute("placements", list);
        m.addAttribute("companyCount", list.stream().map(p -> p.getCompany().toLowerCase()).distinct().count());
        m.addAttribute("highest", list.stream().mapToDouble(Placement::getPackageLpa).max().orElse(0));
        m.addAttribute("totalPlaced", list.stream().mapToInt(Placement::getStudentsPlaced).sum());
        m.addAttribute("links", placementService.getLinks());
        m.addAttribute("syncStatus", placementService.getLastStatus());
        m.addAttribute("sourceOnline", placementService.isSourceOnline());
        m.addAttribute("lastAttempt", placementService.getLastAttempt());
        m.addAttribute("sourceUrl", placementService.getSourceUrl());
        return "info/placements";
    }

    @GetMapping("/toppers")
    public String toppers(@RequestParam(defaultValue = "") String branch,
                          @RequestParam(defaultValue = "") String academicYear, Model m) {
        List<Topper> all = topperRepo.findAllByOrderByAcademicYearDescBranchAscRankNoAsc();
        m.addAttribute("toppers", all.stream()
                .filter(t -> branch.isBlank() || branch.equalsIgnoreCase(t.getBranch()))
                .filter(t -> academicYear.isBlank() || academicYear.equalsIgnoreCase(t.getAcademicYear())).toList());
        m.addAttribute("years", all.stream().map(Topper::getAcademicYear).filter(Objects::nonNull)
                .filter(s -> !s.isBlank()).distinct().toList());
        m.addAttribute("branches", branchService.all());
        m.addAttribute("fBranch", branch);
        m.addAttribute("fYear", academicYear);
        return "info/toppers";
    }

    @GetMapping("/fees")
    public String fees(@RequestParam(required = false) String branch, Authentication auth, Model m) {
        Student me = studentRepo.findByUserUsername(auth.getName()).orElse(null);
        if (me != null && branch == null) branch = me.getBranch();
        final String b = branch;
        m.addAttribute("fees", feeRepo.findAllByOrderByBranchAscYearAsc().stream()
                .filter(f -> b == null || b.isBlank() || b.equalsIgnoreCase(f.getBranch())).toList());
        m.addAttribute("branches", branchService.all());
        m.addAttribute("fBranch", b == null ? "" : b);
        return "info/fees";
    }

    private boolean match(String fBranch, Integer fYear, String branch, Integer year) {
        return (fBranch == null || fBranch.isBlank() || fBranch.equalsIgnoreCase(branch))
                && (fYear == null || fYear.equals(year));
    }
}