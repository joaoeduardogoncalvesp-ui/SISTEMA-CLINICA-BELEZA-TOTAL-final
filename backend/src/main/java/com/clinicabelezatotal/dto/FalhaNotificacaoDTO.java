package com.clinicabelezatotal.dto;

import java.time.LocalDateTime;

/** Registro da última falha de envio de WhatsApp, para diagnóstico na tela. */
public record FalhaNotificacaoDTO(LocalDateTime quando, String destinatario, String motivo) {
}
