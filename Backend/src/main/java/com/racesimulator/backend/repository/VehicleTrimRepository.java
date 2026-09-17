package com.racesimulator.backend.repository;

import com.racesimulator.backend.entity.VehicleTrim;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VehicleTrimRepository extends JpaRepository<VehicleTrim, Long> {
    @Query("select distinct t.modelYear from VehicleTrim t order by t.modelYear desc")
    List<Short> findDistinctModelYears();

    @Query("select distinct t.generation.model.make.id from VehicleTrim t where t.modelYear = :year")
    List<Long> findMakeIdsForYear(@Param("year") short year);

    @EntityGraph(attributePaths = {"generation.model.make", "engine", "transmission"})
    List<VehicleTrim> findAllByGeneration_Model_IdAndModelYearOrderByTrimNameAsc(Long modelId, short modelYear);

    // Fetch the aggregate's to-one relationships here. The ordered child
    // collections are loaded inside the service's read-only transaction;
    // joining several List collections in one query causes Hibernate's
    // MultipleBagFetchException and also creates a large Cartesian product.
    @EntityGraph(attributePaths = {"generation.model.make", "engine", "transmission", "specification"})
    @Query("select t from VehicleTrim t where t.id = :id")
    Optional<VehicleTrim> findDetailedById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"generation.model.make", "engine", "transmission", "specification"})
    List<VehicleTrim> findAllByPopularTrueOrderByModelYearDesc();
}
