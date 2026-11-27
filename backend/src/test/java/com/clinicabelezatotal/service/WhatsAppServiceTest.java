package com.clinicabelezatotal.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.clinicabelezatotal.model.Agendamento;
import com.clinicabelezatotal.model.Cliente;
import com.clinicabelezatotal.model.Procedimento;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class WhatsAppServiceTest {

    private ListAppender<ILoggingEvent> appender;
    private Logger logger;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(WhatsAppService.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
    }

    private long contarLogsMock() {
        return appender.list.stream()
            .filter(e -> e.getFormattedMessage().contains("[WHATSAPP MOCK]"))
            .count();
    }

    private Agendamento novoAgendamento(boolean consentimento) {
        Cliente cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNome("Maria Silva");
        cliente.setTelefone("(62) 99999-0000");
        cliente.setConsentimentoWhatsApp(consentimento);

        Procedimento procedimento = new Procedimento();
        procedimento.setId(1L);
        procedimento.setNome("Limpeza de Pele");

        Agendamento agendamento = new Agendamento();
        agendamento.setId(1L);
        agendamento.setCliente(cliente);
        agendamento.setProcedimento(procedimento);
        agendamento.setData(LocalDate.now().plusDays(1));
        agendamento.setHoraInicio(LocalTime.of(10, 0));
        agendamento.setStatus(Agendamento.StatusAgendamento.AGENDADO);
        return agendamento;
    }

    @Test
    void enviarConfirmacao_modoMock_deveLogarMensagemSemLancarExcecao() {
        WhatsAppService service = new WhatsAppService(false, 5);
        Agendamento agendamento = novoAgendamento(true);

        assertDoesNotThrow(() -> service.enviarConfirmacao(agendamento));
        assertEquals(1, contarLogsMock());
        assertTrue(appender.list.stream()
            .anyMatch(e -> e.getFormattedMessage().contains("confirmado para dia")));
    }

    @Test
    void enviarConfirmacao_semConsentimento_naoDeveEnviar() {
        WhatsAppService service = new WhatsAppService(false, 5);
        Agendamento agendamento = novoAgendamento(false);

        assertDoesNotThrow(() -> service.enviarConfirmacao(agendamento));
        assertEquals(0, contarLogsMock());
    }

    @Test
    void enviarConfirmacao_duplicataDentroDe5Minutos_naoDeveReenviar() {
        WhatsAppService service = new WhatsAppService(false, 5);
        Agendamento agendamento = novoAgendamento(true);

        service.enviarConfirmacao(agendamento);
        service.enviarConfirmacao(agendamento);

        assertEquals(1, contarLogsMock());
    }

    @Test
    void enviarConfirmacao_falhaNoEnvioReal_naoInterrompeOFluxoENaoRegistraDedup() {
        WhatsAppService service = new WhatsAppService(true, 1);
        ReflectionTestUtils.setField(service, "apiUrl", "http://localhost:1");
        ReflectionTestUtils.setField(service, "apiKey", "chave-teste");
        ReflectionTestUtils.setField(service, "instanceName", "teste");
        Agendamento agendamento = novoAgendamento(true);

        // O envio é assíncrono: a falha da API externa é registrada em log e não
        // interrompe a operação da recepção (o agendamento já está salvo).
        assertDoesNotThrow(() -> service.enviarConfirmacao(agendamento));
        assertDoesNotThrow(() -> service.enviarConfirmacao(agendamento));
    }

    @Test
    void enviarNotificacaoCancelamento_modoMock_deveLogar() {
        WhatsAppService service = new WhatsAppService(false, 5);
        Agendamento agendamento = novoAgendamento(true);
        agendamento.setStatus(Agendamento.StatusAgendamento.CANCELADO);

        assertDoesNotThrow(() -> service.enviarNotificacaoCancelamento(agendamento));
        assertEquals(1, contarLogsMock());
        assertTrue(appender.list.stream()
            .anyMatch(e -> e.getFormattedMessage().contains("cancelado")));
    }

    @Test
    void enviarLembrete_modoMock_deveLogarComDataEHora() {
        WhatsAppService service = new WhatsAppService(false, 5);
        Agendamento agendamento = novoAgendamento(true);

        assertDoesNotThrow(() -> service.enviarLembrete(agendamento));
        assertEquals(1, contarLogsMock());
        assertTrue(appender.list.stream()
            .anyMatch(e -> e.getFormattedMessage().contains("Lembrete: seu agendamento de Limpeza de Pele")));
    }
}
