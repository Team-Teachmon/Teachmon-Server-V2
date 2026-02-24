package solvit.teachmon.domain.supervision.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import solvit.teachmon.domain.supervision.domain.entity.SupervisionScheduleEntity;
import solvit.teachmon.domain.supervision.domain.enums.SupervisionType;

import java.time.LocalDate;
import java.util.List;

public interface SupervisionScheduleRepository extends JpaRepository<SupervisionScheduleEntity, Long>, SupervisionScheduleQueryDslRepository {
    @Modifying
    @Query("DELETE FROM SupervisionScheduleEntity s WHERE s.day = :day")
    void deleteByDay(@Param("day") LocalDate day);

    @Modifying
    @Query("DELETE FROM SupervisionScheduleEntity s WHERE s.day = :day AND s.type = :type")
    void deleteByDayAndType(@Param("day") LocalDate day, @Param("type") SupervisionType type);
    
    @Query("SELECT DISTINCT s.type FROM SupervisionScheduleEntity s WHERE s.teacher.id = :teacherId AND s.day = :day")
    List<SupervisionType> findTodaySupervisionTypesByTeacher(@Param("teacherId") Long teacherId, @Param("day") LocalDate day);
    
    @Query("SELECT DISTINCT s.day FROM SupervisionScheduleEntity s WHERE s.teacher.id = :teacherId AND MONTH(s.day) = :month ORDER BY s.day")
    List<LocalDate> findSupervisionDaysByTeacherAndMonth(@Param("teacherId") Long teacherId, @Param("month") Integer month);
    
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM SupervisionScheduleEntity s WHERE s.day = :day")
    boolean existsByDay(@Param("day") LocalDate day);

}
