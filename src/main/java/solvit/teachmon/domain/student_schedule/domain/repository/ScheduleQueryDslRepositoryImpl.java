package solvit.teachmon.domain.student_schedule.domain.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import solvit.teachmon.domain.student_schedule.domain.entity.QScheduleEntity;
import solvit.teachmon.domain.student_schedule.domain.enums.ScheduleType;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ScheduleQueryDslRepositoryImpl implements ScheduleQueryDslRepository {
    private final JPAQueryFactory queryFactory;

    @Override
    public void deleteTopSchedulesByStudentScheduleIds(List<Long> studentScheduleIds) {
        QScheduleEntity schedule = QScheduleEntity.scheduleEntity;
        QScheduleEntity subSchedule = new QScheduleEntity("subSchedule");

        // 각 studentScheduleId별로 가장 높은 stackOrder를 가진 Schedule들의 ID 조회 (방과후 타입인 경우만)
        List<Long> scheduleIdsToDelete = queryFactory
                .select(schedule.id)
                .from(schedule)
                .where(
                        schedule.studentSchedule.id.in(studentScheduleIds)
                                .and(schedule.stackOrder.eq(
                                        queryFactory.select(subSchedule.stackOrder.max())
                                                .from(subSchedule)
                                                .where(subSchedule.studentSchedule.id.eq(schedule.studentSchedule.id))
                                ))
                                .and(schedule.type.in(ScheduleType.AFTER_SCHOOL))
                )
                .fetch();

        // 조회된 Schedule들을 일괄 삭제
        if (!scheduleIdsToDelete.isEmpty()) {
            queryFactory
                    .delete(schedule)
                    .where(schedule.id.in(scheduleIdsToDelete))
                    .execute();
        }
    }
}