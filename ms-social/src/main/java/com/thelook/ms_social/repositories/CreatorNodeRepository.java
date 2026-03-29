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

    @Query("MATCH (c:Creator {creatorId: $creatorId}) " +
            "OPTIONAL MATCH (c)-[:POSTED]->(p:Photo) " +
            "OPTIONAL MATCH ()-[l:LIKES]->(p) " +
            "DETACH DELETE c, p, l")
    void deepDeleteCreator(UUID creatorId);
}
