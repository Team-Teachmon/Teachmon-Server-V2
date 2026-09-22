package solvit.teachmon.domain.leave_seat.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import solvit.teachmon.domain.leave_seat.domain.entity.LeaveSeatEntity;
import solvit.teachmon.domain.leave_seat.domain.entity.LeaveSeatStudentEntity;
import solvit.teachmon.domain.leave_seat.presentation.dto.response.LeaveSeatDetailResponse;
import solvit.teachmon.domain.leave_seat.presentation.dto.response.LeaveSeatListResponse;
import solvit.teachmon.domain.leave_seat.presentation.dto.response.StudentInfoResponse;
import solvit.teachmon.domain.management.student.domain.entity.StudentEntity;
import solvit.teachmon.domain.student_schedule.domain.enums.ScheduleType;

import java.util.List;
import java.util.Map;

@Mapper(componentModel = "spring")
public interface LeaveSeatMapper {

    @Mapping(target = "leaveSeatId", source = "leaveSeat.id")
    @Mapping(target = "period", source = "leaveSeat.period")
    @Mapping(target = "teacher", source = "leaveSeat.teacher.name")
    @Mapping(target = "place", source = "leaveSeat.place.name")
    @Mapping(target = "personnel", expression = "java(leaveSeatStudents.size())")
    @Mapping(target = "students", source = "leaveSeatStudents")
    LeaveSeatListResponse toListResponse(
            LeaveSeatEntity leaveSeat,
            List<LeaveSeatStudentEntity> leaveSeatStudents
    );

    @Mapping(target = "day", source = "leaveSeat.day")
    @Mapping(target = "teacher", source = "leaveSeat.teacher.name")
    @Mapping(target = "period", source = "leaveSeat.period")
    @Mapping(target = "place.id", source = "leaveSeat.place.id")
    @Mapping(target = "place.name", source = "leaveSeat.place.name")
    @Mapping(target = "cause", source = "leaveSeat.cause")
    @Mapping(target = "students", expression = "java(mapStudentInfosWithScheduleTypes(students, studentLastScheduleTypes))")
    LeaveSeatDetailResponse toDetailResponse(
            LeaveSeatEntity leaveSeat,
            List<StudentEntity> students,
            Map<Long, ScheduleType> studentLastScheduleTypes
    );

    default String toStudentName(LeaveSeatStudentEntity entity) {
        return entity.getStudent().calculateStudentNumber()
                + entity.getStudent().getName();
    }

    List<String> toStudentNames(List<LeaveSeatStudentEntity> entities);

    default List<StudentInfoResponse> mapStudentInfosWithScheduleTypes(
            List<StudentEntity> students,
            Map<Long, ScheduleType> studentLastScheduleTypes
    ) {
        return students.stream()
                .map(student -> {
                    ScheduleType scheduleType = studentLastScheduleTypes.get(student.getId());
                    String state = scheduleType.name();

                    return StudentInfoResponse.builder()
                            .id(student.getId())
                            .number(student.calculateStudentNumber())
                            .name(student.getName())
                            .state(state)
                            .build();
                })
                .toList();
    }
}
