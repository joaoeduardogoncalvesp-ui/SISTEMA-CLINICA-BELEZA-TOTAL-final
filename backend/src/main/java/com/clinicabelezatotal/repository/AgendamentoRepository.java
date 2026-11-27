package com.clinicabelezatotal.repository;

import com.clinicabelezatotal.model.Agendamento;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {

    @EntityGraph(attributePaths = {"cliente", "funcionario", "procedimento"})
    List<Agendamento> findByClienteIdAndDataBetween(Long clienteId, LocalDate inicio, LocalDate fim);

    @EntityGraph(attributePaths = {"cliente", "funcionario", "procedimento"})
    List<Agendamento> findByFuncionarioIdAndDataAndStatusNot(Long funcionarioId, LocalDate data, Agendamento.StatusAgendamento status);

    @EntityGraph(attributePaths = {"cliente", "funcionario", "procedimento"})
    List<Agendamento> findByFuncionarioIdAndData(Long funcionarioId, LocalDate data);

    @EntityGraph(attributePaths = {"cliente", "funcionario", "procedimento"})
    List<Agendamento> findByClienteId(Long clienteId);

    @EntityGraph(attributePaths = {"cliente", "funcionario", "procedimento"})
    @Query("SELECT a FROM Agendamento a WHERE a.funcionario.id = :funcionarioId AND a.status NOT IN ('CANCELADO') ORDER BY a.data DESC, a.horaInicio DESC")
    List<Agendamento> findByFuncionarioId(Long funcionarioId);

    @EntityGraph(attributePaths = {"cliente", "funcionario", "procedimento"})
    @Query("SELECT a FROM Agendamento a WHERE a.status NOT IN ('CANCELADO') ORDER BY a.data DESC, a.horaInicio DESC")
    List<Agendamento> findAllNaoCancelados();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Agendamento a WHERE a.funcionario.id = :funcionarioId AND a.data = :data AND a.status NOT IN ('CANCELADO', 'NAO_COMPARECEU') AND ((a.horaInicio < :horaFim) AND (a.horaFim > :horaInicio))")
    List<Agendamento> findConflitosHorario(Long funcionarioId, LocalDate data, LocalTime horaInicio, LocalTime horaFim);

    @EntityGraph(attributePaths = {"cliente", "funcionario", "procedimento"})
    @Query("SELECT a FROM Agendamento a WHERE a.data = :data AND a.status NOT IN ('CANCELADO')")
    List<Agendamento> findByData(LocalDate data);

    @EntityGraph(attributePaths = {"cliente", "funcionario", "procedimento"})
    @Query("SELECT a FROM Agendamento a WHERE a.data BETWEEN :inicio AND :fim ORDER BY a.data ASC, a.horaInicio ASC")
    List<Agendamento> findByPeriodo(LocalDate inicio, LocalDate fim);

    @EntityGraph(attributePaths = {"cliente", "funcionario", "procedimento"})
    @Query("SELECT a FROM Agendamento a WHERE a.lembreteEnviado = false AND a.data = :data AND a.status IN ('AGENDADO', 'CONFIRMADO')")
    List<Agendamento> findParaEnviarLembrete(LocalDate data);

    @Query("SELECT a FROM Agendamento a JOIN FETCH a.cliente JOIN FETCH a.funcionario JOIN FETCH a.procedimento WHERE a.cliente.id = :clienteId ORDER BY a.data DESC, a.horaInicio DESC")
    List<Agendamento> findHistoricoByClienteId(Long clienteId);

    @Query("SELECT COUNT(a) FROM Agendamento a WHERE a.data = :data")
    long countByData(@Param("data") LocalDate data);

    @Query("SELECT COUNT(a) FROM Agendamento a WHERE a.data BETWEEN :inicio AND :fim")
    long countByPeriodo(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    @Query("SELECT COUNT(a) FROM Agendamento a WHERE a.data BETWEEN :inicio AND :fim AND a.status = 'REALIZADO'")
    long countRealizadosByPeriodo(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    @Query("SELECT COUNT(a) FROM Agendamento a WHERE a.data BETWEEN :inicio AND :fim AND a.status = 'CANCELADO'")
    long countCanceladosByPeriodo(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    @Query("SELECT COUNT(a) FROM Agendamento a WHERE a.data BETWEEN :inicio AND :fim AND a.status = 'NAO_COMPARECEU'")
    long countNaoCompareceramByPeriodo(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    @Query("SELECT COUNT(a) FROM Agendamento a WHERE a.data BETWEEN :inicio AND :fim AND a.status IN ('AGENDADO', 'CONFIRMADO')")
    long countPendentesByPeriodo(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    @Query("SELECT a.funcionario.id, a.funcionario.nome, a.status, COUNT(a) FROM Agendamento a WHERE a.data BETWEEN :inicio AND :fim GROUP BY a.funcionario.id, a.funcionario.nome, a.status")
    List<Object[]> countByProfissionalAndStatusByPeriodo(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    @Query("SELECT a.procedimento.id, a.procedimento.nome, a.procedimento.categoria, COUNT(a) FROM Agendamento a WHERE a.data BETWEEN :inicio AND :fim GROUP BY a.procedimento.id, a.procedimento.nome, a.procedimento.categoria ORDER BY COUNT(a) DESC")
    List<Object[]> countByProcedimentoByPeriodo(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    Optional<Agendamento> findByIdAndFuncionarioId(Long id, Long funcionarioId);
}
