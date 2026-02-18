package solvit.teachmon.domain.student_schedule.domain.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import solvit.teachmon.domain.management.student.domain.entity.QStudentEntity;
import solvit.teachmon.domain.management.student.domain.entity.StudentEntity;
import solvit.teachmon.domain.student_schedule.application.dto.PeriodScheduleDto;
import solvit.teachmon.domain.student_schedule.application.dto.QPeriodScheduleDto;
import solvit.teachmon.domain.student_schedule.application.dto.QStudentScheduleDto;
import solvit.teachmon.domain.student_schedule.application.dto.StudentScheduleDto;
import solvit.teachmon.domain.student_schedule.domain.entity.QScheduleEntity;
import solvit.teachmon.domain.student_schedule.domain.entity.QStudentScheduleEntity;
import solvit.teachmon.domain.student_schedule.domain.entity.ScheduleEntity;
import solvit.teachmon.domain.student_schedule.domain.enums.ScheduleType;
import solvit.teachmon.global.enums.SchoolPeriod;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.querydsl.core.group.GroupBy.groupBy;
import static com.querydsl.core.group.GroupBy.list;

@Repository
@RequiredArgsConstructor
public class StudentScheduleQueryDslRepositoryImpl implements StudentScheduleQueryDslRepository {
    private final JPAQueryFactory queryFactory;

    @Override
    public Map<Integer, List<StudentScheduleDto>> findByGradeAndPeriodGroupByClass(Integer grade, LocalDate day, SchoolPeriod period) {
        QStudentEntity student = QStudentEntity.studentEntity;
        QStudentScheduleEntity studentSchedule = QStudentScheduleEntity.studentScheduleEntity;
        QScheduleEntity schedule = QScheduleEntity.scheduleEntity;
        QScheduleEntity scheduleSub = new QScheduleEntity("scheduleSub");

        // 최적화: 상관 서브쿼리를 비상관 서브쿼리로 변경
        // 복합키 (student_schedule_id, stack_order)를 IN 절로 조회
        // 이렇게 하면 서브쿼리가 한 번만 실행됨
        return queryFactory
                .from(student)
                .leftJoin(studentSchedule).on(studentSchedule.student.id.eq(student.id))
                .leftJoin(schedule).on(
                        studentSchedule.id.eq(schedule.studentSchedule.id)
                        // stack_order 가 가장 높은 스케줄 가져오기
                        // 복합키 IN 절 사용으로 성능 최적화
                        .and(Expressions.list(schedule.studentSchedule.id, schedule.stackOrder).in(
                                JPAExpressions
                                        .select(scheduleSub.studentSchedule.id, scheduleSub.stackOrder.max())
                                        .from(scheduleSub)
                                        .groupBy(scheduleSub.studentSchedule.id)
                        ))
                )
                .where(
                        gradeEq(grade),
                        dayEq(day),
                        periodEq(period)
                )
                .transform(
                        groupBy(student.classNumber).as(
                                list(
                                        new QStudentScheduleDto(
                                                student.id,
                                                student.grade,
                                                student.classNumber,
                                                student.number,
                                                student.name,
                                                studentSchedule.day,
                                                studentSchedule.period,
                                                studentSchedule.id,
                                                schedule.type
                                        )
                                )
                        )
                );
    }

    @Override
    public Map<StudentEntity, List<PeriodScheduleDto>> findByQueryAndDayGroupByStudent(String query, LocalDate day) {
        QStudentEntity student = QStudentEntity.studentEntity;
        QStudentScheduleEntity studentSchedule = QStudentScheduleEntity.studentScheduleEntity;
        QScheduleEntity schedule = QScheduleEntity.scheduleEntity;
        QScheduleEntity scheduleSub = new QScheduleEntity("scheduleSub");

        // 최적화: 상관 서브쿼리를 비상관 서브쿼리로 변경
        return queryFactory
                .from(student)
                .leftJoin(studentSchedule).on(studentSchedule.student.id.eq(student.id))
                .leftJoin(schedule).on(
                        studentSchedule.id.eq(schedule.studentSchedule.id)
                                // stack_order 가 가장 높은 스케줄 가져오기
                                // 복합키 IN 절 사용으로 성능 최적화
                                .and(Expressions.list(schedule.studentSchedule.id, schedule.stackOrder).in(
                                        JPAExpressions
                                                .select(scheduleSub.studentSchedule.id, scheduleSub.stackOrder.max())
                                                .from(scheduleSub)
                                                .groupBy(scheduleSub.studentSchedule.id)
                                ))
                )
                .where(
                        queryEq(query),
                        dayEq(day)
                )
                .transform(
                        groupBy(student).as(
                                list(
                                        new QPeriodScheduleDto(
                                                studentSchedule.id,
                                                studentSchedule.period,
                                                schedule.type
                                        )
                                )
                        )
                );
    }

