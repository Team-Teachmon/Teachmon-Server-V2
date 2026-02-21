package solvit.teachmon.domain.supervision.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import solvit.teachmon.domain.supervision.application.mapper.SupervisionScheduleRequestMapper;
import solvit.teachmon.domain.supervision.domain.entity.SupervisionScheduleEntity;
import solvit.teachmon.domain.supervision.domain.enums.SupervisionType;
import solvit.teachmon.domain.supervision.domain.repository.SupervisionScheduleRepository;
import solvit.teachmon.domain.supervision.presentation.dto.request.SupervisionScheduleCreateRequestDto;
import solvit.teachmon.domain.supervision.presentation.dto.request.SupervisionScheduleDeleteRequestDto;
import solvit.teachmon.domain.supervision.presentation.dto.request.SupervisionScheduleUpdateRequestDto;
import solvit.teachmon.domain.supervision.presentation.dto.response.SupervisionScheduleResponseDto;
import solvit.teachmon.domain.supervision.presentation.dto.response.SupervisionTodayResponseDto;
import solvit.teachmon.domain.supervision.presentation.dto.response.SupervisionRankResponseDto;
import solvit.teachmon.domain.supervision.domain.enums.SupervisionTodayType;
import solvit.teachmon.domain.supervision.domain.enums.SupervisionSortOrder;
import solvit.teachmon.domain.user.domain.entity.TeacherEntity;
import solvit.teachmon.domain.user.domain.repository.TeacherRepository;
import solvit.teachmon.domain.user.exception.TeacherNotFoundException;
import solvit.teachmon.global.enums.SchoolPeriod;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SupervisionScheduleService {

    private final SupervisionScheduleRepository supervisionScheduleRepository;
    private final TeacherRepository teacherRepository;
    private final SupervisionScheduleRequestMapper mapper;

    @Transactional
    public void createSupervisionSchedule(SupervisionScheduleCreateRequestDto requestDto) {
        createSupervisionSchedulesInternal(requestDto);
    }

    @Transactional
    public void updateSupervisionSchedule(SupervisionScheduleUpdateRequestDto requestDto) {
        // 해당 날짜의 기존 감독 일정들을 모두 삭제
        supervisionScheduleRepository.deleteByDay(requestDto.day());
        
        // 새로운 감독 일정 생성
        createSupervisionSchedulesInternal(mapper.toCreateRequest(requestDto));
    }
    

    private void createSupervisionSchedulesInternal(SupervisionScheduleCreateRequestDto requestDto) {
        // 교사 조회
        TeacherEntity selfStudyTeacher = teacherRepository.findById(requestDto.selfStudySupervisionTeacherId())
                .orElseThrow(TeacherNotFoundException::new);
        
        TeacherEntity leaveSeatTeacher = teacherRepository.findById(requestDto.leaveSeatSupervisionTeacherId())
                .orElseThrow(TeacherNotFoundException::new);

        // 모든 감독 일정을 리스트에 담아서 일괄 저장
        List<SupervisionScheduleEntity> schedules = new ArrayList<>();

        // 8-11교시 자습/이석 감독 일정 생성 (8-9교시, 10-11교시)
        SchoolPeriod[] eightToElevenPeriods = {
            SchoolPeriod.EIGHT_AND_NINE_PERIOD,
            SchoolPeriod.TEN_AND_ELEVEN_PERIOD
        };

        for (SchoolPeriod period : eightToElevenPeriods) {
            // 자습 감독 일정 생성
            SupervisionScheduleEntity selfStudySchedule = SupervisionScheduleEntity.builder()
                    .teacher(selfStudyTeacher)
                    .day(requestDto.day())
                    .period(period)
                    .type(SupervisionType.SELF_STUDY_SUPERVISION)
                    .build();
            schedules.add(selfStudySchedule);

            // 이석 감독 일정 생성
            SupervisionScheduleEntity leaveSeatSchedule = SupervisionScheduleEntity.builder()
                    .teacher(leaveSeatTeacher)
                    .day(requestDto.day())
                    .period(period)
                    .type(SupervisionType.LEAVE_SEAT_SUPERVISION)
                    .build();
            schedules.add(leaveSeatSchedule);
        }

        // 7교시 감독 일정 생성 (7교시 교사가 지정된 경우에만)
        if (requestDto.seventhPeriodSupervisionTeacherId() != null) {
            TeacherEntity seventhPeriodTeacher = teacherRepository.findById(requestDto.seventhPeriodSupervisionTeacherId())
                    .orElseThrow(TeacherNotFoundException::new);

            SupervisionScheduleEntity seventhPeriodSchedule = SupervisionScheduleEntity.builder()
                    .teacher(seventhPeriodTeacher)
                    .day(requestDto.day())
                    .period(SchoolPeriod.SEVEN_PERIOD)
                    .type(SupervisionType.SEVENTH_PERIOD_SUPERVISION)
                    .build();
            schedules.add(seventhPeriodSchedule);
        }
        
        supervisionScheduleRepository.saveAll(schedules);
    }

    @Transactional
    public void deleteSupervisionSchedule(SupervisionScheduleDeleteRequestDto requestDto) {
        if (requestDto.type().isAll()) {
            supervisionScheduleRepository.deleteByDay(requestDto.day());
        } else {
            supervisionScheduleRepository.deleteByDayAndType(requestDto.day(), requestDto.type().toSupervisionType());
        }
    }

    @Transactional(readOnly = true)
    public List<SupervisionScheduleResponseDto> searchSupervisionSchedules(Integer month, String query) {
        return supervisionScheduleRepository.findSchedulesGroupedByDayAndQuery(month, query);
    }

    @Transactional(readOnly = true)
    public List<LocalDate> getMySupervisionDays(Long teacherId, Integer month) {
        return supervisionScheduleRepository.findSupervisionDaysByTeacherAndMonth(teacherId, month);
    }

    @Transactional(readOnly = true)
    public SupervisionTodayResponseDto getMyTodaySupervisionType(Long teacherId) {
        LocalDate today = LocalDate.now();
        List<SupervisionType> todayTypes = supervisionScheduleRepository.findTodaySupervisionTypesByTeacher(teacherId, today);
        
        boolean hasSelfStudy = todayTypes.contains(SupervisionType.SELF_STUDY_SUPERVISION);
        boolean hasLeaveSeat = todayTypes.contains(SupervisionType.LEAVE_SEAT_SUPERVISION);
        boolean hasSeventhPeriod = todayTypes.contains(SupervisionType.SEVENTH_PERIOD_SUPERVISION);
        
        SupervisionTodayType todayType = SupervisionTodayType.from(hasSelfStudy, hasLeaveSeat, hasSeventhPeriod);
        
        return SupervisionTodayResponseDto.builder()
                .type(todayType)
                .build();
    }

    @Transactional(readOnly = true)
    public List<SupervisionRankResponseDto> getSupervisionRankings(String query, String order) {
        // String을 enum으로 변환
        SupervisionSortOrder sortOrder = "desc".equals(order) ? SupervisionSortOrder.DESC : SupervisionSortOrder.ASC;
        
        // query가 null이거나 빈값이면 null로 통일
        String searchQuery = StringUtils.hasText(query) ? query.trim() : null;
        
        return supervisionScheduleRepository.findSupervisionRankings(searchQuery, sortOrder);
    }
}