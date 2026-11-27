package com.clinicabelezatotal.service;

import com.clinicabelezatotal.model.Agendamento;
import com.clinicabelezatotal.model.Cliente;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import io.netty.channel.ChannelOption;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class WhatsAppService {

    private final WebClient webClient;
    private final boolean habilitado;

    @Value("${whatsapp.api.url}")
    private String apiUrl;

    @Value("${whatsapp.api.key}")
    private String apiKey;

    @Value("${whatsapp.phone-number-id}")
    private String instanceName;

    private final ConcurrentHashMap<String, LocalDateTime> mensagensEnviadas = new ConcurrentHashMap<>();

    /** Última falha de envio, exibida no diagnóstico do dashboard. */
    private volatile com.clinicabelezatotal.dto.FalhaNotificacaoDTO ultimaFalha;

    private final int timeoutSegundos;

    public WhatsAppService(
            @Value("${whatsapp.habilitado:false}") boolean habilitado,
            @Value("${whatsapp.timeout-segundos:5}") int timeoutSegundos) {
        this.habilitado = habilitado;
        this.timeoutSegundos = timeoutSegundos;
        HttpClient httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeoutSegundos * 1000)
            .responseTimeout(Duration.ofSeconds(timeoutSegundos));
        this.webClient = WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .build();

        if (habilitado) {
            log.info("WhatsApp HABILITADO - envio real via Evolution em {}", apiUrl);
        } else {
            log.warn("WhatsApp DESABILITADO (WHATSAPP_HABILITADO=false) - modo MOCK: nenhuma mensagem será enviada. "
                + "Defina WHATSAPP_HABILITADO=true para envios reais.");
        }
    }

    public boolean enviarLembrete(Agendamento agendamento) {
        Cliente cliente = agendamento.getCliente();
        if (!podeEnviar(cliente)) {
            log.info("Lembrete não enviado - cliente {} sem consentimento WhatsApp", cliente.getId());
            return false;
        }

        DateTimeFormatter fmtData = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter fmtHora = DateTimeFormatter.ofPattern("HH:mm");

        String mensagem = String.format(
            "Olá %s! Lembrete: seu agendamento de %s é dia %s às %s. Clínica Beleza Total",
            cliente.getNome(),
            agendamento.getProcedimento().getNome(),
            agendamento.getData().format(fmtData),
            agendamento.getHoraInicio().format(fmtHora)
        );

        return enviarMensagem(cliente.getTelefone(), mensagem);
    }

    public void enviarNotificacaoAlteracao(Agendamento agendamento) {
        Cliente cliente = agendamento.getCliente();
        if (!podeEnviar(cliente)) {
            log.info("Notificação de alteração não enviada - cliente {} sem consentimento", cliente.getId());
            return;
        }

        DateTimeFormatter fmtData = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter fmtHora = DateTimeFormatter.ofPattern("HH:mm");

        String mensagem = String.format(
            "Olá %s! Seu agendamento de %s foi alterado para dia %s às %s. Clínica Beleza Total",
            cliente.getNome(),
            agendamento.getProcedimento().getNome(),
            agendamento.getData().format(fmtData),
            agendamento.getHoraInicio().format(fmtHora)
        );

        enviarMensagem(cliente.getTelefone(), mensagem);
    }

    public void enviarConfirmacao(Agendamento agendamento) {
        Cliente cliente = agendamento.getCliente();
        if (!podeEnviar(cliente)) {
            log.info("Confirmação não enviada - cliente {} sem consentimento WhatsApp", cliente.getId());
            return;
        }

        String chave = agendamento.getId() + ":confirmacao";
        if (jaEnviado(chave)) {
            log.info("Confirmação já enviada para agendamento {}, ignorando duplicata", agendamento.getId());
            return;
        }

        DateTimeFormatter fmtData = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter fmtHora = DateTimeFormatter.ofPattern("HH:mm");

        String mensagem = String.format(
            "Olá %s! Seu agendamento de %s foi confirmado para dia %s às %s. Clínica Beleza Total",
            cliente.getNome(),
            agendamento.getProcedimento().getNome(),
            agendamento.getData().format(fmtData),
            agendamento.getHoraInicio().format(fmtHora)
        );

        enviarMensagem(cliente.getTelefone(), mensagem);
        registrarEnvio(chave);
    }

    public void enviarNotificacaoAlteracaoStatus(Agendamento agendamento) {
        Cliente cliente = agendamento.getCliente();
        if (!podeEnviar(cliente)) {
            log.info("Notificação de mudança de status não enviada - cliente {} sem consentimento", cliente.getId());
            return;
        }

        if (agendamento.getStatus() == Agendamento.StatusAgendamento.REALIZADO) {
            enviarAgradecimento(agendamento);
            return;
        }

        log.info("Notificação de status não enviada para {} - apenas REALIZADO gera notificação", agendamento.getStatus());
    }

    public void enviarAgradecimento(Agendamento agendamento) {
        Cliente cliente = agendamento.getCliente();
        if (!podeEnviar(cliente)) {
            return;
        }

        String chave = agendamento.getId() + ":agradecimento";
        if (jaEnviado(chave)) {
            log.info("Agradecimento já enviado para agendamento {}, ignorando duplicata", agendamento.getId());
            return;
        }

        DateTimeFormatter fmtData = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter fmtHora = DateTimeFormatter.ofPattern("HH:mm");

        String mensagem = String.format(
            "Olá, %s! Agradecemos por confiar na Clínica Beleza Total, esperamos nos ver em breve!",
            cliente.getNome()
        );

        enviarMensagem(cliente.getTelefone(), mensagem);
        registrarEnvio(chave);
    }

    public void enviarNotificacaoCancelamento(Agendamento agendamento) {
        Cliente cliente = agendamento.getCliente();
        if (!podeEnviar(cliente)) {
            log.info("Notificação de cancelamento não enviada - cliente {} sem consentimento", cliente.getId());
            return;
        }

        String chave = agendamento.getId() + ":cancelamento";
        if (jaEnviado(chave)) {
            log.info("Cancelamento já notificado para agendamento {}, ignorando duplicata", agendamento.getId());
            return;
        }

        DateTimeFormatter fmtData = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter fmtHora = DateTimeFormatter.ofPattern("HH:mm");

        String mensagem = String.format(
            "Olá %s! Seu agendamento de %s dia %s às %s foi cancelado. Clínica Beleza Total",
            cliente.getNome(),
            agendamento.getProcedimento().getNome(),
            agendamento.getData().format(fmtData),
            agendamento.getHoraInicio().format(fmtHora)
        );

        enviarMensagem(cliente.getTelefone(), mensagem);
        registrarEnvio(chave);
    }

    private boolean podeEnviar(Cliente cliente) {
        return cliente.getConsentimentoWhatsApp() != null && cliente.getConsentimentoWhatsApp();
    }

    private boolean jaEnviado(String chave) {
        LocalDateTime enviado = mensagensEnviadas.get(chave);
        if (enviado != null && enviado.isAfter(LocalDateTime.now().minusMinutes(5))) {
            return true;
        }
        return false;
    }

    private void registrarEnvio(String chave) {
        mensagensEnviadas.put(chave, LocalDateTime.now());
        mensagensEnviadas.entrySet().removeIf(e ->
            e.getValue().isBefore(LocalDateTime.now().minusMinutes(10)));
    }

    /**
     * Envia a mensagem pela Evolution API.
     *
     * @return true se o envio foi aceito pela Evolution (ou, no modo mock, contabilizado);
     *         false se está desabilitado ou se a chamada falhou de imediato.
     */
    private boolean enviarMensagem(String telefone, String mensagem) {
        if (!habilitado) {
            log.info("[WHATSAPP MOCK] Para: {} | Mensagem: {}", telefone, mensagem);
            return false;
        }

        try {
            String telefoneFormatado = telefone.replaceAll("[^0-9]", "");

            if (!telefoneFormatado.startsWith("55")) {
                telefoneFormatado = "55" + telefoneFormatado;
            }

            Map<String, Object> body = Map.of(
                "number", telefoneFormatado,
                "text", mensagem
            );

            webClient.post()
                .uri(apiUrl + "/message/sendText/{instance}", instanceName)
                .header("apikey", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(timeoutSegundos))
                .subscribe(
                    resposta -> log.info("WhatsApp enviado para {}: {}", telefone, resumo(resposta)),
                    erro -> registrarFalha(telefone, erro.getMessage())
                );
            return true;
        } catch (Exception e) {
            log.error("Erro ao preparar envio de WhatsApp para {}: {}", telefone, e.getMessage());
            throw new RuntimeException("Falha ao enviar WhatsApp para " + telefone, e);
        }
    }

    /** Resume a resposta da Evolution para o log (evita despejar JSON inteiro). */
    private String resumo(String resposta) {
        if (resposta == null) return "(sem corpo)";
        return resposta.length() > 120 ? resposta.substring(0, 120) + "..." : resposta;
    }

    private void registrarFalha(String destinatario, String motivo) {
        String texto = traduzirErro(motivo);
        this.ultimaFalha = new com.clinicabelezatotal.dto.FalhaNotificacaoDTO(
                java.time.LocalDateTime.now(), destinatario, texto);
        log.error("Falha ao enviar WhatsApp para {}: {}", destinatario, texto);
    }

    /** Traduz respostas comuns da Evolution para uma mensagem compreensível. */
    private String traduzirErro(String motivo) {
        if (motivo == null) return "erro desconhecido";
        if (motivo.contains("400") && motivo.contains("sendText")) {
            return "número sem WhatsApp ou inválido (a Evolution recusou o envio)";
        }
        if (motivo.contains("401") || motivo.contains("403")) {
            return "chave da API ou sessão do WhatsApp inválida";
        }
        if (motivo.contains("404")) {
            return "instância não encontrada na Evolution API";
        }
        if (motivo.toLowerCase().contains("timeout")) {
            return "Evolution API não respondeu a tempo";
        }
        if (motivo.toLowerCase().contains("connection refused")) {
            return "Evolution API fora do ar (containers parados?)";
        }
        return motivo.length() > 160 ? motivo.substring(0, 160) + "..." : motivo;
    }

    /** Última falha registrada, ou null se nenhuma desde a inicialização. */
    public com.clinicabelezatotal.dto.FalhaNotificacaoDTO ultimaFalha() {
        return ultimaFalha;
    }

    /** Indica se o envio real está habilitado (false = modo mock). */
    public boolean envioRealHabilitado() {
        return habilitado;
    }
}
