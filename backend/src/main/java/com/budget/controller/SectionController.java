package com.budget.controller;

import com.budget.dto.CreateSectionRequest;
import com.budget.dto.SectionDTO;
import com.budget.dto.UpdateSectionRequest;
import com.budget.service.SectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sections")
@RequiredArgsConstructor
public class SectionController {

    private final SectionService sectionService;

    @PostMapping
    public ResponseEntity<SectionDTO> createSection(@Valid @RequestBody CreateSectionRequest request) {
        SectionDTO section = sectionService.createSection(request);
        return ResponseEntity.ok(section);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SectionDTO> updateSection(
            @PathVariable Long id,
            @RequestBody UpdateSectionRequest request) {
        SectionDTO section = sectionService.updateSection(id, request);
        return ResponseEntity.ok(section);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSection(@PathVariable Long id) {
        sectionService.deleteSection(id);
        return ResponseEntity.noContent().build();
    }
}
