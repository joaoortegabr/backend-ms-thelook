package com.thelook.ms_feed.entities;

import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Objects;

public class ItemDocument {

    @Field(type = FieldType.Keyword)
    private String itemId;
    @Field(type = FieldType.Text, analyzer = "portuguese")
    private String itemName;
    @Field(type = FieldType.Keyword)
    private String itemType;
    @Field(type = FieldType.Keyword)
    private String itemImg;
    @Field(type = FieldType.Keyword)
    private String itemUrl;
    @Field(type = FieldType.Keyword)
    private String imageStatus;

    public ItemDocument() {
    }

    public ItemDocument(String itemId, String itemName, String itemType, String itemImg, String itemUrl, String imageStatus) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.itemType = itemType;
        this.itemImg = itemImg;
        this.itemUrl = itemUrl;
        this.imageStatus = imageStatus;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
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

    public String getImageStatus() {
        return imageStatus;
    }

    public void setImageStatus(String imageStatus) {
        this.imageStatus = imageStatus;
    }

    @Override
    public boolean equals(Object o) {
        if(o == null || getClass() != o.getClass())
            return false;
        ItemDocument that = (ItemDocument) o;
        return Objects.equals(itemId, that.itemId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(itemId);
    }

}
