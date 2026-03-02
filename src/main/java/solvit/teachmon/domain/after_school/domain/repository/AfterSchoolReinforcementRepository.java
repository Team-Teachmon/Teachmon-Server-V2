package solvit.teachmon.domain.after_school.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import solvit.teachmon.domain.after_school.domain.entity.AfterSchoolEntity;
import solvit.teachmon.domain.after_school.domain.entity.AfterSchoolReinforcementEntity;

import java.time.LocalDate;
import java.util.List;

public interface AfterSchoolReinforcementRepository extends JpaRepository<AfterSchoolReinforcementEntity, Long> {
    @Query("""
        SELECT a
        FROM AfterSchoolReinforcementEntity a
        JOIN FETCH a.afterSchool
        JOIN FETCH a.place
        WHERE a.changeDay BETWEEN :startDay AND :endDay
    """)
    List<AfterSchoolReinforcementEntity> findAllByChangeDayBetween(
            @Param("startDay") LocalDate startDay,
            @Param("endDay") LocalDate endDay
    );

    @Query("SELECT r FROM AfterSchoolReinforcementEntity r WHERE r.afterSchool IN :afterSchools AND r.changeDay > :currentDate")
    List<AfterSchoolReinforcementEntity> findFutureReinforcementsByAfterSchools(@Param("afterSchools") List<AfterSchoolEntity> afterSchools, @Param("currentDate") LocalDate currentDate);

    @Modifying
    @Query("DELETE FROM AfterSchoolReinforcementEntity r WHERE r.afterSchool = :afterSchool")
    void deleteAllByAfterSchool(@Param("afterSchool") AfterSchoolEntity afterSchool);
}