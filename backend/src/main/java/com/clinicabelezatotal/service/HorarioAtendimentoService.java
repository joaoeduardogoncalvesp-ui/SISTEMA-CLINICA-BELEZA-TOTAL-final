package com.clinicabelezatotal.service;

import com.clinicabelezatotal.dto.HorarioAtendimentoDTO;
import com.clinicabelezatotal.exception.ResourceNotFoundException;
import com.clinicabelezatotal.model.Funcionario;
import com.clinicabelezatotal.model.HorarioAtendimento;
import com.clinicabelezatotal.repository.FuncionarioRepository;
import com.clinicabelezatotal.repository.HorarioAtendimentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HorarioAtendimentoService {

    private final HorarioAtendimentoRepository horarioRepository;
    private final FuncionarioRepository funcionarioRepository;

    public HorarioAtendimentoDTO cadastrar(HorarioAtendimentoDTO dto) {
        if (!dto.getHoraFim().isAfter(dto.getHoraInicio())) {
            throw new RuntimeException("Hora fim deve ser posterior à hora início");
        }

        Funcionario funcionario = funcionarioRepository.findById(dto.getFuncionarioId())
            .orElseThrow(() -> new ResourceNotFoundException("Funcionário não encontrado"));

        List<HorarioAtendimento> existentes = horarioRepository.findByFuncionarioIdAndDiaSemanaAndAtivoTrue(
            dto.getFuncionarioId(), dto.getDiaSemana());

        boolean duplicado = existentes.stream().anyMatch(h ->
            h.getHoraInicio().isBefore(dto.getHoraFim()) && h.getHoraFim().isAfter(dto.getHoraInicio()));

        if (duplicado) {
            throw new RuntimeException("Já existe horário cadastrado para este funcionário neste período");
        }

        HorarioAtendimento horario = HorarioAtendimento.builder()
            .funcionario(funcionario)
            .diaSemana(dto.getDiaSemana())
            .horaInicio(dto.getHoraInicio())
            .horaFim(dto.getHoraFim())
            .ativo(true)
            .build();

        horario = horarioRepository.save(horario);
        dto.setId(horario.getId());
        return dto;
    }

    public List<HorarioAtendimentoDTO> listarPorFuncionario(Long funcionarioId) {
        return horarioRepository.findByFuncionarioIdAndAtivoTrue(funcionarioId).stream()
            .map(this::toDTO)
            .toList();
    }

    public List<HorarioAtendimentoDTO> listarPorFuncionarioEDia(Long funcionarioId, java.time.DayOfWeek diaSemana) {
        return horarioRepository.findByFuncionarioIdAndDiaSemanaAndAtivoTrue(funcionarioId, diaSemana).stream()
            .map(this::toDTO)
            .toList();
    }

    public HorarioAtendimentoDTO buscarPorId(Long id) {
        HorarioAtendimento horario = horarioRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Horário não encontrado"));
        return toDTO(horario);
    }

    public HorarioAtendimentoDTO atualizar(Long id, HorarioAtendimentoDTO dto) {
        HorarioAtendimento horario = horarioRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Horário não encontrado"));

        horario.setDiaSemana(dto.getDiaSemana());
        horario.setHoraInicio(dto.getHoraInicio());
        horario.setHoraFim(dto.getHoraFim());

        horario = horarioRepository.save(horario);
        return toDTO(horario);
    }

    public void inativar(Long id) {
        HorarioAtendimento horario = horarioRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Horário não encontrado"));
        horario.setAtivo(false);
        horarioRepository.save(horario);
    }

    private HorarioAtendimentoDTO toDTO(HorarioAtendimento horario) {
        HorarioAtendimentoDTO dto = new HorarioAtendimentoDTO();
        dto.setId(horario.getId());
        dto.setFuncionarioId(horario.getFuncionario().getId());
        dto.setDiaSemana(horario.getDiaSemana());
        dto.setHoraInicio(horario.getHoraInicio());
        dto.setHoraFim(horario.getHoraFim());
        return dto;
    }
}
