package solvit.teachmon.domain.supervision.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import solvit.teachmon.domain.supervision.application.mapper.SupervisionAssignmentMapper;
import solvit.teachmon.domain.supervision.domain.vo.TeacherPriorityInfo;
import solvit.teachmon.domain.supervision.domain.vo.DailySupervisionAssignment;
import solvit.teachmon.domain.supervision.domain.vo.TeacherSupervisionCalculator;
import solvit.teachmon.domain.supervision.domain.vo.TeacherSupervisionInfo;
import solvit.teachmon.domain.supervision.domain.entity.SupervisionScheduleEntity;
import solvit.teachmon.domain.supervision.domain.enums.SupervisionType;
import solvit.teachmon.domain.supervision.domain.repository.SupervisionAutoAssignQueryDslRepository;
import solvit.teachmon.domain.supervision.domain.strategy.SupervisionPriorityStrategy;
import solvit.teachmon.domain.supervision.exception.InsufficientTeachersException;
import solvit.teachmon.domain.supervision.exception.InvalidAssignmentException;
import solvit.teachmon.domain.user.domain.entity.TeacherEntity;
import solvit.teachmon.domain.user.domain.repository.TeacherRepository;
import solvit.teachmon.domain.user.exception.TeacherNotFoundException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class SupervisionAssignmentProcessor {

    private final SupervisionAutoAssignQueryDslRepository autoAssignRepository;
    private final TeacherRepository teacherRepository;
    private final SupervisionPriorityStrategy priorityStrategy;
    private final SupervisionAssignmentMapper mapper;

    public List<SupervisionScheduleEntity> processDateAssignments(List<LocalDate> targetDates, 
                                                                  List<TeacherSupervisionInfo> teacherInfos) {
        List<SupervisionScheduleEntity> schedules = new ArrayList<>();
        Map<Long, TeacherSupervisionInfo> teacherInfoMap = createTeacherInfoMap(teacherInfos);

        for (LocalDate date : targetDates) {
            if (isScheduleAlreadyExists(date)) {
                log.debug("이미 스케줄 존재하여 스킵: {}", date);
                continue;
            }

            processSingleDateAssignment(date, teacherInfoMap, schedules);
        }
        
        return schedules;
    }

    private void processSingleDateAssignment(LocalDate date, 
                                           Map<Long, TeacherSupervisionInfo> teacherInfoMap,
                                           List<SupervisionScheduleEntity> schedules) {
        DailySupervisionAssignment assignment = assignDailySupervision(teacherInfoMap.values(), date);
        
        List<SupervisionScheduleEntity> dailySchedules = convertToScheduleEntities(assignment, date);
        schedules.addAll(dailySchedules);

        updateTeacherInfosAfterAssignment(teacherInfoMap, assignment, date);
        logDailyAssignmentSuccess(date, assignment);
    }

    private DailySupervisionAssignment assignDailySupervision(Collection<TeacherSupervisionInfo> teacherInfos, LocalDate date) {
        List<TeacherSupervisionInfo> availableTeachers = getAvailableTeachers(teacherInfos, date);
        List<TeacherPriorityInfo> prioritizedTeachers = getPrioritizedTeachers(availableTeachers, date);
        
        validateSufficientTeachers(prioritizedTeachers, date);
        
        return createDailyAssignment(prioritizedTeachers, date);
    }

    private List<TeacherSupervisionInfo> getAvailableTeachers(Collection<TeacherSupervisionInfo> teacherInfos, LocalDate date) {
        List<TeacherSupervisionInfo> availableTeachers = teacherInfos.stream()
                .filter(info -> !new TeacherSupervisionCalculator(info).isBanDay(date.getDayOfWeek()))
                .toList();

        if (availableTeachers.size() < 2) {
            throw new InsufficientTeachersException(
                    "배정 가능한 교사가 부족합니다. 날짜: " + date + ", 가능한 교사 수: " + availableTeachers.size());
        }
        
        return availableTeachers;
    }

    private List<TeacherPriorityInfo> getPrioritizedTeachers(List<TeacherSupervisionInfo> availableTeachers, LocalDate date) {
        return availableTeachers.stream()
                .map(info -> new TeacherPriorityInfo(info, priorityStrategy.calculatePriority(info, date)))
                .filter(priorityInfo -> priorityInfo.priority() > 0)
                .sorted(Comparator.comparing(TeacherPriorityInfo::priority).reversed())
                .toList();
    }

    private void validateSufficientTeachers(List<TeacherPriorityInfo> prioritizedTeachers, LocalDate date) {
        if (prioritizedTeachers.size() < 2) {
            throw new InsufficientTeachersException(
                    "우선순위 계산 결과 배정 가능한 교사가 부족합니다. 날짜: " + date);
        }
    }

    private DailySupervisionAssignment createDailyAssignment(List<TeacherPriorityInfo> prioritizedTeachers, LocalDate date) {
        TeacherSupervisionInfo selfStudyTeacher = prioritizedTeachers.get(0).teacherInfo();
        TeacherSupervisionInfo leaveSeatTeacher = prioritizedTeachers.get(1).teacherInfo();

        validateDifferentTeachers(selfStudyTeacher, leaveSeatTeacher);
        logAssignmentResult(date, prioritizedTeachers, selfStudyTeacher, leaveSeatTeacher);

        return DailySupervisionAssignment.builder()
                .selfStudyTeacher(selfStudyTeacher)
                .leaveSeatTeacher(leaveSeatTeacher)
                .build();
    }

    private void validateDifferentTeachers(TeacherSupervisionInfo selfStudyTeacher, TeacherSupervisionInfo leaveSeatTeacher) {
        if (selfStudyTeacher.teacherId().equals(leaveSeatTeacher.teacherId())) {
            throw new InvalidAssignmentException("동일한 교사를 여러 감독 타입에 배정할 수 없습니다.");
        }
    }

    private void logAssignmentResult(LocalDate date, List<TeacherPriorityInfo> prioritizedTeachers, 
                                   TeacherSupervisionInfo selfStudyTeacher, TeacherSupervisionInfo leaveSeatTeacher) {
        log.debug("날짜 {} 우선순위: 자습감독={}({}), 이석감독={}({})", 
                date, 
                selfStudyTeacher.teacherName(), prioritizedTeachers.get(0).priority(),
                leaveSeatTeacher.teacherName(), prioritizedTeachers.get(1).priority());
    }

    private void logDailyAssignmentSuccess(LocalDate date, DailySupervisionAssignment assignment) {
        log.debug("날짜 {} 배정 완료: 자습감독={}, 이석감독={}", 
                date, 
                assignment.selfStudyTeacher().teacherName(), 
                assignment.leaveSeatTeacher().teacherName());
    }

    private boolean isScheduleAlreadyExists(LocalDate date) {
        return autoAssignRepository.existsScheduleByDate(date);
    }

    private Map<Long, TeacherSupervisionInfo> createTeacherInfoMap(List<TeacherSupervisionInfo> teacherInfos) {
        return teacherInfos.stream()
                .collect(Collectors.toMap(
                        TeacherSupervisionInfo::teacherId,
                        info -> info
                ));
    }

    private void updateTeacherInfosAfterAssignment(Map<Long, TeacherSupervisionInfo> teacherInfoMap,
                                                  DailySupervisionAssignment assignment, LocalDate date) {
        // 자습 감독 교사 정보 업데이트
        Long selfStudyTeacherId = assignment.selfStudyTeacher().teacherId();
        TeacherSupervisionCalculator selfStudyCalculator = new TeacherSupervisionCalculator(assignment.selfStudyTeacher());
        teacherInfoMap.put(selfStudyTeacherId,
                selfStudyCalculator.withUpdatedSupervision(date, SupervisionType.SELF_STUDY_SUPERVISION));

        // 이석 감독 교사 정보 업데이트
        Long leaveSeatTeacherId = assignment.leaveSeatTeacher().teacherId();
        TeacherSupervisionCalculator leaveSeatCalculator = new TeacherSupervisionCalculator(assignment.leaveSeatTeacher());
        teacherInfoMap.put(leaveSeatTeacherId,
                leaveSeatCalculator.withUpdatedSupervision(date, SupervisionType.LEAVE_SEAT_SUPERVISION));
    }

    private List<SupervisionScheduleEntity> convertToScheduleEntities(DailySupervisionAssignment assignment, LocalDate date) {
        TeacherEntity selfStudyTeacherEntity = teacherRepository.findById(assignment.selfStudyTeacher().teacherId())
                .orElseThrow(TeacherNotFoundException::new);
        TeacherEntity leaveSeatTeacherEntity = teacherRepository.findById(assignment.leaveSeatTeacher().teacherId())
                .orElseThrow(TeacherNotFoundException::new);
                
        return mapper.toScheduleEntities(date, selfStudyTeacherEntity, leaveSeatTeacherEntity);
    }
}