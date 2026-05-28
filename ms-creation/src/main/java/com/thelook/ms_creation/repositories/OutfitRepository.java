package com.thelook.ms_creation.repositories;

import com.thelook.ms_creation.entities.Outfit;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OutfitRepository extends JpaRepository<Outfit, UUID> {

    @EntityGraph(attributePaths = {"items", "colors"})
    @Query("SELECT o FROM Outfit o WHERE o.id = :id AND o.isActive = true")
    Optional<Outfit> findWithItemsById(UUID id);

    @Modifying
    @Query("UPDATE Outfit o SET o.isActive = false WHERE o.creatorId = :creatorId")
    void deactivateByCreatorId(UUID creatorId);

    @Modifying
    @Query("UPDATE Outfit o SET o.isActive = true WHERE o.creatorId = :creatorId")
    void reactivateByCreatorId(UUID creatorId);

    @Query("SELECT DISTINCT o FROM Outfit o LEFT JOIN FETCH o.items WHERE o.creatorId = :creatorId")
    List<Outfit> findAllWithItemsByCreatorId(UUID creatorId);

}
