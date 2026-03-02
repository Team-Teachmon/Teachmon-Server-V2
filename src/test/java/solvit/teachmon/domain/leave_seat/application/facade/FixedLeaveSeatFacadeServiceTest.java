package solvit.teachmon.domain.leave_seat.application.facade;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import solvit.teachmon.domain.leave_seat.application.mapper.FixedLeaveSeatMapper;
import solvit.teachmon.domain.leave_seat.domain.entity.FixedLeaveSeatEntity;
import solvit.teachmon.domain.leave_seat.domain.entity.FixedLeaveSeatStudentEntity;
import solvit.teachmon.domain.leave_seat.domain.repository.FixedLeaveSeatRepository;
import solvit.teachmon.domain.leave_seat.domain.repository.FixedLeaveSeatStudentRepository;
import solvit.teachmon.domain.leave_seat.exception.FixedLeaveSeatNotFoundException;
import solvit.teachmon.domain.leave_seat.presentation.dto.request.FixedLeaveSeatCreateRequest;
import solvit.teachmon.domain.leave_seat.presentation.dto.request.FixedLeaveSeatUpdateRequest;
import solvit.teachmon.domain.leave_seat.presentation.dto.response.FixedLeaveSeatDetailResponse;
import solvit.teachmon.domain.leave_seat.presentation.dto.response.FixedLeaveSeatListResponse;
import solvit.teachmon.domain.management.student.domain.entity.StudentEntity;
import solvit.teachmon.domain.management.student.domain.repository.StudentRepository;
import solvit.teachmon.domain.management.student.exception.StudentNotFoundException;
import solvit.teachmon.domain.place.domain.entity.PlaceEntity;
import solvit.teachmon.domain.place.domain.repository.PlaceRepository;
import solvit.teachmon.domain.place.exception.PlaceNotFoundException;
import solvit.teachmon.domain.user.domain.entity.TeacherEntity;
import solvit.teachmon.domain.student_schedule.application.service.StudentScheduleSettingService;
import solvit.teachmon.global.enums.SchoolPeriod;
import solvit.teachmon.global.enums.WeekDay;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("고정 이석 서비스 테스트")
class FixedLeaveSeatFacadeServiceTest {

    @Mock
    private PlaceRepository placeRepository;
    @Mock
    private FixedLeaveSeatRepository fixedLeaveSeatRepository;
    @Mock
    private FixedLeaveSeatStudentRepository fixedLeaveSeatStudentRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private FixedLeaveSeatMapper fixedLeaveSeatMapper;
    @Mock
    private StudentScheduleSettingService studentScheduleSettingService;

    private FixedLeaveSeatFacadeService fixedLeaveSeatFacadeService;

    @BeforeEach
    void setUp() {
        fixedLeaveSeatFacadeService = new FixedLeaveSeatFacadeService(
                placeRepository,
                fixedLeaveSeatRepository,
                fixedLeaveSeatStudentRepository,
                studentRepository,
                fixedLeaveSeatMapper,
                studentScheduleSettingService
        );
    }

