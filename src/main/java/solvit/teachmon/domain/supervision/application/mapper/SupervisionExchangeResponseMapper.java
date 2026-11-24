package solvit.teachmon.domain.supervision.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import solvit.teachmon.domain.supervision.domain.entity.SupervisionExchangeEntity;
import solvit.teachmon.domain.supervision.domain.entity.SupervisionScheduleEntity;
import solvit.teachmon.domain.supervision.domain.enums.SupervisionType;
import solvit.teachmon.domain.supervision.presentation.dto.response.SupervisionExchangeResponseDto;
import solvit.teachmon.domain.user.domain.entity.TeacherEntity;

@Mapper(componentModel = "spring")
public interface SupervisionExchangeResponseMapper {

    /**
     * 교사는 일정의 현재 담당자가 아니라 교체 요청의 송신자/수신자를 사용한다.
     * (수락하면 일정 담당자가 서로 바뀌어 요청자/대상자가 뒤바뀌어 보이기 때문)
     */
    default SupervisionExchangeResponseDto toResponseDto(SupervisionExchangeEntity exchange) {
        return SupervisionExchangeResponseDto.builder()
                .id(exchange.getId())
                .requestor(toSupervisionInfo(exchange.getSender(), exchange.getSenderSchedule()))
                .responser(toSupervisionInfo(exchange.getRecipient(), exchange.getRecipientSchedule()))
                .status(exchange.getState())
                .reason(exchange.getReason())
                .build();
    }

    default SupervisionExchangeResponseDto.SupervisionInfo toSupervisionInfo(TeacherEntity teacher, SupervisionScheduleEntity schedule) {
        return SupervisionExchangeResponseDto.SupervisionInfo.builder()
                .teacher(toTeacherInfo(teacher))
                .day(schedule.getDay())
                .type(mapSupervisionType(schedule.getType()))
                .build();
    }

    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    SupervisionExchangeResponseDto.SupervisionInfo.TeacherInfo toTeacherInfo(TeacherEntity teacher);

    default String mapSupervisionType(SupervisionType type) {
        return switch (type) {
            case SELF_STUDY_SUPERVISION -> "self_study";
            case LEAVE_SEAT_SUPERVISION -> "leave_seat";
            case SEVENTH_PERIOD_SUPERVISION -> "seventh_period";
        };
    }
}
