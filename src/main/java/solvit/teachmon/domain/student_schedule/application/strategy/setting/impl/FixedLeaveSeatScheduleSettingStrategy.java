package solvit.teachmon.domain.student_schedule.application.strategy.setting.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import solvit.teachmon.domain.leave_seat.domain.entity.FixedLeaveSeatEntity;
import solvit.teachmon.domain.leave_seat.domain.entity.LeaveSeatEntity;
import solvit.teachmon.domain.leave_seat.domain.entity.LeaveSeatStudentEntity;
import solvit.teachmon.domain.leave_seat.domain.repository.FixedLeaveSeatRepository;
import solvit.teachmon.domain.leave_seat.domain.repository.FixedLeaveSeatStudentRepository;
import solvit.teachmon.domain.leave_seat.domain.repository.LeaveSeatRepository;
import solvit.teachmon.domain.leave_seat.domain.repository.LeaveSeatStudentRepository;
import solvit.teachmon.domain.management.student.domain.entity.StudentEntity;
import solvit.teachmon.domain.student_schedule.application.strategy.setting.StudentScheduleSettingStrategy;
import solvit.teachmon.domain.student_schedule.domain.enums.ScheduleType;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class FixedLeaveSeatScheduleSettingStrategy implements StudentScheduleSettingStrategy {
    private final LeaveSeatRepository leaveSeatRepository;
    private final FixedLeaveSeatRepository fixedLeaveSeatRepository;
    private final FixedLeaveSeatStudentRepository fixedLeaveSeatStudentRepository;
    private final LeaveSeatStudentRepository leaveSeatStudentRepository;

    @Override
    public ScheduleType getScheduleType() {
        return ScheduleType.FIXED_LEAVE_SEAT;
    }

    @Override
    public void settingSchedule(LocalDate baseDate) {
        List<FixedLeaveSeatEntity> fixedLeaveSeats = fixedLeaveSeatRepository.findAll();

        for (FixedLeaveSeatEntity fixedLeaveSeat : fixedLeaveSeats) {
            LocalDate leaveSeatDay = calculateFixedLeaveSeatDay(fixedLeaveSeat, baseDate);

            if (isBeforeLeaveSeat(fixedLeaveSeat, baseDate))
                continue;

            // 이석이 이미 생성되어 있는지 확인
            Optional<LeaveSeatEntity> existingLeaveSeat = leaveSeatRepository
                    .findByPlaceAndDayAndPeriod(fixedLeaveSeat.getPlace(), leaveSeatDay, fixedLeaveSeat.getPeriod());

            if (existingLeaveSeat.isEmpty()) {
                // 이석이 없으면 새로 생성
                createLeaveSeat(fixedLeaveSeat, leaveSeatDay);
            } else {
                // 이석이 있으면 누락된 학생만 추가
                addMissingStudents(fixedLeaveSeat, existingLeaveSeat.get());
            }
        }
    }

    private Boolean isBeforeLeaveSeat(FixedLeaveSeatEntity fixedLeaveSeat, LocalDate baseDate) {
        LocalDate fixedLeaveSeatDay = calculateFixedLeaveSeatDay(fixedLeaveSeat, baseDate);
        return fixedLeaveSeatDay.isBefore(baseDate);
    }

    private LocalDate calculateFixedLeaveSeatDay(FixedLeaveSeatEntity fixedLeaveSeat, LocalDate baseDate) {
        return baseDate.with(fixedLeaveSeat.getWeekDay().toDayOfWeek());
    }

    private void createLeaveSeat(FixedLeaveSeatEntity fixedLeaveSeat, LocalDate leaveSeatDay) {
        LeaveSeatEntity leaveSeat = LeaveSeatEntity.from(fixedLeaveSeat, leaveSeatDay);

        List<StudentEntity> students = fixedLeaveSeatStudentRepository.findAllByFixedLeaveSeat(fixedLeaveSeat);
        students.forEach(student -> {
            LeaveSeatStudentEntity leaveSeatStudent = LeaveSeatStudentEntity.builder()
                    .leaveSeat(leaveSeat)
                    .student(student)
                    .build();
            leaveSeat.addLeaveSeatStudent(leaveSeatStudent);
        });

        leaveSeatRepository.save(leaveSeat);
    }

    /**
     * 기존 이석에 누락된 학생들을 추가합니다.
     * FixedLeaveSeat에 등록된 학생 중 LeaveSeat에 등록되지 않은 학생을 찾아 추가합니다.
     *
     * N+1 쿼리 방지를 위해 배치 조회를 사용합니다.
     *
     * @param fixedLeaveSeat 고정 이석
     * @param existingLeaveSeat 기존 이석
     */
    private void addMissingStudents(FixedLeaveSeatEntity fixedLeaveSeat, LeaveSeatEntity existingLeaveSeat) {
        // 1단계: FixedLeaveSeat에 등록된 모든 학생 조회 (1번 쿼리)
        List<StudentEntity> fixedLeaveSeatStudents = fixedLeaveSeatStudentRepository.findAllByFixedLeaveSeat(fixedLeaveSeat);

        // 2단계: 기존 LeaveSeat에 이미 등록된 학생 ID 배치 조회 (1번 쿼리)
        List<Long> alreadyAddedStudentIds = leaveSeatStudentRepository.findStudentIdsByLeaveSeat(existingLeaveSeat);

        // 3단계: 누락된 학생들 필터링 (메모리에서 처리, DB 쿼리 없음)
        List<StudentEntity> missingStudents = fixedLeaveSeatStudents.stream()
                .filter(student -> !alreadyAddedStudentIds.contains(student.getId()))
                .toList();

        // 4단계: 누락된 학생들을 일괄 추가 (배치 처리)
        if (!missingStudents.isEmpty()) {
            List<LeaveSeatStudentEntity> newLeaveSeatStudents = missingStudents.stream()
                    .map(student -> LeaveSeatStudentEntity.builder()
                            .leaveSeat(existingLeaveSeat)
                            .student(student)
                            .build())
                    .toList();

            // 일괄 저장 (1번 쿼리)
            leaveSeatStudentRepository.saveAll(newLeaveSeatStudents);

            // LeaveSeat에 학생들 추가
            newLeaveSeatStudents.forEach(existingLeaveSeat::addLeaveSeatStudent);
        }
    }
}
