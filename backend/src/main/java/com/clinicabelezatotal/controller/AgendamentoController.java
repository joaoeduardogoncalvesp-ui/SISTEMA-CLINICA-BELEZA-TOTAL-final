package com.clinicabelezatotal.controller;

import com.clinicabelezatotal.dto.AgendamentoDTO;
import com.clinicabelezatotal.dto.HistoricoAgendamentoDTO;
import com.clinicabelezatotal.model.Agendamento.StatusAgendamento;
import com.clinicabelezatotal.service.AgendamentoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/agendamentos")
@RequiredArgsConstructor
public class AgendamentoController {

    private final AgendamentoService agendamentoService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA', 'ESTETICISTA_ADMINISTRADORA')")
    public ResponseEntity<AgendamentoDTO> agendar(@Valid @RequestBody AgendamentoDTO dto) {
        return ResponseEntity.ok(agendamentoService.agendar(dto));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('FUNCIONARIO', 'ADMIN', 'RECEPCIONISTA', 'ESTETICISTA_ADMINISTRADORA')")
    public ResponseEntity<List<AgendamentoDTO>> listarMeusAgendamentos() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return ResponseEntity.ok(agendamentoService.listarPorEmailFuncionario(email));
    }

    @GetMapping("/funcionario/{funcionarioId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA', 'ESTETICISTA_ADMINISTRADORA', 'FUNCIONARIO')")
    public ResponseEntity<List<AgendamentoDTO>> listarPorFuncionario(@PathVariable Long funcionarioId) {
        return ResponseEntity.ok(agendamentoService.listarTodosPorFuncionario(funcionarioId));
    }

    @GetMapping("/funcionario/{funcionarioId}/data/{data}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA', 'ESTETICISTA_ADMINISTRADORA', 'FUNCIONARIO')")
    public ResponseEntity<List<AgendamentoDTO>> listarPorFuncionarioEData(
            @PathVariable Long funcionarioId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return ResponseEntity.ok(agendamentoService.listarPorFuncionarioEData(funcionarioId, data));
    }

    @GetMapping("/periodo")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA', 'ESTETICISTA_ADMINISTRADORA')")
    public ResponseEntity<List<AgendamentoDTO>> listarPorPeriodo(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return ResponseEntity.ok(agendamentoService.listarPorPeriodo(inicio, fim));
    }

    @GetMapping("/cliente/{clienteId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA', 'ESTETICISTA_ADMINISTRADORA')")
    public ResponseEntity<List<AgendamentoDTO>> listarPorCliente(@PathVariable Long clienteId) {
        return ResponseEntity.ok(agendamentoService.listarPorCliente(clienteId));
    }

    @GetMapping("/cliente/{clienteId}/historico")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA', 'ESTETICISTA_ADMINISTRADORA')")
    public ResponseEntity<List<HistoricoAgendamentoDTO>> buscarHistorico(@PathVariable Long clienteId) {
        return ResponseEntity.ok(agendamentoService.buscarHistoricoPorCliente(clienteId));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA', 'ESTETICISTA_ADMINISTRADORA', 'FUNCIONARIO')")
    public ResponseEntity<AgendamentoDTO> alterarStatus(
            @PathVariable Long id,
            @RequestParam StatusAgendamento status) {
        return ResponseEntity.ok(agendamentoService.alterarStatus(id, status));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA', 'ESTETICISTA_ADMINISTRADORA')")
    public ResponseEntity<AgendamentoDTO> alterar(@PathVariable Long id, @Valid @RequestBody AgendamentoDTO dto) {
        return ResponseEntity.ok(agendamentoService.alterar(id, dto));
    }

    @PatchMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA', 'ESTETICISTA_ADMINISTRADORA')")
    public ResponseEntity<AgendamentoDTO> cancelar(@PathVariable Long id) {
        return ResponseEntity.ok(agendamentoService.cancelar(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA', 'ESTETICISTA_ADMINISTRADORA')")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        agendamentoService.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/diagnostico-whatsapp")
    @PreAuthorize("hasAnyRole('ADMIN', 'ESTETICISTA_ADMINISTRADORA')")
    public ResponseEntity<java.util.Map<String, Object>> diagnosticoWhatsapp() {
        var falha = agendamentoService.ultimaFalhaWhatsapp();
        java.util.Map<String, Object> corpo = new java.util.LinkedHashMap<>();
        corpo.put("envioRealHabilitado", agendamentoService.whatsappHabilitado());
        corpo.put("ultimaFalha", falha);
        return ResponseEntity.ok(corpo);
    }

    @GetMapping("/lembretes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AgendamentoDTO>> buscarAgendamentosParaLembrete() {
        return ResponseEntity.ok(agendamentoService.buscarAgendamentosParaLembrete());
    }
}
