package solvit.teachmon.domain.leave_seat.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import solvit.teachmon.domain.leave_seat.domain.entity.LeaveSeatEntity;
import solvit.teachmon.domain.leave_seat.domain.entity.LeaveSeatStudentEntity;
import solvit.teachmon.domain.management.student.domain.entity.StudentEntity;

import java.util.List;

@Repository
public interface LeaveSeatStudentRepository extends JpaRepository<LeaveSeatStudentEntity, Long> {
    @Query("SELECT ls FROM LeaveSeatStudentEntity ls " +
           "JOIN FETCH ls.student " +
           "WHERE ls.leaveSeat = :leaveSeat")
    List<LeaveSeatStudentEntity> findAllByLeaveSeatWithFetch(@Param("leaveSeat") LeaveSeatEntity leaveSeat);

    @Modifying
    @Query("DELETE FROM LeaveSeatStudentEntity ls WHERE ls.leaveSeat.id = :leaveSeatId")
    void deleteAllByLeaveSeatId(@Param("leaveSeatId") Long leaveSeatId);

    @Query("SELECT CASE WHEN COUNT(ls) > 0 THEN true ELSE false END " +
           "FROM LeaveSeatStudentEntity ls " +
           "WHERE ls.leaveSeat = :leaveSeat AND ls.student = :student")
    boolean existsByLeaveSeatAndStudent(@Param("leaveSeat") LeaveSeatEntity leaveSeat,
                                         @Param("student") StudentEntity student);

    @Query("SELECT ls.student.id FROM LeaveSeatStudentEntity ls " +
           "WHERE ls.leaveSeat = :leaveSeat")
    List<Long> findStudentIdsByLeaveSeat(@Param("leaveSeat") LeaveSeatEntity leaveSeat);
}
