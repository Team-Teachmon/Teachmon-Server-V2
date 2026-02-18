package solvit.teachmon.domain.user.domain.repository.querydsl;

import solvit.teachmon.domain.user.presentation.dto.response.TeacherProfileResponseDto;

import java.util.Optional;

public interface TeacherQueryDslRepository {
    Optional<TeacherProfileResponseDto> findUserProfileById(Long id);
}
