package com.thelook.ms_social.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name="tb_creator")
public class Creator {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;
    @Column(name="user_id", unique = true, nullable = false)
    private UUID userId;
    @Column(length = 64)
    private String name;
    private String avatarUrl;
    @Column(columnDefinition="TEXT", length = 255)
    private String bio;
    @Column(length = 64)
    private String instagram;
    private LocalDate birthDate;
    private String city;
    private String uf;
    @CreationTimestamp
    private LocalDate createdAt;
    @Column(nullable = false)
    private boolean isActive;
    @Column(name = "followers_count")
    private long followersCount = 0;

    public Creator() {
    }

    public Creator(UUID userId, String name, String avatarUrl, String bio, String instagram,
                   LocalDate birthDate, String city, String uf) {
        this.userId = userId;
        this.name = name;
        this.avatarUrl = avatarUrl;
        this.bio = bio;
        this.instagram = instagram;
        this.birthDate = birthDate;
        this.city = city;
        this.uf = uf;
    }

    public Creator(String avatarUrl, String bio, String instagram,
                   String city, String uf) {
        this.avatarUrl = avatarUrl;
        this.bio = bio;
        this.instagram = instagram;
        this.city = city;
        this.uf = uf;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getInstagram() {
        return instagram;
    }

    public void setInstagram(String instagram) {
        this.instagram = instagram;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getUf() {
        return uf;
    }

    public void setUf(String uf) {
        this.uf = uf;
    }

    public LocalDate getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDate createdAt) {
        this.createdAt = createdAt;
    }

    public boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(boolean active) {
        isActive = active;
    }

    public long getFollowersCount() {
        return followersCount;
    }

    public void setFollowersCount(long followersCount) {
        this.followersCount = followersCount;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        Creator creator = (Creator) o;
        return Objects.equals(id, creator.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}

