package com.thelook.ms_creation.controllers;

import com.thelook.exceptions.IncompleteProfileException;
import com.thelook.ms_creation.entities.Outfit;
import com.thelook.ms_creation.models.dtos.OutfitRequest;
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

    public OutfitController(OutfitService outfitService, StorageService storageService) {
        this.outfitService = outfitService;
        this.storageService = storageService;
    }
//
//    @PostMapping("/outfit")
//    public ResponseEntity<String> uploadOutfit(
//            @RequestParam("file") MultipartFile file,
//            @RequestParam("creatorId") String creatorId,
//            @RequestParam("outfitId") String outfitId) {
//
//        String path = storageService.save(file, creatorId, outfitId);
//        return ResponseEntity.ok("Outfit criado com sucesso! Imagem em: " + path);
//    }
//
//    @PostMapping(value = "/outfit/image", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
//    public ResponseEntity<String> createOutfit(
//            @RequestPart("data") OutfitRequest outfitData, // JSON com os dados
//            @RequestPart("images") List<MultipartFile> images) { // Lista de imagens
//
//        // Exemplo de lógica:
//        // 1. Gera um UUID para o outfitId
//        // 2. Chama o storageService para cada imagem na lista
//        // 3. Salva no banco (próximo passo)
//
//        return ResponseEntity.ok("Outfit processado com sucesso!");
//    }

    @PostMapping(value = "/outfit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Outfit> createOutfit(
            @RequestHeader(name="X-Creator-Id") UUID creatorId,
            @RequestPart("data") @Valid OutfitRequest data,
            @RequestPart("image") MultipartFile image) {

        if (creatorId == null)
            throw new IncompleteProfileException("You must complete your profile before creating outfits");

        Outfit created = outfitService.create(creatorId, data, image);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(uri).body(created);
    }

}
