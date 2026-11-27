package com.clinicabelezatotal.dto;

import lombok.Data;

import java.util.List;

@Data
public class IndicadoresDTO {

    private long totalAgendamentosHoje;
    private long totalAgendamentosPeriodo;
    private long realizados;
    private long cancelados;
    private long naoCompareceram;
    private long pendentes;
    private long totalClientes;
    private List<IndicadorPorProfissionalDTO> porProfissional;
    private List<IndicadorPorProcedimentoDTO> porProcedimento;
}
