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

    @PatchMapping("/follow/{targetId}")
    public ResponseEntity<Void> follow(
            @RequestHeader(name="X-Creator-Id", required=false) UUID creatorId,
            @PathVariable UUID targetId) {

        if (creatorId == null)
            throw new IncompleteProfileException("You must complete your profile before following someone");

        socialService.follow(creatorId, targetId);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/unfollow/{targetId}")
    public ResponseEntity<Void> unfollow(
            @RequestHeader(name="X-Creator-Id", required=false) UUID creatorId,
            @PathVariable UUID targetId) {

        if (creatorId == null)
            throw new IncompleteProfileException("You must complete your profile before unfollowing someone");

        socialService.unfollow(creatorId, targetId);
        return ResponseEntity.ok().build();
    }

}