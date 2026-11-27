package com.clinicabelezatotal.service;

import com.clinicabelezatotal.dto.AgendamentoDTO;
import com.clinicabelezatotal.model.*;
import com.clinicabelezatotal.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Regras de negócio do agendamento — RF08, RF09, RF10 e RF11.
 * Cada teste verifica um comportamento observável, sem depender de ordem de execução.
 */
@ExtendWith(MockitoExtension.class)
class AgendamentoRegrasNegocioTest {

    @Mock private AgendamentoRepository agendamentoRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private FuncionarioRepository funcionarioRepository;
    @Mock private ProcedimentoRepository procedimentoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private WhatsAppService whatsappService;

    @InjectMocks private AgendamentoService service;

    private Cliente cliente;
    private Funcionario funcionario;
    private Procedimento procedimento;
    private AgendamentoDTO dto;

    @BeforeEach
    void setUp() {
        cliente = Cliente.builder().id(1L).nome("Maria Silva").ativo(true).build();

        procedimento = Procedimento.builder()
            .id(1L).nome("Limpeza de Pele").duracaoMinutos(60)
            .categoria(Procedimento.CategoriaProcedimento.FACIAL).build();

        funcionario = Funcionario.builder()
            .id(1L).nome("Ana Beatriz").ativo(true)
            .procedimentosHabilitados(new java.util.ArrayList<>(List.of(procedimento))).build();

        dto = new AgendamentoDTO();
        dto.setClienteId(1L);
        dto.setFuncionarioId(1L);
        dto.setProcedimentoId(1L);
        dto.setData(LocalDate.of(2026, 11, 20));
        dto.setHoraInicio(LocalTime.of(14, 0));
    }

