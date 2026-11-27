package com.clinicabelezatotal.repository;

import com.clinicabelezatotal.model.Procedimento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProcedimentoRepository extends JpaRepository<Procedimento, Long> {
    List<Procedimento> findByCategoria(Procedimento.CategoriaProcedimento categoria);
    List<Procedimento> findByAtivoTrue();
    List<Procedimento> findByNomeContainingIgnoreCase(String nome);
}
