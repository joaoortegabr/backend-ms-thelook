package com.thelook.ms_feed.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.LocalDateTime;
import java.util.*;

@Document(indexName="outfits")
@Setting(settingPath="elasticsearch/settings.json")
public class OutfitDocument {

    @Id
    private String id;
    @Field(type = FieldType.Keyword)
    private UUID creatorId;
    @Field(type = FieldType.Text, analyzer = "portuguese")
    private String title;
    @Field(type = FieldType.Keyword)
    private String style;
    @Field(type = FieldType.Keyword)
    private Set<String> colors;
    private String image1Url;
    private String image2Url;
    @Field(type = FieldType.Keyword)
    private String imageStatus;
    @Field(type = FieldType.Nested)
    private List<ItemDocument> items = new ArrayList<>();;
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    public OutfitDocument() {
    }

    public OutfitDocument(String id, UUID creatorId, String title, String style, Set<String> colors, String image1Url, String image2Url, String imageStatus, List<ItemDocument> items) {
        this.id = id;
        this.creatorId = creatorId;
        this.title = title;
        this.style = style;
        this.colors = colors;
        this.image1Url = image1Url;
        this.image2Url = image2Url;
        this.imageStatus = imageStatus;
        this.items = items;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
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

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public Set<String> getColors() {
        return colors;
    }

    public void setColors(Set<String> colors) {
        this.colors = colors;
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

    public String getImageStatus() {
        return imageStatus;
    }

    public void setImageStatus(String imageStatus) {
        this.imageStatus = imageStatus;
    }

    public List<ItemDocument> getItems() {
        return items;
    }

    public void setItems(List<ItemDocument> items) {
        this.items = items;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if(o == null || getClass() != o.getClass())
            return false;
        OutfitDocument that = (OutfitDocument) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
