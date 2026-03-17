package solvit.teachmon.domain.student_schedule.domain.repository.schedules;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import solvit.teachmon.domain.student_schedule.domain.entity.schedules.LeaveSeatScheduleEntity;

import java.util.List;

@Repository
public interface LeaveSeatScheduleRepository extends JpaRepository<LeaveSeatScheduleEntity, Long>, LeaveSeatScheduleQueryDslRepository {
    @Query("SELECT ls FROM LeaveSeatScheduleEntity ls WHERE ls.leaveSeat.id = :leaveSeatId")
    List<LeaveSeatScheduleEntity> findAllByLeaveSeatId(@Param("leaveSeatId") Long leaveSeatId);
}
