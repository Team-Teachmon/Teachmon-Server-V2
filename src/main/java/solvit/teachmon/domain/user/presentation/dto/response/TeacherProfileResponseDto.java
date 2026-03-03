package solvit.teachmon.domain.user.presentation.dto.response;

import com.querydsl.core.annotations.QueryProjection;
import solvit.teachmon.domain.user.domain.enums.Role;

@QueryProjection
public record TeacherProfileResponseDto(
        Long id,
        String name,
        String profile,
        Role role
) {}
