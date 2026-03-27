package com.thelook.ms_social.repositories;

import com.thelook.ms_social.entities.CreatorNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CreatorNodeRepository extends Neo4jRepository<CreatorNode, UUID> {

    @Query("RETURN EXISTS((:Creator {id: $followerId})-[:FOLLOWS]->(:Creator {id: $followedId}))")
    boolean isFollowing(UUID followerId, UUID followedId);

    @Query("MATCH (follower:Creator {id: $followerId}), (followed:Creator {id: $followedId}) " +
            "MERGE (follower)-[:FOLLOWS]->(followed)")
    void follow(UUID followerId, UUID followedId);

    @Query("MATCH (follower:Creator {id: $followerId})-[r:FOLLOWS]->(followed:Creator {id: $followedId}) " +
            "DELETE r")
    void unfollow(UUID followerId, UUID followedId);

    @Query("MATCH (me:Creator {id: $id})-[:FOLLOWS]->(friend)-[:FOLLOWS]->(suggested) " +
            "WHERE NOT (me)-[:FOLLOWS]->(suggested) AND me <> suggested " +
            "RETURN suggested")
    List<CreatorNode> findSuggestions(UUID id);

    @Query("MATCH (c:Creator {id: $id}) DETACH DELETE c")
    void detachDeleteById(UUID id);

    @Query("MATCH (c:Creator {id: $id}) " +
            "OPTIONAL MATCH (c)-[:POSTED]->(p:Photo) " +
            "OPTIONAL MATCH ()-[l:LIKES]->(p) " +
            "DETACH DELETE c, p, l")
    void deepDeleteCreator(UUID id);
}
