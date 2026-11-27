package com.clinicabelezatotal.dto;

import lombok.Data;

@Data
public class IndicadorPorProcedimentoDTO {

    private Long procedimentoId;
    private String nomeProcedimento;
    private String categoria;
    private long total;
}
