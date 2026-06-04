package com.thelook.ms_creation.controllers;

import com.thelook.exceptions.IncompleteProfileException;
import com.thelook.ms_creation.models.dtos.ItemRequest;
import com.thelook.ms_creation.models.dtos.ItemResponse;
import com.thelook.ms_creation.models.dtos.OutfitRequest;
import com.thelook.ms_creation.models.dtos.OutfitResponse;
import com.thelook.ms_creation.models.mappers.OutfitMapper;
import com.thelook.ms_creation.services.ItemService;
import com.thelook.ms_creation.services.OutfitService;
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
@RequestMapping("/api/v1/creation/outfits")
public class OutfitController {

    private final OutfitService outfitService;
    private final ItemService itemService;
    private final OutfitMapper outfitMapper;

    public OutfitController(OutfitService outfitService, ItemService itemService,
                            OutfitMapper outfitMapper) {
        this.outfitService = outfitService;
        this.itemService = itemService;
        this.outfitMapper = outfitMapper;
    }

    @GetMapping("/{outfitId}")
    public ResponseEntity<OutfitResponse> findById(@PathVariable UUID outfitId) {
        OutfitResponse outfit = outfitMapper.toOutfitResponse(outfitService.findById(outfitId));
        return ResponseEntity.ok(outfit);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<OutfitResponse> createOutfit(
            @RequestHeader(name = "X-Creator-Id") UUID creatorId,
            @RequestPart("request") @Valid OutfitRequest request,
            @RequestPart("image1") MultipartFile image1,
            @RequestPart(value = "image2", required = false) MultipartFile image2,
            @RequestPart(value = "itemImages", required = false) List<MultipartFile> itemImages) {

        if (creatorId == null)
            throw new IncompleteProfileException("You must complete your profile before creating outfits");

        OutfitResponse created = outfitMapper.toOutfitResponse(
                outfitService.createOutfit(creatorId, request, image1, image2, itemImages));

        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.outfitId())
                .toUri();

        return ResponseEntity.created(uri).body(created);
    }

    @DeleteMapping("/{outfitId}")
    public ResponseEntity<Void> deleteOutfit(
            @RequestHeader(name = "X-Creator-Id") UUID creatorId,
            @PathVariable UUID outfitId) {

        if (creatorId == null)
            throw new IncompleteProfileException("You must complete your profile before managing outfits");

        outfitService.delete(outfitId, creatorId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{outfitId}/hard")
    public ResponseEntity<Void> hardDeleteOutfit(
            @RequestHeader(name = "X-Creator-Id") UUID creatorId,
            @PathVariable UUID outfitId) {

        if (creatorId == null)
            throw new IncompleteProfileException("You must complete your profile before managing outfits");

        outfitService.hardDelete(outfitId, creatorId);
        return ResponseEntity.noContent().build();
    }

    // --- Item endpoints ---

    @PostMapping(value = "/{outfitId}/items", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ItemResponse> addItem(
            @RequestHeader(name = "X-Creator-Id") UUID creatorId,
            @PathVariable UUID outfitId,
            @RequestPart("request") @Valid ItemRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {

        if (creatorId == null)
            throw new IncompleteProfileException("You must complete your profile before managing outfits");

        ItemResponse created = itemService.addItem(outfitId, creatorId, request, image);

        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.itemId())
                .toUri();

        return ResponseEntity.created(uri).body(created);
    }

    @DeleteMapping("/{outfitId}/items/{itemId}")
    public ResponseEntity<Void> deleteItem(
            @RequestHeader(name = "X-Creator-Id") UUID creatorId,
            @PathVariable UUID outfitId,
            @PathVariable UUID itemId) {

        if (creatorId == null)
            throw new IncompleteProfileException("You must complete your profile before managing outfits");

        itemService.deleteItem(outfitId, itemId, creatorId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{outfitId}/items/{itemId}/hard")
    public ResponseEntity<Void> hardDeleteItem(
            @RequestHeader(name = "X-Creator-Id") UUID creatorId,
            @PathVariable UUID outfitId,
            @PathVariable UUID itemId) {

        if (creatorId == null)
            throw new IncompleteProfileException("You must complete your profile before managing outfits");

        itemService.hardDeleteItem(outfitId, itemId, creatorId);
        return ResponseEntity.noContent().build();
    }
}