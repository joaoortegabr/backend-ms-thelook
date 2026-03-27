package com.thelook.ms_creation.entities;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.thelook.ms_creation.models.enums.OutfitColor;
import com.thelook.ms_creation.models.enums.OutfitStyle;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.util.*;

@Entity
@Table(name="tb_outfit")
public class Outfit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;
    @Column(name="creator_id", nullable = false)
    private UUID creatorId;
    @Column(nullable = false, length=64)
    private String title;
    @Column(nullable = false)
    private String image1Url;
    private String image2Url;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OutfitStyle style;
    @Column(nullable = false)
    @ElementCollection(targetClass = OutfitColor.class)
    @Size(min = 1, max = 3, message = "Select 1 to 3 colors")
    @Enumerated(EnumType.STRING)
    private Set<OutfitColor> colors;
    @Size(max = 6, message = "Maximum of 6 items allowed")
    @OneToMany(mappedBy = "outfit", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JsonManagedReference
    private List<Item> items = new ArrayList<>();
    @CreationTimestamp
    private LocalDate createdAt;

    public Outfit() {
    }

    public Outfit(UUID creatorId, String title, String image1Url, String image2Url, OutfitStyle style,
                  Set<OutfitColor> colors, List<Item> items) {
        this.creatorId = creatorId;
        this.title = title;
        this.image1Url = image1Url;
        this.image2Url = image2Url;
        this.style = style;
        this.colors = colors;
        this.items = items;
    }

    public void addItem(Item item) {
        this.items.add(item);
        item.setOutfit(this);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(UUID creatorId) {
        this.creatorId = creatorId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getImage1Url() {
        return image1Url;
    }

    public void setImage1Url(String image1Url) {
        this.image1Url = image1Url;
    }

    public String getImage2Url() {
        return image2Url;
    }

    public void setImage2Url(String image2Url) {
        this.image2Url = image2Url;
    }

    public OutfitStyle getStyle() {
        return style;
    }

    public void setStyle(OutfitStyle style) {
        this.style = style;
    }

    public Set<OutfitColor> getColors() {
        return colors;
    }

    public void setColors(Set<OutfitColor> colors) {
        this.colors = colors;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public LocalDate getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        Outfit outfit = (Outfit) o;
        return Objects.equals(id, outfit.id) && Objects.equals(creatorId, outfit.creatorId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, creatorId);
    }

}
