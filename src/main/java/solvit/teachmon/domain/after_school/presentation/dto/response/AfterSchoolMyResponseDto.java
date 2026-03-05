package solvit.teachmon.domain.after_school.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AfterSchoolMyResponseDto(
        Long id,
        @JsonProperty("week_day")
        String weekDay,
        String period,
        String name,
        PlaceInfo place,
        @JsonProperty("reinforcement_count")
        Integer reinforcementCount,
        List<StudentInfo> students
) {
    public record PlaceInfo(
            Long id,
            String name
    ) {
    }
}
