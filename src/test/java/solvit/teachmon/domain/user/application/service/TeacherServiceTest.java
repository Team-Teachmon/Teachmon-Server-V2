package solvit.teachmon.domain.user.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import solvit.teachmon.domain.user.domain.repository.TeacherRepository;
import solvit.teachmon.domain.user.exception.TeacherNotFoundException;
import solvit.teachmon.domain.user.presentation.dto.response.TeacherProfileResponseDto;
import solvit.teachmon.global.security.user.TeachmonUserDetails;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("교사 서비스 테스트")
class TeacherServiceTest {

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private TeachmonUserDetails teachmonUserDetails;

    private TeacherService teacherService;

    @BeforeEach
    void setUp() {
        teacherService = new TeacherService(teacherRepository);
    }

    @Test
    @DisplayName("교사 프로필을 조회할 수 있다")
    void shouldGetTeacherProfileSuccessfully() {
        // Given: 교사 ID가 주어졌을 때
        Long teacherId = 1L;
        TeacherProfileResponseDto expectedProfile = new TeacherProfileResponseDto(
                teacherId, "김교사", "수학 전담", null
        );
        
        given(teachmonUserDetails.getId()).willReturn(teacherId);
        given(teacherRepository.findUserProfileById(teacherId)).willReturn(Optional.of(expectedProfile));

        // When: 교사 프로필을 조회하면
        TeacherProfileResponseDto result = teacherService.getMyUserProfile(teachmonUserDetails);

        // Then: 프로필 정보가 반환된다
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(teacherId);
        assertThat(result.name()).isEqualTo("김교사");
        assertThat(result.profile()).isEqualTo("수학 전담");
        
        verify(teachmonUserDetails).getId();
        verify(teacherRepository).findUserProfileById(teacherId);
    }

    @Test
    @DisplayName("존재하지 않는 교사 ID로 프로필 조회 시 예외가 발생한다")
    void shouldThrowExceptionWhenTeacherNotFound() {
        // Given: 존재하지 않는 교사 ID가 주어졌을 때
        Long nonExistentTeacherId = 999L;
        
        given(teachmonUserDetails.getId()).willReturn(nonExistentTeacherId);
        given(teacherRepository.findUserProfileById(nonExistentTeacherId)).willReturn(Optional.empty());

        // When & Then: 교사 프로필을 조회하면 예외가 발생한다
        assertThatThrownBy(() -> teacherService.getMyUserProfile(teachmonUserDetails))
                .isInstanceOf(TeacherNotFoundException.class);
        
        verify(teachmonUserDetails).getId();
        verify(teacherRepository).findUserProfileById(nonExistentTeacherId);
    }
}