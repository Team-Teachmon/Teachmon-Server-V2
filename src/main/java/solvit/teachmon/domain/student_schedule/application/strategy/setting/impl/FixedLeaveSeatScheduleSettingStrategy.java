package solvit.teachmon.domain.student_schedule.application.strategy.setting.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import solvit.teachmon.domain.leave_seat.domain.entity.FixedLeaveSeatEntity;
import solvit.teachmon.domain.leave_seat.domain.entity.LeaveSeatEntity;
import solvit.teachmon.domain.leave_seat.domain.entity.LeaveSeatStudentEntity;
import solvit.teachmon.domain.leave_seat.domain.repository.FixedLeaveSeatRepository;
import solvit.teachmon.domain.leave_seat.domain.repository.FixedLeaveSeatStudentRepository;
import solvit.teachmon.domain.leave_seat.domain.repository.LeaveSeatRepository;
import solvit.teachmon.domain.management.student.domain.entity.StudentEntity;
import solvit.teachmon.domain.student_schedule.application.strategy.setting.StudentScheduleSettingStrategy;
import solvit.teachmon.domain.student_schedule.domain.enums.ScheduleType;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class FixedLeaveSeatScheduleSettingStrategy implements StudentScheduleSettingStrategy {
    private final LeaveSeatRepository leaveSeatRepository;
    private final FixedLeaveSeatRepository fixedLeaveSeatRepository;
    private final FixedLeaveSeatStudentRepository fixedLeaveSeatStudentRepository;

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

            // 기존 LeaveSeat가 있으면 누락된 학생만 추가, 없으면 새로 생성
            ensureLeaveSeatHasAllStudents(fixedLeaveSeat, leaveSeatDay);
        }
    }

    private Boolean isBeforeLeaveSeat(FixedLeaveSeatEntity fixedLeaveSeat, LocalDate baseDate) {
        LocalDate fixedLeaveSeatDay = calculateFixedLeaveSeatDay(fixedLeaveSeat, baseDate);
        return fixedLeaveSeatDay.isBefore(baseDate);
    }

    private LocalDate calculateFixedLeaveSeatDay(FixedLeaveSeatEntity fixedLeaveSeat, LocalDate baseDate) {
        return baseDate.with(fixedLeaveSeat.getWeekDay().toDayOfWeek());
    }

    private void ensureLeaveSeatHasAllStudents(FixedLeaveSeatEntity fixedLeaveSeat, LocalDate leaveSeatDay) {
        // 고정 휴석의 학생 목록 (null 안전 처리)
        List<StudentEntity> fixedStudents = fixedLeaveSeatStudentRepository.findAllByFixedLeaveSeat(fixedLeaveSeat);
        if (fixedStudents == null) {
            fixedStudents = List.of();
        }

        // 기존 LeaveSeat 조회
        Optional<LeaveSeatEntity> existingOpt = leaveSeatRepository.findByPlaceAndDayAndPeriod(fixedLeaveSeat.getPlace(), leaveSeatDay, fixedLeaveSeat.getPeriod());

        if (existingOpt.isEmpty()) {
            // 기존이 없으면 새로 생성
            createLeaveSeat(fixedLeaveSeat, leaveSeatDay, fixedStudents);
            return;
        }

        LeaveSeatEntity existing = existingOpt.get();
        // 현재 LeaveSeat에 등록된 학생들의 id 집합 (null 안전 처리)
        List<LeaveSeatStudentEntity> existingLeaveSeatStudents = existing.getLeaveSeatStudents();
        Set<Long> existingStudentIds;
        if (existingLeaveSeatStudents == null) {
            existingStudentIds = Set.of();
        } else {
            existingStudentIds = existingLeaveSeatStudents.stream()
                    .map(LeaveSeatStudentEntity::getStudent)
                    .map(StudentEntity::getId)
                    .collect(Collectors.toSet());
        }

        // 누락된 학생만 추가
        boolean added = false;
        for (StudentEntity student : fixedStudents) {
            if (!existingStudentIds.contains(student.getId())) {
                LeaveSeatStudentEntity leaveSeatStudent = LeaveSeatStudentEntity.builder()
                        .leaveSeat(existing)
                        .student(student)
                        .build();
                existing.addLeaveSeatStudent(leaveSeatStudent);
                added = true;
            }
        }

        if (added) {
            leaveSeatRepository.save(existing);
        }
    }

    private void createLeaveSeat(FixedLeaveSeatEntity fixedLeaveSeat, LocalDate leaveSeatDay, List<StudentEntity> students) {
        LeaveSeatEntity leaveSeat = LeaveSeatEntity.from(fixedLeaveSeat, leaveSeatDay);

        students.forEach(student -> {
            LeaveSeatStudentEntity leaveSeatStudent = LeaveSeatStudentEntity.builder()
                    .leaveSeat(leaveSeat)
                    .student(student)
                    .build();
            leaveSeat.addLeaveSeatStudent(leaveSeatStudent);
        });

        leaveSeatRepository.save(leaveSeat);
    }
}
