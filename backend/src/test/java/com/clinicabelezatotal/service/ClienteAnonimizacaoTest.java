package com.clinicabelezatotal.service;

import com.clinicabelezatotal.model.Cliente;
import com.clinicabelezatotal.repository.ClienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Direito ao esquecimento (LGPD) — anonimização de dados da cliente.
 * Bug corrigido: telefone recebia null, mas a coluna é NOT NULL, e a operação
 * falhava com erro de banco (SQLState 23502).
 */
@ExtendWith(MockitoExtension.class)
class ClienteAnonimizacaoTest {

    @Mock
    private ClienteRepository clienteRepository;

    @InjectMocks
    private ClienteService clienteService;

    private Cliente cliente;

    @BeforeEach
    void setUp() {
        cliente = new Cliente();
        cliente.setId(42L);
        cliente.setNome("Maria Silva");
        cliente.setTelefone("(62) 99999-9999");
        cliente.setCpf("12345678901");
        cliente.setEmail("maria@email.com");
        cliente.setDataNascimento(LocalDate.of(1990, 5, 20));
        cliente.setObservacoes("Prefere manhã");
        cliente.setAtivo(true);
        cliente.setConsentimentoDados(true);
        cliente.setConsentimentoWhatsApp(true);
    }

    @Test
    @DisplayName("anonimiza todos os dados identificadores da cliente")
    void anonimizaDadosIdentificadores() {
        when(clienteRepository.findById(42L)).thenReturn(Optional.of(cliente));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        clienteService.excluirDados(42L);

        assertTrue(cliente.getNome().startsWith("CLIENTE_ANONIMO_"),
            "nome deve ser substituído por marcador");
        assertFalse(cliente.getNome().contains("Maria"), "nome original não pode permanecer");
        assertNull(cliente.getCpf(), "CPF deve ser eliminado");
        assertNull(cliente.getEmail(), "e-mail deve ser eliminado");
        assertNull(cliente.getObservacoes(), "observações devem ser eliminadas");
        assertNull(cliente.getDataNascimento(), "data de nascimento deve ser eliminada");
        assertFalse(cliente.getConsentimentoDados());
        assertFalse(cliente.getConsentimentoWhatsApp());
        assertFalse(cliente.getAtivo(), "cliente deve ficar inativa");
    }

    @Test
    @DisplayName("telefone nunca fica nulo — a coluna é NOT NULL no banco")
    void telefoneNaoFicaNulo() {
        when(clienteRepository.findById(42L)).thenReturn(Optional.of(cliente));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        clienteService.excluirDados(42L);

        assertNotNull(cliente.getTelefone(),
            "telefone não pode ser null: a coluna é NOT NULL e a exclusão falharia");
        assertNotEquals("(62) 99999-9999", cliente.getTelefone(),
            "o número original não pode permanecer");
    }

    @Test
    @DisplayName("a anonimização é persistida")
    void persisteAnonimizacao() {
        when(clienteRepository.findById(42L)).thenReturn(Optional.of(cliente));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        clienteService.excluirDados(42L);

        ArgumentCaptor<Cliente> captor = ArgumentCaptor.forClass(Cliente.class);
        verify(clienteRepository).save(captor.capture());
        assertFalse(captor.getValue().getAtivo());
    }

    @Test
    @DisplayName("cliente inexistente não pode ser anonimizada")
    void clienteInexistente() {
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(com.clinicabelezatotal.exception.ResourceNotFoundException.class,
            () -> clienteService.excluirDados(99L));
        verify(clienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("a anonimização não altera o identificador da cliente")
    void mantemIdentificador() {
        when(clienteRepository.findById(42L)).thenReturn(Optional.of(cliente));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        clienteService.excluirDados(42L);

        assertEquals(42L, cliente.getId(), "o histórico de atendimentos referencia este id");
    }
}