    @Override
    public Map<ScheduleType, List<ScheduleEntity>> findAllByDayAndPeriodAndTypeIn(LocalDate day, SchoolPeriod period, List<ScheduleType> types) {
        QScheduleEntity schedule = QScheduleEntity.scheduleEntity;
        QStudentScheduleEntity studentSchedule = QStudentScheduleEntity.studentScheduleEntity;
        QScheduleEntity scheduleSub = new QScheduleEntity("scheduleSub");

        // 최적화: 상관 서브쿼리를 비상관 서브쿼리로 변경
        return queryFactory
                .from(studentSchedule)
                .join(schedule).on(
                        studentSchedule.id.eq(schedule.studentSchedule.id)
                                // stack_order 가 가장 높은 스케줄 가져오기
                                // 복합키 IN 절 사용으로 성능 최적화
                                .and(Expressions.list(schedule.studentSchedule.id, schedule.stackOrder).in(
                                        JPAExpressions
                                                .select(scheduleSub.studentSchedule.id, scheduleSub.stackOrder.max())
                                                .from(scheduleSub)
                                                .groupBy(scheduleSub.studentSchedule.id)
                                ))
                )
                .where(
                        studentSchedule.day.eq(day),
                        studentSchedule.period.eq(period),
                        schedule.type.in(types)
                )
                .transform(
                        groupBy(schedule.type).as(list(schedule))
                );
    }

    @Override
    public Map<Long, ScheduleType> findLastScheduleTypeByStudentsAndDayAndPeriod(List<StudentEntity> students, LocalDate day, SchoolPeriod period) {
        QStudentEntity student = QStudentEntity.studentEntity;
        QStudentScheduleEntity studentSchedule = QStudentScheduleEntity.studentScheduleEntity;
        QScheduleEntity schedule = QScheduleEntity.scheduleEntity;
        QScheduleEntity scheduleSub = new QScheduleEntity("scheduleSub");

        return queryFactory
                .from(student)
                .join(studentSchedule).on(studentSchedule.student.id.eq(student.id))
                .join(schedule).on(
                        studentSchedule.id.eq(schedule.studentSchedule.id)
                                // stack_order 가 가장 높은 스케줄 가져오기
                                // stack_order 가 가장 큰 스케줄이 최신 데이터
                                .and(schedule.stackOrder.eq(
                                        JPAExpressions
                                                .select(scheduleSub.stackOrder.max())
                                                .from(scheduleSub)
                                                .where(scheduleSub.studentSchedule.id.eq(studentSchedule.id))
                                ))
                )
                .where(
                        student.in(students),
                        studentSchedule.day.eq(day),
                        studentSchedule.period.eq(period)
                )
                .transform(
                        groupBy(student.id).as(schedule.type)
                );
    }

