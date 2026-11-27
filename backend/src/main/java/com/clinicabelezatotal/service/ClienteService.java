package com.clinicabelezatotal.service;

import com.clinicabelezatotal.dto.ClienteDTO;
import com.clinicabelezatotal.exception.ResourceNotFoundException;
import com.clinicabelezatotal.model.Cliente;
import com.clinicabelezatotal.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteDTO cadastrar(ClienteDTO dto) {
        if (dto.getCpf() != null && clienteRepository.existsByCpf(dto.getCpf())) {
            throw new RuntimeException("CPF já cadastrado");
        }
        if (dto.getEmail() != null && clienteRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email já cadastrado");
        }
        
        // LGPD - Validação de consentimento
        if (dto.getConsentimentoDados() == null || !dto.getConsentimentoDados()) {
            throw new RuntimeException("Consentimento de dados é obrigatório (LGPD)");
        }
        
        Cliente cliente = Cliente.builder()
            .nome(dto.getNome())
            .telefone(dto.getTelefone())
            .cpf(dto.getCpf())
            .email(dto.getEmail())
            .dataNascimento(dto.getDataNascimento())
            .observacoes(dto.getObservacoes())
            .consentimentoDados(dto.getConsentimentoDados())
            .consentimentoWhatsApp(dto.getConsentimentoWhatsApp() != null ? dto.getConsentimentoWhatsApp() : false)
            .dataConsentimento(LocalDateTime.now())
            .ativo(true)
            .build();
        
        cliente = clienteRepository.save(cliente);
        dto.setId(cliente.getId());
        return dto;
    }

    public List<ClienteDTO> listarTodos() {
        return clienteRepository.findAllAtivos().stream()
            .map(this::toDTO)
            .toList();
    }

    public ClienteDTO buscarPorId(Long id) {
        Cliente cliente = clienteRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado"));
        return toDTO(cliente);
    }

    public List<ClienteDTO> buscarPorNome(String nome) {
        return clienteRepository.findByNomeContainingIgnoreCase(nome).stream()
            .map(this::toDTO)
            .toList();
    }

    public ClienteDTO atualizar(Long id, ClienteDTO dto) {
        Cliente cliente = clienteRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado"));
        
        cliente.setNome(dto.getNome());
        cliente.setTelefone(dto.getTelefone());
        cliente.setCpf(dto.getCpf());
        cliente.setEmail(dto.getEmail());
        cliente.setDataNascimento(dto.getDataNascimento());
        cliente.setObservacoes(dto.getObservacoes());
        
        // LGPD - Atualizar consentimento se fornecido
        if (dto.getConsentimentoDados() != null) {
            cliente.setConsentimentoDados(dto.getConsentimentoDados());
        }
        if (dto.getConsentimentoWhatsApp() != null) {
            cliente.setConsentimentoWhatsApp(dto.getConsentimentoWhatsApp());
            if (dto.getConsentimentoWhatsApp()) {
                cliente.setDataConsentimento(LocalDateTime.now());
            }
        }
        
        cliente = clienteRepository.save(cliente);
        return toDTO(cliente);
    }

    // LGPD - Exclusão lógica de dados
    public void inativar(Long id) {
        Cliente cliente = clienteRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado"));
        cliente.setAtivo(false);
        clienteRepository.save(cliente);
    }

    // LGPD - Exclusão definitiva de dados (anonimização)
    public void excluirDados(Long id) {
        Cliente cliente = clienteRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado"));
        cliente.anonimizarDados();
        clienteRepository.save(cliente);
    }

    // LGPD - Revogar consentimento WhatsApp
    public void revogarConsentimentoWhatsApp(Long id) {
        Cliente cliente = clienteRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado"));
        cliente.setConsentimentoWhatsApp(false);
        clienteRepository.save(cliente);
    }

    private ClienteDTO toDTO(Cliente cliente) {
        ClienteDTO dto = new ClienteDTO();
        dto.setId(cliente.getId());
        dto.setNome(cliente.getNome());
        dto.setTelefone(cliente.getTelefone());
        dto.setCpf(cliente.getCpf());
        dto.setEmail(cliente.getEmail());
        dto.setDataNascimento(cliente.getDataNascimento());
        dto.setObservacoes(cliente.getObservacoes());
        dto.setConsentimentoDados(cliente.getConsentimentoDados());
        dto.setConsentimentoWhatsApp(cliente.getConsentimentoWhatsApp());
        return dto;
    }
}
