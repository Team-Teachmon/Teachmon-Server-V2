package solvit.teachmon.domain.after_school.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import solvit.teachmon.domain.after_school.domain.entity.AfterSchoolBusinessTripEntity;
import solvit.teachmon.domain.after_school.domain.entity.AfterSchoolEntity;

import java.time.LocalDate;
import java.util.List;

public interface AfterSchoolBusinessTripRepository extends JpaRepository<AfterSchoolBusinessTripEntity, Long> {
    @Query("select case when (count(b) > 0) then true else false end " +
            "from AfterSchoolBusinessTripEntity b " +
            "where b.afterSchool = :afterSchool and b.day = :day")
    Boolean existsByAfterSchoolAndDay(AfterSchoolEntity afterSchool, LocalDate day);

    @Query("SELECT b FROM AfterSchoolBusinessTripEntity b WHERE b.afterSchool IN :afterSchools AND b.day < :currentDate")
    List<AfterSchoolBusinessTripEntity> findPastBusinessTripsByAfterSchools(List<AfterSchoolEntity> afterSchools, LocalDate currentDate);

    @Query("""
        SELECT DISTINCT b.afterSchool
        FROM AfterSchoolBusinessTripEntity b
        LEFT JOIN AfterSchoolReinforcementEntity r ON b.afterSchool = r.afterSchool
            AND r.changeDay >= (SELECT MIN(bt.day) FROM AfterSchoolBusinessTripEntity bt WHERE bt.afterSchool IN :afterSchools AND bt.day < :currentDate)
            AND r.changeDay <= :currentDate
        WHERE b.afterSchool IN :afterSchools
            AND b.day < :currentDate
        GROUP BY b.afterSchool
        HAVING COUNT(b) > COUNT(r)
    """)
    List<AfterSchoolEntity> findAfterSchoolsWithUnreinforcedTrips(@Param("afterSchools") List<AfterSchoolEntity> afterSchools, @Param("currentDate") LocalDate currentDate);

    @Query("SELECT b.day FROM AfterSchoolBusinessTripEntity b " +
           "WHERE b.afterSchool = :afterSchool " +
           "AND b.day >= :startDate " +
           "AND b.day < :endDate")
    List<LocalDate> findBusinessTripDatesByAfterSchoolAndDateRange(@Param("afterSchool") AfterSchoolEntity afterSchool,
                                                                   @Param("startDate") LocalDate startDate,
                                                                   @Param("endDate") LocalDate endDate);
}