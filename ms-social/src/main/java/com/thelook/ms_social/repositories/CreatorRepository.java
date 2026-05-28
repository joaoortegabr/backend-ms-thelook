package com.thelook.ms_social.repositories;

import com.thelook.ms_social.entities.Creator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CreatorRepository extends JpaRepository<Creator, UUID> {

    @Query("SELECT c.id FROM Creator c WHERE c.userId = :userId")
    Optional<UUID> findIdByUserId(UUID userId);

    @Query("SELECT c FROM Creator c WHERE c.id = :creatorId")
    Optional<Creator> findIdByCreatorId(UUID creatorId);

    Optional<Creator> findByUserId(UUID userId);

    @Query("SELECT c FROM Creator c WHERE c.isActive = false AND c.deletedAt < :cutoff")
    List<Creator> findExpiredDeletedCreators(@Param("cutoff") LocalDateTime cutoff);

    @Modifying
    @Query("UPDATE Creator c SET c.followersCount = :count WHERE c.id = :creatorId")
    void updateFollowerCount(UUID creatorId, long count);

    @Query("SELECT c.id, c.followersCount FROM Creator c WHERE c.isActive = true AND c.followersCount > 0")
    List<Object[]> findActiveFollowerCounts();

}
