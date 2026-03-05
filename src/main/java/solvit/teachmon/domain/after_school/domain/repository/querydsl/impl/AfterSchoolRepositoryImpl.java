package solvit.teachmon.domain.after_school.domain.repository.querydsl.impl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import solvit.teachmon.domain.after_school.domain.entity.AfterSchoolEntity;
import solvit.teachmon.domain.after_school.domain.entity.AfterSchoolStudentEntity;
import solvit.teachmon.domain.after_school.domain.entity.QAfterSchoolEntity;
import solvit.teachmon.domain.after_school.domain.repository.AfterSchoolStudentRepository;
import solvit.teachmon.domain.after_school.domain.repository.querydsl.AfterSchoolQueryDslRepository;
import solvit.teachmon.domain.after_school.presentation.dto.request.AfterSchoolSearchRequestDto;
import solvit.teachmon.domain.after_school.presentation.dto.response.AfterSchoolResponseDto;
import solvit.teachmon.domain.after_school.presentation.dto.response.AfterSchoolMyResponseDto;
import solvit.teachmon.domain.after_school.presentation.dto.response.AfterSchoolSearchResponseDto;
import solvit.teachmon.domain.after_school.presentation.dto.response.AfterSchoolTodayResponseDto;
import solvit.teachmon.domain.after_school.presentation.dto.response.QAfterSchoolSearchResponseDto;
import solvit.teachmon.domain.after_school.presentation.dto.response.StudentInfo;
import solvit.teachmon.domain.branch.domain.entity.QBranchEntity;
import solvit.teachmon.domain.place.domain.entity.QPlaceEntity;
import solvit.teachmon.domain.user.domain.entity.QTeacherEntity;
import solvit.teachmon.global.enums.SchoolPeriod;
import solvit.teachmon.global.enums.WeekDay;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class AfterSchoolRepositoryImpl implements AfterSchoolQueryDslRepository {
    private static final QAfterSchoolEntity afterSchool = QAfterSchoolEntity.afterSchoolEntity;
    private static final QTeacherEntity teacher = QTeacherEntity.teacherEntity;
    private static final QPlaceEntity place = QPlaceEntity.placeEntity;
    private static final QBranchEntity branch = QBranchEntity.branchEntity;
    private final JPAQueryFactory queryFactory;
    private final AfterSchoolStudentRepository afterSchoolStudentRepository;

    @Override
    public Optional<AfterSchoolEntity> findWithAllRelations(Long afterSchoolId) {
        return Optional.ofNullable(
                queryFactory.selectFrom(afterSchool)
                        .leftJoin(afterSchool.teacher, teacher).fetchJoin()
                        .leftJoin(afterSchool.place, place).fetchJoin()
                        .leftJoin(afterSchool.branch, branch).fetchJoin()
                        .where(afterSchool.id.eq(afterSchoolId))
                        .fetchOne()
        );
    }

    @Override
    public List<AfterSchoolResponseDto> findAfterSchoolsByConditions(AfterSchoolSearchRequestDto searchRequest) {
        List<AfterSchoolEntity> entities = queryFactory
                .selectFrom(afterSchool)
                .join(afterSchool.teacher, teacher).fetchJoin()
                .join(afterSchool.place, place).fetchJoin()
                .where(
                        isNotEnded(),
                        gradeEq(searchRequest.grade()),
                        branchEq(searchRequest.branch()),
                        weekDayEq(searchRequest.weekDay()),
                        periodEq(searchRequest.startPeriod(), searchRequest.endPeriod())
                )
                .fetch();

        // Bulk로 학생 정보 조회
        List<Long> afterSchoolIds = entities.stream()
                .map(AfterSchoolEntity::getId)
                .toList();
        
        Map<Long, List<AfterSchoolStudentEntity>> studentsMap = afterSchoolIds.isEmpty() 
                ? Map.of() 
                : afterSchoolStudentRepository
                    .findByAfterSchoolIdsWithStudent(afterSchoolIds)
                    .stream()
                    .collect(Collectors.groupingBy(ast -> ast.getAfterSchool().getId()));

        return entities.stream()
                .map(entity -> convertToAfterSchoolResponseDto(entity, studentsMap.getOrDefault(entity.getId(), List.of())))
                .toList();
    }

    private SchoolPeriod mapToSchoolPeriod(Integer startPeriod, Integer endPeriod) {
        if (startPeriod == 7 && endPeriod == 7) {
            return SchoolPeriod.SEVEN_PERIOD;
        } else if (startPeriod == 8 && endPeriod == 9) {
            return SchoolPeriod.EIGHT_AND_NINE_PERIOD;
        } else if (startPeriod == 10 && endPeriod == 11) {
            return SchoolPeriod.TEN_AND_ELEVEN_PERIOD;
        }
        return null;
    }

    private BooleanExpression isNotEnded() {
        return afterSchool.isEnd.eq(false);
    }

    private BooleanExpression gradeEq(Integer grade) {
        return grade != null ? afterSchool.grade.eq(grade) : null;
    }

    private BooleanExpression branchEq(Integer branchNumber) {
        return branchNumber != null ? afterSchool.branch.branch.eq(branchNumber) : null;
    }

    private BooleanExpression weekDayEq(WeekDay weekDay) {
        return weekDay != null ? afterSchool.weekDay.eq(weekDay) : null;
    }

    private BooleanExpression periodEq(Integer startPeriod, Integer endPeriod) {
        if (startPeriod == null || endPeriod == null) {
            return null;
        }
        SchoolPeriod targetPeriod = mapToSchoolPeriod(startPeriod, endPeriod);
        return targetPeriod != null ? afterSchool.period.eq(targetPeriod) : null;
    }

    @Override
    public List<AfterSchoolMyResponseDto> findMyAfterSchoolsByTeacherId(Long teacherId, Integer grade) {
        QAfterSchoolEntity afterSchool = QAfterSchoolEntity.afterSchoolEntity;
        QPlaceEntity place = QPlaceEntity.placeEntity;

        BooleanBuilder whereCondition = new BooleanBuilder();
        whereCondition.and(afterSchool.teacher.id.eq(teacherId))
                     .and(afterSchool.isEnd.eq(false));
        
        if (grade != null) {
            whereCondition.and(afterSchool.grade.eq(grade));
        }

        List<AfterSchoolEntity> entities = queryFactory
                .selectFrom(afterSchool)
                .join(afterSchool.place, place).fetchJoin()
                .where(whereCondition)
                .fetch();

        return entities.stream()
                .map(entity -> {
                    List<StudentInfo> students = getStudentsByAfterSchoolId(entity.getId());
                    return new AfterSchoolMyResponseDto(
                            entity.getId(),
                            entity.getWeekDay().toKorean(),
                            entity.getPeriod().getPeriod(),
                            entity.getName(),
                            new AfterSchoolMyResponseDto.PlaceInfo(
                                    entity.getPlace().getId(),
                                    entity.getPlace().getName()
                            ),
                            0,
                            students
                    );
                })
                .toList();
    }

    @Override
    public List<AfterSchoolTodayResponseDto> findMyTodayAfterSchoolsByTeacherId(Long teacherId) {
        QAfterSchoolEntity afterSchool = QAfterSchoolEntity.afterSchoolEntity;
        QPlaceEntity place = QPlaceEntity.placeEntity;
        QBranchEntity branch = QBranchEntity.branchEntity;

        LocalDate today = LocalDate.now();
        WeekDay todayWeekDay;
        try {
            todayWeekDay = getTodayWeekDay(today);
        } catch (IllegalArgumentException e) {
            return List.of();
        }

        List<AfterSchoolEntity> entities = queryFactory
                .selectFrom(afterSchool)
                .join(afterSchool.place, place).fetchJoin()
                .join(afterSchool.branch, branch).fetchJoin()
                .where(afterSchool.teacher.id.eq(teacherId)
                        .and(afterSchool.weekDay.eq(todayWeekDay))
                        .and(afterSchool.isEnd.eq(false)))
                .fetch();

        String todayFormatted = formatTodayDate(today, todayWeekDay);

        return entities.stream()
                .map(entity -> {
                    List<StudentInfo> students = getStudentsByAfterSchoolId(entity.getId());
                    return new AfterSchoolTodayResponseDto(
                            entity.getId(),
                            entity.getBranch().getBranch(),
                            entity.getName(),
                            new AfterSchoolTodayResponseDto.PlaceInfo(
                                    entity.getPlace().getId(),
                                    entity.getPlace().getName()
                            ),
                            entity.getGrade(),
                            entity.getPeriod().getPeriod(),
                            todayFormatted,
                            students
                    );
                })
                .toList();
    }

    private WeekDay getTodayWeekDay(LocalDate today) {
        return switch (today.getDayOfWeek()) {
            case MONDAY -> WeekDay.MON;
            case TUESDAY -> WeekDay.TUE;
            case WEDNESDAY -> WeekDay.WED;
            case THURSDAY -> WeekDay.THU;
            default -> throw new IllegalArgumentException("방과후는 월-목요일만 운영됩니다: " + today.getDayOfWeek());
        };
    }

    private String formatTodayDate(LocalDate today, WeekDay weekDay) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd", Locale.KOREA);
        String dateStr = today.format(formatter);
        return dateStr + " " + weekDay.toKoreanFull();
    }

    private List<StudentInfo> getStudentsByAfterSchoolId(Long afterSchoolId) {
        List<AfterSchoolStudentEntity> studentEntities = afterSchoolStudentRepository
                .findByAfterSchoolIdsWithStudent(List.of(afterSchoolId));
        
        return studentEntities.stream()
                .map(ast -> new StudentInfo(
                        ast.getStudent().getId(),
                        Integer.parseInt(ast.getStudent().getGrade().toString() + ast.getStudent().getClassNumber().toString() + String.format("%02d", ast.getStudent().getNumber())),
                        ast.getStudent().getName()
                ))
                .sorted(Comparator.comparingInt(StudentInfo::number))
                .toList();
    }

    private AfterSchoolResponseDto convertToAfterSchoolResponseDto(AfterSchoolEntity entity, List<AfterSchoolStudentEntity> studentEntities) {
        List<StudentInfo> students = studentEntities.stream()
                .map(ast -> new StudentInfo(
                        ast.getStudent().getId(),
                        Integer.parseInt(ast.getStudent().getGrade().toString() + ast.getStudent().getClassNumber().toString() + String.format("%02d", ast.getStudent().getNumber())),
                        ast.getStudent().getName()
                        ))
                .sorted(Comparator.comparingInt(StudentInfo::number))
                .toList();

        return new AfterSchoolResponseDto(
                entity.getId().toString(),
                entity.getWeekDay().toKorean(),
                entity.getPeriod().getPeriod(),
                entity.getName(),
                new AfterSchoolResponseDto.TeacherInfo(
                        entity.getTeacher().getId(),
                        entity.getTeacher().getName()
                ),
                new AfterSchoolResponseDto.PlaceInfo(
                        entity.getPlace().getId(),
                        entity.getPlace().getName()
                ),
                students
        );
    }

    @Override
    public List<AfterSchoolSearchResponseDto> searchAfterSchoolsByKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }

        BooleanBuilder builder = new BooleanBuilder();
        
        // 방과후 이름으로 검색
        builder.or(afterSchool.name.containsIgnoreCase(keyword));
        
        // 담당 선생님 이름으로 검색
        builder.or(teacher.name.containsIgnoreCase(keyword));
        
        return queryFactory.select(
                new QAfterSchoolSearchResponseDto(
                        afterSchool.id,
                        afterSchool.name.concat("(").concat(afterSchool.teacher.name).concat(")")
                )
        )
        .from(afterSchool)
        .join(afterSchool.teacher, teacher)
        .where(builder)
        .fetch();
    }
}
