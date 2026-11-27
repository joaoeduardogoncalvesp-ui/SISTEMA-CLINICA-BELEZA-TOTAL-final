package com.clinicabelezatotal.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class HistoricoAgendamentoDTO {

    private Long id;
    private String nomeCliente;
    private String telefoneCliente;
    private String nomeFuncionario;
    private String nomeProcedimento;
    private String categoria;
    private LocalDate data;
    private LocalTime horaInicio;
    private LocalTime horaFim;
    private String status;
    private LocalDateTime criadoEm;
}
