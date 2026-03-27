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
    private UUID id;
    private String name;

    @Relationship(type = "FOLLOWS", direction = Relationship.Direction.OUTGOING)
    private Set<CreatorNode> following = new HashSet<>();

    public CreatorNode() {
    }

    public CreatorNode(UUID id, String name) {
        this.id = id;
        this.name = name;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

}