    @Override
    public Map<ScheduleType, List<ScheduleEntity>> findPlaceBasedSchedulesByDayAndPeriodAndTypeIn(LocalDate day, SchoolPeriod period, List<ScheduleType> types) {
        QScheduleEntity schedule = QScheduleEntity.scheduleEntity;
        QStudentScheduleEntity studentSchedule = QStudentScheduleEntity.studentScheduleEntity;
        QScheduleEntity scheduleSub = new QScheduleEntity("scheduleSub");
        QScheduleEntity scheduleMax = new QScheduleEntity("scheduleMax");
        QScheduleEntity schedulePlaceBased = new QScheduleEntity("schedulePlaceBased");

        // 장소 기반 스케줄 조회 (EXIT/AWAY 특별 처리)
        List<ScheduleEntity> placeBasedSchedules = queryFactory
                .select(schedule)
                .from(studentSchedule)
                .join(schedule).on(
                        studentSchedule.id.eq(schedule.studentSchedule.id)
                                .and(
                                        // 케이스 1: 최신 스케줄이 placeScheduleType에 포함되는 경우
                                        schedule.type.in(types)
                                                .and(Expressions.list(schedule.studentSchedule.id, schedule.stackOrder).in(
                                                        JPAExpressions
                                                                .select(scheduleSub.studentSchedule.id, scheduleSub.stackOrder.max())
                                                                .from(scheduleSub)
                                                                .where(scheduleSub.studentSchedule.id.eq(studentSchedule.id))
                                                                .groupBy(scheduleSub.studentSchedule.id)
                                                ))
                                        // 케이스 2: 최신 스케줄이 EXIT/AWAY이고,
                                        //          현재 스케줄이 placeScheduleType에 포함되며,
                                        //          EXIT/AWAY가 아닌 스케줄 중 가장 최근 스케줄
                                        .or(
                                                schedule.type.in(types)
                                                        .and(schedule.type.notIn(ScheduleType.EXIT, ScheduleType.AWAY))
                                                        .and(schedule.stackOrder.eq(
                                                                JPAExpressions
                                                                        .select(schedulePlaceBased.stackOrder.max())
                                                                        .from(schedulePlaceBased)
                                                                        .where(
                                                                                schedulePlaceBased.studentSchedule.id.eq(studentSchedule.id),
                                                                                schedulePlaceBased.type.notIn(ScheduleType.EXIT, ScheduleType.AWAY)
                                                                        )
                                                        ))
                                                        .and(JPAExpressions
                                                                .selectOne()
                                                                .from(scheduleMax)
                                                                .where(
                                                                        scheduleMax.studentSchedule.id.eq(studentSchedule.id),
                                                                        scheduleMax.stackOrder.eq(
                                                                                JPAExpressions
                                                                                        .select(scheduleSub.stackOrder.max())
                                                                                        .from(scheduleSub)
                                                                                        .where(scheduleSub.studentSchedule.id.eq(studentSchedule.id))
                                                                        ),
                                                                        scheduleMax.type.in(ScheduleType.EXIT, ScheduleType.AWAY)
                                                                )
                                                                .exists()
                                                        )
                                        )
                                )
                )
                .where(
                        studentSchedule.day.eq(day),
                        studentSchedule.period.eq(period)
                )
                .fetch();

        // ScheduleType별로 그룹화
        return placeBasedSchedules.stream()
                .collect(Collectors.groupingBy(ScheduleEntity::getType));
    }

    private BooleanExpression gradeEq(Integer grade) {
        QStudentEntity student = QStudentEntity.studentEntity;
        return grade != null ? student.grade.eq(grade) : null;
    }

    private BooleanExpression dayEq(LocalDate day) {
        QStudentScheduleEntity studentSchedule = QStudentScheduleEntity.studentScheduleEntity;
        return day != null ? studentSchedule.day.eq(day) : null;
    }

    private BooleanExpression periodEq(SchoolPeriod period) {
        QStudentScheduleEntity studentSchedule = QStudentScheduleEntity.studentScheduleEntity;
        return period != null ? studentSchedule.period.eq(period) : null;
    }

    private BooleanExpression queryEq(String query) {
        QStudentEntity student = QStudentEntity.studentEntity;

        if(query == null || query.isBlank()) {
            return null;
        }

        // 공백 제거
        String normalizedQuery = query.replace(" ", "");

        // 학생 번호는 2자리로 맞춰서 검색
        StringExpression paddedNumber = Expressions.stringTemplate("LPAD({0}, 2, '0')", student.number.stringValue());

        return student.grade.stringValue()
                .concat(student.classNumber.stringValue())
                .concat(paddedNumber)
                .concat(student.name)
                .contains(normalizedQuery);
    }
}
