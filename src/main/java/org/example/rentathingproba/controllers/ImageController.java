package org.example.rentathingproba.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/images")
public class ImageController {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<List<String>> uploadImages(
            @RequestParam("files") List<MultipartFile> files) throws IOException {

        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;

            String originalName = file.getOriginalFilename();
            String extension = (originalName != null && originalName.contains("."))
                    ? originalName.substring(originalName.lastIndexOf('.'))
                    : ".jpg";

            String fileName = UUID.randomUUID() + extension;
            Path filePath = uploadPath.resolve(fileName);
            Files.write(filePath, file.getBytes());

            // Automatski generira točan URL s pravim IP-jem, portom i /api context-pathom
            String fileUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/images/")
                    .path(fileName)
                    .toUriString();

            urls.add(fileUrl);
        }

        return ResponseEntity.ok(urls);
    }

    @GetMapping("/{fileName:.+}")
    public ResponseEntity<byte[]> serveImage(@PathVariable String fileName) throws IOException {
        Path filePath = Paths.get(uploadDir).resolve(fileName);
        if (!Files.exists(filePath)) {
            return ResponseEntity.notFound().build();
        }
        byte[] bytes = Files.readAllBytes(filePath);
        String contentType = fileName.endsWith(".png") ? "image/png" : "image/jpeg";
        return ResponseEntity.ok()
                .header("Content-Type", contentType)
                .body(bytes);
    }
}