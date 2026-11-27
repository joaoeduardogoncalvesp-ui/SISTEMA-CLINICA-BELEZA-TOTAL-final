package com.clinicabelezatotal.integracao;

import com.clinicabelezatotal.dto.AgendamentoDTO;
import com.clinicabelezatotal.model.*;
import com.clinicabelezatotal.repository.*;
import com.clinicabelezatotal.service.AgendamentoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Teste de INTEGRAÇÃO real: sobe o contexto completo do Spring, usa o PostgreSQL
 * configurado e as mesmas entidades/repositórios/serviços da aplicação.
 *
 * <p>Cada teste roda dentro de uma transação que sofre ROLLBACK no final, então
 * nada é gravado de verdade no banco.
 *
 * <p>Pré-requisito: o PostgreSQL precisa estar no ar (mesmo banco do
 * application.properties). Rode com:
 * <pre>
 *   WHATSAPP_HABILITADO=false mvn test -Dtest=AgendamentoIntegracaoTest
 * </pre>
 */
@SpringBootTest
@Transactional
class AgendamentoIntegracaoTest {

    @Autowired private AgendamentoService agendamentoService;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private FuncionarioRepository funcionarioRepository;
    @Autowired private ProcedimentoRepository procedimentoRepository;

    private Cliente cliente;
    private Funcionario funcionario;
    private Procedimento procedimento;

    @BeforeEach
    void preparar() {
        // Usa os dados criados pelo DataInitializer.
        cliente = clienteRepository.findAll().get(0);

        // Escolhe um funcionário habilitado para "Massagem Relaxante" (60 min) no banco real.
        procedimento = procedimentoRepository.findAll().stream()
            .filter(p -> p.getNome().equals("Massagem Relaxante"))
            .findFirst()
            .orElseThrow();

        funcionario = funcionarioRepository.findAll().stream()
            .filter(f -> f.getProcedimentosHabilitados().contains(procedimento))
            .findFirst()
            .orElseThrow();
    }

    private AgendamentoDTO novoAgendamento(LocalDate data, LocalTime hora) {
        AgendamentoDTO dto = new AgendamentoDTO();
        dto.setClienteId(cliente.getId());
        dto.setFuncionarioId(funcionario.getId());
        dto.setProcedimentoId(procedimento.getId());
        dto.setData(data);
        dto.setHoraInicio(hora);
        return dto;
    }

    @Test
    @DisplayName("Agendamento é gravado e horaFim calculada pela duração do procedimento")
    void deveGravarAgendamentoComHoraFimCalculada() {
        LocalDate data = LocalDate.now().plusDays(30);
        LocalTime inicio = LocalTime.of(10, 0);

        AgendamentoDTO criado = agendamentoService.agendar(novoAgendamento(data, inicio));

        assertNotNull(criado.getId());
        assertEquals(inicio.plusMinutes(procedimento.getDuracaoMinutos()), criado.getHoraFim());
        assertEquals(Agendamento.StatusAgendamento.AGENDADO, criado.getStatus());
        assertEquals(cliente.getNome(), criado.getClienteNome());

        // Confirma que realmente foi para o banco de dados.
        List<AgendamentoDTO> doBanco = agendamentoService.listarPorFuncionarioEData(funcionario.getId(), data);
        assertTrue(doBanco.stream().anyMatch(a -> a.getId().equals(criado.getId())));
    }

    @Test
    @DisplayName("Não permite dois agendamentos sobrepostos para o mesmo funcionário")
    void deveRecusarConflitoDeHorario() {
        LocalDate data = LocalDate.now().plusDays(31);

        agendamentoService.agendar(novoAgendamento(data, LocalTime.of(14, 0)));

        RuntimeException erro = assertThrows(RuntimeException.class,
            () -> agendamentoService.agendar(novoAgendamento(data, LocalTime.of(14, 30))));
        assertTrue(erro.getMessage().contains("Horário já agendado"));
    }

    @Test
    @DisplayName("Permite agendamentos encostados (fim de um = início do outro)")
    void devePermitirAgendamentosConsecutivos() {
        LocalDate data = LocalDate.now().plusDays(32);

        agendamentoService.agendar(novoAgendamento(data, LocalTime.of(8, 0)));
        AgendamentoDTO segundo = agendamentoService.agendar(
            novoAgendamento(data, LocalTime.of(8, 0).plusMinutes(procedimento.getDuracaoMinutos())));

        assertNotNull(segundo.getId());
    }

    @Test
    @DisplayName("Não permite agendar procedimento para o qual o funcionário não está habilitado")
    void deveRecusarFuncionarioNaoHabilitado() {
        Procedimento naoHabilitado = procedimentoRepository.findAll().stream()
            .filter(p -> !funcionario.getProcedimentosHabilitados().contains(p))
            .findFirst()
            .orElseThrow();

        AgendamentoDTO dto = novoAgendamento(LocalDate.now().plusDays(33), LocalTime.of(9, 0));
        dto.setProcedimentoId(naoHabilitado.getId());

        RuntimeException erro = assertThrows(RuntimeException.class, () -> agendamentoService.agendar(dto));
        assertTrue(erro.getMessage().contains("não está habilitado"));
    }

    @Test
    @DisplayName("Cancelar libera o horário para um novo agendamento")
    void cancelarDeveLiberarHorario() {
        LocalDate data = LocalDate.now().plusDays(34);
        AgendamentoDTO primeiro = agendamentoService.agendar(novoAgendamento(data, LocalTime.of(11, 0)));

        agendamentoService.cancelar(primeiro.getId());

        AgendamentoDTO novo = agendamentoService.agendar(novoAgendamento(data, LocalTime.of(11, 0)));
        assertNotNull(novo.getId());
        assertNotEquals(primeiro.getId(), novo.getId());
    }
}
