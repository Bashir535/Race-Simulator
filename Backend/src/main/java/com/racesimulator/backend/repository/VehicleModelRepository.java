package com.racesimulator.backend.repository;

import com.racesimulator.backend.entity.VehicleModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VehicleModelRepository extends JpaRepository<VehicleModel, Long> {

    List<VehicleModel> findAllByMake_IdOrderByNameAsc(Long makeId);

    @Query("select distinct m from VehicleTrim t join t.generation g join g.model m "
            + "where m.make.id = :makeId and t.modelYear = :year order by m.name")
    List<VehicleModel> findAvailableByMakeAndYear(@Param("makeId") Long makeId, @Param("year") short year);
}
