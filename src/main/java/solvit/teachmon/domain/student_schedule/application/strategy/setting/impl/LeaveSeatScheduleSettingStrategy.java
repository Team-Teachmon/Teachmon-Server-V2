package solvit.teachmon.domain.student_schedule.application.strategy.setting.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import solvit.teachmon.domain.leave_seat.domain.entity.LeaveSeatEntity;
import solvit.teachmon.domain.leave_seat.domain.entity.LeaveSeatStudentEntity;
import solvit.teachmon.domain.leave_seat.domain.repository.LeaveSeatRepository;
import solvit.teachmon.domain.management.student.domain.entity.StudentEntity;
import solvit.teachmon.domain.student_schedule.application.strategy.setting.StudentScheduleSettingStrategy;
import solvit.teachmon.domain.student_schedule.domain.entity.ScheduleEntity;
import solvit.teachmon.domain.student_schedule.domain.entity.StudentScheduleEntity;
import solvit.teachmon.domain.student_schedule.domain.entity.schedules.LeaveSeatScheduleEntity;
import solvit.teachmon.domain.student_schedule.domain.enums.ScheduleType;
import solvit.teachmon.domain.student_schedule.domain.repository.ScheduleRepository;
import solvit.teachmon.domain.student_schedule.domain.repository.StudentScheduleRepository;
import solvit.teachmon.domain.student_schedule.domain.repository.schedules.LeaveSeatScheduleRepository;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class LeaveSeatScheduleSettingStrategy implements StudentScheduleSettingStrategy {
    private final ScheduleRepository scheduleRepository;
    private final StudentScheduleRepository studentScheduleRepository;
    private final LeaveSeatRepository leaveSeatRepository;
    private final LeaveSeatScheduleRepository leaveSeatScheduleRepository;

    @Override
    public ScheduleType getScheduleType() {
        return ScheduleType.LEAVE_SEAT;
    }

    @Override
    public void settingSchedule(LocalDate baseDate) {
        List<LeaveSeatEntity> leaveSeats = leaveSeatRepository.findAllFromDate(baseDate);

        for (LeaveSeatEntity leaveSeat : leaveSeats) {
            List<StudentEntity> students = leaveSeat.getLeaveSeatStudents().stream()
                    .map(LeaveSeatStudentEntity::getStudent)
                    .toList();

            List<StudentScheduleEntity> studentSchedules = studentScheduleRepository
                    .findAllByStudentsAndDayAndPeriod(students, leaveSeat.getDay(), leaveSeat.getPeriod());

            for (StudentScheduleEntity studentSchedule : studentSchedules) {
                ScheduleEntity newSchedule = createNewSchedule(studentSchedule);
                createLeaveSeatSchedule(newSchedule, leaveSeat);
            }
        }
    }

    private ScheduleEntity createNewSchedule(StudentScheduleEntity studentSchedule) {
        Integer lastStackOrder = scheduleRepository.findLastStackOrderByStudentScheduleId(studentSchedule.getId());
        ScheduleEntity newSchedule = ScheduleEntity.createNewStudentSchedule(
                studentSchedule, lastStackOrder, ScheduleType.LEAVE_SEAT);
        return scheduleRepository.save(newSchedule);
    }

    private void createLeaveSeatSchedule(ScheduleEntity schedule, LeaveSeatEntity leaveSeat) {
        LeaveSeatScheduleEntity leaveSeatSchedule = LeaveSeatScheduleEntity.builder()
                .schedule(schedule)
                .leaveSeat(leaveSeat)
                .build();
        leaveSeatScheduleRepository.save(leaveSeatSchedule);
    }
}
