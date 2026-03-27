package com.thelook.ms_creation.repositories;

import com.thelook.ms_creation.entities.Outfit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OutfitRepository extends JpaRepository<Outfit, UUID> {

}
