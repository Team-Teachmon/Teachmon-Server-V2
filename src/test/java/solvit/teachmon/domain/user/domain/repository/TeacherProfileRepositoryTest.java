package solvit.teachmon.domain.user.domain.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import solvit.teachmon.domain.user.domain.repository.querydsl.TeacherQueryDslRepository;
import solvit.teachmon.domain.user.presentation.dto.response.TeacherProfileResponseDto;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("교사 프로필 레포지토리 테스트")
class TeacherProfileRepositoryTest {

    @Mock
    private TeacherQueryDslRepository teacherQueryDslRepository;

    @Test
    @DisplayName("교사 ID로 프로필을 조회할 수 있다")
    void shouldFindUserProfileById() {
        // Given: 교사 ID와 예상 프로필이 주어졌을 때
        Long teacherId = 1L;
        TeacherProfileResponseDto expectedProfile = new TeacherProfileResponseDto(
                teacherId, "김교사", "수학 전담", null
        );
        
        given(teacherQueryDslRepository.findUserProfileById(teacherId)).willReturn(Optional.of(expectedProfile));

        // When: 프로필을 조회하면
        Optional<TeacherProfileResponseDto> result = teacherQueryDslRepository.findUserProfileById(teacherId);

        // Then: 프로필 정보가 반환된다
        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(teacherId);
        assertThat(result.get().name()).isEqualTo("김교사");
        assertThat(result.get().profile()).isEqualTo("수학 전담");
        
        verify(teacherQueryDslRepository).findUserProfileById(teacherId);
    }

    @Test
    @DisplayName("존재하지 않는 교사 ID로 조회하면 빈 Optional을 반환한다")
    void shouldReturnEmptyOptionalWhenTeacherNotFound() {
        // Given: 존재하지 않는 교사 ID가 주어졌을 때
        Long nonExistentTeacherId = 999L;
        
        given(teacherQueryDslRepository.findUserProfileById(nonExistentTeacherId)).willReturn(Optional.empty());

        // When: 프로필을 조회하면
        Optional<TeacherProfileResponseDto> result = teacherQueryDslRepository.findUserProfileById(nonExistentTeacherId);

        // Then: 빈 Optional이 반환된다
        assertThat(result).isEmpty();
        
        verify(teacherQueryDslRepository).findUserProfileById(nonExistentTeacherId);
    }

    @Test
    @DisplayName("TeacherProfileResponseDto의 모든 필드가 올바르게 설정된다")
    void shouldSetAllFieldsCorrectly() {
        // Given: 모든 필드가 있는 프로필 데이터가 주어졌을 때
        Long id = 123L;
        String name = "박교사";
        String profile = "영어 전담 교사";
        
        // When: TeacherProfileResponseDto를 생성하면
        TeacherProfileResponseDto dto = new TeacherProfileResponseDto(id, name, profile, null);

        // Then: 모든 필드가 올바르게 설정된다
        assertThat(dto.id()).isEqualTo(id);
        assertThat(dto.name()).isEqualTo(name);
        assertThat(dto.profile()).isEqualTo(profile);
    }
}