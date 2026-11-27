package com.clinicabelezatotal.controller;

import com.clinicabelezatotal.dto.ClienteDTO;
import com.clinicabelezatotal.service.ClienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService clienteService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA', 'ESTETICISTA_ADMINISTRADORA')")
    public ResponseEntity<ClienteDTO> cadastrar(@Valid @RequestBody ClienteDTO dto) {
        return ResponseEntity.ok(clienteService.cadastrar(dto));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA', 'ESTETICISTA_ADMINISTRADORA')")
    public ResponseEntity<List<ClienteDTO>> listarTodos() {
        return ResponseEntity.ok(clienteService.listarTodos());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA', 'ESTETICISTA_ADMINISTRADORA')")
    public ResponseEntity<ClienteDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.buscarPorId(id));
    }

    @GetMapping("/buscar")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA', 'ESTETICISTA_ADMINISTRADORA')")
    public ResponseEntity<List<ClienteDTO>> buscarPorNome(@RequestParam String nome) {
        return ResponseEntity.ok(clienteService.buscarPorNome(nome));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA', 'ESTETICISTA_ADMINISTRADORA')")
    public ResponseEntity<ClienteDTO> atualizar(@PathVariable Long id, @Valid @RequestBody ClienteDTO dto) {
        return ResponseEntity.ok(clienteService.atualizar(id, dto));
    }

    // LGPD - Exclusão lógica
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<Void> inativar(@PathVariable Long id) {
        clienteService.inativar(id);
        return ResponseEntity.noContent().build();
    }

    // LGPD - Exclusão definitiva de dados (anonimização)
    @DeleteMapping("/{id}/dados")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> excluirDados(@PathVariable Long id) {
        clienteService.excluirDados(id);
        return ResponseEntity.noContent().build();
    }

    // LGPD - Revogar consentimento WhatsApp
    @PatchMapping("/{id}/consentimento/whatsapp/revogar")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<Void> revogarConsentimentoWhatsApp(@PathVariable Long id) {
        clienteService.revogarConsentimentoWhatsApp(id);
        return ResponseEntity.noContent().build();
    }
}
