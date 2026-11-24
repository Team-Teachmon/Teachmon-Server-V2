package solvit.teachmon.domain.supervision.domain.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import solvit.teachmon.domain.supervision.domain.entity.SupervisionExchangeEntity;
import solvit.teachmon.domain.supervision.domain.enums.SupervisionType;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SupervisionExchangeRepository extends JpaRepository<SupervisionExchangeEntity, Long> {
    /**
     * 내가 아직 확인하지 않은 교체 요청 (송신자면 senderChecked=false, 수신자면 recipientChecked=false)
     * 이전 방식으로 CHECKED 처리된 데이터는 마이그레이션 전에도 계속 숨긴다.
     */
    @Query("""
            SELECT e FROM SupervisionExchangeEntity e
            WHERE ((e.sender.id = :userId AND e.senderChecked = false)
                OR (e.recipient.id = :userId AND e.recipientChecked = false))
              AND e.state <> 'CHECKED'
            """)
    List<SupervisionExchangeEntity> findUncheckedByTeacherId(@Param("userId") Long userId);

    /**
     * 수락/거절/확인처럼 상태를 바꾸는 요청용 조회. 같은 요청이 동시에 들어오면 뒤 요청은 앞 트랜잭션이 끝날 때까지 기다린다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM SupervisionExchangeEntity e WHERE e.id = :id")
    Optional<SupervisionExchangeEntity> findByIdForUpdate(@Param("id") Long id);

    @Modifying
    @Query("DELETE FROM SupervisionExchangeEntity se WHERE se.senderSchedule.day = :day OR se.recipientSchedule.day = :day")
    void deleteExchangeByDay(@Param("day") LocalDate day);

    @Modifying
    @Query("DELETE FROM SupervisionExchangeEntity se WHERE (se.senderSchedule.day = :day OR se.recipientSchedule.day = :day) AND (se.senderSchedule.type = :type OR se.recipientSchedule.type = :type)")
    void deleteExchangeByDayAndType(@Param("day") LocalDate day, @Param("type") SupervisionType type);
}
