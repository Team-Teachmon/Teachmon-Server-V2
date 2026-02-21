package solvit.teachmon.domain.supervision.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import solvit.teachmon.domain.supervision.domain.entity.SupervisionScheduleEntity;
import solvit.teachmon.domain.supervision.presentation.dto.response.SupervisionScheduleResponseDto;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface SupervisionResponseMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "teacher.id", source = "teacher.id")
    @Mapping(target = "teacher.name", source = "teacher.name")
    SupervisionScheduleResponseDto.SupervisionInfo toSupervisionInfo(SupervisionScheduleEntity schedule);

    default List<SupervisionScheduleResponseDto> convertToResponseDtos(List<SupervisionScheduleEntity> schedules) {
        Map<LocalDate, List<SupervisionScheduleEntity>> schedulesByDay = 
                schedules.stream().collect(Collectors.groupingBy(SupervisionScheduleEntity::getDay));
                
        return schedulesByDay.entrySet().stream()
                .map(this::createDayResponseDto)
                .sorted(Comparator.comparing(SupervisionScheduleResponseDto::day))
                .toList();
    }

    default SupervisionScheduleResponseDto createDayResponseDto(Map.Entry<LocalDate, List<SupervisionScheduleEntity>> entry) {
        LocalDate day = entry.getKey();
        List<SupervisionScheduleEntity> daySchedules = entry.getValue();

        return SupervisionScheduleResponseDto.builder()
                .day(day)
                .selfStudySupervision(findSupervisionByType(daySchedules, solvit.teachmon.domain.supervision.domain.enums.SupervisionType.SELF_STUDY_SUPERVISION))
                .leaveSeatSupervision(findSupervisionByType(daySchedules, solvit.teachmon.domain.supervision.domain.enums.SupervisionType.LEAVE_SEAT_SUPERVISION))
                .seventhPeriodSupervision(findSupervisionByType(daySchedules, solvit.teachmon.domain.supervision.domain.enums.SupervisionType.SEVENTH_PERIOD_SUPERVISION))
                .build();
    }

    default SupervisionScheduleResponseDto.SupervisionInfo findSupervisionByType(
            List<SupervisionScheduleEntity> schedules, solvit.teachmon.domain.supervision.domain.enums.SupervisionType type) {
        return schedules.stream()
                .filter(schedule -> schedule.getType() == type)
                .findFirst()
                .map(this::toSupervisionInfo)
                .orElse(null);
    }
}