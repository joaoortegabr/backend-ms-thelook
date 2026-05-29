package com.thelook.ms_social.controllers;

import com.thelook.exceptions.IncompleteProfileException;
import com.thelook.ms_social.services.SocialService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/social")
public class SocialController {

    private final SocialService socialService;

    public SocialController(SocialService socialService) {
        this.socialService = socialService;
    }

    @PostMapping("/follow/{targetId}")
    public ResponseEntity<Void> follow(
            @RequestHeader(name="X-Creator-Id", required=false) UUID creatorId,
            @PathVariable UUID targetId) {

        if (creatorId == null)
            throw new IncompleteProfileException("You must complete your profile before following someone");

        socialService.follow(creatorId, targetId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/follow/{targetId}")
    public ResponseEntity<Void> unfollow(
            @RequestHeader(name="X-Creator-Id", required=false) UUID creatorId,
            @PathVariable UUID targetId) {

        if (creatorId == null)
            throw new IncompleteProfileException("You must complete your profile before unfollowing someone");

        socialService.unfollow(creatorId, targetId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/favorite/{targetId}")
    public ResponseEntity<Void> favorite(
            @RequestHeader(name="X-Creator-Id", required=false) UUID creatorId,
            @PathVariable UUID targetId) {

        if (creatorId == null)
            throw new IncompleteProfileException("You must complete your profile before favoriting someone");

        socialService.favorite(creatorId, targetId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/favorite/{targetId}")
    public ResponseEntity<Void> unfavorite(
            @RequestHeader(name="X-Creator-Id", required=false) UUID creatorId,
            @PathVariable UUID targetId) {

        if (creatorId == null)
            throw new IncompleteProfileException("You must complete your profile before unfavoriting someone");

        socialService.unfavorite(creatorId, targetId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/like/{outfitId}")
    public ResponseEntity<Void> likeOutfit(
            @RequestHeader(name="X-Creator-Id", required=false) UUID creatorId,
            @PathVariable UUID outfitId) {

        if (creatorId == null)
            throw new IncompleteProfileException("You must complete your profile before liking an outfit");

        socialService.likeOutfit(creatorId, outfitId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/like/{outfitId}")
    public ResponseEntity<Void> unlikeOutfit(
            @RequestHeader(name="X-Creator-Id", required=false) UUID creatorId,
            @PathVariable UUID outfitId) {

        if (creatorId == null)
            throw new IncompleteProfileException("You must complete your profile before unliking an outfit");

        socialService.unlikeOutfit(creatorId, outfitId);
        return ResponseEntity.noContent().build();
    }

}