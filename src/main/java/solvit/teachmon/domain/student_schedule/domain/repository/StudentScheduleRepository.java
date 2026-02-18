package solvit.teachmon.domain.student_schedule.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import solvit.teachmon.domain.after_school.domain.entity.AfterSchoolEntity;
import solvit.teachmon.domain.leave_seat.domain.entity.FixedLeaveSeatEntity;
import solvit.teachmon.domain.management.student.domain.entity.StudentEntity;
import solvit.teachmon.domain.student_schedule.domain.entity.StudentScheduleEntity;
import solvit.teachmon.global.enums.SchoolPeriod;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentScheduleRepository extends JpaRepository<StudentScheduleEntity, Long>, StudentScheduleQueryDslRepository {
    @Query("SELECT s FROM StudentScheduleEntity s JOIN FETCH s.student WHERE s.student IN :students AND s.day = :day AND s.period = :period")
    List<StudentScheduleEntity> findAllByStudentsAndDayAndPeriod(List<StudentEntity> students, LocalDate day, SchoolPeriod period);

    @Query("""
    SELECT s
    FROM StudentScheduleEntity s
    JOIN FETCH s.student
    WHERE s.student.grade = :grade
      AND s.day = :day
      AND s.period = :period
""")
    List<StudentScheduleEntity> findAllByGradeAndDayAndPeriod(
            @Param("grade") Integer grade,
            @Param("day") LocalDate day,
            @Param("period") SchoolPeriod period
    );

    @Query("""
    SELECT s
    FROM StudentScheduleEntity s
    JOIN FETCH s.student
    INNER JOIN FixedLeaveSeatStudentEntity f ON s.student = f.student
    WHERE f.fixedLeaveSeat = :fixedLeaveSeat
      AND s.day = :day
      AND s.period = :period
    """)
    List<StudentScheduleEntity> findAllByFixedLeaveSeatAndDay(
            @Param("fixedLeaveSeat") FixedLeaveSeatEntity fixedLeaveSeat,
            @Param("day") LocalDate day,
            @Param("period") SchoolPeriod period
    );

    @Query("""
    SELECT s
    FROM StudentScheduleEntity s
    JOIN FETCH s.student
    INNER JOIN AfterSchoolStudentEntity a ON s.student = a.student
    WHERE a.afterSchool = :afterSchool
      AND s.day = :day
      AND s.period = :period
    """)
    List<StudentScheduleEntity> findAllByAfterSchoolAndDayAndPeriod(
            @Param("afterSchool") AfterSchoolEntity afterSchool,
            @Param("day") LocalDate day,
            @Param("period") SchoolPeriod period
    );

    @Query("SELECT s FROM StudentScheduleEntity s JOIN FETCH s.student WHERE s.day BETWEEN :startDay AND :endDay")
    List<StudentScheduleEntity> findAllByDayBetween(
            @Param("startDay") LocalDate startDay,
            @Param("endDay") LocalDate endDay
    );

    @Query("SELECT s FROM StudentScheduleEntity s JOIN FETCH s.student WHERE s.student = :student AND s.day = :day AND s.period = :period")
    Optional<StudentScheduleEntity> findByStudentAndDayAndPeriod(
            @Param("student") StudentEntity student,
            @Param("day") LocalDate day,
            @Param("period") SchoolPeriod period
    );
}
