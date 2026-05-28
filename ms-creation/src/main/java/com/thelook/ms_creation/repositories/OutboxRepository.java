package com.thelook.ms_creation.repositories;

import com.thelook.ms_creation.entities.OutboxMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxMessage, UUID> {

    List<OutboxMessage> findByProcessedFalse(Pageable pageable);

    @Modifying
    @Query("UPDATE OutboxMessage o SET o.processed = true WHERE o.id IN :ids")
    void markAsProcessed(List<UUID> ids);

}
