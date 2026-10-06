package com.example.veterinaria.repository;

import com.example.veterinaria.entity.ExpedienteClinico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExpedienteClinicoRepository extends JpaRepository<ExpedienteClinico, Long> {

    Optional<ExpedienteClinico> findByCitaId(Long citaId);

    boolean existsByCitaId(Long citaId);

    @Query("SELECT e FROM ExpedienteClinico e " +
           "JOIN FETCH e.cita c " +
           "JOIN FETCH c.mascota m " +
           "JOIN FETCH c.veterinario v " +
           "WHERE m.id = :mascotaId " +
           "ORDER BY e.fechaRegistro DESC")
    List<ExpedienteClinico> findByMascotaIdOrderByFechaRegistroDesc(@Param("mascotaId") Long mascotaId);
}
