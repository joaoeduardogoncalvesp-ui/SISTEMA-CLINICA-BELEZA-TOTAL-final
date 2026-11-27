package com.clinicabelezatotal.controller;

import com.clinicabelezatotal.dto.FuncionarioDTO;
import com.clinicabelezatotal.dto.ProcedimentoDTO;
import com.clinicabelezatotal.model.Procedimento.CategoriaProcedimento;
import com.clinicabelezatotal.service.FuncionarioService;
import com.clinicabelezatotal.service.ProcedimentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

    private final ProcedimentoService procedimentoService;
    private final FuncionarioService funcionarioService;

    // RF24 - Homepage institucional e portfólio de serviços (acesso público)
    @GetMapping("/homepage")
    public ResponseEntity<Map<String, Object>> homepage() {
        Map<String, Object> info = Map.of(
            "nome", "Clínica Beleza Total",
            "responsavel", "Silvana da Costa",
            "servicos", List.of(
                "Procedimentos Faciais",
                "Procedimentos Corporais",
                "Procedimentos Capilares",
                "Procedimentos Invasivos"
            ),
            "horarioFuncionamento", "Segunda a Sexta: 8h às 18h | Sábado: 8h às 14h",
            "contato", Map.of(
                "telefone", "(62) 99999-9999",
                "email", "contato@belezatotal.com.br"
            )
        );
        return ResponseEntity.ok(info);
    }

    // RF24 - Portfólio de serviços (acesso público)
    @GetMapping("/portfolio")
    public ResponseEntity<List<ProcedimentoDTO>> portfolio() {
        return ResponseEntity.ok(procedimentoService.listarTodos());
    }

    // RF24 - Procedimentos por categoria (acesso público)
    @GetMapping("/portfolio/categoria/{categoria}")
    public ResponseEntity<List<ProcedimentoDTO>> portfolioPorCategoria(@PathVariable CategoriaProcedimento categoria) {
        return ResponseEntity.ok(procedimentoService.listarPorCategoria(categoria));
    }

    // RF24 - Profissionais (acesso público)
    @GetMapping("/funcionarios")
    public ResponseEntity<List<FuncionarioDTO>> funcionarios() {
        return ResponseEntity.ok(funcionarioService.listarTodos());
    }
}
