package com.clinicabelezatotal.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "clientes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String telefone;

    @Column(unique = true)
    private String cpf;

    @Column(unique = true)
    private String email;

    private LocalDate dataNascimento;

    private String observacoes;

    @Column(nullable = false)
    private Boolean ativo = true;

    // LGPD - Consentimento para uso de dados
    @Column(nullable = false)
    private Boolean consentimentoWhatsApp = false;

    @Column(nullable = false)
    private Boolean consentimentoDados = false;

    private LocalDateTime dataConsentimento;

    @OneToMany(mappedBy = "cliente")
    private List<Agendamento> agendamentos;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    private LocalDateTime atualizadoEm;

    // LGPD - Método para anonimizar dados (exclusão lógica)
    public void anonimizarDados() {
        this.nome = "CLIENTE_ANONIMO_" + this.id;
        // telefone é NOT NULL no banco: substitui por marcador em vez de null
        this.telefone = "ANONIMIZADO";
        this.cpf = null;
        this.email = null;
        this.observacoes = null;
        this.dataNascimento = null;
        this.consentimentoWhatsApp = false;
        this.consentimentoDados = false;
        this.ativo = false;
    }
}
