package com.clinicabelezatotal.service;

import com.clinicabelezatotal.dto.ClienteDTO;
import com.clinicabelezatotal.exception.ResourceNotFoundException;
import com.clinicabelezatotal.model.Cliente;
import com.clinicabelezatotal.repository.ClienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @InjectMocks
    private ClienteService clienteService;

    private Cliente cliente;
    private ClienteDTO clienteDTO;

    @BeforeEach
    void setUp() {
        cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNome("Maria Silva");
        cliente.setTelefone("(62) 99999-9999");
        cliente.setEmail("maria@email.com");
        cliente.setCpf("12345678901");
        cliente.setAtivo(true);
        cliente.setConsentimentoDados(true);

        clienteDTO = new ClienteDTO();
        clienteDTO.setNome("Maria Silva");
        clienteDTO.setTelefone("(62) 99999-9999");
        clienteDTO.setEmail("maria@email.com");
        clienteDTO.setCpf("12345678901");
        clienteDTO.setConsentimentoDados(true);
    }

    @Test
    void listarTodos_DeveRetornarListaDeClientes() {
        when(clienteRepository.findAllAtivos()).thenReturn(Arrays.asList(cliente));

        List<ClienteDTO> resultado = clienteService.listarTodos();

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals("Maria Silva", resultado.get(0).getNome());
    }

    @Test
    void buscarPorId_DeveRetornarCliente() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));

        ClienteDTO resultado = clienteService.buscarPorId(1L);

        assertNotNull(resultado);
        assertEquals("Maria Silva", resultado.getNome());
    }

    @Test
    void buscarPorId_DeveLancarExcecao_QuandoNaoEncontrado() {
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> clienteService.buscarPorId(99L));
    }

    @Test
    void cadastrar_DeveSalvarCliente() {
        when(clienteRepository.save(any(Cliente.class))).thenReturn(cliente);

        ClienteDTO resultado = clienteService.cadastrar(clienteDTO);

        assertNotNull(resultado);
        verify(clienteRepository, times(1)).save(any(Cliente.class));
    }

    @Test
    void cadastrar_DeveExigirConsentimentoLGPD() {
        clienteDTO.setConsentimentoDados(false);

        assertThrows(RuntimeException.class, () -> clienteService.cadastrar(clienteDTO));
    }

    @Test
    void atualizar_DeveAtualizarCliente() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(clienteRepository.save(any(Cliente.class))).thenReturn(cliente);

        ClienteDTO resultado = clienteService.atualizar(1L, clienteDTO);

        assertNotNull(resultado);
        verify(clienteRepository, times(1)).save(any(Cliente.class));
    }

    @Test
    void inativar_DeveDesativarCliente() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));

        clienteService.inativar(1L);

        assertFalse(cliente.getAtivo());
        verify(clienteRepository, times(1)).save(cliente);
    }
}
