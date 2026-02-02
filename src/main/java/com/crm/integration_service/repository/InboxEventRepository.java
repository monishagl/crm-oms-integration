package com.crm.integration_service.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.crm.integration_service.domain.InboxEvent;

public interface InboxEventRepository extends JpaRepository<InboxEvent, UUID> {
    Optional<InboxEvent> findByEventId(String eventId);
}

