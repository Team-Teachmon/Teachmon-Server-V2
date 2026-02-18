package solvit.teachmon.domain.student_schedule.domain.repository.schedules;

import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import solvit.teachmon.domain.after_school.domain.entity.QAfterSchoolEntity;
import solvit.teachmon.domain.after_school.domain.entity.QAfterSchoolReinforcementEntity;
import solvit.teachmon.domain.management.student.domain.entity.QStudentEntity;
import solvit.teachmon.domain.place.domain.entity.QPlaceEntity;
import solvit.teachmon.domain.student_schedule.application.dto.PlaceScheduleDto;
import solvit.teachmon.domain.student_schedule.application.dto.QPlaceScheduleDto;
import solvit.teachmon.domain.student_schedule.application.dto.QStudentScheduleDto;
import solvit.teachmon.domain.student_schedule.application.dto.StudentScheduleDto;
import solvit.teachmon.domain.student_schedule.domain.entity.QScheduleEntity;
import solvit.teachmon.domain.student_schedule.domain.entity.QStudentScheduleEntity;
import solvit.teachmon.domain.student_schedule.domain.entity.ScheduleEntity;
import solvit.teachmon.domain.student_schedule.domain.entity.schedules.QAfterSchoolScheduleEntity;
import solvit.teachmon.domain.student_schedule.domain.enums.ScheduleType;
import solvit.teachmon.global.enums.SchoolPeriod;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.querydsl.core.group.GroupBy.groupBy;

@Repository
@RequiredArgsConstructor
public class AfterSchoolScheduleQueryDslRepositoryImpl implements AfterSchoolScheduleQueryDslRepository {
    private final JPAQueryFactory queryFactory;

    @Override
    public Map<Integer, Long> getAfterSchoolPlaceCount(List<ScheduleEntity> schedules) {
        QAfterSchoolEntity afterSchool = QAfterSchoolEntity.afterSchoolEntity;
        QAfterSchoolScheduleEntity afterSchoolSchedule = QAfterSchoolScheduleEntity.afterSchoolScheduleEntity;
        QPlaceEntity place = QPlaceEntity.placeEntity;
        return queryFactory
                .from(afterSchoolSchedule)
                .join(afterSchoolSchedule.afterSchool, afterSchool)
                .join(afterSchoolSchedule.afterSchool.place, place)
                .where(
                        afterSchoolSchedule.schedule.in(schedules),
                        afterSchoolSchedule.schedule.type.eq(ScheduleType.AFTER_SCHOOL)
                )
                .groupBy(place.floor)
                .transform(
                        groupBy(place.floor)
                                .as(afterSchool.place.id.countDistinct())
                );
    }

    @Override
    public Map<Integer, Long> getAfterSchoolReinforcementPlaceCount(List<ScheduleEntity> schedules) {
        QAfterSchoolReinforcementEntity afterSchoolReinforcement = QAfterSchoolReinforcementEntity.afterSchoolReinforcementEntity;
        QAfterSchoolScheduleEntity afterSchoolSchedule = QAfterSchoolScheduleEntity.afterSchoolScheduleEntity;
        QPlaceEntity place = QPlaceEntity.placeEntity;
        return queryFactory
                .from(afterSchoolSchedule)
                .join(afterSchoolReinforcement).on(afterSchoolSchedule.afterSchool.id.eq(afterSchoolReinforcement.afterSchool.id))
                .join(afterSchoolReinforcement.place, place)
                .where(
                        afterSchoolSchedule.schedule.in(schedules),
                        afterSchoolSchedule.schedule.type.eq(ScheduleType.AFTER_SCHOOL_REINFORCEMENT)
                )
                .groupBy(place.floor)
                .transform(
                        groupBy(place.floor)
                                .as(afterSchoolReinforcement.place.id.countDistinct())
                );
    }

