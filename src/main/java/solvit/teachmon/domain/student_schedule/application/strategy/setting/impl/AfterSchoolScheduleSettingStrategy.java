package solvit.teachmon.domain.student_schedule.application.strategy.setting.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import solvit.teachmon.domain.after_school.domain.entity.AfterSchoolEntity;
import solvit.teachmon.domain.after_school.domain.repository.AfterSchoolBusinessTripRepository;
import solvit.teachmon.domain.after_school.domain.repository.AfterSchoolRepository;
import solvit.teachmon.domain.branch.domain.entity.BranchEntity;
import solvit.teachmon.domain.branch.domain.repository.BranchRepository;
import solvit.teachmon.domain.student_schedule.application.dto.ScheduleRestoreResult;
import solvit.teachmon.domain.student_schedule.application.strategy.setting.StudentScheduleSettingStrategy;
import solvit.teachmon.domain.student_schedule.domain.entity.ScheduleEntity;
import solvit.teachmon.domain.student_schedule.domain.entity.StudentScheduleEntity;
import solvit.teachmon.domain.student_schedule.domain.entity.schedules.AfterSchoolScheduleEntity;
import solvit.teachmon.domain.student_schedule.domain.enums.ScheduleType;
import solvit.teachmon.domain.student_schedule.domain.repository.ScheduleRepository;
import solvit.teachmon.domain.student_schedule.domain.repository.StudentScheduleRepository;
import solvit.teachmon.domain.student_schedule.domain.repository.schedules.AfterSchoolScheduleRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AfterSchoolScheduleSettingStrategy implements StudentScheduleSettingStrategy {
    private final ScheduleRepository scheduleRepository;
    private final AfterSchoolRepository afterSchoolRepository;
    private final AfterSchoolScheduleRepository afterSchoolScheduleRepository;
    private final StudentScheduleRepository studentScheduleRepository;
    private final BranchRepository branchRepository;
    private final AfterSchoolBusinessTripRepository afterSchoolBusinessTripRepository;

    @Override
    public ScheduleType getScheduleType() {
        return ScheduleType.AFTER_SCHOOL;
    }

    @Override
    public void settingSchedule(LocalDate baseDate) {
        // baseDate(그 주의 월요일)가 속한 분기가 아니라, baseDate ~ 일요일까지 걸쳐있는
        // 모든 분기를 가져온다. 분기 전환 주에는 이 기간에 분기가 2개 걸릴 수 있다.
        List<BranchEntity> branches = branchRepository.findAllOverlapping(baseDate, baseDate.plusDays(6));

        for (BranchEntity branch : branches) {
            List<AfterSchoolEntity> afterSchools = afterSchoolRepository.findAllByBranch(branch);

            for (AfterSchoolEntity afterSchool : afterSchools) {
                LocalDate afterSchoolDay = calculateAfterSchoolDay(afterSchool, baseDate);

                // 종료되었는지 확인
                if (afterSchool.getIsEnd())
                    continue;
                // 이번 주에 해당하는 날짜가 이 분기 기간 밖이면 넘어가기
                else if (isOutOfBranchPeriod(afterSchoolDay, branch))
                    continue;
                // 출장이면 넘어가기
                else if (afterSchoolBusinessTripRepository.existsByAfterSchoolAndDay(afterSchool, afterSchoolDay))
                    continue;
                // 이전 날짜면 넘어가기
                else if (isBeforeAfterSchool(afterSchool, baseDate))
                    continue;
                List<StudentScheduleEntity> studentSchedules = findStudentScheduleByAfterSchool(afterSchool, baseDate);
                settingAfterSchoolSchedule(studentSchedules, afterSchool);
            }
        }
    }

    /**
     * 누락된 방과후 스케줄 복구 (일회성)
     * fromDate 주의 fromDate 이후(포함) 방과후 중 아직 연결되지 않은 학생에게만 생성한다.
     * 방과후는 자습 바로 위에 있어야 하므로, 자습 외 다른 스케줄(이석, 이탈 등)이 이미 있는 학생은
     * 맨 위에 쌓으면 그 스케줄을 가리게 되어 건너뛰고 결과로 돌려준다.
     */
    public ScheduleRestoreResult restoreSchedule(LocalDate fromDate) {
        List<BranchEntity> branches = branchRepository.findAllOverlapping(fromDate, fromDate.with(DayOfWeek.SUNDAY));

        int restoredCount = 0;
        List<Long> skippedStudentScheduleIds = new ArrayList<>();
        for (BranchEntity branch : branches) {
            for (AfterSchoolEntity afterSchool : afterSchoolRepository.findAllByBranch(branch)) {
                LocalDate afterSchoolDay = calculateAfterSchoolDay(afterSchool, fromDate);

                if (afterSchool.getIsEnd())
                    continue;
                else if (isOutOfBranchPeriod(afterSchoolDay, branch))
                    continue;
                else if (afterSchoolBusinessTripRepository.existsByAfterSchoolAndDay(afterSchool, afterSchoolDay))
                    continue;
                else if (isBeforeAfterSchool(afterSchool, fromDate))
                    continue;

                List<Long> alreadyLinkedStudentScheduleIds = afterSchoolScheduleRepository.findLinkedStudentScheduleIdsByAfterSchool(afterSchool);

                for (StudentScheduleEntity studentSchedule : findStudentScheduleByAfterSchool(afterSchool, fromDate)) {
                    if (alreadyLinkedStudentScheduleIds.contains(studentSchedule.getId()))
                        continue;
                    if (scheduleRepository.existsByStudentScheduleIdAndTypeNot(studentSchedule.getId(), ScheduleType.SELF_STUDY)) {
                        skippedStudentScheduleIds.add(studentSchedule.getId());
                        continue;
                    }

                    ScheduleEntity newSchedule = createNewSchedule(studentSchedule);
                    createAfterSchoolSchedule(newSchedule, afterSchool);
                    restoredCount++;
                }
            }
        }

        if (!skippedStudentScheduleIds.isEmpty()) {
            log.warn("방과후 복구 건너뜀 (다른 스케줄 존재) - studentScheduleIds: {}", skippedStudentScheduleIds);
        }

        return new ScheduleRestoreResult(restoredCount, skippedStudentScheduleIds);
    }

    private boolean isOutOfBranchPeriod(LocalDate day, BranchEntity branch) {
        return day.isBefore(branch.getStartDay()) || day.isAfter(branch.getEndDay());
    }

    private Boolean isBeforeAfterSchool(AfterSchoolEntity afterSchool, LocalDate baseDate) {
        LocalDate afterSchoolDay = calculateAfterSchoolDay(afterSchool, baseDate);
        return afterSchoolDay.isBefore(baseDate);
    }

    private List<StudentScheduleEntity> findStudentScheduleByAfterSchool(AfterSchoolEntity afterSchool, LocalDate baseDate) {
        return studentScheduleRepository.findAllByAfterSchoolAndDayAndPeriod(
                afterSchool, calculateAfterSchoolDay(afterSchool, baseDate), afterSchool.getPeriod()
        );
    }

    private LocalDate calculateAfterSchoolDay(AfterSchoolEntity afterSchool, LocalDate baseDate) {
        return baseDate.with(afterSchool.getWeekDay().toDayOfWeek());
    }

    private void settingAfterSchoolSchedule(
            List<StudentScheduleEntity> studentSchedules,
            AfterSchoolEntity afterSchool
    ) {
        for(StudentScheduleEntity studentSchedule : studentSchedules) {
            ScheduleEntity newSchedule = createNewSchedule(studentSchedule);
            createAfterSchoolSchedule(newSchedule, afterSchool);
        }
    }

    private void createAfterSchoolSchedule(
            ScheduleEntity schedule,
            AfterSchoolEntity afterSchool
    ) {
        AfterSchoolScheduleEntity afterSchoolSchedule = AfterSchoolScheduleEntity.builder()
                .schedule(schedule)
                .afterSchool(afterSchool)
                .build();

        afterSchoolScheduleRepository.save(afterSchoolSchedule);
    }

    private ScheduleEntity createNewSchedule(StudentScheduleEntity studentSchedule) {
        // 새로운 스케줄 생성
        Integer lastStackOrder = scheduleRepository.findLastStackOrderByStudentScheduleId(studentSchedule.getId());
        ScheduleEntity newSchedule = ScheduleEntity.createNewStudentSchedule(studentSchedule, lastStackOrder, ScheduleType.AFTER_SCHOOL);

        scheduleRepository.save(newSchedule);

        return newSchedule;
    }
}
