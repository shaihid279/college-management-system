package com.college.cms.controller;

import com.college.cms.model.Event;
import com.college.cms.model.EventRegistration;
import com.college.cms.model.User;
import com.college.cms.repository.EventRegistrationRepository;
import com.college.cms.repository.EventRepository;
import com.college.cms.repository.UserRepository;
import com.college.cms.service.AudienceService;
import com.college.cms.service.BranchService;
import com.college.cms.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/organizer")
@RequiredArgsConstructor
public class OrganizerController {
    private final EventRepository eventRepo;
    private final EventRegistrationRepository regRepo;
    private final UserRepository users;
    private final FileStorageService storage;
    private final BranchService branchService;
    private final AudienceService audience;

    private User me(Authentication auth) { return users.findByUsername(auth.getName()).orElseThrow(); }

    @GetMapping("/dashboard")
    public String dashboard(Authentication auth, @RequestParam(required = false) Long edit, Model m) {
        List<Event> list = eventRepo.findByCreatedByOrderByEventDateTimeDesc(me(auth));
        Map<Long, Long> counts = new HashMap<>();
        for (Event e : list) counts.put(e.getId(), regRepo.countByEvent(e));
        Event form = edit == null ? new Event()
                : list.stream().filter(x -> x.getId().equals(edit)).findFirst().orElse(new Event());
        m.addAttribute("events", list);
        m.addAttribute("counts", counts);
        m.addAttribute("form", form);
        m.addAttribute("formDateTime", form.getEventDateTime() == null ? "" : form.getEventDateTime().toString().substring(0, 16));
        m.addAttribute("branches", branchService.all());
        m.addAttribute("now", LocalDateTime.now());
        return "organizer/dashboard";
    }

    @PostMapping("/events")
    public String save(Authentication auth, @RequestParam(required = false) Long id, @RequestParam String title,
                       @RequestParam(defaultValue = "") String description, @RequestParam(defaultValue = "") String venue,
                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime when,
                       @RequestParam(defaultValue = "false") boolean allDepartments,
                       @RequestParam(required = false) List<String> branches,
                       @RequestParam(required = false) MultipartFile banner, RedirectAttributes ra) {
        User u = me(auth);
        Event e = new Event();
        if (id != null) {
            e = eventRepo.findById(id).orElse(null);
            if (e == null || e.getCreatedBy() == null || !e.getCreatedBy().getId().equals(u.getId())) {
                ra.addFlashAttribute("error", "Ye event aapka nahi hai.");
                return "redirect:/organizer/dashboard";
            }
        }
        if (title.isBlank() || title.length() > 190) {
            ra.addFlashAttribute("error", "Event ka title likhein (max 190 character).");
            return "redirect:/organizer/dashboard";
        }
        String target = audience.buildTarget(allDepartments, branches, branchService.all());
        if (target == null) {
            ra.addFlashAttribute("error", "Kam se kam ek department chunein ya 'Sabhi departments' tick karein.");
            return "redirect:/organizer/dashboard";
        }
        try {
            String saved = storage.saveImage(banner, "events");
            if (saved != null) {
                storage.delete(e.getBanner());
                e.setBanner(saved);
            }
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/organizer/dashboard";
        } catch (IOException ex) {
            ra.addFlashAttribute("error", "Banner save nahi ho paya.");
            return "redirect:/organizer/dashboard";
        }
        e.setTitle(title.trim());
        e.setDescription(description.trim().length() > 2900 ? description.trim().substring(0, 2900) : description.trim());
        e.setVenue(venue.trim());
        e.setEventDateTime(when);
        e.setTargetBranches(target);
        e.setCreatedBy(u);
        eventRepo.save(e);
        ra.addFlashAttribute("success", id == null ? "Event add ho gaya." : "Event update ho gaya.");
        return "redirect:/organizer/dashboard";
    }

    @PostMapping("/events/{id}/delete")
    @Transactional
    public String delete(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        User u = me(auth);
        Event e = eventRepo.findById(id).orElse(null);
        if (e != null && e.getCreatedBy() != null && e.getCreatedBy().getId().equals(u.getId())) {
            regRepo.deleteByEvent(e);
            storage.delete(e.getBanner());
            eventRepo.delete(e);
            ra.addFlashAttribute("success", "Event delete ho gaya.");
        } else {
            ra.addFlashAttribute("error", "Ye event aapka nahi hai.");
        }
        return "redirect:/organizer/dashboard";
    }

    @GetMapping("/events/{id}/registrations")
    public String registrations(@PathVariable Long id, Authentication auth, Model m, RedirectAttributes ra) {
        User u = me(auth);
        Event e = eventRepo.findById(id).orElse(null);
        if (e == null || e.getCreatedBy() == null || !e.getCreatedBy().getId().equals(u.getId())) {
            ra.addFlashAttribute("error", "Ye event aapka nahi hai.");
            return "redirect:/organizer/dashboard";
        }
        List<EventRegistration> regs = regRepo.findByEventOrderByRegisteredAtAsc(e);
        m.addAttribute("event", e);
        m.addAttribute("regs", regs);
        return "organizer/registrations";
    }
}