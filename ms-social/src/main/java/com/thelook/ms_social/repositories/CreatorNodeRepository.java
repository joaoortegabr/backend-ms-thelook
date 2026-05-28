package com.thelook.ms_social.repositories;

import com.thelook.ms_social.entities.CreatorNode;
import com.thelook.ms_social.models.dtos.CreatorFollowerCount;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CreatorNodeRepository extends Neo4jRepository<CreatorNode, UUID> {

    @Query("RETURN EXISTS((:Creator {creatorId: $followerId})-[:FOLLOWS]->(:Creator {creatorId: $followedId}))")
    boolean isFollowing(UUID followerId, UUID followedId);

    @Query("MATCH (follower:Creator {creatorId: $followerId}) " +
            "MATCH (followed:Creator {creatorId: $followedId}) " +
            "MERGE (follower)-[:FOLLOWS]->(followed)")
    void follow(UUID followerId, UUID followedId);

    @Query("MATCH (follower:Creator {creatorId: $followerId})-[r:FOLLOWS]->(followed:Creator {creatorId: $followedId}) " +
            "DELETE r")
    void unfollow(UUID followerId, UUID followedId);

    @Query("MATCH (c:Creator)<-[:FOLLOWS]-(follower) " +
            "RETURN c.creatorId as creatorId, count(follower) as total")
    List<CreatorFollowerCount> countFollowersPerCreator();

    // --- Favorites ---

    @Query("RETURN EXISTS((:Creator {creatorId: $creatorId})-[:FAVORITES]->(:Creator {creatorId: $targetId}))")
    boolean isFavorite(UUID creatorId, UUID targetId);

    @Query("MATCH (a:Creator {creatorId: $creatorId}) " +
            "MATCH (b:Creator {creatorId: $targetId}) " +
            "MERGE (a)-[:FAVORITES]->(b)")
    void favorite(UUID creatorId, UUID targetId);

    @Query("MATCH (:Creator {creatorId: $creatorId})-[r:FAVORITES]->(:Creator {creatorId: $targetId}) DELETE r")
    void unfavorite(UUID creatorId, UUID targetId);

    // --- Outfit Likes ---

    @Query("RETURN EXISTS((:Creator {creatorId: $creatorId})-[:LIKES]->(:Outfit {outfitId: $outfitId}))")
    boolean isLiking(UUID creatorId, UUID outfitId);

    @Query("MATCH (c:Creator {creatorId: $creatorId}) " +
            "MERGE (o:Outfit {outfitId: $outfitId}) " +
            "MERGE (c)-[:LIKES]->(o)")
    void like(UUID creatorId, UUID outfitId);

    @Query("MATCH (:Creator {creatorId: $creatorId})-[r:LIKES]->(:Outfit {outfitId: $outfitId}) DELETE r")
    void unlike(UUID creatorId, UUID outfitId);

    @Query("MATCH (c:Creator {creatorId: $creatorId}) DETACH DELETE c")
    void deepDeleteCreator(UUID creatorId);
}
