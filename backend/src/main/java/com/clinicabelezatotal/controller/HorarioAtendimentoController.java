package com.clinicabelezatotal.controller;

import com.clinicabelezatotal.dto.HorarioAtendimentoDTO;
import com.clinicabelezatotal.service.HorarioAtendimentoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.util.List;

@RestController
@RequestMapping("/api/horarios")
@RequiredArgsConstructor
public class HorarioAtendimentoController {

    private final HorarioAtendimentoService horarioService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ESTETICISTA_ADMINISTRADORA')")
    public ResponseEntity<HorarioAtendimentoDTO> cadastrar(@Valid @RequestBody HorarioAtendimentoDTO dto) {
        return ResponseEntity.ok(horarioService.cadastrar(dto));
    }

    @GetMapping("/funcionario/{funcionarioId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA', 'ESTETICISTA_ADMINISTRADORA')")
    public ResponseEntity<List<HorarioAtendimentoDTO>> listarPorFuncionario(@PathVariable Long funcionarioId) {
        return ResponseEntity.ok(horarioService.listarPorFuncionario(funcionarioId));
    }

    @GetMapping("/funcionario/{funcionarioId}/dia/{diaSemana}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA', 'ESTETICISTA_ADMINISTRADORA')")
    public ResponseEntity<List<HorarioAtendimentoDTO>> listarPorFuncionarioEDia(
            @PathVariable Long funcionarioId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) DayOfWeek diaSemana) {
        return ResponseEntity.ok(horarioService.listarPorFuncionarioEDia(funcionarioId, diaSemana));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA', 'ESTETICISTA_ADMINISTRADORA')")
    public ResponseEntity<HorarioAtendimentoDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(horarioService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ESTETICISTA_ADMINISTRADORA')")
    public ResponseEntity<HorarioAtendimentoDTO> atualizar(@PathVariable Long id, @Valid @RequestBody HorarioAtendimentoDTO dto) {
        return ResponseEntity.ok(horarioService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ESTETICISTA_ADMINISTRADORA')")
    public ResponseEntity<Void> inativar(@PathVariable Long id) {
        horarioService.inativar(id);
        return ResponseEntity.noContent().build();
    }
}
