package solvit.teachmon.domain.after_school.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import solvit.teachmon.domain.after_school.domain.entity.AfterSchoolBusinessTripEntity;
import solvit.teachmon.domain.after_school.domain.entity.AfterSchoolEntity;
import solvit.teachmon.domain.after_school.domain.entity.AfterSchoolReinforcementEntity;
import solvit.teachmon.domain.after_school.domain.entity.AfterSchoolStudentEntity;
import solvit.teachmon.domain.after_school.domain.repository.AfterSchoolBusinessTripRepository;
import solvit.teachmon.domain.after_school.domain.repository.AfterSchoolReinforcementRepository;
import solvit.teachmon.domain.after_school.domain.repository.AfterSchoolRepository;
import solvit.teachmon.domain.after_school.domain.service.AfterSchoolStudentDomainService;
import solvit.teachmon.domain.after_school.domain.vo.StudentAssignmentResultVo;
import solvit.teachmon.domain.after_school.exception.AfterSchoolNotFoundException;
import solvit.teachmon.domain.after_school.exception.AfterSchoolBusinessTripScheduleNotFoundException;
import solvit.teachmon.domain.after_school.exception.PlaceAlreadyBookedException;
import solvit.teachmon.domain.after_school.presentation.dto.response.*;
import solvit.teachmon.domain.management.teacher.domain.entity.SupervisionBanDayEntity;
import solvit.teachmon.domain.management.teacher.domain.repository.SupervisionBanDayRepository;
import solvit.teachmon.domain.place.exception.PlaceNotFoundException;
import solvit.teachmon.domain.after_school.presentation.dto.request.AfterSchoolBusinessTripRequestDto;
import solvit.teachmon.domain.after_school.presentation.dto.request.AfterSchoolCreateRequestDto;
import solvit.teachmon.domain.after_school.presentation.dto.request.AfterSchoolReinforcementRequestDto;
import solvit.teachmon.domain.after_school.presentation.dto.request.AfterSchoolUpdateRequestDto;
import solvit.teachmon.domain.after_school.presentation.dto.request.AfterSchoolSearchRequestDto;
import solvit.teachmon.domain.branch.domain.entity.BranchEntity;
import solvit.teachmon.domain.branch.domain.repository.BranchRepository;
import solvit.teachmon.domain.branch.exception.BranchNotFoundException;
import solvit.teachmon.domain.management.student.domain.entity.StudentEntity;
import solvit.teachmon.domain.management.student.domain.repository.StudentRepository;
import solvit.teachmon.domain.management.student.exception.StudentNotFoundException;
import solvit.teachmon.domain.management.student.exception.InvalidStudentInfoException;
import solvit.teachmon.domain.place.domain.entity.PlaceEntity;
import solvit.teachmon.domain.place.domain.repository.PlaceRepository;
import solvit.teachmon.domain.student_schedule.domain.entity.StudentScheduleEntity;
import solvit.teachmon.domain.student_schedule.domain.repository.StudentScheduleRepository;
import solvit.teachmon.domain.student_schedule.domain.entity.ScheduleEntity;
import solvit.teachmon.domain.student_schedule.domain.repository.ScheduleRepository;
import solvit.teachmon.domain.student_schedule.domain.enums.ScheduleType;
import solvit.teachmon.domain.student_schedule.domain.repository.schedules.AfterSchoolScheduleRepository;
import solvit.teachmon.domain.user.domain.entity.TeacherEntity;
import solvit.teachmon.domain.user.domain.repository.TeacherRepository;
import solvit.teachmon.domain.user.exception.TeacherNotFoundException;
import solvit.teachmon.global.enums.SchoolPeriod;
import solvit.teachmon.global.enums.WeekDay;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AfterSchoolService {
    private final AfterSchoolStudentDomainService afterSchoolStudentDomainService;
    private final SupervisionBanDayRepository supervisionBanDayRepository;
    private final AfterSchoolRepository afterSchoolRepository;
    private final AfterSchoolBusinessTripRepository afterSchoolBusinessTripRepository;
    private final AfterSchoolReinforcementRepository afterSchoolReinforcementRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final BranchRepository branchRepository;
    private final PlaceRepository placeRepository;
    private final StudentScheduleRepository studentScheduleRepository;
    private final ScheduleRepository scheduleRepository;
    private final AfterSchoolScheduleService afterSchoolScheduleService;
    private final AfterSchoolScheduleRepository afterSchoolScheduleRepository;

    @Transactional
    public void createAfterSchool(AfterSchoolCreateRequestDto requestDto) {
        TeacherEntity teacher = getTeacherById(requestDto.teacherId());
        PlaceEntity place = getPlaceById(requestDto.placeId());
        BranchEntity branch = getBranchByYearAndId(requestDto.year(), requestDto.branch());
        List<StudentEntity> students = fetchStudentsByIds(requestDto.studentsId());
        
        validateStudentsGrade(students, requestDto.grade());

        AfterSchoolEntity afterSchool = AfterSchoolEntity.builder()
                .teacher(teacher)
                .branch(branch)
                .place(place)
                .weekDay(requestDto.weekDay())
                .period(requestDto.period())
                .name(requestDto.name())
                .grade(requestDto.grade())
                .year(requestDto.year())
                .build();

        afterSchoolRepository.save(afterSchool);
        
        StudentAssignmentResultVo studentAssignmentResultVo = afterSchoolStudentDomainService.assignStudents(afterSchool, students);
        afterSchoolScheduleService.save(List.of(studentAssignmentResultVo));

        SupervisionBanDayEntity supervisionBanDayEntity = SupervisionBanDayEntity.builder()
                .teacher(teacher)
                .weekDay(requestDto.weekDay())
                .isAfterschool(true)
                .build();

        supervisionBanDayRepository.save(supervisionBanDayEntity);
    }

    @Transactional
    public void updateAfterSchool(AfterSchoolUpdateRequestDto requestDto) {
        AfterSchoolEntity afterSchool = getAfterSchoolById(requestDto.afterSchoolId());
        supervisionBanDayRepository.deleteAfterSchoolBanDay(afterSchool.getTeacher().getId(), afterSchool.getWeekDay());

        TeacherEntity teacher = resolveTeacher(requestDto.teacherId(), afterSchool);
        PlaceEntity place = resolvePlace(requestDto.placeId(), afterSchool);
        WeekDay weekDay = resolveWeekDay(requestDto.weekDay(), afterSchool);
        SchoolPeriod schoolPeriod = resolveSchoolPeriod(requestDto.period(), afterSchool);
        String name = requestDto.name() != null ? requestDto.name() : afterSchool.getName();
        Integer grade = requestDto.grade() != null ? requestDto.grade() : afterSchool.getGrade();
        Integer year = requestDto.year() != null ? requestDto.year() : afterSchool.getYear();

        // 변경사항이 있는지 확인
        boolean hasChanges = hasAnyChange(teacher, place, weekDay, schoolPeriod, year, name, grade, afterSchool);

        afterSchool.updateAfterSchool(
                teacher,
                place,
                weekDay,
                schoolPeriod,
                year,
                name,
                grade
        );

        SupervisionBanDayEntity supervisionBanDayEntity = SupervisionBanDayEntity.builder()
                .teacher(teacher)
                .weekDay(requestDto.weekDay())
                .isAfterschool(true)
                .build();

        supervisionBanDayRepository.save(supervisionBanDayEntity);

        // 변경사항이 있고 이번주라면 모든 학생들의 스케줄을 업데이트
        if (hasChanges && isDateInCurrentWeek(LocalDate.now().with(weekDay.toDayOfWeek()))) {
            List<Long> allStudentIds = afterSchool.getAfterSchoolStudents().stream()
                    .map(afterSchoolStudent -> afterSchoolStudent.getStudent().getId())
                    .toList();
            
            if (!allStudentIds.isEmpty()) {
                List<StudentEntity> allStudents = fetchStudentsByIds(allStudentIds);
                StudentAssignmentResultVo studentAssignmentResultVo = afterSchoolStudentDomainService.assignStudents(afterSchool, allStudents);
                afterSchoolScheduleService.save(List.of(studentAssignmentResultVo));
            }
        }
        else {
            updateStudentsIfPresent(requestDto.studentsId(), afterSchool);
        }
    }

    @Transactional
    public void deleteAfterSchool(Long afterSchoolId) {
        AfterSchoolEntity afterSchool = afterSchoolRepository.findById(afterSchoolId)
                .orElseThrow(() -> new AfterSchoolNotFoundException(afterSchoolId));

        afterSchoolBusinessTripRepository.deleteAllByAfterSchool(afterSchool);
        afterSchoolReinforcementRepository.deleteAllByAfterSchool(afterSchool);
        supervisionBanDayRepository.deleteAfterSchoolBanDay(afterSchool.getTeacher().getId(), afterSchool.getWeekDay());
        
        afterSchoolRepository.delete(afterSchool);
    }

    @Transactional
    public void quitAfterSchool(Long afterSchoolId) {
        AfterSchoolEntity afterSchool = afterSchoolRepository.findById(afterSchoolId)
                .orElseThrow(() -> new AfterSchoolNotFoundException(afterSchoolId));
        supervisionBanDayRepository.deleteAfterSchoolBanDay(afterSchool.getTeacher().getId(), afterSchool.getWeekDay());
        afterSchool.endAfterSchool();
    }

    @Transactional(readOnly = true)
    public List<AfterSchoolResponseDto> searchAfterSchools(AfterSchoolSearchRequestDto searchRequest) {
        return afterSchoolRepository.findAfterSchoolsByConditions(searchRequest);
    }

    @Transactional(readOnly = true)
    public List<AfterSchoolMyResponseDto> searchMyAfterSchools(Long teacherId, Integer grade) {
        return afterSchoolRepository.findMyAfterSchoolsByTeacherId(teacherId, grade);
    }

    public List<AfterSchoolByTeacherResponseDto> getAfterSchoolsByTeacherId(Long teacherId) {
        List<AfterSchoolEntity> afterSchools = afterSchoolRepository.findByTeacherIdWithRelations(teacherId);
        
        return afterSchools.stream()
                .map(afterSchool -> {
                    // 보강 횟수 계산
                    int reinforcementCount = afterSchoolReinforcementRepository
                            .findAllByChangeDayBetween(LocalDate.now().minusMonths(1), LocalDate.now().plusDays(1))
                            .stream()
                            .mapToInt(reinforcement -> reinforcement.getAfterSchool().getId().equals(afterSchool.getId()) ? 1 : 0)
                            .sum();

                    return new AfterSchoolByTeacherResponseDto(
                            afterSchool.getId(),
                            afterSchool.getWeekDay().toKorean(),
                            afterSchool.getPeriod().getPeriod(),
                            afterSchool.getName(),
                            new AfterSchoolByTeacherResponseDto.PlaceInfo(
                                    afterSchool.getPlace().getId(),
                                    afterSchool.getPlace().getName()
                            ),
                            reinforcementCount
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AfterSchoolTodayResponseDto> searchMyTodayAfterSchools(Long teacherId) {
        return afterSchoolRepository.findMyTodayAfterSchoolsByTeacherId(teacherId);
    }

    private List<StudentEntity> fetchStudentsByIds(List<Long> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return List.of();
        }

        List<StudentEntity> students = studentRepository.findAllById(studentIds);

        if (students.size() != studentIds.size()) {
            throw new StudentNotFoundException();
        }

        return students;
    }

    private TeacherEntity getTeacherById(Long teacherId) {
        return teacherRepository.findById(teacherId)
                .orElseThrow(TeacherNotFoundException::new);
    }

    private PlaceEntity getPlaceById(Long placeId) {
        return placeRepository.findById(placeId)
                .orElseThrow(PlaceNotFoundException::new);
    }

    private BranchEntity getBranchByYearAndId(Integer year, Integer branch) {
        return branchRepository.findByYearAndBranch(year, branch)
                .orElseThrow(BranchNotFoundException::new);
    }

    private AfterSchoolEntity getAfterSchoolById(Long id) {
        return afterSchoolRepository.findWithAllRelations(id)
                .orElseThrow(() -> new AfterSchoolNotFoundException(id));
    }

    private TeacherEntity resolveTeacher(Long teacherId, AfterSchoolEntity afterSchool) {
        return teacherId != null ? getTeacherById(teacherId) : afterSchool.getTeacher();
    }

    private PlaceEntity resolvePlace(Long placeId, AfterSchoolEntity afterSchool) {
        return placeId != null ? getPlaceById(placeId) : afterSchool.getPlace();
    }

    private WeekDay resolveWeekDay(WeekDay weekDay, AfterSchoolEntity afterSchool) {
        return weekDay != null ? weekDay : afterSchool.getWeekDay();
    }

    private SchoolPeriod resolveSchoolPeriod(SchoolPeriod period, AfterSchoolEntity afterSchool) {
        return period != null ? period : afterSchool.getPeriod();
    }

    private void updateStudentsIfPresent(List<Long> studentIds, AfterSchoolEntity afterSchool) {
        if (studentIds == null) return;
        List<StudentEntity> students = fetchStudentsByIds(studentIds);
        validateStudentsGrade(students, afterSchool.getGrade());
        
        StudentAssignmentResultVo studentAssignmentResultVo = afterSchoolStudentDomainService.assignStudents(
                afterSchool,
                students
        );
        afterSchoolScheduleService.save(List.of(studentAssignmentResultVo));
    }

    @Transactional
    public void createBusinessTrip(AfterSchoolBusinessTripRequestDto requestDto) {
        AfterSchoolEntity afterSchool = getAfterSchoolById(requestDto.afterschoolId());

        AfterSchoolBusinessTripEntity businessTrip = AfterSchoolBusinessTripEntity.builder()
                .day(requestDto.day())
                .afterSchool(afterSchool)
                .build();

        afterSchoolBusinessTripRepository.save(businessTrip);
        
        // 1. 출장 날짜가 이번주인지 검사
        if (isDateInCurrentWeek(requestDto.day())) {
            // 2. 이번주라면 해당 방과후를 듣는 학생들의 가장 최근 스케줄 삭제
            deleteRecentAfterSchoolSchedules(afterSchool, requestDto.day());
        }
    }

    @Transactional
    public void createReinforcement(AfterSchoolReinforcementRequestDto requestDto) {
        AfterSchoolEntity afterSchool = getAfterSchoolById(requestDto.afterschoolId());
        PlaceEntity changePlace = getPlaceById(requestDto.changePlaceId());
        if(placeRepository.existAfterSchoolPlaceByDayAndPeriodAndPlace(requestDto.day(), requestDto.changePeriod(), changePlace)) {
            throw new PlaceAlreadyBookedException();
        }
        
        AfterSchoolReinforcementEntity reinforcement = AfterSchoolReinforcementEntity.builder()
                .changeDay(requestDto.day())
                .afterSchool(afterSchool)
                .changePeriod(requestDto.changePeriod())
                .place(changePlace)
                .build();
        
        afterSchoolReinforcementRepository.save(reinforcement);
        
        // 1. 보강 날짜가 이번주인지 검사
        if (isDateInCurrentWeek(requestDto.day())) {
            // 2. 이번주라면 해당 방과후를 듣는 학생들에게 방과후 보강 스케줄 생성
            createAfterSchoolReinforcementSchedules(afterSchool, requestDto.day(), requestDto.changePeriod());
        }
    }

    @Transactional(readOnly = true)
    public AfterSchoolAffordableBusinessResponseDto getBusinessTrip(Long afterSchoolId) {
        LocalDate now = LocalDate.now();
        BranchEntity branchEntity = branchRepository.findCurrentBranch(now).orElseThrow(BranchNotFoundException::new);
        LocalDate startDay = branchEntity.getStartDay();
        LocalDate afterSchoolEndDay = branchEntity.getAfterSchoolEndDay();
        AfterSchoolEntity afterSchool = getAfterSchoolById(afterSchoolId);
        
        // 한 번의 쿼리로 해당 기간의 모든 출장 날짜를 조회
        List<LocalDate> existingBusinessTripDates = afterSchoolBusinessTripRepository
                .findBusinessTripDatesByAfterSchoolAndDateRange(afterSchool, startDay, afterSchoolEndDay);
        
        List<LocalDate> localDates = new ArrayList<>();
        DayOfWeek targetDayOfWeek = afterSchool.getWeekDay().toDayOfWeek();
        
        for(LocalDate day = startDay; day.isBefore(afterSchoolEndDay); day = day.plusDays(1)) {
            if(!day.getDayOfWeek().equals(targetDayOfWeek)) continue;
            if(existingBusinessTripDates.contains(day)) continue;
            localDates.add(day);
        }
        
        return AfterSchoolAffordableBusinessResponseDto.builder()
                .dates(localDates)
                .build();
    }

    private boolean isDateInCurrentWeek(LocalDate date) {
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(DayOfWeek.MONDAY);
        LocalDate endOfWeek = today.with(DayOfWeek.SUNDAY);
        
        return !date.isBefore(startOfWeek) && !date.isAfter(endOfWeek);
    }

    private void deleteRecentAfterSchoolSchedules(AfterSchoolEntity afterSchool, LocalDate businessTripDay) {
        log.info("=== 출장 스케줄 삭제 시작 ===");
        log.info("방과후: {}, 출장날짜: {}, 교시: {}", afterSchool.getName(), businessTripDay, afterSchool.getPeriod());
        
        // 해당 방과후를 듣는 학생들의 출장 날짜 StudentSchedule 조회
        List<StudentScheduleEntity> afterSchoolSchedules = studentScheduleRepository
                .findAllByAfterSchoolAndDayAndPeriod(afterSchool, businessTripDay, afterSchool.getPeriod());
        
        log.info("찾은 StudentSchedule 수: {}", afterSchoolSchedules.size());
        
        // N+1 문제 해결: 한 번의 쿼리로 모든 StudentSchedule의 최상위 Schedule 삭제
        List<Long> studentScheduleIds = afterSchoolSchedules.stream()
                .map(StudentScheduleEntity::getId)
                .toList();
        
        log.info("StudentSchedule IDs: {}", studentScheduleIds);
        
        if (studentScheduleIds.isEmpty()) {
            log.info("StudentSchedule이 없어서 예외 발생");
            throw new AfterSchoolBusinessTripScheduleNotFoundException(afterSchool.getName());
        }

        // 먼저 각 StudentSchedule의 최상위 Schedule ID들을 조회 (AFTER_SCHOOL 타입만)
        List<Long> scheduleIds = scheduleRepository.findTopScheduleIdsByStudentScheduleIds(studentScheduleIds, ScheduleType.AFTER_SCHOOL);
        
        log.info("삭제할 Schedule IDs: {}", scheduleIds);
        
        if (!scheduleIds.isEmpty()) {
            // 그 다음 after_school_schedule 테이블의 참조 레코드들을 삭제
            afterSchoolScheduleRepository.deleteByScheduleIds(scheduleIds);
            // 마지막으로 schedule 테이블의 레코드들을 삭제
            scheduleRepository.deleteByIds(scheduleIds);
            log.info("스케줄 삭제 완료");
        } else {
            log.info("삭제할 방과후 타입 스케줄이 없음");
        }
        log.info("=== 출장 스케줄 삭제 끝 ===");
    }

    private boolean hasAnyChange(TeacherEntity teacher, PlaceEntity place, WeekDay weekDay, 
                                SchoolPeriod schoolPeriod, Integer year, String name, Integer grade,
                                AfterSchoolEntity afterSchool) {
        return !teacher.equals(afterSchool.getTeacher()) ||
               !place.equals(afterSchool.getPlace()) ||
               !weekDay.equals(afterSchool.getWeekDay()) ||
               !schoolPeriod.equals(afterSchool.getPeriod()) ||
               !year.equals(afterSchool.getYear()) ||
               !name.equals(afterSchool.getName()) ||
               !grade.equals(afterSchool.getGrade());
    }

    private void validateStudentsGrade(List<StudentEntity> students, Integer requiredGrade) {
        List<StudentEntity> invalidGradeStudents = students.stream()
                .filter(student -> !student.getGrade().equals(requiredGrade))
                .toList();
        
        if (!invalidGradeStudents.isEmpty()) {
            throw new InvalidStudentInfoException("방과후 수업 학년과 일치하지 않는 학생이 포함되어 있습니다.");
        }
    }

    private void createAfterSchoolReinforcementSchedules(
            AfterSchoolEntity afterSchool, 
            LocalDate reinforcementDay, 
            SchoolPeriod reinforcementPeriod
    ) {
        log.info("=== 보강 스케줄 생성 시작 ===");
        log.info("방과후: {}, 보강날짜: {}, 보강교시: {}", afterSchool.getName(), reinforcementDay, reinforcementPeriod);
        
        // 해당 방과후를 듣는 모든 학생들을 가져와서 보강 스케줄 생성
        List<StudentEntity> afterSchoolStudents = afterSchool.getAfterSchoolStudents().stream()
                .map(AfterSchoolStudentEntity::getStudent)
                .toList();

        log.info("방과후를 듣는 학생 수: {}", afterSchoolStudents.size());

        // N+1 문제 해결: Student Schedule 들을 일괄 생성
        List<StudentScheduleEntity> reinforcementStudentSchedules = afterSchoolStudents.stream()
                .map(student -> StudentScheduleEntity.builder()
                        .student(student)
                        .day(reinforcementDay)
                        .period(reinforcementPeriod)
                        .build())
                .toList();

        log.info("생성할 StudentSchedule 수: {}", reinforcementStudentSchedules.size());
        
        studentScheduleRepository.saveAll(reinforcementStudentSchedules);
        log.info("StudentSchedule 저장 완료");

        // N+1 문제 해결: Schedule 들을 일괄 생성
        List<ScheduleEntity> reinforcementSchedules = reinforcementStudentSchedules.stream()
                .map(studentSchedule -> {
                    Integer lastStackOrder = scheduleRepository.findLastStackOrderByStudentScheduleId(studentSchedule.getId());
                    log.debug("학생 {}, StudentSchedule ID: {}, 마지막 stackOrder: {}", 
                            studentSchedule.getStudent().getName(), studentSchedule.getId(), lastStackOrder);
                    return ScheduleEntity.createNewStudentSchedule(
                            studentSchedule,
                            lastStackOrder,
                            ScheduleType.AFTER_SCHOOL_REINFORCEMENT
                    );
                })
                .toList();

        log.info("생성할 보강 Schedule 수: {}", reinforcementSchedules.size());
        
        scheduleRepository.saveAll(reinforcementSchedules);
        log.info("보강 스케줄 저장 완료");
        
        log.info("=== 보강 스케줄 생성 끝 ===");
    }

}
