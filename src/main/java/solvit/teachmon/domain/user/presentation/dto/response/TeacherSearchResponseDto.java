package solvit.teachmon.domain.user.presentation.dto.response;

import lombok.Builder;

@Builder
public record TeacherSearchResponseDto(
        Long id,
        String name
) {
}