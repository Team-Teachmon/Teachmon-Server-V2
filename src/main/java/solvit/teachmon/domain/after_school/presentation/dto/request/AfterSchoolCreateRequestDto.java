package solvit.teachmon.domain.after_school.presentation.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import solvit.teachmon.global.enums.SchoolPeriod;
import solvit.teachmon.global.enums.WeekDay;

import java.util.List;

public record AfterSchoolCreateRequestDto(
        @NotNull(message = "연도는 필수입니다.")
        @Min(value = 2000, message = "연도는 2000년 이상이어야 합니다.")
        @Max(value = 2100, message = "연도는 2100년 이하여야 합니다.")
        Integer year,

        @NotNull(message = "학년은 필수입니다.")
        @Min(value = 1, message = "학년은 1부터 3까지만 가능합니다.")
        @Max(value = 3, message = "학년은 1부터 3까지만 가능합니다.")
        Integer grade,

        @NotNull(message = "분기는 필수입니다.")
        @Min(value = 1, message = "분기는 1부터 4까지만 가능합니다.")
        @Max(value = 4, message = "분기는 1부터 4까지만 가능합니다.")
        Integer branch,

        @NotNull(message = "요일은 필수입니다.")
        @JsonProperty("week_day")
        WeekDay weekDay,

        @NotNull(message = "교시는 필수입니다.")
        SchoolPeriod period,

        @NotNull(message = "담당 선생님은 필수입니다.")
        @JsonProperty("teacher_id")
        Long teacherId,

        @NotNull(message = "장소는 필수입니다.")
        @JsonProperty("place_id")
        Long placeId,

        @NotBlank(message = "방과후 이름은 필수입니다.")
        String name,

        @JsonProperty("students_id")
        List<Long> studentsId
) {
}
