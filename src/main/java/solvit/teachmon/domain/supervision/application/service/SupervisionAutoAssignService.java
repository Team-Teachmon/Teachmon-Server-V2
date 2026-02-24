package solvit.teachmon.domain.supervision.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import solvit.teachmon.domain.supervision.domain.vo.TeacherSupervisionInfo;
import solvit.teachmon.domain.supervision.domain.entity.SupervisionScheduleEntity;
import solvit.teachmon.domain.supervision.domain.repository.SupervisionScheduleRepository;
import solvit.teachmon.domain.supervision.presentation.dto.response.SupervisionScheduleResponseDto;
import solvit.teachmon.domain.supervision.application.mapper.SupervisionResponseMapper;
import solvit.teachmon.domain.supervision.exception.InsufficientTeachersException;

import java.time.LocalDate;
import java.util.List;

/**
 * 감독 일정 자동 배정 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SupervisionAutoAssignService {

    private final SupervisionScheduleRepository scheduleRepository;
    private final TeacherSupervisionInfoService teacherSupervisionInfoService;
    private final SupervisionAssignmentProcessor assignmentProcessor;
    private final SupervisionDateExtractor dateExtractor;
    private final SupervisionResponseMapper responseMapper;

    /**
     * 지정된 기간동안 감독 일정을 자동 배정
     * 
     * @param startDate 배정 시작 날짜
     * @param endDate 배정 종료 날짜  
     * @return 생성된 감독 스케줄 목록
     * @throws IllegalArgumentException 시작일이 종료일보다 늦은 경우
     * @throws InsufficientTeachersException 배정 가능한 교사가 부족한 경우
     */
    @Transactional
    public List<SupervisionScheduleResponseDto> autoAssignSupervisionSchedules(LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);
        
        log.info("감독 자동 배정 시작: startDate={}, endDate={}", startDate, endDate);

        List<TeacherSupervisionInfo> teacherInfos = teacherSupervisionInfoService.getTeacherSupervisionInfos();
        List<LocalDate> targetDates = dateExtractor.extractWeekdays(startDate, endDate);
        
        logInitialInfo(teacherInfos, targetDates);
        
        List<SupervisionScheduleEntity> schedules = assignmentProcessor.processDateAssignments(targetDates, teacherInfos);
        List<SupervisionScheduleEntity> savedSchedules = scheduleRepository.saveAll(schedules);
        
        log.info("감독 자동 배정 완료: 총 {}개 스케줄 생성", savedSchedules.size());
        return responseMapper.convertToResponseDtos(savedSchedules);
    }

    private void logInitialInfo(List<TeacherSupervisionInfo> teacherInfos, List<LocalDate> targetDates) {
        log.info("배정 가능한 교사 수: {}", teacherInfos.size());
        log.info("배정 대상 날짜 수: {}", targetDates.size());
    }

    /**
     * 날짜 범위 유효성 검증
     */
    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("시작일과 종료일은 필수입니다.");
        }
        
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException(
                String.format("시작일(%s)이 종료일(%s)보다 늦을 수 없습니다.", startDate, endDate));
        }
        
        // 다음달에만 자동배정 가능
        LocalDate now = LocalDate.now();
        LocalDate nextMonth = now.plusMonths(1);
        
        if (startDate.getYear() != nextMonth.getYear() || startDate.getMonth() != nextMonth.getMonth()) {
            throw new IllegalArgumentException("자동배정은 다음달에만 가능합니다.");
        }
        
        // startDate 전날이 배정되었는지 확인
        LocalDate previousDay = startDate.minusDays(1);
        if (previousDay.isAfter(now) || !scheduleRepository.existsByDay(previousDay)) {
            throw new IllegalArgumentException(
                String.format("전날(%s)의 자동배정이 완료되지 않았습니다.", previousDay));
        }
    }
}