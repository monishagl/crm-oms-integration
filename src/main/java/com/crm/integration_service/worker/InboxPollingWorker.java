package com.crm.integration_service.worker;


import java.time.OffsetDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.crm.integration_service.domain.DlqEvent;
import com.crm.integration_service.domain.InboxEvent;
import com.crm.integration_service.repository.DlqEventRepository;
import com.crm.integration_service.repository.InboxEventRepository;

import jakarta.transaction.Transactional;

@Component
public class InboxPollingWorker {

    private static final Logger log = LoggerFactory.getLogger(InboxPollingWorker.class);

    private final InboxEventRepository repo;
    private final DlqEventRepository dlqRepo;
    // tune these later
    private final int batchSize = 10;
    private final int maxAttempts = 5;
    
    public InboxPollingWorker(InboxEventRepository repo,  DlqEventRepository dlqRepo) {
        this.repo = repo;
        this.dlqRepo=dlqRepo;
    }
    


    @Scheduled(fixedDelay = 1000) // every 1s after previous run finishes
    @Transactional
    public void poll() {
        List<InboxEvent> events = repo.lockNextBatch(OffsetDateTime.now(), batchSize);
        if (events.isEmpty()) return;

        for (InboxEvent e : events) {
            try {
                e.markProcessing();
                repo.save(e);

                // For now, "process" = log it
                log.info("PROCESSING eventId={} type={} payload={}", e.getEventId(), e.getEventType(), e.getPayload());

                // Later this becomes: call OMS API
                if (e.getPayload().contains("crm_fail")) {
                    throw new RuntimeException("Simulated OMS outage");
                }
                e.markProcessed();
                repo.save(e);
                
            } catch (Exception ex) {
                int attempt = e.getAttemptCount() + 1;

                if (attempt >= maxAttempts) {
                    log.error("DLQ eventId={} after {} attempts: {}", e.getEventId(), attempt, ex.getMessage());
                    // For next step we’ll insert into dlq_events table
                    dlqRepo.save(DlqEvent.of(
                        e.getEventId(),
                        e.getEventType(),
                        e.getPayload(),
                        ex.getMessage(),
                        e.getCorrelationId()
                    ));
                    e.markFailed(ex.getMessage(), attempt, OffsetDateTime.now());
                    repo.save(e);
                } else {
                    OffsetDateTime next = OffsetDateTime.now().plusSeconds(backoffSeconds(attempt));
                    log.warn("Retrying eventId={} attempt={} nextAttemptAt={}", e.getEventId(), attempt, next);
                    e.markPendingRetry(attempt, next);
                    repo.save(e);
                }
            }
        }
    }

    private long backoffSeconds(int attempt) {
        return switch (attempt) {
            case 1 -> 2;
            case 2 -> 5;
            case 3 -> 15;
            default -> 30;
        };
    }
}