    @Override
    public List<PlaceScheduleDto> getPlaceScheduleByFloor(List<ScheduleEntity> schedules, Integer floor) {
        QPlaceEntity place = QPlaceEntity.placeEntity;
        QAfterSchoolScheduleEntity afterSchoolSchedule = QAfterSchoolScheduleEntity.afterSchoolScheduleEntity;

        return queryFactory
                .selectDistinct(new QPlaceScheduleDto(
                        place,
                        Expressions.constant(ScheduleType.AFTER_SCHOOL)
                ))
                .from(afterSchoolSchedule)
                .join(afterSchoolSchedule.afterSchool.place, place)
                .where(
                        afterSchoolSchedule.schedule.in(schedules),
                        afterSchoolSchedule.schedule.type.eq(ScheduleType.AFTER_SCHOOL),
                        place.floor.eq(floor)
                )
                .fetch();
    }

    @Override
    public List<PlaceScheduleDto> getReinforcementPlaceScheduleByFloor(List<ScheduleEntity> schedules, Integer floor) {
        QPlaceEntity place = QPlaceEntity.placeEntity;
        QAfterSchoolReinforcementEntity afterSchoolReinforcement = QAfterSchoolReinforcementEntity.afterSchoolReinforcementEntity;
        QAfterSchoolScheduleEntity afterSchoolSchedule = QAfterSchoolScheduleEntity.afterSchoolScheduleEntity;

        return queryFactory
                .selectDistinct(new QPlaceScheduleDto(
                        place,
                        Expressions.constant(ScheduleType.AFTER_SCHOOL)
                ))
                .from(afterSchoolSchedule)
                .join(afterSchoolReinforcement).on(afterSchoolSchedule.afterSchool.id.eq(afterSchoolReinforcement.afterSchool.id))
                .join(afterSchoolReinforcement.place, place)
                .where(
                        afterSchoolSchedule.schedule.in(schedules),
                        afterSchoolSchedule.schedule.type.eq(ScheduleType.AFTER_SCHOOL_REINFORCEMENT),
                        place.floor.eq(floor)
                )
                .fetch();
    }

    @Override
    public List<StudentScheduleDto> getStudentScheduleByPlaceAndDayAndPeriod(Long placeId, LocalDate day, SchoolPeriod period) {
        QAfterSchoolScheduleEntity afterSchoolSchedule = QAfterSchoolScheduleEntity.afterSchoolScheduleEntity;
        QAfterSchoolEntity afterSchool = QAfterSchoolEntity.afterSchoolEntity;
        QScheduleEntity schedule = QScheduleEntity.scheduleEntity;
        QStudentScheduleEntity studentSchedule = QStudentScheduleEntity.studentScheduleEntity;
        QStudentEntity student = QStudentEntity.studentEntity;
        QScheduleEntity scheduleSub = new QScheduleEntity("scheduleSub");
        QScheduleEntity scheduleMax = new QScheduleEntity("scheduleMax");
        QScheduleEntity schedulePlaceBased = new QScheduleEntity("schedulePlaceBased");

        return queryFactory
                .select(new QStudentScheduleDto(
                        student.id,
                        student.grade,
                        student.classNumber,
                        student.number,
                        student.name,
                        studentSchedule.day,
                        studentSchedule.period,
                        studentSchedule.id,
                        scheduleMax.type
                ))
                .from(afterSchool)
                .join(afterSchoolSchedule).on(afterSchoolSchedule.afterSchool.id.eq(afterSchool.id))
                .join(schedule).on(
                        afterSchoolSchedule.schedule.id.eq(schedule.id)
                                .and(schedule.type.eq(ScheduleType.AFTER_SCHOOL))
                )
                .join(schedule.studentSchedule, studentSchedule)
                .join(studentSchedule.student, student)
                // 최신 스케줄 조인 (EXIT/AWAY 정보 표시용)
                .join(scheduleMax).on(
                        scheduleMax.studentSchedule.id.eq(studentSchedule.id)
                                .and(Expressions.list(scheduleMax.studentSchedule.id, scheduleMax.stackOrder).in(
                                        JPAExpressions
                                                .select(scheduleSub.studentSchedule.id, scheduleSub.stackOrder.max())
                                                .from(scheduleSub)
                                                .groupBy(scheduleSub.studentSchedule.id)
                                ))
                )
                .where(
                        afterSchool.place.id.eq(placeId),
                        studentSchedule.day.eq(day),
                        studentSchedule.period.eq(period),
                        // 케이스 1: 최신 스케줄이 방과후인 경우
                        schedule.id.eq(scheduleMax.id)
                                // 케이스 2: 최신 스케줄이 EXIT/AWAY이고, 현재 스케줄이 EXIT/AWAY를 제외한 가장 최근 스케줄인 경우
                                .or(
                                        scheduleMax.type.in(ScheduleType.EXIT, ScheduleType.AWAY)
                                                .and(schedule.stackOrder.eq(
                                                        JPAExpressions
                                                                .select(schedulePlaceBased.stackOrder.max())
                                                                .from(schedulePlaceBased)
                                                                .where(
                                                                        schedulePlaceBased.studentSchedule.id.eq(studentSchedule.id),
                                                                        schedulePlaceBased.type.notIn(ScheduleType.EXIT, ScheduleType.AWAY)
                                                                )
                                                ))
                                )
                )
                .fetch();
    }

