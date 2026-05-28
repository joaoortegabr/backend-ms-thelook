package com.thelook.ms_social.repositories;

import com.thelook.ms_social.entities.OutfitNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OutfitNodeRepository extends Neo4jRepository<OutfitNode, UUID> {

    @Query("MATCH (o:Outfit {outfitId: $outfitId}) DETACH DELETE o")
    void deleteWithRelationships(UUID outfitId);

    @Query("MATCH (o:Outfit)<-[:LIKES]-() WITH o, count(*) AS cnt WHERE cnt > 0 RETURN o.outfitId AS outfitId, cnt AS likeCount")
    List<OutfitLikeCount> findLikeCountsPerOutfit();

    interface OutfitLikeCount {
        String getOutfitId();
        long getLikeCount();
    }
}