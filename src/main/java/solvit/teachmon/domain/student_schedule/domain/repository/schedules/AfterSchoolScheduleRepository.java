package solvit.teachmon.domain.student_schedule.domain.repository.schedules;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import solvit.teachmon.domain.student_schedule.domain.entity.schedules.AfterSchoolScheduleEntity;

import java.util.List;

@Repository
public interface AfterSchoolScheduleRepository extends JpaRepository<AfterSchoolScheduleEntity, Long>, AfterSchoolScheduleQueryDslRepository {

    @Modifying
    @Query("DELETE FROM AfterSchoolScheduleEntity a WHERE a.schedule.id IN :scheduleIds")
    void deleteByScheduleIds(@Param("scheduleIds") List<Long> scheduleIds);

    @Query("SELECT a.schedule.id FROM AfterSchoolScheduleEntity a WHERE a.afterSchool.id = :afterSchoolId")
    List<Long> findScheduleIdsByAfterSchoolId(@Param("afterSchoolId") Long afterSchoolId);

    @Modifying
    @Query("DELETE FROM AfterSchoolScheduleEntity a WHERE a.afterSchool.id = :afterSchoolId")
    void deleteByAfterSchoolId(@Param("afterSchoolId") Long afterSchoolId);
}
