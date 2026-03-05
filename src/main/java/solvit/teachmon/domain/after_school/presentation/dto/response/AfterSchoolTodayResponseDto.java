package solvit.teachmon.domain.after_school.presentation.dto.response;

import java.util.List;

public record AfterSchoolTodayResponseDto(
        Long id,
        Integer branch,
        String name,
        PlaceInfo place,
        Integer grade,
        String period,
        String day,
        List<StudentInfo> students
) {
    public record PlaceInfo(
            Long id,
            String name
    ) {
    }
}
