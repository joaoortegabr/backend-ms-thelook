package com.thelook.ms_feed.repositories;

import com.thelook.ms_feed.entities.OutfitDocument;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutfitElasticRepository extends ListCrudRepository<OutfitDocument, String>, PagingAndSortingRepository<OutfitDocument, String> {

    List<OutfitDocument> findByCreatorId(String creatorId);

    void deleteByCreatorId(String creatorId);

    List<OutfitDocument> findByStyle(String style);

    List<OutfitDocument> findByImageStatusOrderByCreatedAtDesc(String status);
}
