package solvit.teachmon.domain.student_schedule.domain.repository.schedules;

import solvit.teachmon.domain.student_schedule.application.dto.PlaceScheduleDto;
import solvit.teachmon.domain.student_schedule.application.dto.StudentScheduleDto;
import solvit.teachmon.domain.student_schedule.domain.entity.ScheduleEntity;
import solvit.teachmon.global.enums.SchoolPeriod;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface AfterSchoolScheduleQueryDslRepository {
    Map<Integer, Long> getAfterSchoolPlaceCount(List<ScheduleEntity> schedules);
    Map<Integer, Long> getAfterSchoolReinforcementPlaceCount(List<ScheduleEntity> schedules);
    List<PlaceScheduleDto> getPlaceScheduleByFloor(List<ScheduleEntity> schedules, Integer floor);
    List<PlaceScheduleDto> getReinforcementPlaceScheduleByFloor(List<ScheduleEntity> schedules, Integer floor);
    List<StudentScheduleDto> getStudentScheduleByPlaceAndDayAndPeriod(Long placeId, LocalDate day, SchoolPeriod period);
    List<StudentScheduleDto> getReinforcementStudentScheduleByPlaceAndDayAndPeriod(Long placeId, LocalDate day, SchoolPeriod period);
}
