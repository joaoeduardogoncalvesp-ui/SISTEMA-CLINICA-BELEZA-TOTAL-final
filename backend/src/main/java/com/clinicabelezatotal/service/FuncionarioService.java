package com.clinicabelezatotal.service;

import com.clinicabelezatotal.dto.FuncionarioDTO;
import com.clinicabelezatotal.exception.ResourceNotFoundException;
import com.clinicabelezatotal.model.Funcionario;
import com.clinicabelezatotal.model.Procedimento;
import com.clinicabelezatotal.repository.FuncionarioRepository;
import com.clinicabelezatotal.repository.ProcedimentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FuncionarioService {

    private final FuncionarioRepository funcionarioRepository;
    private final ProcedimentoRepository procedimentoRepository;

    public FuncionarioDTO cadastrar(FuncionarioDTO dto) {
        if (funcionarioRepository.existsByCpf(dto.getCpf())) {
            throw new RuntimeException("CPF já cadastrado");
        }
        
        List<Procedimento> procedimentos = procedimentoRepository.findAllById(dto.getProcedimentoIds());
        
        Funcionario funcionario = Funcionario.builder()
            .nome(dto.getNome())
            .telefone(dto.getTelefone())
            .cpf(dto.getCpf())
            .especialidade(dto.getEspecialidade())
            .procedimentosHabilitados(procedimentos)
            .ativo(true)
            .build();
        
        funcionario = funcionarioRepository.save(funcionario);
        dto.setId(funcionario.getId());
        return dto;
    }

    public List<FuncionarioDTO> listarTodos() {
        return funcionarioRepository.findByAtivoTrue().stream()
            .map(this::toDTO)
            .toList();
    }

    public FuncionarioDTO buscarPorId(Long id) {
        Funcionario funcionario = funcionarioRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Funcionário não encontrado"));
        return toDTO(funcionario);
    }

    public List<FuncionarioDTO> buscarPorProcedimento(Long procedimentoId) {
        return funcionarioRepository.findByProcedimentoHabilitado(procedimentoId).stream()
            .map(this::toDTO)
            .toList();
    }

    public FuncionarioDTO atualizar(Long id, FuncionarioDTO dto) {
        Funcionario funcionario = funcionarioRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Funcionário não encontrado"));
        
        List<Procedimento> procedimentos = procedimentoRepository.findAllById(dto.getProcedimentoIds());
        
        funcionario.setNome(dto.getNome());
        funcionario.setTelefone(dto.getTelefone());
        funcionario.setCpf(dto.getCpf());
        funcionario.setEspecialidade(dto.getEspecialidade());
        funcionario.setProcedimentosHabilitados(procedimentos);
        
        funcionario = funcionarioRepository.save(funcionario);
        return toDTO(funcionario);
    }

    public void inativar(Long id) {
        Funcionario funcionario = funcionarioRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Funcionário não encontrado"));
        funcionario.setAtivo(false);
        funcionarioRepository.save(funcionario);
    }

    private FuncionarioDTO toDTO(Funcionario funcionario) {
        FuncionarioDTO dto = new FuncionarioDTO();
        dto.setId(funcionario.getId());
        dto.setNome(funcionario.getNome());
        dto.setTelefone(funcionario.getTelefone());
        dto.setCpf(funcionario.getCpf());
        dto.setEspecialidade(funcionario.getEspecialidade());
        dto.setProcedimentoIds(funcionario.getProcedimentosHabilitados().stream()
            .map(Procedimento::getId)
            .toList());
        return dto;
    }
}
