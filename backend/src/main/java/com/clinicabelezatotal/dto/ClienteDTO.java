package com.clinicabelezatotal.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ClienteDTO {
    
    private Long id;
    
    @NotBlank(message = "Nome é obrigatório")
    private String nome;
    
    @NotBlank(message = "Telefone é obrigatório")
    private String telefone;
    
    private String cpf;
    
    @Email(message = "Email inválido")
    private String email;
    
    private LocalDate dataNascimento;
    
    private String observacoes;
    
    // LGPD - Consentimento obrigatório
    @NotNull(message = "Consentimento de dados é obrigatório")
    private Boolean consentimentoDados;
    
    private Boolean consentimentoWhatsApp;
}
