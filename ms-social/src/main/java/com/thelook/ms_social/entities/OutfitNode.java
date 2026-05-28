package com.thelook.ms_social.entities;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

import java.util.Objects;
import java.util.UUID;

@Node("Outfit")
public class OutfitNode {

    @Id
    private UUID outfitId;

    public OutfitNode() {}

    public OutfitNode(UUID outfitId) {
        this.outfitId = outfitId;
    }

    public UUID getOutfitId() { return outfitId; }
    public void setOutfitId(UUID outfitId) { this.outfitId = outfitId; }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        return Objects.equals(outfitId, ((OutfitNode) o).outfitId);
    }

    @Override
    public int hashCode() { return Objects.hashCode(outfitId); }
}