package com.localmarket.main.controller.region;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.Arrays;

@RestController
@RequestMapping("/api/regions")
@Tag(name = "Regions", description = "Moroccan regions API")
public class RegionController {

    @GetMapping
    @Operation(summary = "Get all regions", description = "Get list of all Moroccan regions")
    public ResponseEntity<List<String>> getAllRegions() {
        List<String> regions = Arrays.asList(
            "Tanger-Tetouan-Al Hoceima",
            "L'Oriental",
            "Fès-Meknès",
            "Rabat-Salé-Kénitra",
            "Béni Mellal-Khénifra",
            "Casablanca-Settat",
            "Marrakesh-Safi",
            "Drâa-Tafilalet",
            "Souss-Massa",
            "Guelmim-Oued Noun",
            "Laâyoune-Sakia El Hamra",
            "Dakhla-Oued Ed-Dahab"
        );
        
        return ResponseEntity.ok(regions);
    }
} 