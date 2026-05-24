package com.thelook.ms_creation.controllers;

import com.thelook.exceptions.IncompleteProfileException;
import com.thelook.ms_creation.entities.Outfit;
import com.thelook.ms_creation.models.dtos.OutfitRequest;
import com.thelook.ms_creation.models.dtos.OutfitResponse;
import com.thelook.ms_creation.models.mappers.OutfitMapper;
import com.thelook.ms_creation.services.OutfitService;
import com.thelook.ms_creation.services.StorageService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/creation")
public class OutfitController {

    private final OutfitService outfitService;
    private final StorageService storageService;
    private final OutfitMapper outfitMapper;

    public OutfitController(OutfitService outfitService, StorageService storageService,
                            OutfitMapper outfitMapper) {
        this.outfitService = outfitService;
        this.storageService = storageService;
        this.outfitMapper = outfitMapper;
    }

    @GetMapping(value = "/{outfitId}")
    public ResponseEntity<OutfitResponse> findById(@PathVariable UUID outfitId) {
        OutfitResponse outfit = outfitMapper.toOutfitResponse(outfitService.findById(outfitId));
        return ResponseEntity.ok().body(outfit);
    }

    @PostMapping(value = "/outfit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Outfit> createOutfit(
            @RequestHeader(name = "X-Creator-Id") UUID creatorId,
            @RequestPart("request") @Valid OutfitRequest request,
            @RequestPart("image1") MultipartFile image1,
            @RequestPart(value = "image2", required = false) MultipartFile image2,
            @RequestPart(value = "itemImages", required = false) List<MultipartFile> itemImages) {

        if (creatorId == null)
            throw new IncompleteProfileException("You must complete your profile before creating outfits");

        Outfit created = outfitService.createOutfit(creatorId, request, image1, image2, itemImages);

        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();

        return ResponseEntity.created(uri).body(created);
    }

}
