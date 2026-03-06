package solvit.teachmon.domain.supervision.domain.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import solvit.teachmon.domain.management.teacher.presentation.dto.response.QTeacherListResponse;
import solvit.teachmon.domain.management.teacher.presentation.dto.response.TeacherListResponse;
import solvit.teachmon.domain.supervision.application.mapper.SupervisionResponseMapper;
import solvit.teachmon.domain.supervision.domain.entity.QSupervisionScheduleEntity;
import solvit.teachmon.domain.supervision.domain.entity.SupervisionScheduleEntity;
import solvit.teachmon.domain.supervision.domain.enums.SupervisionType;
import solvit.teachmon.domain.supervision.domain.enums.SupervisionSortOrder;
import solvit.teachmon.domain.supervision.presentation.dto.response.SupervisionRankResponseDto;
import solvit.teachmon.domain.supervision.presentation.dto.response.SupervisionScheduleResponseDto;
import solvit.teachmon.domain.user.domain.entity.QTeacherEntity;

import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class SupervisionScheduleQueryDslRepositoryImpl implements SupervisionScheduleQueryDslRepository {
    private final JPAQueryFactory queryFactory;
    private final SupervisionResponseMapper mapper;

    @Override
    public List<TeacherListResponse> countTeacherSupervision(String query) {
        QTeacherEntity teacher = QTeacherEntity.teacherEntity;
        QSupervisionScheduleEntity schedule = QSupervisionScheduleEntity.supervisionScheduleEntity;

        return queryFactory
                .select(new QTeacherListResponse(
                        teacher.id,
                        teacher.role,
                        teacher.name,
                        teacher.mail,
                        schedule.day.countDistinct().intValue()
                ))
                .from(teacher)
                .leftJoin(schedule).on(schedule.teacher.eq(teacher))
                .where(teacherNameContains(teacher, query))
                .groupBy(teacher.id)
                .fetch();
    }

    @Override
    public List<SupervisionScheduleEntity> findByMonthAndQuery(Integer month, String query) {
        QSupervisionScheduleEntity schedule = QSupervisionScheduleEntity.supervisionScheduleEntity;
        QTeacherEntity teacher = QTeacherEntity.teacherEntity;

        return queryFactory
                .selectFrom(schedule)
                .join(schedule.teacher, teacher).fetchJoin()
                .where(
                    monthEquals(month),
                    teacherNameContains(teacher, query)
                )
                .orderBy(schedule.day.asc(), schedule.period.asc(), schedule.type.asc())
                .fetch();
    }

    @Override
    public List<SupervisionScheduleResponseDto> findSchedulesGroupedByDayAndQuery(Integer month, String query) {
        // 먼저 데이터를 조회
        List<SupervisionScheduleEntity> schedules = findByMonthAndQuery(month, query);
        
        // SupervisionScheduleResponseDto로 변환
        return mapper.convertToResponseDtos(schedules);
    }


    private BooleanExpression monthEquals(Integer month) {
        QSupervisionScheduleEntity schedule = QSupervisionScheduleEntity.supervisionScheduleEntity;
        return month != null 
                ? schedule.day.month().eq(month)
                : null;
    }




    @Override
    public List<SupervisionRankResponseDto> findSupervisionRankings(String query, SupervisionSortOrder sortOrder) {
        QTeacherEntity teacher = QTeacherEntity.teacherEntity;
        QSupervisionScheduleEntity selfStudySchedule = new QSupervisionScheduleEntity("selfStudySchedule");
        QSupervisionScheduleEntity leaveSeatSchedule = new QSupervisionScheduleEntity("leaveSeatSchedule");
        QSupervisionScheduleEntity seventhPeriodSchedule = new QSupervisionScheduleEntity("seventhPeriodSchedule");

        var totalCount = selfStudySchedule.day.countDistinct().add(leaveSeatSchedule.day.countDistinct()).add(seventhPeriodSchedule.day.countDistinct());
        
        var results = queryFactory
                .select(
                    teacher.name,
                    selfStudySchedule.day.countDistinct().coalesce(0L),
                    leaveSeatSchedule.day.countDistinct().coalesce(0L),
                    seventhPeriodSchedule.day.countDistinct().coalesce(0L)
                )
                .from(teacher)
                .leftJoin(selfStudySchedule).on(selfStudySchedule.teacher.eq(teacher)
                    .and(selfStudySchedule.type.eq(SupervisionType.SELF_STUDY_SUPERVISION)))
                .leftJoin(leaveSeatSchedule).on(leaveSeatSchedule.teacher.eq(teacher)
                    .and(leaveSeatSchedule.type.eq(SupervisionType.LEAVE_SEAT_SUPERVISION)))
                .leftJoin(seventhPeriodSchedule).on(seventhPeriodSchedule.teacher.eq(teacher)
                    .and(seventhPeriodSchedule.type.eq(SupervisionType.SEVENTH_PERIOD_SUPERVISION)))
                .where(teacherNameContains(teacher, query))
                .groupBy(teacher.id, teacher.name)
                .orderBy(sortOrder == SupervisionSortOrder.DESC ? 
                    totalCount.desc() : totalCount.asc())
                .fetch();

        List<SupervisionRankResponseDto> rankList = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            var tuple = results.get(i);
            Long selfStudyCount = tuple.get(1, Long.class);
            Long leaveSeatCount = tuple.get(2, Long.class);
            Long seventhPeriodCount = tuple.get(3, Long.class);
            
            int selfStudy = selfStudyCount != null ? selfStudyCount.intValue() : 0;
            int leaveSeat = leaveSeatCount != null ? leaveSeatCount.intValue() : 0;
            int seventhPeriod = seventhPeriodCount != null ? seventhPeriodCount.intValue() : 0;
            
            SupervisionRankResponseDto dto = SupervisionRankResponseDto.builder()
                    .rank(i + 1)
                    .name(tuple.get(teacher.name))
                    .selfStudySupervisionCount(selfStudy)
                    .leaveSeatSupervisionCount(leaveSeat)
                    .seventhPeriodSupervisionCount(seventhPeriod)
                    .totalSupervisionCount(selfStudy + leaveSeat + seventhPeriod)
                    .build();
            rankList.add(dto);
        }
        
        return rankList;
    }
    
    private BooleanExpression teacherNameContains(QTeacherEntity teacher, String query) {
        return query != null && !query.isBlank() 
                ? teacher.name.containsIgnoreCase(query) 
                : null;
    }
}
