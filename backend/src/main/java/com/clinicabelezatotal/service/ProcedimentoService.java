package com.clinicabelezatotal.service;

import com.clinicabelezatotal.dto.ProcedimentoDTO;
import com.clinicabelezatotal.exception.ResourceNotFoundException;
import com.clinicabelezatotal.model.Procedimento;
import com.clinicabelezatotal.repository.ProcedimentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProcedimentoService {

    private final ProcedimentoRepository procedimentoRepository;

    public ProcedimentoDTO cadastrar(ProcedimentoDTO dto) {
        Procedimento procedimento = Procedimento.builder()
            .nome(dto.getNome())
            .descricao(dto.getDescricao())
            .duracaoMinutos(dto.getDuracaoMinutos())
            .valor(dto.getValor())
            .categoria(dto.getCategoria())
            .imagemUrl(dto.getImagemUrl())
            .ativo(true)
            .build();
        
        procedimento = procedimentoRepository.save(procedimento);
        dto.setId(procedimento.getId());
        return dto;
    }

    public List<ProcedimentoDTO> listarTodos() {
        return procedimentoRepository.findByAtivoTrue().stream()
            .map(this::toDTO)
            .toList();
    }

    public List<ProcedimentoDTO> listarPorCategoria(Procedimento.CategoriaProcedimento categoria) {
        return procedimentoRepository.findByCategoria(categoria).stream()
            .map(this::toDTO)
            .toList();
    }

    public ProcedimentoDTO buscarPorId(Long id) {
        Procedimento procedimento = procedimentoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Procedimento não encontrado"));
        return toDTO(procedimento);
    }

    public ProcedimentoDTO atualizar(Long id, ProcedimentoDTO dto) {
        Procedimento procedimento = procedimentoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Procedimento não encontrado"));
        
        procedimento.setNome(dto.getNome());
        procedimento.setDescricao(dto.getDescricao());
        procedimento.setDuracaoMinutos(dto.getDuracaoMinutos());
        procedimento.setValor(dto.getValor());
        procedimento.setCategoria(dto.getCategoria());
        procedimento.setImagemUrl(dto.getImagemUrl());
        
        procedimento = procedimentoRepository.save(procedimento);
        return toDTO(procedimento);
    }

    public void inativar(Long id) {
        Procedimento procedimento = procedimentoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Procedimento não encontrado"));
        procedimento.setAtivo(false);
        procedimentoRepository.save(procedimento);
    }

    private ProcedimentoDTO toDTO(Procedimento procedimento) {
        ProcedimentoDTO dto = new ProcedimentoDTO();
        dto.setId(procedimento.getId());
        dto.setNome(procedimento.getNome());
        dto.setDescricao(procedimento.getDescricao());
        dto.setDuracaoMinutos(procedimento.getDuracaoMinutos());
        dto.setValor(procedimento.getValor());
        dto.setCategoria(procedimento.getCategoria());
        dto.setImagemUrl(procedimento.getImagemUrl());
        return dto;
    }
}
