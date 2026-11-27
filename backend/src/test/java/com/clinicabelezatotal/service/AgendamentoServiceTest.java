package com.clinicabelezatotal.service;

import com.clinicabelezatotal.dto.AgendamentoDTO;
import com.clinicabelezatotal.exception.ResourceNotFoundException;
import com.clinicabelezatotal.model.*;
import com.clinicabelezatotal.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgendamentoServiceTest {

    @Mock
    private AgendamentoRepository agendamentoRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private FuncionarioRepository funcionarioRepository;
    @Mock
    private ProcedimentoRepository procedimentoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private WhatsAppService whatsappService;

    @InjectMocks
    private AgendamentoService agendamentoService;

    private Agendamento agendamento;
    private Cliente cliente;
    private Funcionario funcionario;
    private Procedimento procedimento;

    @BeforeEach
    void setUp() {
        cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNome("Maria Silva");
        cliente.setAtivo(true);

        funcionario = new Funcionario();
        funcionario.setId(1L);
        funcionario.setNome("Ana Beatriz");
        funcionario.setAtivo(true);

        procedimento = new Procedimento();
        procedimento.setId(1L);
        procedimento.setNome("Limpeza de Pele");
        procedimento.setDuracaoMinutos(45);
        procedimento.setCategoria(Procedimento.CategoriaProcedimento.FACIAL);

        agendamento = new Agendamento();
        agendamento.setId(1L);
        agendamento.setCliente(cliente);
        agendamento.setFuncionario(funcionario);
        agendamento.setProcedimento(procedimento);
        agendamento.setData(LocalDate.now().plusDays(1));
        agendamento.setHoraInicio(LocalTime.of(10, 0));
        agendamento.setHoraFim(LocalTime.of(10, 45));
        agendamento.setStatus(Agendamento.StatusAgendamento.AGENDADO);
    }

    @Test
    void listarPorPeriodo_DeveRetornarAgendamentos() {
        when(agendamentoRepository.findByPeriodo(any(), any())).thenReturn(Arrays.asList(agendamento));

        List<AgendamentoDTO> resultado = agendamentoService.listarPorPeriodo(
            LocalDate.now(), LocalDate.now().plusDays(7));

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
    }

    @Test
    void cancelar_DeveCancelarAgendamento() {
        when(agendamentoRepository.findById(1L)).thenReturn(Optional.of(agendamento));
        when(agendamentoRepository.save(any(Agendamento.class))).thenReturn(agendamento);

        AgendamentoDTO resultado = agendamentoService.cancelar(1L);

        assertNotNull(resultado);
        assertEquals(Agendamento.StatusAgendamento.CANCELADO, agendamento.getStatus());
        verify(agendamentoRepository, times(1)).save(agendamento);
        verify(whatsappService, times(1)).enviarNotificacaoCancelamento(agendamento);
    }

    @Test
    void alterarStatus_DeveAtualizarStatus() {
        when(agendamentoRepository.findById(1L)).thenReturn(Optional.of(agendamento));
        when(agendamentoRepository.save(any(Agendamento.class))).thenReturn(agendamento);

        AgendamentoDTO resultado = agendamentoService.alterarStatus(1L, Agendamento.StatusAgendamento.CONFIRMADO);

        assertNotNull(resultado);
        assertEquals(Agendamento.StatusAgendamento.CONFIRMADO, agendamento.getStatus());
        verify(whatsappService, times(1)).enviarNotificacaoAlteracaoStatus(agendamento);
    }
}
