package com.clinicabelezatotal.repository;

import com.clinicabelezatotal.model.Funcionario;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FuncionarioRepository extends JpaRepository<Funcionario, Long> {
    @EntityGraph(attributePaths = {"procedimentosHabilitados"})
    List<Funcionario> findByAtivoTrue();
    
    @Query("SELECT f FROM Funcionario f JOIN f.procedimentosHabilitados p WHERE p.id = :procedimentoId AND f.ativo = true")
    List<Funcionario> findByProcedimentoHabilitado(Long procedimentoId);
    
    Boolean existsByCpf(String cpf);
}
