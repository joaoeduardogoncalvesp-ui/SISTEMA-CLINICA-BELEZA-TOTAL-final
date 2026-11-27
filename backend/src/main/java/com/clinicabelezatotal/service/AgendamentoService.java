package com.clinicabelezatotal.service;

import com.clinicabelezatotal.dto.AgendamentoDTO;
import com.clinicabelezatotal.dto.HistoricoAgendamentoDTO;
import com.clinicabelezatotal.exception.ResourceNotFoundException;
import com.clinicabelezatotal.model.*;
import com.clinicabelezatotal.model.Agendamento.StatusAgendamento;
import com.clinicabelezatotal.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgendamentoService {

    private final AgendamentoRepository agendamentoRepository;
    private final ClienteRepository clienteRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final ProcedimentoRepository procedimentoRepository;
    private final UsuarioRepository usuarioRepository;
    private final WhatsAppService whatsappService;

    @Transactional
    public AgendamentoDTO agendar(AgendamentoDTO dto) {
        Cliente cliente = clienteRepository.findById(dto.getClienteId())
            .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado"));
        
        Funcionario funcionario = funcionarioRepository.findById(dto.getFuncionarioId())
            .orElseThrow(() -> new ResourceNotFoundException("Funcionário não encontrado"));
        
        Procedimento procedimento = procedimentoRepository.findById(dto.getProcedimentoId())
            .orElseThrow(() -> new ResourceNotFoundException("Procedimento não encontrado"));
        
        // Validar se funcionário está habilitado para o procedimento
        if (!funcionario.getProcedimentosHabilitados().contains(procedimento)) {
            throw new RuntimeException("Funcionário não está habilitado para este procedimento");
        }
        
        // Calcular hora fim baseado na duração do procedimento
        LocalTime horaFim = dto.getHoraInicio().plusMinutes(procedimento.getDuracaoMinutos());
        
        // Verificar conflito de horário
        List<Agendamento> conflitos = agendamentoRepository.findConflitosHorario(
            dto.getFuncionarioId(), dto.getData(), dto.getHoraInicio(), horaFim
        );
        
        if (!conflitos.isEmpty()) {
            throw new RuntimeException("Horário já agendado para este funcionário");
        }
        
        Agendamento agendamento = Agendamento.builder()
            .cliente(cliente)
            .funcionario(funcionario)
            .procedimento(procedimento)
            .data(dto.getData())
            .horaInicio(dto.getHoraInicio())
            .horaFim(horaFim)
            .status(StatusAgendamento.AGENDADO)
            .lembreteEnviado(false)
            .build();
        
        agendamento = agendamentoRepository.save(agendamento);

        try {
            whatsappService.enviarConfirmacao(agendamento);
        } catch (Exception e) {
            log.error("Erro ao enviar confirmação de agendamento: {}", e.getMessage());
        }

        return toDTO(agendamento);
    }

    public List<AgendamentoDTO> listarPorFuncionarioEData(Long funcionarioId, LocalDate data) {
        return agendamentoRepository.findByFuncionarioIdAndDataAndStatusNot(
            funcionarioId, data, StatusAgendamento.CANCELADO
        ).stream()
            .map(this::toDTO)
            .toList();
    }

    public List<AgendamentoDTO> listarPorPeriodo(LocalDate inicio, LocalDate fim) {
        return agendamentoRepository.findByPeriodo(inicio, fim).stream()
            .map(this::toDTO)
            .toList();
    }

    public List<AgendamentoDTO> listarPorCliente(Long clienteId) {
        return agendamentoRepository.findByClienteId(clienteId).stream()
            .map(this::toDTO)
            .toList();
    }

    public List<AgendamentoDTO> listarTodosPorFuncionario(Long funcionarioId) {
        return agendamentoRepository.findByFuncionarioId(funcionarioId).stream()
            .map(this::toDTO)
            .toList();
    }

    public List<AgendamentoDTO> listarPorEmailFuncionario(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        if (usuario.getFuncionario() == null) {
            if (usuario.getPerfil() == Usuario.PerfilUsuario.ADMIN) {
                return agendamentoRepository.findAllNaoCancelados().stream()
                    .map(this::toDTO)
                    .toList();
            }
            return List.of();
        }

        return agendamentoRepository.findByFuncionarioId(usuario.getFuncionario().getId()).stream()
            .map(this::toDTO)
            .toList();
    }

    public AgendamentoDTO alterarStatus(Long id, StatusAgendamento status) {
        Agendamento agendamento = agendamentoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado"));
        
        agendamento.setStatus(status);
        agendamento = agendamentoRepository.save(agendamento);

        try {
            whatsappService.enviarNotificacaoAlteracaoStatus(agendamento);
        } catch (Exception e) {
            log.error("Erro ao enviar notificação de mudança de status: {}", e.getMessage());
        }

        return toDTO(agendamento);
    }


    @Transactional
    public AgendamentoDTO alterar(Long id, AgendamentoDTO dto) {
        Agendamento agendamento = agendamentoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado"));

        Cliente cliente = clienteRepository.findById(dto.getClienteId())
            .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado"));
        
        Funcionario funcionario = funcionarioRepository.findById(dto.getFuncionarioId())
            .orElseThrow(() -> new ResourceNotFoundException("Funcionário não encontrado"));
        
        Procedimento procedimento = procedimentoRepository.findById(dto.getProcedimentoId())
            .orElseThrow(() -> new ResourceNotFoundException("Procedimento não encontrado"));
        
        // Validar se funcionário está habilitado para o procedimento
        if (!funcionario.getProcedimentosHabilitados().contains(procedimento)) {
            throw new RuntimeException("Funcionário não está habilitado para este procedimento");
        }
        
        // Calcular hora fim baseado na duração do procedimento
        LocalTime horaFim = dto.getHoraInicio().plusMinutes(procedimento.getDuracaoMinutos());
        
        // Verificar conflito de horário (excluindo o próprio agendamento)
        List<Agendamento> conflitos = agendamentoRepository.findConflitosHorario(
            dto.getFuncionarioId(), dto.getData(), dto.getHoraInicio(), horaFim
        );
        
        if (!conflitos.isEmpty() && !conflitos.stream().anyMatch(a -> a.getId().equals(id))) {
            throw new RuntimeException("Horário já agendado para este funcionário");
        }
        
        agendamento.setCliente(cliente);
        agendamento.setFuncionario(funcionario);
        agendamento.setProcedimento(procedimento);
        agendamento.setData(dto.getData());
        agendamento.setHoraInicio(dto.getHoraInicio());
        agendamento.setHoraFim(horaFim);
        
        agendamento = agendamentoRepository.save(agendamento);

        try {
            whatsappService.enviarNotificacaoAlteracao(agendamento);
        } catch (Exception e) {
            log.error("Erro ao enviar notificação de alteração: {}", e.getMessage());
        }

        return toDTO(agendamento);
    }

    @Transactional
    public AgendamentoDTO cancelar(Long id) {
        Agendamento agendamento = agendamentoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado"));
        
        agendamento.setStatus(StatusAgendamento.CANCELADO);
        agendamento = agendamentoRepository.save(agendamento);

        try {
            whatsappService.enviarNotificacaoCancelamento(agendamento);
        } catch (Exception e) {
            log.error("Erro ao enviar notificação de cancelamento: {}", e.getMessage());
        }

        return toDTO(agendamento);
    }

    public void excluir(Long id) {
        Agendamento agendamento = agendamentoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado"));
        agendamentoRepository.delete(agendamento);
    }

    public List<AgendamentoDTO> buscarAgendamentosParaLembrete() {
        return agendamentoRepository.findParaEnviarLembrete(LocalDate.now()).stream()
            .map(this::toDTO)
            .toList();
    }

    public List<HistoricoAgendamentoDTO> buscarHistoricoPorCliente(Long clienteId) {
        return agendamentoRepository.findHistoricoByClienteId(clienteId).stream()
            .map(this::toHistoricoDTO)
            .toList();
    }

    private HistoricoAgendamentoDTO toHistoricoDTO(Agendamento agendamento) {
        HistoricoAgendamentoDTO dto = new HistoricoAgendamentoDTO();
        dto.setId(agendamento.getId());
        dto.setNomeCliente(agendamento.getCliente().getNome());
        dto.setTelefoneCliente(agendamento.getCliente().getTelefone());
        dto.setNomeFuncionario(agendamento.getFuncionario().getNome());
        dto.setNomeProcedimento(agendamento.getProcedimento().getNome());
        dto.setCategoria(agendamento.getProcedimento().getCategoria().name());
        dto.setData(agendamento.getData());
        dto.setHoraInicio(agendamento.getHoraInicio());
        dto.setHoraFim(agendamento.getHoraFim());
        dto.setStatus(agendamento.getStatus().name());
        dto.setCriadoEm(agendamento.getCriadoEm());
        return dto;
    }

    private AgendamentoDTO toDTO(Agendamento agendamento) {
        AgendamentoDTO dto = new AgendamentoDTO();
        dto.setId(agendamento.getId());
        dto.setClienteId(agendamento.getCliente().getId());
        dto.setFuncionarioId(agendamento.getFuncionario().getId());
        dto.setProcedimentoId(agendamento.getProcedimento().getId());
        dto.setData(agendamento.getData());
        dto.setHoraInicio(agendamento.getHoraInicio());
        dto.setHoraFim(agendamento.getHoraFim());
        dto.setStatus(agendamento.getStatus());
        dto.setClienteNome(agendamento.getCliente().getNome());
        dto.setFuncionarioNome(agendamento.getFuncionario().getNome());
        dto.setProcedimentoNome(agendamento.getProcedimento().getNome());
        return dto;
    }

    /** Encaminha o diagnóstico de WhatsApp para o controller. */
    public com.clinicabelezatotal.dto.FalhaNotificacaoDTO ultimaFalhaWhatsapp() {
        return whatsappService.ultimaFalha();
    }

    public boolean whatsappHabilitado() {
        return whatsappService.envioRealHabilitado();
    }
}
