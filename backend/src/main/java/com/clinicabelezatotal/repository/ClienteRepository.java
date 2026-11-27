package com.clinicabelezatotal.repository;

import com.clinicabelezatotal.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    Optional<Cliente> findByCpf(String cpf);
    Optional<Cliente> findByEmail(String email);
    List<Cliente> findByNomeContainingIgnoreCase(String nome);
    Boolean existsByCpf(String cpf);
    Boolean existsByEmail(String email);
    
    @Query("SELECT c FROM Cliente c WHERE c.ativo = true")
    List<Cliente> findAllAtivos();

    @Query("SELECT COUNT(c) FROM Cliente c WHERE c.ativo = true")
    long countByAtivoTrue();
}