    private void clienteFuncionarioProcedimentoExistem() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(funcionarioRepository.findById(1L)).thenReturn(Optional.of(funcionario));
        when(procedimentoRepository.findById(1L)).thenReturn(Optional.of(procedimento));
    }

    @Nested
    @DisplayName("Cálculo do horário de término (RF08)")
    class CalculoHoraFim {

        @Test
        @DisplayName("hora final é a inicial mais a duração do procedimento")
        void usaDuracaoDoProcedimento() {
            clienteFuncionarioProcedimentoExistem();
            when(agendamentoRepository.findConflitosHorario(anyLong(), any(), any(), any()))
                .thenReturn(Collections.emptyList());
            when(agendamentoRepository.save(any(Agendamento.class)))
                .thenAnswer(inv -> inv.getArgument(0));

            AgendamentoDTO resultado = service.agendar(dto);

            assertEquals(LocalTime.of(14, 0), resultado.getHoraInicio());
            assertEquals(LocalTime.of(15, 0), resultado.getHoraFim(),
                "procedimento de 60 min iniciado às 14h deve terminar às 15h");
        }

        @Test
        @DisplayName("procedimento de 45 min iniciado às 14h termina às 14h45")
        void respeitaDuracaoDiferente() {
            procedimento.setDuracaoMinutos(45);
            clienteFuncionarioProcedimentoExistem();
            when(agendamentoRepository.findConflitosHorario(anyLong(), any(), any(), any()))
                .thenReturn(Collections.emptyList());
            when(agendamentoRepository.save(any(Agendamento.class)))
                .thenAnswer(inv -> inv.getArgument(0));

            AgendamentoDTO resultado = service.agendar(dto);

            assertEquals(LocalTime.of(14, 45), resultado.getHoraFim());
        }
    }

    @Nested
    @DisplayName("Conflito de horário (RF09)")
    class ConflitoHorario {

        @Test
        @DisplayName("quando existe atendimento sobreposto, recusa com mensagem específica")
        void recusaHorarioOcupado() {
            clienteFuncionarioProcedimentoExistem();
            Agendamento existente = Agendamento.builder().id(99L).build();
            when(agendamentoRepository.findConflitosHorario(anyLong(), any(), any(), any()))
                .thenReturn(List.of(existente));

            RuntimeException erro = assertThrows(RuntimeException.class, () -> service.agendar(dto));

            assertEquals("Horário já agendado para este funcionário", erro.getMessage());
            verify(agendamentoRepository, never()).save(any());
            verifyNoInteractions(whatsappService);
        }

        @Test
        @DisplayName("sem conflito, persiste o agendamento como AGENDADO e com lembrete pendente")
        void persisteQuandoLivre() {
            clienteFuncionarioProcedimentoExistem();
            when(agendamentoRepository.findConflitosHorario(anyLong(), any(), any(), any()))
                .thenReturn(Collections.emptyList());
            when(agendamentoRepository.save(any(Agendamento.class)))
                .thenAnswer(inv -> inv.getArgument(0));

            service.agendar(dto);

            var captor = org.mockito.ArgumentCaptor.forClass(Agendamento.class);
            verify(agendamentoRepository).save(captor.capture());
            assertEquals(Agendamento.StatusAgendamento.AGENDADO, captor.getValue().getStatus());
            assertFalse(captor.getValue().getLembreteEnviado());
        }
    }

    @Nested
    @DisplayName("Habilitação do profissional (RF10)")
    class HabilitacaoProfissional {

        @Test
        @DisplayName("profissional não habilitado ao procedimento é recusado")
        void recusaProfissionalNaoHabilitado() {
            funcionario.setProcedimentosHabilitados(new java.util.ArrayList<>());
            clienteFuncionarioProcedimentoExistem();

            RuntimeException erro = assertThrows(RuntimeException.class, () -> service.agendar(dto));

            assertEquals("Funcionário não está habilitado para este procedimento", erro.getMessage());
            verify(agendamentoRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Notificação na alteração (RF22)")
    class NotificacaoNaAlteracao {

        @Test
        @DisplayName("ao alterar um agendamento, a cliente é avisada da mudança")
        void notificaClienteAoAlterar() {
            Agendamento existente = Agendamento.builder().id(7L).build();
            when(agendamentoRepository.findById(7L)).thenReturn(Optional.of(existente));
            clienteFuncionarioProcedimentoExistem();
            when(agendamentoRepository.findConflitosHorario(anyLong(), any(), any(), any()))
                .thenReturn(Collections.emptyList());
            when(agendamentoRepository.save(any(Agendamento.class)))
                .thenAnswer(inv -> inv.getArgument(0));

            service.alterar(7L, dto);

            verify(whatsappService).enviarNotificacaoAlteracao(existente);
        }

        @Test
        @DisplayName("falha no envio da notificação não desfaz a alteração")
        void falhaNoEnvioNaoImpedeAlteracao() {
            Agendamento existente = Agendamento.builder().id(7L).build();
            when(agendamentoRepository.findById(7L)).thenReturn(Optional.of(existente));
            clienteFuncionarioProcedimentoExistem();
            when(agendamentoRepository.findConflitosHorario(anyLong(), any(), any(), any()))
                .thenReturn(Collections.emptyList());
            when(agendamentoRepository.save(any(Agendamento.class)))
                .thenAnswer(inv -> inv.getArgument(0));
            doThrow(new RuntimeException("Evolution API indisponível"))
                .when(whatsappService).enviarNotificacaoAlteracao(any());

            AgendamentoDTO resultado = assertDoesNotThrow(() -> service.alterar(7L, dto));

            assertNotNull(resultado, "a alteração deve ser concluída mesmo com falha na notificação");
        }
    }

    @Nested
    @DisplayName("Registros inexistentes")
    class RecursosInexistentes {

        @Test
        @DisplayName("cliente inexistente gera ResourceNotFoundException")
        void clienteInexistente() {
            when(clienteRepository.findById(1L)).thenReturn(Optional.empty());

            var erro = assertThrows(com.clinicabelezatotal.exception.ResourceNotFoundException.class,
                () -> service.agendar(dto));

            assertEquals("Cliente não encontrado", erro.getMessage());
        }

        @Test
        @DisplayName("funcionário inexistente gera ResourceNotFoundException")
        void funcionarioInexistente() {
            when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
            when(funcionarioRepository.findById(1L)).thenReturn(Optional.empty());

            var erro = assertThrows(com.clinicabelezatotal.exception.ResourceNotFoundException.class,
                () -> service.agendar(dto));

            assertEquals("Funcionário não encontrado", erro.getMessage());
        }

        @Test
        @DisplayName("procedimento inexistente gera ResourceNotFoundException")
        void procedimentoInexistente() {
            when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
            when(funcionarioRepository.findById(1L)).thenReturn(Optional.of(funcionario));
            when(procedimentoRepository.findById(1L)).thenReturn(Optional.empty());

            var erro = assertThrows(com.clinicabelezatotal.exception.ResourceNotFoundException.class,
                () -> service.agendar(dto));

            assertEquals("Procedimento não encontrado", erro.getMessage());
        }
    }
}
