package solvit.teachmon.domain.supervision.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.time.LocalDate;

@Builder
public record SupervisionScheduleResponseDto(
        @JsonProperty("day")
        LocalDate day,

        @JsonProperty("self_study_supervision")
        SupervisionInfo selfStudySupervision,

        @JsonProperty("leave_seat_supervision")
        SupervisionInfo leaveSeatSupervision,

        @JsonProperty("seventh_period_supervision")
        SupervisionInfo seventhPeriodSupervision
) {
    @Builder
    public record SupervisionInfo(
            @JsonProperty("id")
            Long id,

            @JsonProperty("teacher")
            TeacherInfo teacher
    ) {
        @Builder
        public record TeacherInfo(
                @JsonProperty("id")
                Long id,

                @JsonProperty("name")
                String name
        ) {}
    }
}