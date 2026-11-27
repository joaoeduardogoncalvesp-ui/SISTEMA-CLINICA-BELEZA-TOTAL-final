package com.clinicabelezatotal.controller;

import com.clinicabelezatotal.dto.IndicadoresDTO;
import com.clinicabelezatotal.service.IndicadoresService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/indicadores")
@RequiredArgsConstructor
public class IndicadoresController {

    private final IndicadoresService indicadoresService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ESTETICISTA_ADMINISTRADORA')")
    public ResponseEntity<IndicadoresDTO> buscarIndicadores(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return ResponseEntity.ok(indicadoresService.buscarIndicadores(inicio, fim));
    }
}
