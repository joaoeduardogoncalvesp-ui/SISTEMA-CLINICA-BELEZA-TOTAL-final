package com.clinicabelezatotal.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Data
public class HorarioAtendimentoDTO {
    
    private Long id;
    
    @NotNull(message = "Funcionário é obrigatório")
    private Long funcionarioId;
    
    @NotNull(message = "Dia da semana é obrigatório")
    private DayOfWeek diaSemana;
    
    @NotNull(message = "Hora de início é obrigatória")
    private LocalTime horaInicio;
    
    @NotNull(message = "Hora de fim é obrigatória")
    private LocalTime horaFim;
}
