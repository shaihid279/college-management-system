package com.college.cms.service;

import com.college.cms.model.AuditLog;
import com.college.cms.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditService {
    private final AuditLogRepository repo;

    public void log(String actor, String action, String targetIdCard, String details) {
        AuditLog a = new AuditLog();
        a.setActor(actor);
        a.setAction(action);
        a.setTargetIdCard(targetIdCard);
        a.setDetails(details != null && details.length() > 490 ? details.substring(0, 490) : details);
        repo.save(a);
    }
}