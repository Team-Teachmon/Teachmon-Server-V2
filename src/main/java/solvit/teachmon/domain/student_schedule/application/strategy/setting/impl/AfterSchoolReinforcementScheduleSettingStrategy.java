package solvit.teachmon.domain.student_schedule.application.strategy.setting.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import solvit.teachmon.domain.after_school.domain.entity.AfterSchoolEntity;
import solvit.teachmon.domain.after_school.domain.entity.AfterSchoolReinforcementEntity;
import solvit.teachmon.domain.after_school.domain.repository.AfterSchoolReinforcementRepository;
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
import java.util.List;

@Component
@RequiredArgsConstructor
public class AfterSchoolReinforcementScheduleSettingStrategy implements StudentScheduleSettingStrategy {
    private final ScheduleRepository scheduleRepository;
    private final AfterSchoolReinforcementRepository afterSchoolReinforcementRepository;
    private final AfterSchoolScheduleRepository afterSchoolScheduleRepository;
    private final StudentScheduleRepository studentScheduleRepository;

    @Override
    public ScheduleType getScheduleType() {
        return ScheduleType.AFTER_SCHOOL_REINFORCEMENT;
    }

    @Override
    public void settingSchedule(LocalDate baseDate) {
        List<AfterSchoolReinforcementEntity> afterSchoolReinforcements = findWeeklyAfterSchoolReinforcements(baseDate);

        for(AfterSchoolReinforcementEntity reinforcement : afterSchoolReinforcements) {
            if(isBeforeAfterSchoolReinforcement(reinforcement, baseDate))
                continue;
            AfterSchoolEntity afterSchool = reinforcement.getAfterSchool();
            List<StudentScheduleEntity> studentSchedules = findStudentScheduleByReinforcement(reinforcement);
            settingAfterSchoolReinforcementSchedule(studentSchedules, afterSchool);
        }
    }

    private Boolean isBeforeAfterSchoolReinforcement(AfterSchoolReinforcementEntity reinforcement, LocalDate baseDate) {
        return reinforcement.getChangeDay().isBefore(baseDate);
    }

    private List<AfterSchoolReinforcementEntity> findWeeklyAfterSchoolReinforcements(LocalDate baseDate) {
        LocalDate startDay = baseDate.with(DayOfWeek.MONDAY);
        LocalDate endDay = baseDate.with(DayOfWeek.SUNDAY);

        return afterSchoolReinforcementRepository.findAllByChangeDayBetween(startDay, endDay);
    }

    private List<StudentScheduleEntity> findStudentScheduleByReinforcement(AfterSchoolReinforcementEntity reinforcement) {
        return studentScheduleRepository.findAllByAfterSchoolAndDayAndPeriod(
                reinforcement.getAfterSchool(),
                reinforcement.getChangeDay(),
                reinforcement.getChangePeriod()
        );
    }

    private void settingAfterSchoolReinforcementSchedule(
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
        // Create a regular after-school schedule (not a special reinforcement type)
        AfterSchoolScheduleEntity afterSchoolSchedule = AfterSchoolScheduleEntity.builder()
                .schedule(schedule)
                .afterSchool(afterSchool)
                .build();

        afterSchoolScheduleRepository.save(afterSchoolSchedule);
    }

    private ScheduleEntity createNewSchedule(StudentScheduleEntity studentSchedule) {
        // Create a regular AFTER_SCHOOL schedule (the "reinforcement" is just that it's on a different day/period)
        Integer lastStackOrder = scheduleRepository.findLastStackOrderByStudentScheduleId(studentSchedule.getId());
        ScheduleEntity newSchedule = ScheduleEntity.createNewStudentSchedule(studentSchedule, lastStackOrder, ScheduleType.AFTER_SCHOOL_REINFORCEMENT);

        scheduleRepository.save(newSchedule);

        return newSchedule;
    }
}
