package com.clinicabelezatotal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class FuncionarioDTO {
    
    private Long id;
    
    @NotBlank(message = "Nome é obrigatório")
    private String nome;
    
    @NotBlank(message = "Telefone é obrigatório")
    private String telefone;
    
    @NotBlank(message = "CPF é obrigatório")
    private String cpf;
    
    private String especialidade;
    
    @NotNull(message = "Lista de procedimentos é obrigatória")
    private List<Long> procedimentoIds;
}
