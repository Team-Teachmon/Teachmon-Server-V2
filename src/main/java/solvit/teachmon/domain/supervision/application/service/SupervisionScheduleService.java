package solvit.teachmon.domain.supervision.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import solvit.teachmon.domain.supervision.domain.entity.SupervisionScheduleEntity;
import solvit.teachmon.domain.supervision.domain.enums.SupervisionType;
import solvit.teachmon.domain.supervision.domain.repository.SupervisionExchangeRepository;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SupervisionScheduleService {

    private final SupervisionScheduleRepository supervisionScheduleRepository;
    private final SupervisionExchangeRepository supervisionExchangeRepository;
    private final TeacherRepository teacherRepository;

    @Transactional
    public void createSupervisionSchedule(SupervisionScheduleCreateRequestDto requestDto) {
        createSupervisionSchedulesInternal(requestDto);
    }

    /**
     * 그날 일정을 지우고 다시 만들지 않고, 칸(타입·교시)별로 기존 일정의 담당 교사만 바꾼다.
     * 일정 ID 가 유지되므로 그 일정에 걸린 교체 신청도 유지된다.
     * - 칸이 비어 있으면 새로 만든다.
     * - 요청에서 빠진 칸(7교시 미지정 등)과 같은 칸의 중복 행은 삭제하고, 그 일정에 걸린 교체 신청만 함께 삭제한다.
     */
    @Transactional
    public void updateSupervisionSchedule(SupervisionScheduleUpdateRequestDto requestDto) {
        LocalDate day = requestDto.day();
        Map<ScheduleSlot, TeacherEntity> requested = requestedAssignments(requestDto);

        Map<ScheduleSlot, List<SupervisionScheduleEntity>> existing = new HashMap<>();
        supervisionScheduleRepository.findAllByDay(day).stream()
                .sorted(Comparator.comparing(SupervisionScheduleEntity::getId))
                .forEach(schedule -> existing.computeIfAbsent(ScheduleSlot.of(schedule), key -> new ArrayList<>()).add(schedule));

        List<SupervisionScheduleEntity> toCreate = new ArrayList<>();
        List<SupervisionScheduleEntity> toRemove = new ArrayList<>();

        requested.forEach((slot, teacher) -> {
            List<SupervisionScheduleEntity> schedules = existing.getOrDefault(slot, List.of());
            existing.remove(slot);
            if (teacher == null) {
                toRemove.addAll(schedules);
            } else if (schedules.isEmpty()) {
                toCreate.add(slot.create(teacher, day));
            } else {
                schedules.getFirst().changeTeacher(teacher);
                toRemove.addAll(schedules.subList(1, schedules.size()));
            }
        });
        // 요청에 없는 칸의 일정(예상하지 못한 교시 등)은 정리한다
        existing.values().forEach(toRemove::addAll);

        removeSchedules(toRemove);
        supervisionScheduleRepository.saveAll(toCreate);
    }

    private Map<ScheduleSlot, TeacherEntity> requestedAssignments(SupervisionScheduleUpdateRequestDto requestDto) {
        TeacherEntity selfStudyTeacher = teacherRepository.findById(requestDto.selfStudySupervisionTeacherId())
                .orElseThrow(TeacherNotFoundException::new);
        TeacherEntity leaveSeatTeacher = teacherRepository.findById(requestDto.leaveSeatSupervisionTeacherId())
                .orElseThrow(TeacherNotFoundException::new);
        TeacherEntity seventhPeriodTeacher = requestDto.seventhPeriodSupervisionTeacherId() == null
                ? null
                : teacherRepository.findById(requestDto.seventhPeriodSupervisionTeacherId())
                        .orElseThrow(TeacherNotFoundException::new);

        Map<ScheduleSlot, TeacherEntity> requested = new LinkedHashMap<>();
        for (SchoolPeriod period : List.of(SchoolPeriod.EIGHT_AND_NINE_PERIOD, SchoolPeriod.TEN_AND_ELEVEN_PERIOD)) {
            requested.put(new ScheduleSlot(SupervisionType.SELF_STUDY_SUPERVISION, period), selfStudyTeacher);
            requested.put(new ScheduleSlot(SupervisionType.LEAVE_SEAT_SUPERVISION, period), leaveSeatTeacher);
        }
        requested.put(new ScheduleSlot(SupervisionType.SEVENTH_PERIOD_SUPERVISION, SchoolPeriod.SEVEN_PERIOD), seventhPeriodTeacher);
        return requested;
    }

    /**
     * 일정을 삭제한다. 삭제되는 일정을 참조하는 교체 신청만 먼저 삭제한다(FK).
     */
    private void removeSchedules(List<SupervisionScheduleEntity> schedules) {
        if (schedules.isEmpty()) {
            return;
        }
        supervisionExchangeRepository.deleteAllBySchedules(schedules);
        supervisionScheduleRepository.deleteAllInBatch(schedules);
    }

    private record ScheduleSlot(SupervisionType type, SchoolPeriod period) {
        static ScheduleSlot of(SupervisionScheduleEntity schedule) {
            return new ScheduleSlot(schedule.getType(), schedule.getPeriod());
        }

        SupervisionScheduleEntity create(TeacherEntity teacher, LocalDate day) {
            return SupervisionScheduleEntity.builder()
                    .teacher(teacher)
                    .day(day)
                    .period(period)
                    .type(type)
                    .build();
        }
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
            removeSchedules(supervisionScheduleRepository.findAllByDay(requestDto.day()));
        } else {
            removeSchedules(supervisionScheduleRepository.findAllByDayAndType(requestDto.day(), requestDto.type().toSupervisionType()));
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