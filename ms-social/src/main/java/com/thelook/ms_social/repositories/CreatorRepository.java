package com.thelook.ms_social.repositories;

import com.thelook.ms_social.entities.Creator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CreatorRepository extends JpaRepository<Creator, UUID> {

    @Query("SELECT c.id FROM Creator c WHERE c.userId = :userId")
    Optional<UUID> findIdByUserId(UUID userId);

    @Query("SELECT c FROM Creator c WHERE c.id = :creatorId")
    Optional<Creator> findIdByCreatorId(UUID creatorId);


}
