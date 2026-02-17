package solvit.teachmon.domain.after_school.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import solvit.teachmon.domain.after_school.domain.entity.AfterSchoolBusinessTripEntity;
import solvit.teachmon.domain.after_school.domain.entity.AfterSchoolEntity;
import solvit.teachmon.domain.after_school.domain.entity.AfterSchoolReinforcementEntity;
import solvit.teachmon.domain.after_school.domain.repository.AfterSchoolBusinessTripRepository;
import solvit.teachmon.domain.after_school.domain.repository.AfterSchoolReinforcementRepository;
import solvit.teachmon.domain.after_school.domain.repository.AfterSchoolRepository;
import solvit.teachmon.domain.after_school.domain.service.AfterSchoolStudentDomainService;
import solvit.teachmon.domain.after_school.domain.vo.StudentAssignmentResultVo;
import solvit.teachmon.domain.after_school.exception.AfterSchoolNotFoundException;
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
import solvit.teachmon.domain.place.domain.entity.PlaceEntity;
import solvit.teachmon.domain.place.domain.repository.PlaceRepository;
import solvit.teachmon.domain.user.domain.entity.TeacherEntity;
import solvit.teachmon.domain.user.domain.repository.TeacherRepository;
import solvit.teachmon.domain.user.exception.TeacherNotFoundException;
import solvit.teachmon.global.enums.SchoolPeriod;
import solvit.teachmon.global.enums.WeekDay;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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
    private final AfterSchoolScheduleService afterSchoolScheduleService;

    @Transactional
    public void createAfterSchool(AfterSchoolCreateRequestDto requestDto) {
        TeacherEntity teacher = getTeacherById(requestDto.teacherId());
        PlaceEntity place = getPlaceById(requestDto.placeId());
        BranchEntity branch = getCurrentBranch();
        List<StudentEntity> students = fetchStudentsByIds(requestDto.studentsId());

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

        updateStudentsIfPresent(requestDto.studentsId(), afterSchool);
    }

    @Transactional
    public void deleteAfterSchool(Long afterSchoolId) {
        AfterSchoolEntity afterSchool = afterSchoolRepository.findById(afterSchoolId)
                .orElseThrow(() -> new AfterSchoolNotFoundException(afterSchoolId));

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

    private BranchEntity getCurrentBranch() {
        LocalDate today = LocalDate.now();
        int currentYear = today.getYear();
        return branchRepository.findByYearAndDate(currentYear, today)
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
        StudentAssignmentResultVo studentAssignmentResultVo = afterSchoolStudentDomainService.assignStudents(
                afterSchool,
                fetchStudentsByIds(studentIds)
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
    }

    @Transactional
    public void createReinforcement(AfterSchoolReinforcementRequestDto requestDto) {
        AfterSchoolEntity afterSchool = getAfterSchoolById(requestDto.afterschoolId());
        PlaceEntity changePlace = getPlaceById(requestDto.changePlaceId());
        
        AfterSchoolReinforcementEntity reinforcement = AfterSchoolReinforcementEntity.builder()
                .changeDay(requestDto.day())
                .afterSchool(afterSchool)
                .changePeriod(requestDto.changePeriod())
                .place(changePlace)
                .build();
        
        afterSchoolReinforcementRepository.save(reinforcement);
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
}
