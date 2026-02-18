package solvit.teachmon.domain.student_schedule.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import solvit.teachmon.domain.student_schedule.domain.entity.ScheduleEntity;
import solvit.teachmon.domain.student_schedule.domain.enums.ScheduleType;

import java.util.Optional;

@Repository
public interface ScheduleRepository extends JpaRepository<ScheduleEntity, Long>, ScheduleQueryDslRepository {
    @Query("SELECT COALESCE(MAX(s.stackOrder), 0) FROM ScheduleEntity s WHERE s.studentSchedule.id = :studentScheduleId")
    Integer findLastStackOrderByStudentScheduleId(@Param("studentScheduleId") Long studentScheduleId);

    @Modifying
    @Query("DELETE FROM ScheduleEntity s WHERE s.studentSchedule.id = :studentScheduleId AND s.type = :type")
    void deleteByStudentScheduleIdAndType(@Param("studentScheduleId") Long studentScheduleId, @Param("type") ScheduleType type);

    @Query("SELECT s FROM ScheduleEntity s JOIN FETCH s.studentSchedule WHERE s.studentSchedule.id = :studentScheduleId AND s.type = :type")
    Optional<ScheduleEntity> findByStudentScheduleIdAndType(@Param("studentScheduleId") Long studentScheduleId, @Param("type") ScheduleType type);

    @Query("SELECT s FROM ScheduleEntity s WHERE s.studentSchedule.id = :studentScheduleId AND s.stackOrder = (SELECT MAX(s2.stackOrder) FROM ScheduleEntity s2 WHERE s2.studentSchedule.id = :studentScheduleId)")
    Optional<ScheduleEntity> findTopByStudentScheduleIdOrderByStackOrderDesc(@Param("studentScheduleId") Long studentScheduleId);
}
