package solvit.teachmon.domain.leave_seat.application.facade;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import solvit.teachmon.domain.leave_seat.application.mapper.LeaveSeatMapper;
import solvit.teachmon.domain.leave_seat.domain.entity.LeaveSeatEntity;
import solvit.teachmon.domain.leave_seat.domain.entity.LeaveSeatStudentEntity;
import solvit.teachmon.domain.leave_seat.domain.repository.LeaveSeatRepository;
import solvit.teachmon.domain.leave_seat.domain.repository.LeaveSeatStudentRepository;
import solvit.teachmon.domain.leave_seat.exception.LeaveSeatNotFoundException;
import solvit.teachmon.domain.leave_seat.exception.LeaveSeatValueInvalidException;
import solvit.teachmon.domain.leave_seat.presentation.dto.request.LeaveSeatCreateRequest;
import solvit.teachmon.domain.leave_seat.presentation.dto.request.LeaveSeatUpdateRequest;
import solvit.teachmon.domain.leave_seat.presentation.dto.response.LeaveSeatDetailResponse;
import solvit.teachmon.domain.leave_seat.presentation.dto.response.LeaveSeatListResponse;
import solvit.teachmon.domain.leave_seat.presentation.dto.response.PlaceAvailabilityResponse;
import solvit.teachmon.domain.management.student.domain.entity.StudentEntity;
import solvit.teachmon.domain.management.student.domain.repository.StudentRepository;
import solvit.teachmon.domain.management.student.exception.StudentNotFoundException;
import solvit.teachmon.domain.place.domain.entity.PlaceEntity;
import solvit.teachmon.domain.place.domain.repository.PlaceRepository;
import solvit.teachmon.domain.place.exception.PlaceNotFoundException;
import solvit.teachmon.domain.student_schedule.domain.entity.ScheduleEntity;
import solvit.teachmon.domain.student_schedule.domain.entity.StudentScheduleEntity;
import solvit.teachmon.domain.student_schedule.domain.entity.schedules.LeaveSeatScheduleEntity;
import solvit.teachmon.domain.student_schedule.domain.enums.ScheduleType;
import solvit.teachmon.domain.student_schedule.domain.repository.ScheduleRepository;
import solvit.teachmon.domain.student_schedule.domain.repository.StudentScheduleRepository;
import solvit.teachmon.domain.student_schedule.domain.repository.schedules.LeaveSeatScheduleRepository;
import solvit.teachmon.domain.student_schedule.exception.StudentScheduleNotFoundException;
import solvit.teachmon.domain.user.domain.entity.TeacherEntity;
import solvit.teachmon.global.enums.SchoolPeriod;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("이석 서비스 테스트")
class LeaveSeatFacadeServiceTest {

    @Mock
    private PlaceRepository placeRepository;
    @Mock
    private LeaveSeatRepository leaveSeatRepository;
    @Mock
    private ScheduleRepository scheduleRepository;
    @Mock
    private StudentScheduleRepository studentScheduleRepository;
    @Mock
    private LeaveSeatScheduleRepository leaveSeatScheduleRepository;
    @Mock
    private LeaveSeatStudentRepository leaveSeatStudentRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private LeaveSeatMapper leaveSeatMapper;

    private LeaveSeatFacadeService leaveSeatFacadeService;

    @BeforeEach
    void setUp() {
        leaveSeatFacadeService = new LeaveSeatFacadeService(
                placeRepository,
                leaveSeatRepository,
                scheduleRepository,
                studentScheduleRepository,
                leaveSeatScheduleRepository,
                leaveSeatStudentRepository,
                studentRepository,
                leaveSeatMapper
        );
    }

