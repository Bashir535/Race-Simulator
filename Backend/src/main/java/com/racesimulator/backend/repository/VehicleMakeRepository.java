package com.racesimulator.backend.repository;

import com.racesimulator.backend.entity.VehicleMake;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Collection;

public interface VehicleMakeRepository extends JpaRepository<VehicleMake, Long> {

    List<VehicleMake> findAllByOrderByNameAsc();

    List<VehicleMake> findAllByIdInOrderByNameAsc(Collection<Long> ids);
}
