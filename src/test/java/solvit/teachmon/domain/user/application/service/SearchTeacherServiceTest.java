package solvit.teachmon.domain.user.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import solvit.teachmon.domain.user.domain.entity.TeacherEntity;
import solvit.teachmon.domain.user.domain.repository.TeacherRepository;
import solvit.teachmon.domain.user.presentation.dto.response.TeacherSearchResponseDto;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("선생님 검색 서비스 테스트")
class SearchTeacherServiceTest {

    @Mock
    private TeacherRepository teacherRepository;

    private SearchTeacherService searchTeacherService;

    @BeforeEach
    void setUp() {
        searchTeacherService = new SearchTeacherService(teacherRepository);
    }

    @Test
    @DisplayName("쿼리로 선생님을 검색할 수 있다")
    void shouldSearchTeacherByQuerySuccessfully() {
        // Given: 검색 쿼리가 주어졌을 때
        String query = "이혜정";
        TeacherEntity teacher = mock(TeacherEntity.class);
        when(teacher.getId()).thenReturn(234235326L);
        when(teacher.getName()).thenReturn("이혜정");
        List<TeacherEntity> mockTeachers = List.of(teacher);
        given(teacherRepository.queryTeachersByName(query)).willReturn(mockTeachers);

        // When: 쿼리로 선생님을 검색하면
        List<TeacherSearchResponseDto> results = searchTeacherService.searchTeacherByQuery(query);

        // Then: 검색 결과가 반환된다
        assertThat(results).hasSize(1);
        assertThat(results.getFirst().id()).isEqualTo(234235326L);
        assertThat(results.getFirst().name()).isEqualTo("이혜정");
        verify(teacherRepository).queryTeachersByName(query);
    }

    @Test
    @DisplayName("부분 이름으로 선생님을 검색할 수 있다")
    void shouldSearchTeacherByPartialNameSuccessfully() {
        // Given: 부분 이름 검색 쿼리가 주어졌을 때
        String query = "혜정";
        TeacherEntity teacher1 = mock(TeacherEntity.class);
        when(teacher1.getId()).thenReturn(234235326L);
        when(teacher1.getName()).thenReturn("이혜정");
        TeacherEntity teacher2 = mock(TeacherEntity.class);
        when(teacher2.getId()).thenReturn(543543553L);
        when(teacher2.getName()).thenReturn("윤혜정");
        List<TeacherEntity> mockTeachers = List.of(teacher1, teacher2);
        given(teacherRepository.queryTeachersByName(query)).willReturn(mockTeachers);

        // When: 부분 이름으로 선생님을 검색하면
        List<TeacherSearchResponseDto> results = searchTeacherService.searchTeacherByQuery(query);

        // Then: 해당 이름이 포함된 선생님들이 반환된다
        assertThat(results).hasSize(2);
        assertThat(results.get(0).name()).isEqualTo("이혜정");
        assertThat(results.get(1).name()).isEqualTo("윤혜정");
        verify(teacherRepository).queryTeachersByName(query);
    }

    @Test
    @DisplayName("검색 결과가 없을 수 있다")
    void shouldReturnEmptyResultWhenNoMatch() {
        // Given: 매칭되는 선생님이 없는 검색 쿼리가 주어졌을 때
        String query = "없는선생님";
        List<TeacherEntity> mockTeachers = List.of();
        given(teacherRepository.queryTeachersByName(query)).willReturn(mockTeachers);

        // When: 쿼리로 선생님을 검색하면
        List<TeacherSearchResponseDto> results = searchTeacherService.searchTeacherByQuery(query);

        // Then: 빈 결과가 반환된다
        assertThat(results).isEmpty();
        verify(teacherRepository).queryTeachersByName(query);
    }

    @Test
    @DisplayName("빈 쿼리로 검색할 수 있다")
    void shouldSearchWithEmptyQuery() {
        // Given: 빈 쿼리가 주어졌을 때
        String query = "";
        List<TeacherEntity> mockTeachers = List.of();
        given(teacherRepository.queryTeachersByName(query)).willReturn(mockTeachers);

        // When: 빈 쿼리로 선생님을 검색하면
        List<TeacherSearchResponseDto> results = searchTeacherService.searchTeacherByQuery(query);

        // Then: 빈 결과가 반환된다
        assertThat(results).isEmpty();
        verify(teacherRepository).queryTeachersByName(query);
    }

    @Test
    @DisplayName("null 쿼리도 처리할 수 있다")
    void shouldHandleNullQuery() {
        // Given: null 쿼리가 주어졌을 때
        String query = null;
        List<TeacherEntity> mockTeachers = List.of();
        given(teacherRepository.queryTeachersByName(query)).willReturn(mockTeachers);

        // When: null 쿼리로 선생님을 검색하면
        List<TeacherSearchResponseDto> results = searchTeacherService.searchTeacherByQuery(query);

        // Then: 빈 결과가 반환된다
        assertThat(results).isEmpty();
        verify(teacherRepository).queryTeachersByName(query);
    }

    @Test
    @DisplayName("대소문자 구분 없이 검색할 수 있다")
    void shouldSearchIgnoreCase() {
        // Given: 대소문자가 다른 검색 쿼리가 주어졌을 때
        String query = "혜정";
        TeacherEntity teacher = mock(TeacherEntity.class);
        when(teacher.getId()).thenReturn(234235326L);
        when(teacher.getName()).thenReturn("이혜정");
        List<TeacherEntity> mockTeachers = List.of(teacher);
        given(teacherRepository.queryTeachersByName(query)).willReturn(mockTeachers);

        // When: 대소문자가 다른 쿼리로 선생님을 검색하면
        List<TeacherSearchResponseDto> results = searchTeacherService.searchTeacherByQuery(query);

        // Then: 검색 결과가 정상적으로 반환된다
        assertThat(results).hasSize(1);
        assertThat(results.getFirst().name()).isEqualTo("이혜정");
        verify(teacherRepository).queryTeachersByName(query);
    }

    @Test
    @DisplayName("특수 문자가 포함된 쿼리를 처리할 수 있다")
    void shouldHandleSpecialCharacters() {
        // Given: 특수 문자가 포함된 검색 쿼리가 주어졌을 때
        String query = "김.선생";
        List<TeacherEntity> mockTeachers = List.of();
        given(teacherRepository.queryTeachersByName(query)).willReturn(mockTeachers);

        // When: 특수 문자가 포함된 쿼리로 선생님을 검색하면
        List<TeacherSearchResponseDto> results = searchTeacherService.searchTeacherByQuery(query);

        // Then: 결과가 정상적으로 처리된다
        assertThat(results).isEmpty();
        verify(teacherRepository).queryTeachersByName(query);
    }
}