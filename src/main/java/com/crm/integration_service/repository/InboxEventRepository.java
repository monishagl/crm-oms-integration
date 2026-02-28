package com.crm.integration_service.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.crm.integration_service.domain.InboxEvent;
import com.crm.integration_service.domain.InboxStatus;

public interface InboxEventRepository extends JpaRepository<InboxEvent, UUID> {
    Optional<InboxEvent> findByEventId(String eventId);

     // Claim events safely (row-locking) for a single worker instance
    @Query(value = """
        select * from inbox_events
        where status = 'PENDING'
          and next_attempt_at <= :now
        order by created_at
        for update skip locked
        limit :limit
        """, nativeQuery = true)
    List<InboxEvent> lockNextBatch(@Param("now") OffsetDateTime now, @Param("limit") int limit);

    @Modifying
    @Query("update InboxEvent e set e.status = :status where e.id in :ids")
    int bulkUpdateStatus(@Param("ids") List<UUID> ids, @Param("status") InboxStatus status);
      
  }

