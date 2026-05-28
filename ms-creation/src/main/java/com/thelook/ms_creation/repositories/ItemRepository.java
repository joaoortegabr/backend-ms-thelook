package com.thelook.ms_creation.repositories;

import com.thelook.ms_creation.entities.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ItemRepository extends JpaRepository<Item, UUID> {

    @Query("SELECT i FROM Item i JOIN FETCH i.outfit WHERE i.id = :itemId")
    Optional<Item> findWithOutfitById(UUID itemId);
}
