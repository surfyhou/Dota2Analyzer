package com.dota2analyzer.data.controller;

import com.dota2analyzer.data.service.HeroImageCache;
import com.dota2analyzer.data.service.ItemImageCache;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;

@RestController
@RequestMapping("/api/assets")
public class AssetController {

    private final HeroImageCache heroImages;
    private final ItemImageCache itemImages;

    public AssetController(HeroImageCache heroImages, ItemImageCache itemImages) {
        this.heroImages = heroImages;
        this.itemImages = itemImages;
    }

    @GetMapping("/heroes/{heroId}")
    public ResponseEntity<Resource> getHeroImage(@PathVariable int heroId) {
        String path = heroImages.ensureHeroImage(heroId);
        if (path == null || path.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        File file = new File(path);
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(new FileSystemResource(file));
    }

    @GetMapping("/items/{itemKey}")
    public ResponseEntity<Resource> getItemImage(@PathVariable String itemKey) {
        String path = itemImages.ensureItemImage(itemKey);
        if (path == null || path.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        File file = new File(path);
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(new FileSystemResource(file));
    }
}
