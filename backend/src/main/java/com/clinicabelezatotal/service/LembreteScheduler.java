package com.clinicabelezatotal.service;

import com.clinicabelezatotal.model.Agendamento;
import com.clinicabelezatotal.repository.AgendamentoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LembreteScheduler {

    private final AgendamentoRepository agendamentoRepository;
    private final WhatsAppService whatsappService;

    @Scheduled(fixedRateString = "${lembrete.intervalo:900000}")
    @Transactional
    public void enviarLembretesPendentes() {
        log.info("Verificando lembretes pendentes...");

        List<Agendamento> agendamentos = agendamentoRepository.findParaEnviarLembrete(java.time.LocalDate.now());

        if (agendamentos.isEmpty()) {
            log.info("Nenhum lembrete pendente para hoje");
            return;
        }

        log.info("Encontrados {} agendamentos para enviar lembrete", agendamentos.size());

        for (Agendamento agendamento : agendamentos) {
            try {
                if (!whatsappService.enviarLembrete(agendamento)) {
                    log.warn("Lembrete do agendamento ID {} não foi enviado (sem consentimento ou WhatsApp desabilitado). Será retentado.",
                        agendamento.getId());
                    continue;
                }
                agendamento.setLembreteEnviado(true);
                agendamentoRepository.save(agendamento);
                log.info("Lembrete enviado para agendamento ID: {}", agendamento.getId());
            } catch (Exception e) {
                log.error("Erro ao enviar lembrete para agendamento ID {}: {}. Lembrete NÃO marcado como enviado, será retentado.",
                    agendamento.getId(), e.getMessage());
            }
        }
    }
}
