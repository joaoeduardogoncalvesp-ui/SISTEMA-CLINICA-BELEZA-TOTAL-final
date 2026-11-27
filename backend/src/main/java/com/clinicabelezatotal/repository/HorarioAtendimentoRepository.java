package com.clinicabelezatotal.repository;

import com.clinicabelezatotal.model.HorarioAtendimento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;

@Repository
public interface HorarioAtendimentoRepository extends JpaRepository<HorarioAtendimento, Long> {
    List<HorarioAtendimento> findByFuncionarioIdAndAtivoTrue(Long funcionarioId);
    List<HorarioAtendimento> findByFuncionarioIdAndDiaSemanaAndAtivoTrue(Long funcionarioId, DayOfWeek diaSemana);
    
    @Query("SELECT h FROM HorarioAtendimento h WHERE h.funcionario.id = :funcionarioId AND h.diaSemana = :diaSemana AND h.ativo = true")
    List<HorarioAtendimento> findHorariosDisponiveis(Long funcionarioId, DayOfWeek diaSemana);
}
