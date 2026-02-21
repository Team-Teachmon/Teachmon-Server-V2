package solvit.teachmon.domain.supervision.presentation.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.LocalDate;

@Builder
public record SupervisionScheduleCreateRequestDto(
        @NotNull(message = "감독 일정 날짜는 필수입니다.")
        LocalDate day,

        @JsonProperty("self_study_supervision_teacher_id")
        @NotNull(message = "자습 감독 교사 ID는 필수입니다.")
        Long selfStudySupervisionTeacherId,

        @JsonProperty("leave_seat_supervision_teacher_id")
        @NotNull(message = "이석 감독 교사 ID는 필수입니다.")
        Long leaveSeatSupervisionTeacherId,

        @JsonProperty("seventh_period_supervision_teacher_id")
        Long seventhPeriodSupervisionTeacherId
) {
}