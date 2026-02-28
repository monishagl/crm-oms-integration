package com.crm.integration_service.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.crm.integration_service.domain.DlqEvent;
import com.crm.integration_service.domain.InboxEvent;
import com.crm.integration_service.repository.DlqEventRepository;
import com.crm.integration_service.repository.InboxEventRepository;

@RestController
@RequestMapping("/v1/dlq")
public class DlqController {

    private final DlqEventRepository dlqRepo;
    private final InboxEventRepository inboxRepo;

    public DlqController(DlqEventRepository dlqRepo, InboxEventRepository inboxRepo) {
        this.dlqRepo = dlqRepo;
        this.inboxRepo = inboxRepo;
    }

    @GetMapping
    public List<DlqEvent> list() {
        return dlqRepo.findAll();
    }

    @PostMapping("/{eventId}/replay")
    public ResponseEntity<?> replay(@PathVariable String eventId) {
        DlqEvent d = dlqRepo.findById(eventId).orElse(null);
        if (d == null) return ResponseEntity.notFound().build();

        // put back into inbox as PENDING (reset attempts)
        InboxEvent e = inboxRepo.findByEventId(eventId).orElse(null);
       if (e == null) {
        // edge case: inbox row doesn't exist (deleted), recreate it
        inboxRepo.save(InboxEvent.newPending(d.getEventId(), d.getEventType(), d.getPayload(), d.getCorrelationId()));
    } else {
        e.resetForReplay(); // we'll add this helper
        inboxRepo.save(e);
    }

        dlqRepo.deleteById(eventId);
        return ResponseEntity.ok().body(new Ack("replayed", eventId));
    }

    record Ack(String status, String eventId) {}
}