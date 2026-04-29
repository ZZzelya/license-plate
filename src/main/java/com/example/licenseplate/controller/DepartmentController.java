package com.example.licenseplate.controller;

import com.example.licenseplate.dto.request.DepartmentCreateDto;
import com.example.licenseplate.dto.response.DepartmentDto;
import com.example.licenseplate.service.DepartmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
@Tag(name = "РћС‚РґРµР»С‹ Р“РђР", description = "API РґР»СЏ СѓРїСЂР°РІР»РµРЅРёСЏ РѕС‚РґРµР»Р°РјРё СЂРµРіРёСЃС‚СЂР°С†РёРё")
public class DepartmentController {

    private final DepartmentService departmentService;

    @Operation(summary = "РЎРѕР·РґР°С‚СЊ РѕС‚РґРµР» Р“РђР", description = "Р РµРіРёСЃС‚СЂРёСЂСѓРµС‚ РЅРѕРІС‹Р№ РѕС‚РґРµР» СЂРµРіРёСЃС‚СЂР°С†РёРё")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "РћС‚РґРµР» СѓСЃРїРµС€РЅРѕ СЃРѕР·РґР°РЅ",
            content = @Content(schema = @Schema(implementation = DepartmentDto.class))),
        @ApiResponse(responseCode = "400", description = "РќРµРІРµСЂРЅС‹Рµ РґР°РЅРЅС‹Рµ Р·Р°РїСЂРѕСЃР°", content = @Content),
        @ApiResponse(responseCode = "409", description = "РћС‚РґРµР» СЃ С‚Р°РєРёРј С‚РµР»РµС„РѕРЅРѕРј СѓР¶Рµ СЃСѓС‰РµСЃС‚РІСѓРµС‚", content = @Content)
    })
    @PostMapping
    public ResponseEntity<DepartmentDto> createDepartment(@Valid @RequestBody final DepartmentCreateDto createDto) {
        DepartmentDto created = departmentService.createDepartment(createDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "РџРѕР»СѓС‡РёС‚СЊ РІСЃРµ РѕС‚РґРµР»С‹", description = "Р’РѕР·РІСЂР°С‰Р°РµС‚ СЃРїРёСЃРѕРє РІСЃРµС… РѕС‚РґРµР»РѕРІ Р“РђР")
    @GetMapping
    public ResponseEntity<List<DepartmentDto>> getAllDepartments() {
        return ResponseEntity.ok(departmentService.getAllDepartments());
    }

    @Operation(summary = "РџРѕР»СѓС‡РёС‚СЊ РѕС‚РґРµР» РїРѕ ID", description = "Р’РѕР·РІСЂР°С‰Р°РµС‚ РёРЅС„РѕСЂРјР°С†РёСЋ РѕР± РѕС‚РґРµР»Рµ РїРѕ РµРіРѕ РёРґРµРЅС‚РёС„РёРєР°С‚РѕСЂСѓ")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "РћС‚РґРµР» РЅР°Р№РґРµРЅ",
            content = @Content(schema = @Schema(implementation = DepartmentDto.class))),
        @ApiResponse(responseCode = "404", description = "РћС‚РґРµР» РЅРµ РЅР°Р№РґРµРЅ", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<DepartmentDto> getDepartmentById(@PathVariable final Long id) {
        return ResponseEntity.ok(departmentService.getDepartmentById(id));
    }

    @Operation(summary = "РџРѕР»СѓС‡РёС‚СЊ РѕС‚РґРµР»С‹ РїРѕ СЂРµРіРёРѕРЅСѓ", description = "Р’РѕР·РІСЂР°С‰Р°РµС‚ СЃРїРёСЃРѕРє РѕС‚РґРµР»РѕРІ РІ СѓРєР°Р·Р°РЅРЅРѕРј СЂРµРіРёРѕРЅРµ")
    @GetMapping("/by-region")
    public ResponseEntity<List<DepartmentDto>> getDepartmentsByRegion(
        @Parameter(description = "Р РµРіРёРѕРЅ", required = true, example = "MINSK")
        @RequestParam final String region) {
        return ResponseEntity.ok(departmentService.getDepartmentsByRegion(region));
    }

    @Operation(summary = "РћР±РЅРѕРІРёС‚СЊ РѕС‚РґРµР»", description = "РћР±РЅРѕРІР»СЏРµС‚ РёРЅС„РѕСЂРјР°С†РёСЋ Рѕ СЃСѓС‰РµСЃС‚РІСѓСЋС‰РµРј РѕС‚РґРµР»Рµ")
    @PutMapping("/{id}")
    public ResponseEntity<DepartmentDto> updateDepartment(
        @PathVariable final Long id,
        @Valid @RequestBody final DepartmentCreateDto updateDto) {
        return ResponseEntity.ok(departmentService.updateDepartment(id, updateDto));
    }

    @Operation(summary = "РЈРґР°Р»РёС‚СЊ РѕС‚РґРµР»", description = "РЈРґР°Р»СЏРµС‚ РѕС‚РґРµР» РёР· СЃРёСЃС‚РµРјС‹. РќРµРІРѕР·РјРѕР¶РЅРѕ СѓРґР°Р»РёС‚СЊ РѕС‚РґРµР» СЃ Р·Р°СЏРІР»РµРЅРёСЏРјРё.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "РћС‚РґРµР» СѓСЃРїРµС€РЅРѕ СѓРґР°Р»РµРЅ", content = @Content),
        @ApiResponse(responseCode = "404", description = "РћС‚РґРµР» РЅРµ РЅР°Р№РґРµРЅ", content = @Content),
        @ApiResponse(responseCode = "409", description = "РќРµРІРѕР·РјРѕР¶РЅРѕ СѓРґР°Р»РёС‚СЊ РѕС‚РґРµР» СЃ Р·Р°СЏРІР»РµРЅРёСЏРјРё", content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDepartment(@PathVariable final Long id) {
        departmentService.deleteDepartment(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Р”РµРјРѕРЅСЃС‚СЂР°С†РёСЏ РѕР±С…РѕРґР° РѕС‚РґРµР»РѕРІ", description = "РџРѕРєР°Р·С‹РІР°РµС‚ РѕР±С…РѕРґ РѕС‚РґРµР»РѕРІ РїРѕ СЂРµРіРёРѕРЅСѓ")
    @GetMapping("/demo/nplus1")
    public ResponseEntity<String> demonstrateNPlusOne(@RequestParam final String region) {
        departmentService.demonstrateNPlusOneProblem(region);
        return ResponseEntity.ok("Department traversal demonstrated. Check logs!");
    }

    @Operation(summary = "РћР±С…РѕРґ РѕС‚РґРµР»РѕРІ РїРѕ СЂРµРіРёРѕРЅСѓ", description = "РџРѕРєР°Р·С‹РІР°РµС‚ РѕР±С…РѕРґ РѕС‚РґРµР»РѕРІ РґР»СЏ РѕС‚Р»Р°РґРєРё")
    @GetMapping("/demo/solved")
    public ResponseEntity<String> solveNPlusOne(@RequestParam final String region) {
        departmentService.solveNPlusOneWithFetchJoin(region);
        return ResponseEntity.ok("Department traversal completed. Check logs!");
    }
}
