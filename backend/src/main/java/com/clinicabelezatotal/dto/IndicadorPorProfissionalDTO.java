package com.clinicabelezatotal.dto;

import lombok.Data;

@Data
public class IndicadorPorProfissionalDTO {

    private Long funcionarioId;
    private String nomeProfissional;
    private long total;
    private long realizados;
    private long cancelados;
    private long naoCompareceram;
}
