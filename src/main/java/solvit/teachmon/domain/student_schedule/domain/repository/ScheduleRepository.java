package solvit.teachmon.domain.student_schedule.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import solvit.teachmon.domain.student_schedule.domain.entity.ScheduleEntity;
import solvit.teachmon.domain.student_schedule.domain.enums.ScheduleType;

import java.time.LocalDate;
import java.util.List;
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

    @Query("SELECT s.id FROM ScheduleEntity s WHERE s.studentSchedule.id IN :studentScheduleIds AND s.stackOrder = (SELECT MAX(s2.stackOrder) FROM ScheduleEntity s2 WHERE s2.studentSchedule.id = s.studentSchedule.id) AND s.type = :type")
    List<Long> findTopScheduleIdsByStudentScheduleIds(@Param("studentScheduleIds") List<Long> studentScheduleIds, @Param("type") ScheduleType type);

    @Modifying
    @Query("DELETE FROM ScheduleEntity s WHERE s.id IN :scheduleIds")
    void deleteByIds(@Param("scheduleIds") List<Long> scheduleIds);

    @Query("SELECT s FROM ScheduleEntity s JOIN FETCH s.studentSchedule WHERE s.studentSchedule.day BETWEEN :startDay AND :endDay")
    List<ScheduleEntity> findAllByDateRange(@Param("startDay") LocalDate startDay, @Param("endDay") LocalDate endDay);
}