    @Override
    public List<StudentScheduleDto> getReinforcementStudentScheduleByPlaceAndDayAndPeriod(Long placeId, LocalDate day, SchoolPeriod period) {
        QAfterSchoolReinforcementEntity afterSchoolReinforcement = QAfterSchoolReinforcementEntity.afterSchoolReinforcementEntity;
        QScheduleEntity schedule = QScheduleEntity.scheduleEntity;
        QStudentScheduleEntity studentSchedule = QStudentScheduleEntity.studentScheduleEntity;
        QStudentEntity student = QStudentEntity.studentEntity;
        QScheduleEntity scheduleSub = new QScheduleEntity("scheduleSub");
        QScheduleEntity scheduleMax = new QScheduleEntity("scheduleMax");
        QScheduleEntity schedulePlaceBased = new QScheduleEntity("schedulePlaceBased");

        return queryFactory
                .select(new QStudentScheduleDto(
                        student.id,
                        student.grade,
                        student.classNumber,
                        student.number,
                        student.name,
                        studentSchedule.day,
                        studentSchedule.period,
                        studentSchedule.id,
                        scheduleMax.type
                ))
                .from(afterSchoolReinforcement)
                .join(studentSchedule).on(
                        studentSchedule.day.eq(afterSchoolReinforcement.changeDay)
                                .and(studentSchedule.period.eq(afterSchoolReinforcement.changePeriod))
                )
                .join(schedule).on(
                        schedule.studentSchedule.id.eq(studentSchedule.id)
                                .and(schedule.type.eq(ScheduleType.AFTER_SCHOOL_REINFORCEMENT))
                )
                .join(studentSchedule.student, student)
                // 최신 스케줄 조인 (EXIT/AWAY 정보 표시용)
                .join(scheduleMax).on(
                        scheduleMax.studentSchedule.id.eq(studentSchedule.id)
                                .and(Expressions.list(scheduleMax.studentSchedule.id, scheduleMax.stackOrder).in(
                                        JPAExpressions
                                                .select(scheduleSub.studentSchedule.id, scheduleSub.stackOrder.max())
                                                .from(scheduleSub)
                                                .groupBy(scheduleSub.studentSchedule.id)
                                ))
                )
                .where(
                        afterSchoolReinforcement.place.id.eq(placeId),
                        studentSchedule.day.eq(day),
                        studentSchedule.period.eq(period),
                        // 케이스 1: 최신 스케줄이 방과후인 경우
                        schedule.id.eq(scheduleMax.id)
                                // 케이스 2: 최신 스케줄이 EXIT/AWAY이고, 현재 스케줄이 EXIT/AWAY를 제외한 가장 최근 스케줄인 경우
                                .or(
                                        scheduleMax.type.in(ScheduleType.EXIT, ScheduleType.AWAY)
                                                .and(schedule.stackOrder.eq(
                                                        JPAExpressions
                                                                .select(schedulePlaceBased.stackOrder.max())
                                                                .from(schedulePlaceBased)
                                                                .where(
                                                                        schedulePlaceBased.studentSchedule.id.eq(studentSchedule.id),
                                                                        schedulePlaceBased.type.notIn(ScheduleType.EXIT, ScheduleType.AWAY)
                                                                )
                                                ))
                                )
                )
                .fetch();
    }
}
