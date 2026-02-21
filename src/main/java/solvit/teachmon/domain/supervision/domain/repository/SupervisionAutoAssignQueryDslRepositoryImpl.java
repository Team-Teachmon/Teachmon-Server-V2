package solvit.teachmon.domain.supervision.domain.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import solvit.teachmon.domain.management.teacher.domain.entity.QSupervisionBanDayEntity;
import solvit.teachmon.domain.supervision.domain.entity.QSupervisionScheduleEntity;
import solvit.teachmon.domain.supervision.domain.vo.SupervisionBanDayVo;
import solvit.teachmon.domain.supervision.domain.vo.TeacherSupervisionInfoVo;
import solvit.teachmon.domain.user.domain.entity.QTeacherEntity;
import solvit.teachmon.domain.user.domain.enums.Role;
import solvit.teachmon.global.enums.SchoolPeriod;

import java.time.LocalDate;
import java.util.List;

/**
 * 감독 자동 배정을 위한 Repository 구현체
 */
@Repository
@RequiredArgsConstructor
public class SupervisionAutoAssignQueryDslRepositoryImpl implements SupervisionAutoAssignQueryDslRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<TeacherSupervisionInfoVo> findEligibleTeacherSupervisionInfo() {
        QTeacherEntity teacher = QTeacherEntity.teacherEntity;
        QSupervisionScheduleEntity schedule = QSupervisionScheduleEntity.supervisionScheduleEntity;

        return queryFactory
                .select(Projections.constructor(
                        TeacherSupervisionInfoVo.class,
                        teacher.id,
                        teacher.name,
                        schedule.day.max(),
                        schedule.id.count(),
                        Expressions.numberTemplate(Long.class, 
                                "SUM(CASE WHEN {0} = {1} THEN 1 ELSE 0 END)", 
                                schedule.period, SchoolPeriod.SEVEN_PERIOD),
                        Expressions.numberTemplate(Long.class, 
                                "SUM(CASE WHEN {0} IN ({1}, {2}) THEN 1 ELSE 0 END) / 2", 
                                schedule.period, SchoolPeriod.EIGHT_AND_NINE_PERIOD, SchoolPeriod.TEN_AND_ELEVEN_PERIOD)
                ))
                .from(teacher)
                .leftJoin(schedule).on(schedule.teacher.eq(teacher))
                .where(teacher.role.ne(Role.VIEWER)
                        .and(teacher.mail.endsWith("@bssm.hs.kr"))
                        .and(teacher.isActive.eq(true)))
                .groupBy(teacher.id, teacher.name)
                .fetch();
    }

    @Override
    public List<SupervisionBanDayVo> findBanDaysByTeacherIds(List<Long> teacherIds) {
        QSupervisionBanDayEntity banDay = QSupervisionBanDayEntity.supervisionBanDayEntity;

        return queryFactory
                .select(Projections.constructor(
                        SupervisionBanDayVo.class,
                        banDay.teacher.id,
                        banDay.weekDay
                ))
                .from(banDay)
                .where(banDay.teacher.id.in(teacherIds))
                .fetch();
    }

    @Override
    public boolean existsScheduleByDate(LocalDate date) {
        QSupervisionScheduleEntity schedule = QSupervisionScheduleEntity.supervisionScheduleEntity;

        Integer count = queryFactory
                .selectOne()
                .from(schedule)
                .where(schedule.day.eq(date))
                .fetchFirst();

        return count != null;
    }
}