package com.thelook.ms_creation.repositories;

import com.thelook.ms_creation.entities.OutboxMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxMessage, UUID> {

    List<OutboxMessage> findByProcessedFalse();

}
