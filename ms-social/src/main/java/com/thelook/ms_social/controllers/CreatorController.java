package com.thelook.ms_social.controllers;

import com.thelook.ms_social.models.dtos.CreatorRequest;
import com.thelook.ms_social.models.dtos.CreatorResponse;
import com.thelook.ms_social.models.dtos.CreatorUpdateRequest;
import com.thelook.ms_social.services.CreatorService;
import com.thelook.ms_social.models.mappers.CreatorMapper;
import org.mapstruct.factory.Mappers;
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

    CreatorMapper mapper = Mappers.getMapper(CreatorMapper.class);

    private final CreatorService creatorService;

    public CreatorController(CreatorService creatorService) {
        this.creatorService = creatorService;
    }

    @GetMapping(value = "/{creatorId}")
    public ResponseEntity<CreatorResponse> findById(
            @PathVariable UUID creatorId) {
        CreatorResponse creator = mapper.toCreatorResponse(creatorService.findById(creatorId));
        return ResponseEntity.ok().body(creator);
    }

    @PostMapping
    public ResponseEntity<CreatorResponse> create(
            @RequestHeader(name="X-User-Id") UUID userId,
            @RequestBody CreatorRequest request) {
        CreatorResponse createdCreator = mapper.toCreatorResponse(creatorService.create(userId, request));
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(createdCreator.id()).toUri();
        return ResponseEntity.created(uri).body(createdCreator);
    }

    @PutMapping
    public ResponseEntity<CreatorResponse> update(
            @RequestHeader(name="X-Creator-Id") UUID creatorId,
            @RequestBody CreatorUpdateRequest request) {
        CreatorResponse updatedCreator = mapper.toCreatorResponse(creatorService.update(creatorId, request));
        return ResponseEntity.ok().body(updatedCreator);
    }

    @DeleteMapping
    public ResponseEntity<Map<String,String>> delete(
            @RequestHeader(name="X-Creator-Id") UUID creatorId) {

        Map<String, String> response = new HashMap<>();
        String msg = creatorService.delete(creatorId);
        response.put("message", msg);
        return ResponseEntity.ok().body(response);
    }

}