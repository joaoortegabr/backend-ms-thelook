package com.thelook.ms_creation.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.thelook.enums.ImageProcessStatus;
import com.thelook.ms_creation.models.enums.ItemType;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name="tb_item")
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "outfit_id", nullable = false)
    @JsonBackReference
    private Outfit outfit;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ItemType itemType;
    @Column(nullable = false, length=32)
    private String itemName;
    @Column(nullable = false)
    private String itemImg;
    @Column(nullable = false)
    private String itemUrl;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Enumerated(EnumType.STRING)
    private ImageProcessStatus imageStatus;
    @CreationTimestamp
    private LocalDateTime createdAt;

    public Item() {
    }

    public Item(Outfit outfit, ItemType itemType, String itemName, String itemImg, String itemUrl) {
        this.outfit = outfit;
        this.itemType = itemType;
        this.itemName = itemName;
        this.itemImg = itemImg;
        this.itemUrl = itemUrl;
        this.imageStatus = ImageProcessStatus.PENDING;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Outfit getOutfit() {
        return outfit;
    }

    public void setOutfit(Outfit outfit) {
        this.outfit = outfit;
    }

    public ItemType getItemType() {
        return itemType;
    }

    public void setItemType(ItemType itemType) {
        this.itemType = itemType;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getItemImg() {
        return itemImg;
    }

    public void setItemImg(String itemImg) {
        this.itemImg = itemImg;
    }

    public String getItemUrl() {
        return itemUrl;
    }

    public void setItemUrl(String itemUrl) {
        this.itemUrl = itemUrl;
    }

    public ImageProcessStatus getImageStatus() {
        return imageStatus;
    }

    public void setImageStatus(ImageProcessStatus imageStatus) {
        this.imageStatus = imageStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        Item item = (Item) o;
        return Objects.equals(id, item.id) && Objects.equals(outfit, item.outfit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, outfit);
    }
}