    @Test
    @DisplayName("고정 이석을 생성할 수 있다")
    void shouldCreateFixedLeaveSeat() {
        // Given: 고정 이석 생성 요청이 있을 때
        FixedLeaveSeatCreateRequest request = new FixedLeaveSeatCreateRequest(
                WeekDay.MON,
                SchoolPeriod.SEVEN_PERIOD,
                1L,
                "도서관 이용",
                List.of(1L, 2L)
        );

        PlaceEntity place = mock(PlaceEntity.class);
        TeacherEntity teacher = mock(TeacherEntity.class);
        given(teacher.hasStudentScheduleChangeAuthority()).willReturn(true);

        StudentEntity student1 = mock(StudentEntity.class);
        StudentEntity student2 = mock(StudentEntity.class);
        List<StudentEntity> students = Arrays.asList(student1, student2);

        FixedLeaveSeatEntity fixedLeaveSeat = mock(FixedLeaveSeatEntity.class);

        given(placeRepository.findById(1L)).willReturn(Optional.of(place));
        given(studentRepository.findAllById(List.of(1L, 2L))).willReturn(students);
        given(fixedLeaveSeatRepository.save(any(FixedLeaveSeatEntity.class))).willReturn(fixedLeaveSeat);

        // When: 고정 이석을 생성하면
        fixedLeaveSeatFacadeService.createStaticLeaveSeat(request, teacher);

        // Then: 고정 이석과 학생 관계가 저장된다
        verify(placeRepository, times(1)).findById(1L);
        verify(studentRepository, times(1)).findAllById(List.of(1L, 2L));
        verify(fixedLeaveSeatRepository, times(1)).save(any(FixedLeaveSeatEntity.class));
        verify(fixedLeaveSeatStudentRepository, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("존재하지 않는 장소로 고정 이석 생성 시 예외가 발생한다")
    void shouldThrowExceptionWhenPlaceNotFound() {
        // Given: 존재하지 않는 장소 ID로 요청이 있을 때
        FixedLeaveSeatCreateRequest request = new FixedLeaveSeatCreateRequest(
                WeekDay.MON,
                SchoolPeriod.SEVEN_PERIOD,
                999L,
                "도서관 이용",
                List.of(1L)
        );
        TeacherEntity teacher = mock(TeacherEntity.class);

        given(placeRepository.findById(999L)).willReturn(Optional.empty());

        // When & Then: 고정 이석 생성 시 예외가 발생한다
        assertThatThrownBy(() -> fixedLeaveSeatFacadeService.createStaticLeaveSeat(request, teacher))
                .isInstanceOf(PlaceNotFoundException.class);

        verify(placeRepository, times(1)).findById(999L);
        verify(fixedLeaveSeatRepository, never()).save(any());
    }

    @Test
    @DisplayName("존재하지 않는 학생으로 고정 이석 생성 시 예외가 발생한다")
    void shouldThrowExceptionWhenStudentNotFound() {
        // Given: 존재하지 않는 학생 ID로 요청이 있을 때
        FixedLeaveSeatCreateRequest request = new FixedLeaveSeatCreateRequest(
                WeekDay.MON,
                SchoolPeriod.SEVEN_PERIOD,
                1L,
                "도서관 이용",
                List.of(1L, 999L)
        );
        PlaceEntity place = mock(PlaceEntity.class);
        TeacherEntity teacher = mock(TeacherEntity.class);
        StudentEntity student1 = mock(StudentEntity.class);

        given(placeRepository.findById(1L)).willReturn(Optional.of(place));
        given(studentRepository.findAllById(List.of(1L, 999L))).willReturn(List.of(student1)); // 1개만 반환

        // When & Then: 고정 이석 생성 시 예외가 발생한다
        assertThatThrownBy(() -> fixedLeaveSeatFacadeService.createStaticLeaveSeat(request, teacher))
                .isInstanceOf(StudentNotFoundException.class);

        verify(studentRepository, times(1)).findAllById(List.of(1L, 999L));
    }

    @Test
    @DisplayName("고정 이석 목록을 조회할 수 있다")
    void shouldGetFixedLeaveSeatList() {
        // Given: 고정 이석이 존재할 때
        FixedLeaveSeatEntity fixedLeaveSeat1 = mock(FixedLeaveSeatEntity.class);
        FixedLeaveSeatEntity fixedLeaveSeat2 = mock(FixedLeaveSeatEntity.class);
        List<FixedLeaveSeatEntity> fixedLeaveSeats = Arrays.asList(fixedLeaveSeat1, fixedLeaveSeat2);

        FixedLeaveSeatStudentEntity student1 = mock(FixedLeaveSeatStudentEntity.class);
        List<FixedLeaveSeatStudentEntity> fixedLeaveSeatStudents1 = List.of(student1);

        FixedLeaveSeatStudentEntity student2 = mock(FixedLeaveSeatStudentEntity.class);
        List<FixedLeaveSeatStudentEntity> fixedLeaveSeatStudents2 = List.of(student2);

        FixedLeaveSeatListResponse response1 = mock(FixedLeaveSeatListResponse.class);
        FixedLeaveSeatListResponse response2 = mock(FixedLeaveSeatListResponse.class);

        given(fixedLeaveSeatRepository.findAllWithFetch()).willReturn(fixedLeaveSeats);
        given(fixedLeaveSeatStudentRepository.findAllByFixedLeaveSeatWithFetch(fixedLeaveSeat1))
                .willReturn(fixedLeaveSeatStudents1);
        given(fixedLeaveSeatStudentRepository.findAllByFixedLeaveSeatWithFetch(fixedLeaveSeat2))
                .willReturn(fixedLeaveSeatStudents2);
        given(fixedLeaveSeatMapper.toListResponse(fixedLeaveSeat1, fixedLeaveSeatStudents1)).willReturn(response1);
        given(fixedLeaveSeatMapper.toListResponse(fixedLeaveSeat2, fixedLeaveSeatStudents2)).willReturn(response2);

        // When: 고정 이석 목록을 조회하면
        List<FixedLeaveSeatListResponse> results = fixedLeaveSeatFacadeService.getStaticLeaveSeatList();

        // Then: 조회된 고정 이석 목록이 반환된다
        assertThat(results).hasSize(2);
        assertThat(results).containsExactly(response1, response2);

        verify(fixedLeaveSeatRepository, times(1)).findAllWithFetch();
        verify(fixedLeaveSeatStudentRepository, times(2)).findAllByFixedLeaveSeatWithFetch(any());
        verify(fixedLeaveSeatMapper, times(2)).toListResponse(any(), anyList());
    }

    @Test
    @DisplayName("고정 이석이 없을 때 빈 목록이 반환된다")
    void shouldReturnEmptyListWhenNoFixedLeaveSeat() {
        // Given: 고정 이석이 없을 때
        given(fixedLeaveSeatRepository.findAllWithFetch()).willReturn(List.of());

        // When: 고정 이석 목록을 조회하면
        List<FixedLeaveSeatListResponse> results = fixedLeaveSeatFacadeService.getStaticLeaveSeatList();

        // Then: 빈 목록이 반환된다
        assertThat(results).isEmpty();

        verify(fixedLeaveSeatRepository, times(1)).findAllWithFetch();
        verify(fixedLeaveSeatStudentRepository, never()).findAllByFixedLeaveSeatWithFetch(any());
        verify(fixedLeaveSeatMapper, never()).toListResponse(any(), anyList());
    }

    @Test
    @DisplayName("고정 이석 상세 정보를 조회할 수 있다")
    void shouldGetFixedLeaveSeatDetail() {
        // Given: 고정 이석이 존재할 때
        Long fixedLeaveSeatId = 1L;
        FixedLeaveSeatEntity fixedLeaveSeat = mock(FixedLeaveSeatEntity.class);

        StudentEntity student1 = mock(StudentEntity.class);
        StudentEntity student2 = mock(StudentEntity.class);
        List<StudentEntity> students = Arrays.asList(student1, student2);

        FixedLeaveSeatStudentEntity fixedLeaveSeatStudent1 = mock(FixedLeaveSeatStudentEntity.class);
        FixedLeaveSeatStudentEntity fixedLeaveSeatStudent2 = mock(FixedLeaveSeatStudentEntity.class);
        List<FixedLeaveSeatStudentEntity> fixedLeaveSeatStudents = Arrays.asList(
                fixedLeaveSeatStudent1, fixedLeaveSeatStudent2
        );

        FixedLeaveSeatDetailResponse response = mock(FixedLeaveSeatDetailResponse.class);

        given(fixedLeaveSeatRepository.findByIdWithFetch(fixedLeaveSeatId))
                .willReturn(Optional.of(fixedLeaveSeat));
        given(fixedLeaveSeatStudentRepository.findAllByFixedLeaveSeatWithFetch(fixedLeaveSeat))
                .willReturn(fixedLeaveSeatStudents);
        given(fixedLeaveSeatStudent1.getStudent()).willReturn(student1);
        given(fixedLeaveSeatStudent2.getStudent()).willReturn(student2);
        given(fixedLeaveSeatMapper.toDetailResponse(fixedLeaveSeat, students)).willReturn(response);

        // When: 고정 이석 상세 정보를 조회하면
        FixedLeaveSeatDetailResponse result = fixedLeaveSeatFacadeService.getStaticLeaveSeatDetail(fixedLeaveSeatId);

        // Then: 상세 정보가 반환된다
        assertThat(result).isEqualTo(response);

        verify(fixedLeaveSeatRepository, times(1)).findByIdWithFetch(fixedLeaveSeatId);
        verify(fixedLeaveSeatStudentRepository, times(1)).findAllByFixedLeaveSeatWithFetch(fixedLeaveSeat);
        verify(fixedLeaveSeatMapper, times(1)).toDetailResponse(fixedLeaveSeat, students);
    }

    @Test
    @DisplayName("존재하지 않는 고정 이석 조회 시 예외가 발생한다")
    void shouldThrowExceptionWhenFixedLeaveSeatNotFoundOnDetail() {
        // Given: 존재하지 않는 고정 이석 ID로 조회할 때
        Long fixedLeaveSeatId = 999L;

        given(fixedLeaveSeatRepository.findByIdWithFetch(fixedLeaveSeatId)).willReturn(Optional.empty());

        // When & Then: 예외가 발생한다
        assertThatThrownBy(() -> fixedLeaveSeatFacadeService.getStaticLeaveSeatDetail(fixedLeaveSeatId))
                .isInstanceOf(FixedLeaveSeatNotFoundException.class);

        verify(fixedLeaveSeatRepository, times(1)).findByIdWithFetch(fixedLeaveSeatId);
    }

    @Test
    @DisplayName("고정 이석을 수정할 수 있다")
    void shouldUpdateFixedLeaveSeat() {
        // Given: 수정할 고정 이석과 요청이 있을 때
        Long fixedLeaveSeatId = 1L;
        FixedLeaveSeatUpdateRequest request = new FixedLeaveSeatUpdateRequest(
                WeekDay.TUE,
                SchoolPeriod.EIGHT_AND_NINE_PERIOD,
                2L,
                "변경된 사유",
                List.of(3L, 4L)
        );

        FixedLeaveSeatEntity fixedLeaveSeat = mock(FixedLeaveSeatEntity.class);
        PlaceEntity newPlace = mock(PlaceEntity.class);
        TeacherEntity teacher = mock(TeacherEntity.class);
        lenient().when(teacher.hasStudentScheduleChangeAuthority()).thenReturn(true);

        StudentEntity student1 = mock(StudentEntity.class);
        StudentEntity student2 = mock(StudentEntity.class);
        List<StudentEntity> students = Arrays.asList(student1, student2);

        given(fixedLeaveSeatRepository.findById(fixedLeaveSeatId)).willReturn(Optional.of(fixedLeaveSeat));
        given(placeRepository.findById(2L)).willReturn(Optional.of(newPlace));
        given(studentRepository.findAllById(List.of(3L, 4L))).willReturn(students);

        // When: 고정 이석을 수정하면
        fixedLeaveSeatFacadeService.updateStaticLeaveSeat(fixedLeaveSeatId, request, teacher);

        // Then: 기존 학생 관계가 삭제되고 새로운 데이터가 저장된다
        verify(fixedLeaveSeatRepository, times(1)).findById(fixedLeaveSeatId);
        verify(placeRepository, times(1)).findById(2L);
        verify(fixedLeaveSeatStudentRepository, times(1)).deleteAllByFixedLeaveSeatId(fixedLeaveSeatId);
        verify(fixedLeaveSeat, times(1)).updateFixedLeaveSeatInfo(
                teacher, newPlace, WeekDay.TUE, SchoolPeriod.EIGHT_AND_NINE_PERIOD, "변경된 사유"
        );
        verify(fixedLeaveSeatStudentRepository, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("존재하지 않는 고정 이석 수정 시 예외가 발생한다")
    void shouldThrowExceptionWhenFixedLeaveSeatNotFoundOnUpdate() {
        // Given: 존재하지 않는 고정 이석 ID로 수정 요청이 있을 때
        Long fixedLeaveSeatId = 999L;
        FixedLeaveSeatUpdateRequest request = new FixedLeaveSeatUpdateRequest(
                WeekDay.MON,
                SchoolPeriod.SEVEN_PERIOD,
                1L,
                "사유",
                List.of(1L)
        );
        TeacherEntity teacher = mock(TeacherEntity.class);

        given(fixedLeaveSeatRepository.findById(fixedLeaveSeatId)).willReturn(Optional.empty());

        // When & Then: 예외가 발생한다
        assertThatThrownBy(() -> fixedLeaveSeatFacadeService.updateStaticLeaveSeat(fixedLeaveSeatId, request, teacher))
                .isInstanceOf(FixedLeaveSeatNotFoundException.class);

        verify(fixedLeaveSeatRepository, times(1)).findById(fixedLeaveSeatId);
    }

    @Test
    @DisplayName("수정 시 존재하지 않는 장소로 예외가 발생한다")
    void shouldThrowExceptionWhenPlaceNotFoundOnUpdate() {
        // Given: 수정 시 존재하지 않는 장소 ID가 있을 때
        Long fixedLeaveSeatId = 1L;
        FixedLeaveSeatUpdateRequest request = new FixedLeaveSeatUpdateRequest(
                WeekDay.MON,
                SchoolPeriod.SEVEN_PERIOD,
                999L,
                "사유",
                List.of(1L)
        );

        FixedLeaveSeatEntity fixedLeaveSeat = mock(FixedLeaveSeatEntity.class);
        TeacherEntity teacher = mock(TeacherEntity.class);

        given(fixedLeaveSeatRepository.findById(fixedLeaveSeatId)).willReturn(Optional.of(fixedLeaveSeat));
        given(placeRepository.findById(999L)).willReturn(Optional.empty());

        // When & Then: 예외가 발생한다
        assertThatThrownBy(() -> fixedLeaveSeatFacadeService.updateStaticLeaveSeat(fixedLeaveSeatId, request, teacher))
                .isInstanceOf(PlaceNotFoundException.class);

        verify(placeRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("고정 이석을 삭제할 수 있다")
    void shouldDeleteFixedLeaveSeat() {
        // Given: 삭제할 고정 이석이 존재할 때
        Long fixedLeaveSeatId = 1L;
        FixedLeaveSeatEntity fixedLeaveSeat = mock(FixedLeaveSeatEntity.class);

        given(fixedLeaveSeatRepository.findById(fixedLeaveSeatId)).willReturn(Optional.of(fixedLeaveSeat));

        // When: 고정 이석을 삭제하면
        fixedLeaveSeatFacadeService.deleteStaticLeaveSeat(fixedLeaveSeatId);

        // Then: 고정 이석이 삭제된다
        verify(fixedLeaveSeatRepository, times(1)).findById(fixedLeaveSeatId);
        verify(fixedLeaveSeatRepository, times(1)).delete(fixedLeaveSeat);
    }

    @Test
    @DisplayName("존재하지 않는 고정 이석 삭제 시 예외가 발생한다")
    void shouldThrowExceptionWhenFixedLeaveSeatNotFoundOnDelete() {
        // Given: 존재하지 않는 고정 이석 ID로 삭제 요청이 있을 때
        Long fixedLeaveSeatId = 999L;

        given(fixedLeaveSeatRepository.findById(fixedLeaveSeatId)).willReturn(Optional.empty());

        // When & Then: 예외가 발생한다
        assertThatThrownBy(() -> fixedLeaveSeatFacadeService.deleteStaticLeaveSeat(fixedLeaveSeatId))
                .isInstanceOf(FixedLeaveSeatNotFoundException.class);

        verify(fixedLeaveSeatRepository, times(1)).findById(fixedLeaveSeatId);
        verify(fixedLeaveSeatRepository, never()).delete(any());
    }

    @Test
    @DisplayName("여러 요일과 교시에 대한 고정 이석을 생성할 수 있다")
    void shouldCreateFixedLeaveSeatForDifferentDaysAndPeriods() {
        // Given: 다양한 요일과 교시의 고정 이석 생성 요청이 있을 때
        FixedLeaveSeatCreateRequest mondayRequest = new FixedLeaveSeatCreateRequest(
                WeekDay.MON,
                SchoolPeriod.SEVEN_PERIOD,
                1L,
                "월요일 도서관",
                List.of(1L)
        );

        FixedLeaveSeatCreateRequest tuesdayRequest = new FixedLeaveSeatCreateRequest(
                WeekDay.TUE,
                SchoolPeriod.EIGHT_AND_NINE_PERIOD,
                1L,
                "화요일 도서관",
                List.of(1L)
        );

        PlaceEntity place = mock(PlaceEntity.class);
        TeacherEntity teacher = mock(TeacherEntity.class);
        given(teacher.hasStudentScheduleChangeAuthority()).willReturn(true);

        StudentEntity student = mock(StudentEntity.class);
        FixedLeaveSeatEntity fixedLeaveSeat = mock(FixedLeaveSeatEntity.class);

        given(placeRepository.findById(1L)).willReturn(Optional.of(place));
        given(studentRepository.findAllById(List.of(1L))).willReturn(List.of(student));
        given(fixedLeaveSeatRepository.save(any(FixedLeaveSeatEntity.class))).willReturn(fixedLeaveSeat);

        // When: 월요일과 화요일 고정 이석을 각각 생성하면
        fixedLeaveSeatFacadeService.createStaticLeaveSeat(mondayRequest, teacher);
        fixedLeaveSeatFacadeService.createStaticLeaveSeat(tuesdayRequest, teacher);

        // Then: 두 개의 고정 이석이 생성된다
        verify(fixedLeaveSeatRepository, times(2)).save(any(FixedLeaveSeatEntity.class));
        verify(fixedLeaveSeatStudentRepository, times(2)).saveAll(anyList());
    }
}
