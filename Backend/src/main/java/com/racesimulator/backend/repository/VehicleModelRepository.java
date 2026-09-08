package com.racesimulator.backend.repository;

import com.racesimulator.backend.entity.VehicleModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VehicleModelRepository extends JpaRepository<VehicleModel, Long> {

    List<VehicleModel> findAllByMake_IdOrderByNameAsc(Long makeId);
}