    @Test
    @DisplayName("새로운 이석을 생성할 수 있다")
    void shouldCreateNewLeaveSeat() {
        // Given: 이석 생성 요청이 있을 때
        LocalDate day = LocalDate.now().plusDays(1);
        LeaveSeatCreateRequest request = new LeaveSeatCreateRequest(
                day,
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

        StudentScheduleEntity schedule1 = mock(StudentScheduleEntity.class);
        StudentScheduleEntity schedule2 = mock(StudentScheduleEntity.class);
        List<StudentScheduleEntity> studentSchedules = Arrays.asList(schedule1, schedule2);

        LeaveSeatEntity leaveSeat = mock(LeaveSeatEntity.class);

        given(placeRepository.findById(1L)).willReturn(Optional.of(place));
        given(studentRepository.findAllById(List.of(1L, 2L))).willReturn(students);
        given(leaveSeatRepository.findByPlaceAndDayAndPeriod(place, day, SchoolPeriod.SEVEN_PERIOD))
                .willReturn(Optional.empty());
        given(leaveSeatRepository.save(any(LeaveSeatEntity.class))).willReturn(leaveSeat);
        given(studentScheduleRepository.findAllByStudentsAndDayAndPeriod(students, day, SchoolPeriod.SEVEN_PERIOD))
                .willReturn(studentSchedules);
        given(schedule1.getId()).willReturn(1L);
        given(schedule2.getId()).willReturn(2L);
        given(scheduleRepository.findLastStackOrderByStudentScheduleId(1L)).willReturn(0);
        given(scheduleRepository.findLastStackOrderByStudentScheduleId(2L)).willReturn(0);

        // When: 이석을 생성하면
        leaveSeatFacadeService.createLeaveSeat(request, teacher);

        // Then: 이석과 관련 데이터가 저장된다
        verify(placeRepository, times(1)).findById(1L);
        verify(studentRepository, times(1)).findAllById(List.of(1L, 2L));
        verify(leaveSeatRepository, times(1)).save(any(LeaveSeatEntity.class));
        verify(leaveSeatStudentRepository, times(1)).saveAll(anyList());
        verify(scheduleRepository, times(1)).saveAll(anyList());
        verify(leaveSeatScheduleRepository, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("이미 존재하는 장소/날짜/교시의 이석에 학생을 추가할 수 있다")
    void shouldAddStudentsToExistingLeaveSeat() {
        // Given: 이미 존재하는 이석이 있을 때
        LocalDate day = LocalDate.now().plusDays(1);
        LeaveSeatCreateRequest request = new LeaveSeatCreateRequest(
                day,
                SchoolPeriod.SEVEN_PERIOD,
                1L,
                "도서관 이용",
                List.of(3L)
        );

        PlaceEntity place = mock(PlaceEntity.class);
        TeacherEntity teacher = mock(TeacherEntity.class);
        lenient().when(teacher.hasStudentScheduleChangeAuthority()).thenReturn(true);

        StudentEntity student = mock(StudentEntity.class);
        LeaveSeatEntity existingLeaveSeat = mock(LeaveSeatEntity.class);
        StudentScheduleEntity schedule = mock(StudentScheduleEntity.class);

        given(placeRepository.findById(1L)).willReturn(Optional.of(place));
        given(studentRepository.findAllById(List.of(3L))).willReturn(List.of(student));
        given(leaveSeatRepository.findByPlaceAndDayAndPeriod(place, day, SchoolPeriod.SEVEN_PERIOD))
                .willReturn(Optional.of(existingLeaveSeat));
        given(studentScheduleRepository.findAllByStudentsAndDayAndPeriod(List.of(student), day, SchoolPeriod.SEVEN_PERIOD))
                .willReturn(List.of(schedule));
        given(schedule.getId()).willReturn(3L);
        given(scheduleRepository.findLastStackOrderByStudentScheduleId(3L)).willReturn(0);

        // When: 이석을 생성하면
        leaveSeatFacadeService.createLeaveSeat(request, teacher);

        // Then: 새로운 LeaveSeat을 저장하지 않고 기존 것을 사용한다
        verify(leaveSeatRepository, never()).save(any(LeaveSeatEntity.class));
        verify(leaveSeatStudentRepository, times(1)).saveAll(anyList());
        verify(scheduleRepository, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("존재하지 않는 장소로 이석 생성 시 예외가 발생한다")
    void shouldThrowExceptionWhenPlaceNotFound() {
        // Given: 존재하지 않는 장소 ID로 요청이 있을 때
        LeaveSeatCreateRequest request = new LeaveSeatCreateRequest(
                LocalDate.now().plusDays(1),
                SchoolPeriod.SEVEN_PERIOD,
                999L,
                "도서관 이용",
                List.of(1L)
        );
        TeacherEntity teacher = mock(TeacherEntity.class);

        given(placeRepository.findById(999L)).willReturn(Optional.empty());

        // When & Then: 이석 생성 시 예외가 발생한다
        assertThatThrownBy(() -> leaveSeatFacadeService.createLeaveSeat(request, teacher))
                .isInstanceOf(PlaceNotFoundException.class);

        verify(placeRepository, times(1)).findById(999L);
        verify(leaveSeatRepository, never()).save(any());
    }

    @Test
    @DisplayName("존재하지 않는 학생으로 이석 생성 시 예외가 발생한다")
    void shouldThrowExceptionWhenStudentNotFound() {
        // Given: 존재하지 않는 학생 ID로 요청이 있을 때
        LeaveSeatCreateRequest request = new LeaveSeatCreateRequest(
                LocalDate.now().plusDays(1),
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

        // When & Then: 이석 생성 시 예외가 발생한다
        assertThatThrownBy(() -> leaveSeatFacadeService.createLeaveSeat(request, teacher))
                .isInstanceOf(StudentNotFoundException.class);

        verify(studentRepository, times(1)).findAllById(List.of(1L, 999L));
    }

    @Test
    @DisplayName("학생 스케줄이 없을 때 이석 생성 시 예외가 발생한다")
    void shouldThrowExceptionWhenStudentScheduleNotFound() {
        // Given: 학생 스케줄이 없을 때
        LocalDate day = LocalDate.now().plusDays(1);
        LeaveSeatCreateRequest request = new LeaveSeatCreateRequest(
                day,
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
        LeaveSeatEntity leaveSeat = mock(LeaveSeatEntity.class);
        StudentScheduleEntity schedule1 = mock(StudentScheduleEntity.class);

        given(placeRepository.findById(1L)).willReturn(Optional.of(place));
        given(studentRepository.findAllById(List.of(1L, 2L))).willReturn(students);
        given(leaveSeatRepository.findByPlaceAndDayAndPeriod(place, day, SchoolPeriod.SEVEN_PERIOD))
                .willReturn(Optional.empty());
        given(leaveSeatRepository.save(any(LeaveSeatEntity.class))).willReturn(leaveSeat);
        given(studentScheduleRepository.findAllByStudentsAndDayAndPeriod(students, day, SchoolPeriod.SEVEN_PERIOD))
                .willReturn(List.of(schedule1)); // 1개만 반환
        lenient().when(schedule1.getId()).thenReturn(1L);
        lenient().when(scheduleRepository.findLastStackOrderByStudentScheduleId(1L)).thenReturn(0);

        // When & Then: 이석 생성 시 예외가 발생한다
        assertThatThrownBy(() -> leaveSeatFacadeService.createLeaveSeat(request, teacher))
                .isInstanceOf(StudentScheduleNotFoundException.class);
    }

    @Test
    @DisplayName("특정 날짜와 교시의 이석 목록을 조회할 수 있다")
    void shouldGetLeaveSeatList() {
        // Given: 특정 날짜와 교시에 이석이 존재할 때
        LocalDate day = LocalDate.now().plusDays(1);
        SchoolPeriod period = SchoolPeriod.SEVEN_PERIOD;

        LeaveSeatEntity leaveSeat1 = mock(LeaveSeatEntity.class);
        LeaveSeatEntity leaveSeat2 = mock(LeaveSeatEntity.class);
        List<LeaveSeatEntity> leaveSeats = Arrays.asList(leaveSeat1, leaveSeat2);

        LeaveSeatStudentEntity student1 = mock(LeaveSeatStudentEntity.class);
        List<LeaveSeatStudentEntity> leaveSeatStudents1 = List.of(student1);

        LeaveSeatStudentEntity student2 = mock(LeaveSeatStudentEntity.class);
        List<LeaveSeatStudentEntity> leaveSeatStudents2 = List.of(student2);

        LeaveSeatListResponse response1 = mock(LeaveSeatListResponse.class);
        LeaveSeatListResponse response2 = mock(LeaveSeatListResponse.class);

        given(leaveSeatRepository.findAllByDayAndPeriodWithFetch(day, period)).willReturn(leaveSeats);
        given(leaveSeatStudentRepository.findAllByLeaveSeatWithFetch(leaveSeat1)).willReturn(leaveSeatStudents1);
        given(leaveSeatStudentRepository.findAllByLeaveSeatWithFetch(leaveSeat2)).willReturn(leaveSeatStudents2);
        given(leaveSeatMapper.toListResponse(leaveSeat1, leaveSeatStudents1)).willReturn(response1);
        given(leaveSeatMapper.toListResponse(leaveSeat2, leaveSeatStudents2)).willReturn(response2);

        // When: 이석 목록을 조회하면
        List<LeaveSeatListResponse> results = leaveSeatFacadeService.getLeaveSeatList(day, period);

        // Then: 조회된 이석 목록이 반환된다
        assertThat(results).hasSize(2);
        assertThat(results).containsExactly(response1, response2);

        verify(leaveSeatRepository, times(1)).findAllByDayAndPeriodWithFetch(day, period);
        verify(leaveSeatStudentRepository, times(2)).findAllByLeaveSeatWithFetch(any());
        verify(leaveSeatMapper, times(2)).toListResponse(any(), anyList());
    }

    @Test
    @DisplayName("이석 상세 정보를 조회할 수 있다")
    void shouldGetLeaveSeatDetail() {
        // Given: 이석이 존재할 때
        Long leaveSeatId = 1L;
        LeaveSeatEntity leaveSeat = mock(LeaveSeatEntity.class);

        StudentEntity student1 = mock(StudentEntity.class);
        StudentEntity student2 = mock(StudentEntity.class);
        List<StudentEntity> students = Arrays.asList(student1, student2);

        LeaveSeatStudentEntity leaveSeatStudent1 = mock(LeaveSeatStudentEntity.class);
        LeaveSeatStudentEntity leaveSeatStudent2 = mock(LeaveSeatStudentEntity.class);
        List<LeaveSeatStudentEntity> leaveSeatStudents = Arrays.asList(leaveSeatStudent1, leaveSeatStudent2);

        Map<Long, ScheduleType> scheduleTypes = Map.of(
                1L, ScheduleType.LEAVE_SEAT,
                2L, ScheduleType.LEAVE_SEAT
        );

        LeaveSeatDetailResponse response = mock(LeaveSeatDetailResponse.class);

        given(leaveSeatRepository.findByIdWithFetch(leaveSeatId)).willReturn(Optional.of(leaveSeat));
        given(leaveSeatStudentRepository.findAllByLeaveSeatWithFetch(leaveSeat)).willReturn(leaveSeatStudents);
        given(leaveSeatStudent1.getStudent()).willReturn(student1);
        given(leaveSeatStudent2.getStudent()).willReturn(student2);
        given(leaveSeat.getDay()).willReturn(LocalDate.now());
        given(leaveSeat.getPeriod()).willReturn(SchoolPeriod.SEVEN_PERIOD);
        given(studentScheduleRepository.findLastScheduleTypeByStudentsAndDayAndPeriod(
                students, leaveSeat.getDay(), leaveSeat.getPeriod()
        )).willReturn(scheduleTypes);
        given(leaveSeatMapper.toDetailResponse(leaveSeat, students, scheduleTypes)).willReturn(response);

        // When: 이석 상세 정보를 조회하면
        LeaveSeatDetailResponse result = leaveSeatFacadeService.getLeaveSeatDetail(leaveSeatId);

        // Then: 상세 정보가 반환된다
        assertThat(result).isEqualTo(response);

        verify(leaveSeatRepository, times(1)).findByIdWithFetch(leaveSeatId);
        verify(leaveSeatStudentRepository, times(1)).findAllByLeaveSeatWithFetch(leaveSeat);
        verify(studentScheduleRepository, times(1)).findLastScheduleTypeByStudentsAndDayAndPeriod(
                students, leaveSeat.getDay(), leaveSeat.getPeriod()
        );
        verify(leaveSeatMapper, times(1)).toDetailResponse(leaveSeat, students, scheduleTypes);
    }

    @Test
    @DisplayName("존재하지 않는 이석 조회 시 예외가 발생한다")
    void shouldThrowExceptionWhenLeaveSeatNotFoundOnDetail() {
        // Given: 존재하지 않는 이석 ID로 조회할 때
        Long leaveSeatId = 999L;

        given(leaveSeatRepository.findByIdWithFetch(leaveSeatId)).willReturn(Optional.empty());

        // When & Then: 예외가 발생한다
        assertThatThrownBy(() -> leaveSeatFacadeService.getLeaveSeatDetail(leaveSeatId))
                .isInstanceOf(LeaveSeatNotFoundException.class);

        verify(leaveSeatRepository, times(1)).findByIdWithFetch(leaveSeatId);
    }

    @Test
    @DisplayName("이석을 수정할 수 있다")
    void shouldUpdateLeaveSeat() {
        // Given: 수정할 이석과 요청이 있을 때
        Long leaveSeatId = 1L;
        LocalDate newDay = LocalDate.now().plusDays(2);
        LeaveSeatUpdateRequest request = new LeaveSeatUpdateRequest(
                newDay,
                SchoolPeriod.EIGHT_AND_NINE_PERIOD,
                2L,
                "변경된 사유",
                List.of(3L, 4L)
        );

        LeaveSeatEntity leaveSeat = mock(LeaveSeatEntity.class);
        PlaceEntity newPlace = mock(PlaceEntity.class);
        TeacherEntity teacher = mock(TeacherEntity.class);
        lenient().when(teacher.hasStudentScheduleChangeAuthority()).thenReturn(true);

        StudentEntity student1 = mock(StudentEntity.class);
        StudentEntity student2 = mock(StudentEntity.class);
        List<StudentEntity> students = Arrays.asList(student1, student2);

        StudentScheduleEntity schedule1 = mock(StudentScheduleEntity.class);
        StudentScheduleEntity schedule2 = mock(StudentScheduleEntity.class);
        List<StudentScheduleEntity> studentSchedules = Arrays.asList(schedule1, schedule2);

        given(leaveSeatRepository.findById(leaveSeatId)).willReturn(Optional.of(leaveSeat));
        given(placeRepository.findById(2L)).willReturn(Optional.of(newPlace));
        given(studentRepository.findAllById(List.of(3L, 4L))).willReturn(students);
        given(studentScheduleRepository.findAllByStudentsAndDayAndPeriod(students, newDay, SchoolPeriod.EIGHT_AND_NINE_PERIOD))
                .willReturn(studentSchedules);
        lenient().when(schedule1.getId()).thenReturn(3L);
        lenient().when(schedule2.getId()).thenReturn(4L);
        lenient().when(scheduleRepository.findLastStackOrderByStudentScheduleId(3L)).thenReturn(0);
        lenient().when(scheduleRepository.findLastStackOrderByStudentScheduleId(4L)).thenReturn(0);

        // When: 이석을 수정하면
        leaveSeatFacadeService.updateLeaveSeat(leaveSeatId, request, teacher);

        // Then: 기존 데이터가 삭제되고 새로운 데이터가 저장된다
        // LeaveSeatSchedule 삭제 시 cascade = CascadeType.REMOVE로 인해 Schedule도 자동으로 삭제됨
        verify(leaveSeatRepository, times(1)).findById(leaveSeatId);
        verify(placeRepository, times(1)).findById(2L);
        verify(leaveSeat, times(1)).changeLeaveSeatInfo(
                teacher, newPlace, newDay, SchoolPeriod.EIGHT_AND_NINE_PERIOD, "변경된 사유"
        );
        verify(leaveSeatStudentRepository, times(1)).saveAll(anyList());
        verify(scheduleRepository, times(1)).saveAll(anyList());
        verify(leaveSeatScheduleRepository, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("존재하지 않는 이석 수정 시 예외가 발생한다")
    void shouldThrowExceptionWhenLeaveSeatNotFoundOnUpdate() {
        // Given: 존재하지 않는 이석 ID로 수정 요청이 있을 때
        Long leaveSeatId = 999L;
        LeaveSeatUpdateRequest request = new LeaveSeatUpdateRequest(
                LocalDate.now().plusDays(1),
                SchoolPeriod.SEVEN_PERIOD,
                1L,
                "사유",
                List.of(1L)
        );
        TeacherEntity teacher = mock(TeacherEntity.class);

        given(leaveSeatRepository.findById(leaveSeatId)).willReturn(Optional.empty());

        // When & Then: 예외가 발생한다
        assertThatThrownBy(() -> leaveSeatFacadeService.updateLeaveSeat(leaveSeatId, request, teacher))
                .isInstanceOf(LeaveSeatValueInvalidException.class)
                .hasMessageContaining("이석을 찾을 수 없습니다");

        verify(leaveSeatRepository, times(1)).findById(leaveSeatId);
    }

    @Test
    @DisplayName("이석을 삭제할 수 있다")
    void shouldDeleteLeaveSeat() {
        // Given: 삭제할 이석이 존재할 때
        Long leaveSeatId = 1L;
        LeaveSeatEntity leaveSeat = mock(LeaveSeatEntity.class);

        LeaveSeatScheduleEntity leaveSeatSchedule = mock(LeaveSeatScheduleEntity.class);

        given(leaveSeatRepository.findById(leaveSeatId)).willReturn(Optional.of(leaveSeat));
        given(leaveSeatScheduleRepository.findAllByLeaveSeatId(leaveSeatId)).willReturn(List.of(leaveSeatSchedule));

        // When: 이석을 삭제하면
        leaveSeatFacadeService.deleteLeaveSeat(leaveSeatId);

        // Then: 이석과 관련 데이터가 삭제된다
        // LeaveSeatSchedule 삭제 시 cascade = CascadeType.REMOVE로 인해 Schedule도 자동으로 삭제됨
        verify(leaveSeatRepository, times(1)).findById(leaveSeatId);
        verify(leaveSeatScheduleRepository, times(1)).findAllByLeaveSeatId(leaveSeatId);
        verify(leaveSeatScheduleRepository, times(1)).deleteAllInBatch(anyList());
        verify(leaveSeatStudentRepository, times(1)).deleteAllByLeaveSeatId(leaveSeatId);
        verify(leaveSeatRepository, times(1)).delete(leaveSeat);
    }

    @Test
    @DisplayName("존재하지 않는 이석 삭제 시 예외가 발생한다")
    void shouldThrowExceptionWhenLeaveSeatNotFoundOnDelete() {
        // Given: 존재하지 않는 이석 ID로 삭제 요청이 있을 때
        Long leaveSeatId = 999L;

        given(leaveSeatRepository.findById(leaveSeatId)).willReturn(Optional.empty());

        // When & Then: 예외가 발생한다
        assertThatThrownBy(() -> leaveSeatFacadeService.deleteLeaveSeat(leaveSeatId))
                .isInstanceOf(LeaveSeatValueInvalidException.class)
                .hasMessageContaining("이석을 찾을 수 없습니다");

        verify(leaveSeatRepository, times(1)).findById(leaveSeatId);
        verify(leaveSeatRepository, never()).delete(any());
    }

    @Test
    @DisplayName("장소 사용 가능 여부를 확인할 수 있다")
    void shouldCheckPlaceAvailability() {
        // Given: 장소가 비어있을 때
        Long placeId = 1L;
        LocalDate day = LocalDate.now().plusDays(1);
        SchoolPeriod period = SchoolPeriod.SEVEN_PERIOD;

        given(leaveSeatRepository.isPlaceAvailableForLeaveSeat(placeId, day, period)).willReturn(true);

        // When: 장소 사용 가능 여부를 확인하면
        PlaceAvailabilityResponse result = leaveSeatFacadeService.checkPlaceAvailability(placeId, day, period);

        // Then: 사용 가능 여부가 반환된다
        assertThat(result.isEmpty()).isTrue();

        verify(leaveSeatRepository, times(1)).isPlaceAvailableForLeaveSeat(placeId, day, period);
    }

    @Test
    @DisplayName("장소가 이미 사용중인지 확인할 수 있다")
    void shouldCheckPlaceIsNotAvailable() {
        // Given: 장소가 이미 사용중일 때
        Long placeId = 1L;
        LocalDate day = LocalDate.now().plusDays(1);
        SchoolPeriod period = SchoolPeriod.SEVEN_PERIOD;

        given(leaveSeatRepository.isPlaceAvailableForLeaveSeat(placeId, day, period)).willReturn(false);

        // When: 장소 사용 가능 여부를 확인하면
        PlaceAvailabilityResponse result = leaveSeatFacadeService.checkPlaceAvailability(placeId, day, period);

        // Then: 사용 불가능 상태가 반환된다
        assertThat(result.isEmpty()).isFalse();

        verify(leaveSeatRepository, times(1)).isPlaceAvailableForLeaveSeat(placeId, day, period);
    }
}
