package com.crm.integration_service.controller;


import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.crm.integration_service.domain.InboxEvent;
import com.crm.integration_service.repository.InboxEventRepository;

@RestController
@RequestMapping("/v1/webhooks")
public class WebhookController {

    private final InboxEventRepository inboxRepo;

    public WebhookController(InboxEventRepository inboxRepo) {
        this.inboxRepo = inboxRepo;
    }

    @PostMapping("/orders")
    public ResponseEntity<Ack> receive(
            @RequestHeader("X-CRM-Event-Id") String eventId,
            @RequestHeader("X-CRM-Event-Type") String eventType,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId,
            @RequestBody String rawJson
    ) {
        try {
            inboxRepo.save(InboxEvent.newPending(eventId, eventType, rawJson, correlationId));
            return ResponseEntity.ok(new Ack("accepted", eventId));
        } catch (DataIntegrityViolationException dup) {
            return ResponseEntity.ok(new Ack("duplicate_accepted", eventId));
        }
    }

    public record Ack(String status, String eventId) {}
}
