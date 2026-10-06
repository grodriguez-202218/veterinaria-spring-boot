package com.example.veterinaria.repository;

import com.example.veterinaria.entity.CitaMedica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CitaMedicaRepository extends JpaRepository<CitaMedica, Long> {

    @Query("SELECT COUNT(c) > 0 FROM CitaMedica c " +
           "WHERE c.veterinario.id = :vetId " +
           "AND c.estado != com.example.veterinaria.enums.EstadoCita.CANCELADA " +
           "AND c.fechaHora < :fechaFin " +
           "AND c.fechaHora > :fechaInicioMenos30")
    boolean existsOverlappingCita(
        @Param("vetId") Long vetId,
        @Param("fechaFin") LocalDateTime fechaFin,
        @Param("fechaInicioMenos30") LocalDateTime fechaInicioMenos30
    );

    @Query("SELECT COUNT(c) FROM CitaMedica c " +
           "WHERE c.mascota.cliente.id = :clienteId " +
           "AND c.estado = com.example.veterinaria.enums.EstadoCita.PENDIENTE " +
           "AND c.fechaHora >= :startOfDay " +
           "AND c.fechaHora <= :endOfDay")
    long countCitasPendientesClienteEnDia(
        @Param("clienteId") Long clienteId,
        @Param("startOfDay") LocalDateTime startOfDay,
        @Param("endOfDay") LocalDateTime endOfDay
    );

    @Query("SELECT c FROM CitaMedica c " +
           "JOIN FETCH c.mascota m " +
           "JOIN FETCH m.cliente cl " +
           "JOIN FETCH c.veterinario v " +
           "WHERE (:vetId IS NULL OR c.veterinario.id = :vetId) " +
           "AND (cast(:startOfDay as localdatetime) IS NULL OR c.fechaHora >= :startOfDay) " +
           "AND (cast(:endOfDay as localdatetime) IS NULL OR c.fechaHora <= :endOfDay) " +
           "ORDER BY c.fechaHora ASC")
    List<CitaMedica> findAgenda(
        @Param("vetId") Long vetId,
        @Param("startOfDay") LocalDateTime startOfDay,
        @Param("endOfDay") LocalDateTime endOfDay
    );

    List<CitaMedica> findByMascotaIdOrderByFechaHoraDesc(Long mascotaId);
}
