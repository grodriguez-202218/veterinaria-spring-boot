package com.example.veterinaria.repository;

import com.example.veterinaria.entity.Mascota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MascotaRepository extends JpaRepository<Mascota, Long> {

    List<Mascota> findByClienteId(Long clienteId);

    Optional<Mascota> findByIdAndClienteId(Long id, Long clienteId);

    boolean existsByIdAndClienteId(Long id, Long clienteId);
}
