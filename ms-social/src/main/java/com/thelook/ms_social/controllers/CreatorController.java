package com.thelook.ms_social.controllers;

import com.thelook.ms_social.models.dtos.CreatorRequest;
import com.thelook.ms_social.models.dtos.CreatorResponse;
import com.thelook.ms_social.models.dtos.CreatorUpdateRequest;
import com.thelook.ms_social.models.mappers.CreatorMapper;
import com.thelook.ms_social.services.CreatorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/creators")
public class CreatorController {

    private final CreatorService creatorService;
    private final CreatorMapper mapper;

    public CreatorController(CreatorService creatorService, CreatorMapper mapper) {
        this.creatorService = creatorService;
        this.mapper = mapper;
    }

    @GetMapping(value = "/{creatorId}")
    public ResponseEntity<CreatorResponse> findById(@PathVariable UUID creatorId) {
        CreatorResponse creator = mapper.toCreatorResponse(creatorService.findById(creatorId));
        return ResponseEntity.ok().body(creator);
    }

    @PostMapping
    public ResponseEntity<CreatorResponse> create(
            @RequestHeader(name = "X-User-Id") UUID userId,
            @RequestBody @Valid CreatorRequest request) {
        CreatorResponse createdCreator = mapper.toCreatorResponse(creatorService.create(userId, request));
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(createdCreator.id()).toUri();
        return ResponseEntity.created(uri).body(createdCreator);
    }

    @PutMapping
    public ResponseEntity<CreatorResponse> update(
            @RequestHeader(name = "X-Creator-Id") UUID creatorId,
            @RequestBody @Valid CreatorUpdateRequest request) {
        CreatorResponse updatedCreator = mapper.toCreatorResponse(creatorService.update(creatorId, request));
        return ResponseEntity.ok().body(updatedCreator);
    }

    @DeleteMapping
    public ResponseEntity<Map<String, String>> delete(
            @RequestHeader(name = "X-Creator-Id") UUID creatorId) {
        Map<String, String> response = new HashMap<>();
        response.put("message", creatorService.delete(creatorId));
        return ResponseEntity.ok().body(response);
    }

    @DeleteMapping("/hard")
    public ResponseEntity<Void> hardDelete(
            @RequestHeader(name = "X-Creator-Id") UUID creatorId) {
        creatorService.hardDelete(creatorId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reactivate")
    public ResponseEntity<CreatorResponse> reactivate(
            @RequestHeader(name = "X-User-Id") UUID userId) {
        return ResponseEntity.ok(mapper.toCreatorResponse(creatorService.reactivate(userId)));
    }

}