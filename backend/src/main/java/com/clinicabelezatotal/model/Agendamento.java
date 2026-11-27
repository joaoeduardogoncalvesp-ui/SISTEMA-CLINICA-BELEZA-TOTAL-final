package com.clinicabelezatotal.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "agendamentos", indexes = {
    @Index(name = "idx_agendamento_cliente", columnList = "cliente_id"),
    @Index(name = "idx_agendamento_funcionario", columnList = "funcionario_id"),
    @Index(name = "idx_agendamento_data", columnList = "data"),
    @Index(name = "idx_agendamento_status", columnList = "status"),
    @Index(name = "idx_agendamento_funcionario_data", columnList = "funcionario_id, data")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Agendamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funcionario_id", nullable = false)
    private Funcionario funcionario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "procedimento_id", nullable = false)
    private Procedimento procedimento;

    @Column(nullable = false)
    private LocalDate data;

    @Column(nullable = false)
    private LocalTime horaInicio;

    @Column(nullable = false)
    private LocalTime horaFim;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusAgendamento status;


    @Column(nullable = false)
    private Boolean lembreteEnviado = false;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    private LocalDateTime atualizadoEm;

    public enum StatusAgendamento {
        AGENDADO,
        CONFIRMADO,
        EM_ATENDIMENTO,
        REALIZADO,
        CANCELADO,
        NAO_COMPARECEU
    }
}
