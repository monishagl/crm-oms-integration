package com.crm.integration_service.worker;


import com.crm.integration_service.domain.InboxEvent;
import com.crm.integration_service.repository.InboxEventRepository;
import jakarta.transaction.Transactional;
import org.slf4j.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

@Component
public class InboxPollingWorker {

    private static final Logger log = LoggerFactory.getLogger(InboxPollingWorker.class);

    private final InboxEventRepository repo;

    // tune these later
    private final int batchSize = 10;
    private final int maxAttempts = 5;

    public InboxPollingWorker(InboxEventRepository repo) {
        this.repo = repo;
    }

    @Scheduled(fixedDelay = 1000) // every 1s after previous run finishes
    @Transactional
    public void poll() {
        List<InboxEvent> events = repo.lockNextBatch(OffsetDateTime.now(), batchSize);
        if (events.isEmpty()) return;

        for (InboxEvent e : events) {
            try {
                e.markProcessing();

                // ✅ For now, "process" = log it
                log.info("PROCESSING eventId={} type={} payload={}", e.getEventId(), e.getEventType(), e.getPayload());

                // Later this becomes: call OMS API
                e.markProcessed();

            } catch (Exception ex) {
                int attempt = e.getAttemptCount() + 1;

                if (attempt >= maxAttempts) {
                    log.error("DLQ eventId={} after {} attempts: {}", e.getEventId(), attempt, ex.getMessage());
                    // For next step we’ll insert into dlq_events table
                    e.markFailed(ex.getMessage(), attempt, OffsetDateTime.now());
                } else {
                    OffsetDateTime next = OffsetDateTime.now().plusSeconds(backoffSeconds(attempt));
                    log.warn("Retrying eventId={} attempt={} nextAttemptAt={}", e.getEventId(), attempt, next);
                    e.markPendingRetry(attempt, next);
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
