package com.clinicabelezatotal.controller;

import com.clinicabelezatotal.dto.ProcedimentoDTO;
import com.clinicabelezatotal.model.Procedimento.CategoriaProcedimento;
import com.clinicabelezatotal.service.ProcedimentoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/procedimentos")
@RequiredArgsConstructor
public class ProcedimentoController {

    private final ProcedimentoService procedimentoService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ESTETICISTA_ADMINISTRADORA')")
    public ResponseEntity<ProcedimentoDTO> cadastrar(@Valid @RequestBody ProcedimentoDTO dto) {
        return ResponseEntity.ok(procedimentoService.cadastrar(dto));
    }

    @GetMapping
    public ResponseEntity<List<ProcedimentoDTO>> listarTodos() {
        return ResponseEntity.ok(procedimentoService.listarTodos());
    }

    @GetMapping("/categoria/{categoria}")
    public ResponseEntity<List<ProcedimentoDTO>> listarPorCategoria(@PathVariable CategoriaProcedimento categoria) {
        return ResponseEntity.ok(procedimentoService.listarPorCategoria(categoria));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProcedimentoDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(procedimentoService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ESTETICISTA_ADMINISTRADORA')")
    public ResponseEntity<ProcedimentoDTO> atualizar(@PathVariable Long id, @Valid @RequestBody ProcedimentoDTO dto) {
        return ResponseEntity.ok(procedimentoService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> inativar(@PathVariable Long id) {
        procedimentoService.inativar(id);
        return ResponseEntity.noContent().build();
    }
}
