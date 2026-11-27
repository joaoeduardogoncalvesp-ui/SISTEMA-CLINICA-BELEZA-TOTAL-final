package com.clinicabelezatotal.dto;

import com.clinicabelezatotal.model.Usuario.PerfilUsuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UsuarioDTO {
    
    private Long id;
    
    @NotBlank(message = "Nome é obrigatório")
    private String nome;
    
    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email inválido")
    private String email;
    
    @Size(min = 8, message = "Senha deve ter no mínimo 8 caracteres")
    private String senha;
    
    @NotNull(message = "Perfil é obrigatório")
    private PerfilUsuario perfil;
    
    private Long funcionarioId;
}
