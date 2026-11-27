package com.clinicabelezatotal.controller;

import com.clinicabelezatotal.dto.FuncionarioDTO;
import com.clinicabelezatotal.service.FuncionarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/funcionarios")
@RequiredArgsConstructor
public class FuncionarioController {

    private final FuncionarioService funcionarioService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ESTETICISTA_ADMINISTRADORA')")
    public ResponseEntity<FuncionarioDTO> cadastrar(@Valid @RequestBody FuncionarioDTO dto) {
        return ResponseEntity.ok(funcionarioService.cadastrar(dto));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA', 'ESTETICISTA_ADMINISTRADORA')")
    public ResponseEntity<List<FuncionarioDTO>> listarTodos() {
        return ResponseEntity.ok(funcionarioService.listarTodos());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA', 'ESTETICISTA_ADMINISTRADORA')")
    public ResponseEntity<FuncionarioDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(funcionarioService.buscarPorId(id));
    }

    @GetMapping("/procedimento/{procedimentoId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA', 'ESTETICISTA_ADMINISTRADORA')")
    public ResponseEntity<List<FuncionarioDTO>> buscarPorProcedimento(@PathVariable Long procedimentoId) {
        return ResponseEntity.ok(funcionarioService.buscarPorProcedimento(procedimentoId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ESTETICISTA_ADMINISTRADORA')")
    public ResponseEntity<FuncionarioDTO> atualizar(@PathVariable Long id, @Valid @RequestBody FuncionarioDTO dto) {
        return ResponseEntity.ok(funcionarioService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> inativar(@PathVariable Long id) {
        funcionarioService.inativar(id);
        return ResponseEntity.noContent().build();
    }
}
