package com.thelook.ms_creation.repositories;

import com.thelook.ms_creation.entities.OutboxMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxMessage, UUID> {

    List<OutboxMessage> findByProcessedFalseAndRetryCountLessThan(int maxRetries, Pageable pageable);

    @Modifying
    @Query("UPDATE OutboxMessage o SET o.processed = true WHERE o.id IN :ids")
    void markAsProcessed(List<UUID> ids);

    @Modifying
    @Query("UPDATE OutboxMessage o SET o.retryCount = o.retryCount + 1, o.lastAttemptAt = CURRENT_TIMESTAMP WHERE o.id = :id")
    void incrementRetryCount(UUID id);

    @Modifying
    @Query("DELETE FROM OutboxMessage o WHERE o.processed = true AND o.createdAt < :cutoff")
    void deleteProcessedBefore(LocalDateTime cutoff);

}
