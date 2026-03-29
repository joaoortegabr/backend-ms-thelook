package com.thelook.ms_social.entities;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Node("Creator")
public class CreatorNode {

    @Id
    private UUID creatorId;
    private String name;

    @Relationship(type = "FOLLOWS", direction = Relationship.Direction.OUTGOING)
    private Set<CreatorNode> following = new HashSet<>();

    public CreatorNode() {
    }

    public CreatorNode(UUID creatorId, String name) {
        this.creatorId = creatorId;
        this.name = name;
    }

    public UUID getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(UUID creatorId) {
        this.creatorId = creatorId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<CreatorNode> getFollowing() {
        return following;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        CreatorNode that = (CreatorNode) o;
        return Objects.equals(creatorId, that.creatorId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(creatorId);
    }

}
