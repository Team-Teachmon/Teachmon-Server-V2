package solvit.teachmon.domain.student_schedule.application.facade;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import solvit.teachmon.domain.place.exception.PlaceNotFoundException;
import solvit.teachmon.domain.place.domain.entity.PlaceEntity;
import solvit.teachmon.domain.place.domain.repository.PlaceRepository;
import solvit.teachmon.domain.student_schedule.application.dto.PlaceScheduleDto;
import solvit.teachmon.domain.student_schedule.application.dto.StudentScheduleDto;
import solvit.teachmon.domain.student_schedule.application.mapper.PlaceStudentScheduleMapper;
import solvit.teachmon.domain.student_schedule.application.mapper.StudentScheduleMapper;
import solvit.teachmon.domain.student_schedule.domain.entity.ScheduleEntity;
import solvit.teachmon.domain.student_schedule.domain.enums.ScheduleType;
import solvit.teachmon.domain.student_schedule.domain.repository.StudentScheduleRepository;
import solvit.teachmon.domain.student_schedule.domain.repository.schedules.AdditionalSelfStudyScheduleRepository;
import solvit.teachmon.domain.student_schedule.domain.repository.schedules.AfterSchoolScheduleRepository;
import solvit.teachmon.domain.student_schedule.domain.repository.schedules.LeaveSeatScheduleRepository;
import solvit.teachmon.domain.student_schedule.domain.repository.schedules.SelfStudyScheduleRepository;
import solvit.teachmon.domain.student_schedule.presentation.dto.response.FloorStateResponse;
import solvit.teachmon.domain.student_schedule.presentation.dto.response.PlaceStateResponse;
import solvit.teachmon.domain.student_schedule.presentation.dto.response.PlaceStudentScheduleResponse;
import solvit.teachmon.domain.student_schedule.presentation.dto.response.StudentScheduleResponse;
import solvit.teachmon.global.enums.SchoolPeriod;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("장소별 학생 스케줄 서비스 테스트")
class PlaceStudentScheduleServiceTest {

    @Mock
    private StudentScheduleRepository studentScheduleRepository;

    @Mock
    private SelfStudyScheduleRepository selfStudyScheduleRepository;

    @Mock
    private AdditionalSelfStudyScheduleRepository additionalSelfStudyScheduleRepository;

    @Mock
    private LeaveSeatScheduleRepository leaveSeatScheduleRepository;

    @Mock
    private AfterSchoolScheduleRepository afterSchoolScheduleRepository;

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private PlaceStudentScheduleMapper placeStudentScheduleMapper;

    @Mock
    private StudentScheduleMapper studentScheduleMapper;

    @InjectMocks
    private PlaceStudentScheduleService placeStudentScheduleService;

    @Test
    @DisplayName("모든 층의 장소 사용 인원을 조회할 수 있다")
    void shouldGetAllFloorsPlaceCount() {
        // Given: 특정 날짜와 교시가 주어졌을 때
        LocalDate day = LocalDate.of(2024, 1, 15);
        SchoolPeriod period = SchoolPeriod.ONE_PERIOD;

        ScheduleEntity selfStudySchedule = mock(ScheduleEntity.class);
        ScheduleEntity additionalSelfStudySchedule = mock(ScheduleEntity.class);
        ScheduleEntity leaveSeatSchedule = mock(ScheduleEntity.class);
        ScheduleEntity afterSchoolSchedule = mock(ScheduleEntity.class);

        Map<ScheduleType, List<ScheduleEntity>> placeFillScheduleMap = Map.of(
                ScheduleType.SELF_STUDY, List.of(selfStudySchedule),
                ScheduleType.ADDITIONAL_SELF_STUDY, List.of(additionalSelfStudySchedule),
                ScheduleType.LEAVE_SEAT, List.of(leaveSeatSchedule),
                ScheduleType.AFTER_SCHOOL, List.of(afterSchoolSchedule)
        );

        Map<Integer, Long> selfStudyCountMap = Map.of(1, 5L, 2, 3L);
        Map<Integer, Long> additionalSelfStudyCountMap = Map.of(2, 2L, 3, 4L);
        Map<Integer, Long> leaveSeatCountMap = Map.of(1, 1L);
        Map<Integer, Long> afterSchoolCountMap = Map.of(3, 2L);
        Map<Integer, Long> afterSchoolReinforcementCountMap = Map.of(1, 2L, 3, 1L);

        FloorStateResponse floor1Response = FloorStateResponse.builder()
                .floor(1).count(8L).build();
        FloorStateResponse floor2Response = FloorStateResponse.builder()
                .floor(2).count(5L).build();
        FloorStateResponse floor3Response = FloorStateResponse.builder()
                .floor(3).count(7L).build();

        given(studentScheduleRepository.findPlaceBasedSchedulesByDayAndPeriodAndTypeIn(eq(day), eq(period), anyList()))
                .willReturn(placeFillScheduleMap);
        given(selfStudyScheduleRepository.getSelfStudyPlaceCount(List.of(selfStudySchedule)))
                .willReturn(selfStudyCountMap);
        given(additionalSelfStudyScheduleRepository.getAdditionalSelfStudyPlaceCount(List.of(additionalSelfStudySchedule)))
                .willReturn(additionalSelfStudyCountMap);
        given(leaveSeatScheduleRepository.getLeaveSeatPlaceCount(List.of(leaveSeatSchedule)))
                .willReturn(leaveSeatCountMap);
        given(afterSchoolScheduleRepository.getAfterSchoolPlaceCount(List.of(afterSchoolSchedule)))
                .willReturn(afterSchoolCountMap);
        given(afterSchoolScheduleRepository.getAfterSchoolReinforcementPlaceCount(List.of()))
                .willReturn(afterSchoolReinforcementCountMap);
        given(placeStudentScheduleMapper.toFloorStateResponses(anyMap()))
                .willReturn(List.of(floor1Response, floor2Response, floor3Response));

        // When: 모든 층의 장소 사용 인원을 조회하면
        List<FloorStateResponse> result = placeStudentScheduleService.getAllFloorsPlaceCount(day, period);

        // Then: 각 층별로 집계된 인원 수가 반환된다
        assertThat(result).hasSize(3);
        assertThat(result).extracting(FloorStateResponse::floor)
                .containsExactlyInAnyOrder(1, 2, 3);

        verify(studentScheduleRepository, times(1))
                .findPlaceBasedSchedulesByDayAndPeriodAndTypeIn(eq(day), eq(period), anyList());
        verify(selfStudyScheduleRepository, times(1))
                .getSelfStudyPlaceCount(List.of(selfStudySchedule));
        verify(additionalSelfStudyScheduleRepository, times(1))
                .getAdditionalSelfStudyPlaceCount(List.of(additionalSelfStudySchedule));
        verify(leaveSeatScheduleRepository, times(1))
                .getLeaveSeatPlaceCount(List.of(leaveSeatSchedule));
        verify(afterSchoolScheduleRepository, times(1))
                .getAfterSchoolPlaceCount(List.of(afterSchoolSchedule));
        verify(afterSchoolScheduleRepository, times(1))
                .getAfterSchoolReinforcementPlaceCount(List.of());
        verify(placeStudentScheduleMapper, times(1))
                .toFloorStateResponses(anyMap());
    }

    @Test
    @DisplayName("스케줄이 없는 경우 빈 층별 인원 목록이 반환된다")
    void shouldReturnEmptyFloorCountWhenNoSchedules() {
        // Given: 스케줄이 없는 날짜와 교시가 주어졌을 때
        LocalDate day = LocalDate.of(2024, 1, 15);
        SchoolPeriod period = SchoolPeriod.ONE_PERIOD;

        Map<ScheduleType, List<ScheduleEntity>> emptyScheduleMap = Map.of(
                ScheduleType.SELF_STUDY, List.of(),
                ScheduleType.ADDITIONAL_SELF_STUDY, List.of(),
                ScheduleType.LEAVE_SEAT, List.of(),
                ScheduleType.AFTER_SCHOOL, List.of()
        );

        given(studentScheduleRepository.findPlaceBasedSchedulesByDayAndPeriodAndTypeIn(eq(day), eq(period), anyList()))
                .willReturn(emptyScheduleMap);
        given(selfStudyScheduleRepository.getSelfStudyPlaceCount(List.of())).willReturn(Map.of());
        given(additionalSelfStudyScheduleRepository.getAdditionalSelfStudyPlaceCount(List.of())).willReturn(Map.of());
        given(leaveSeatScheduleRepository.getLeaveSeatPlaceCount(List.of())).willReturn(Map.of());
        given(afterSchoolScheduleRepository.getAfterSchoolPlaceCount(List.of())).willReturn(Map.of());
        given(afterSchoolScheduleRepository.getAfterSchoolReinforcementPlaceCount(List.of())).willReturn(Map.of());
        given(placeStudentScheduleMapper.toFloorStateResponses(anyMap())).willReturn(List.of());

        // When: 모든 층의 장소 사용 인원을 조회하면
        List<FloorStateResponse> result = placeStudentScheduleService.getAllFloorsPlaceCount(day, period);

        // Then: 빈 목록이 반환된다
        assertThat(result).isEmpty();

        verify(studentScheduleRepository, times(1))
                .findPlaceBasedSchedulesByDayAndPeriodAndTypeIn(eq(day), eq(period), anyList());
    }

    @Test
    @DisplayName("특정 층의 장소 상태를 조회할 수 있다")
    void shouldGetPlaceStatesByFloor() {
        // Given: 특정 층, 날짜, 교시가 주어졌을 때
        Integer floor = 2;
        LocalDate day = LocalDate.of(2024, 1, 15);
        SchoolPeriod period = SchoolPeriod.ONE_PERIOD;

        ScheduleEntity selfStudySchedule = mock(ScheduleEntity.class);
        ScheduleEntity additionalSelfStudySchedule = mock(ScheduleEntity.class);

        Map<ScheduleType, List<ScheduleEntity>> placeFillScheduleMap = Map.of(
                ScheduleType.SELF_STUDY, List.of(selfStudySchedule),
                ScheduleType.ADDITIONAL_SELF_STUDY, List.of(additionalSelfStudySchedule),
                ScheduleType.LEAVE_SEAT, List.of(),
                ScheduleType.AFTER_SCHOOL, List.of()
        );

        PlaceEntity place1 = mock(PlaceEntity.class);
        PlaceEntity place2 = mock(PlaceEntity.class);

        PlaceScheduleDto placeSchedule1 = new PlaceScheduleDto(place1, ScheduleType.SELF_STUDY);
        PlaceScheduleDto placeSchedule2 = new PlaceScheduleDto(place2, ScheduleType.ADDITIONAL_SELF_STUDY);

        PlaceStateResponse placeState1 = PlaceStateResponse.builder()
                .placeId(1L).placeName("2층 자습실1").state(ScheduleType.SELF_STUDY).build();
        PlaceStateResponse placeState2 = PlaceStateResponse.builder()
                .placeId(2L).placeName("2층 자습실2").state(ScheduleType.ADDITIONAL_SELF_STUDY).build();

        given(studentScheduleRepository.findPlaceBasedSchedulesByDayAndPeriodAndTypeIn(eq(day), eq(period), anyList()))
                .willReturn(placeFillScheduleMap);
        given(selfStudyScheduleRepository.getPlaceScheduleByFloor(List.of(selfStudySchedule), floor))
                .willReturn(List.of(placeSchedule1));
        given(additionalSelfStudyScheduleRepository.getPlaceScheduleByFloor(List.of(additionalSelfStudySchedule), floor))
                .willReturn(List.of(placeSchedule2));
        given(leaveSeatScheduleRepository.getPlaceScheduleByFloor(List.of(), floor))
                .willReturn(List.of());
        given(afterSchoolScheduleRepository.getPlaceScheduleByFloor(List.of(), floor))
                .willReturn(List.of());
        given(afterSchoolScheduleRepository.getReinforcementPlaceScheduleByFloor(List.of(), floor))
                .willReturn(List.of());
        given(placeStudentScheduleMapper.toPlaceStateResponses(anyList()))
                .willReturn(List.of(placeState1, placeState2));

        // When: 특정 층의 장소 상태를 조회하면
        List<PlaceStateResponse> result = placeStudentScheduleService.getPlaceStatesByFloor(floor, day, period);

        // Then: 해당 층의 장소 상태 목록이 반환된다
        assertThat(result).hasSize(2);
        assertThat(result).extracting(PlaceStateResponse::state)
                .containsExactlyInAnyOrder(ScheduleType.SELF_STUDY, ScheduleType.ADDITIONAL_SELF_STUDY);

        verify(studentScheduleRepository, times(1))
                .findPlaceBasedSchedulesByDayAndPeriodAndTypeIn(eq(day), eq(period), anyList());
        verify(selfStudyScheduleRepository, times(1))
                .getPlaceScheduleByFloor(List.of(selfStudySchedule), floor);
        verify(additionalSelfStudyScheduleRepository, times(1))
                .getPlaceScheduleByFloor(List.of(additionalSelfStudySchedule), floor);
        verify(placeStudentScheduleMapper, times(1))
                .toPlaceStateResponses(anyList());
    }

    @Test
    @DisplayName("특정 층에 장소가 없는 경우 빈 목록이 반환된다")
    void shouldReturnEmptyListWhenNoPlacesOnFloor() {
        // Given: 장소가 없는 층, 날짜, 교시가 주어졌을 때
        Integer floor = 5;
        LocalDate day = LocalDate.of(2024, 1, 15);
        SchoolPeriod period = SchoolPeriod.ONE_PERIOD;

        Map<ScheduleType, List<ScheduleEntity>> emptyScheduleMap = Map.of(
                ScheduleType.SELF_STUDY, List.of(),
                ScheduleType.ADDITIONAL_SELF_STUDY, List.of(),
                ScheduleType.LEAVE_SEAT, List.of(),
                ScheduleType.AFTER_SCHOOL, List.of()
        );

        given(studentScheduleRepository.findPlaceBasedSchedulesByDayAndPeriodAndTypeIn(eq(day), eq(period), anyList()))
                .willReturn(emptyScheduleMap);
        given(selfStudyScheduleRepository.getPlaceScheduleByFloor(List.of(), floor)).willReturn(List.of());
        given(additionalSelfStudyScheduleRepository.getPlaceScheduleByFloor(List.of(), floor)).willReturn(List.of());
        given(leaveSeatScheduleRepository.getPlaceScheduleByFloor(List.of(), floor)).willReturn(List.of());
        given(afterSchoolScheduleRepository.getPlaceScheduleByFloor(List.of(), floor)).willReturn(List.of());
        given(afterSchoolScheduleRepository.getReinforcementPlaceScheduleByFloor(List.of(), floor)).willReturn(List.of());
        given(placeStudentScheduleMapper.toPlaceStateResponses(anyList())).willReturn(List.of());

        // When: 특정 층의 장소 상태를 조회하면
        List<PlaceStateResponse> result = placeStudentScheduleService.getPlaceStatesByFloor(floor, day, period);

        // Then: 빈 목록이 반환된다
        assertThat(result).isEmpty();

        verify(studentScheduleRepository, times(1))
                .findPlaceBasedSchedulesByDayAndPeriodAndTypeIn(eq(day), eq(period), anyList());
    }

    @Test
    @DisplayName("특정 장소의 학생 스케줄을 조회할 수 있다")
    void shouldGetStudentsByPlaceId() {
        // Given: 특정 장소 ID, 날짜, 교시가 주어졌을 때
        Long placeId = 1L;
        LocalDate day = LocalDate.of(2024, 1, 15);
        SchoolPeriod period = SchoolPeriod.ONE_PERIOD;

        PlaceEntity place = mock(PlaceEntity.class);
        lenient().when(place.getId()).thenReturn(placeId);
        lenient().when(place.getName()).thenReturn("3층 자습실");

        StudentScheduleDto student1 = new StudentScheduleDto(
                1L, 1, 1, 1, "김학생", day, period, 10L, ScheduleType.SELF_STUDY
        );
        StudentScheduleDto student2 = new StudentScheduleDto(
                2L, 1, 1, 2, "이학생", day, period, 11L, ScheduleType.ADDITIONAL_SELF_STUDY
        );

        StudentScheduleResponse studentResponse1 = StudentScheduleResponse.builder()
                .studentId(1L).number(1).name("김학생")
                .state(ScheduleType.SELF_STUDY).scheduleId(10L).build();
        StudentScheduleResponse studentResponse2 = StudentScheduleResponse.builder()
                .studentId(2L).number(2).name("이학생")
                .state(ScheduleType.ADDITIONAL_SELF_STUDY).scheduleId(11L).build();

        PlaceStudentScheduleResponse expectedResponse = PlaceStudentScheduleResponse.builder()
                .placeId(placeId)
                .placeName("3층 자습실")
                .students(List.of(studentResponse1, studentResponse2))
                .build();

        given(placeRepository.findById(placeId)).willReturn(Optional.of(place));
        given(selfStudyScheduleRepository.getStudentScheduleByPlaceAndDayAndPeriod(placeId, day, period))
                .willReturn(List.of(student1));
        given(additionalSelfStudyScheduleRepository.getStudentScheduleByPlaceAndDayAndPeriod(placeId, day, period))
                .willReturn(List.of(student2));
        given(leaveSeatScheduleRepository.getStudentScheduleByPlaceAndDayAndPeriod(placeId, day, period))
                .willReturn(List.of());
        given(afterSchoolScheduleRepository.getStudentScheduleByPlaceAndDayAndPeriod(placeId, day, period))
                .willReturn(List.of());
        given(afterSchoolScheduleRepository.getReinforcementStudentScheduleByPlaceAndDayAndPeriod(placeId, day, period))
                .willReturn(List.of());
        lenient().when(studentScheduleMapper.toStudentScheduleResponse(student1)).thenReturn(studentResponse1);
        lenient().when(studentScheduleMapper.toStudentScheduleResponse(student2)).thenReturn(studentResponse2);
        given(placeStudentScheduleMapper.toPlaceStudentScheduleResponse(eq(place), anyList()))
                .willReturn(expectedResponse);

        // When: 특정 장소의 학생 스케줄을 조회하면
        PlaceStudentScheduleResponse result = placeStudentScheduleService.getStudentsByPlaceId(placeId, day, period);

        // Then: 해당 장소의 학생 목록이 반환된다
        assertThat(result).isNotNull();
        assertThat(result.placeId()).isEqualTo(placeId);
        assertThat(result.placeName()).isEqualTo("3층 자습실");
        assertThat(result.students()).hasSize(2);

        verify(placeRepository, times(1)).findById(placeId);
        verify(selfStudyScheduleRepository, times(1))
                .getStudentScheduleByPlaceAndDayAndPeriod(placeId, day, period);
        verify(additionalSelfStudyScheduleRepository, times(1))
                .getStudentScheduleByPlaceAndDayAndPeriod(placeId, day, period);
        verify(leaveSeatScheduleRepository, times(1))
                .getStudentScheduleByPlaceAndDayAndPeriod(placeId, day, period);
        verify(afterSchoolScheduleRepository, times(1))
                .getStudentScheduleByPlaceAndDayAndPeriod(placeId, day, period);
        verify(afterSchoolScheduleRepository, times(1))
                .getReinforcementStudentScheduleByPlaceAndDayAndPeriod(placeId, day, period);
        verify(placeStudentScheduleMapper, times(1))
                .toPlaceStudentScheduleResponse(eq(place), anyList());
    }

    @Test
    @DisplayName("존재하지 않는 장소 ID로 조회 시 예외가 발생한다")
    void shouldThrowExceptionWhenPlaceNotFound() {
        // Given: 존재하지 않는 장소 ID가 주어졌을 때
        Long invalidPlaceId = 999L;
        LocalDate day = LocalDate.of(2024, 1, 15);
        SchoolPeriod period = SchoolPeriod.ONE_PERIOD;

        given(placeRepository.findById(invalidPlaceId)).willReturn(Optional.empty());

        // When & Then: 장소를 조회하면 PlaceNotFoundException이 발생한다
        assertThatThrownBy(() -> placeStudentScheduleService.getStudentsByPlaceId(invalidPlaceId, day, period))
                .isInstanceOf(PlaceNotFoundException.class);

        verify(placeRepository, times(1)).findById(invalidPlaceId);
        verify(selfStudyScheduleRepository, never())
                .getStudentScheduleByPlaceAndDayAndPeriod(any(), any(), any());
    }

    @Test
    @DisplayName("특정 장소에 학생이 없는 경우 빈 학생 목록이 반환된다")
    void shouldReturnEmptyStudentListWhenNoStudentsInPlace() {
        // Given: 학생이 없는 장소 ID가 주어졌을 때
        Long placeId = 1L;
        LocalDate day = LocalDate.of(2024, 1, 15);
        SchoolPeriod period = SchoolPeriod.ONE_PERIOD;

        PlaceEntity place = mock(PlaceEntity.class);
        lenient().when(place.getId()).thenReturn(placeId);
        lenient().when(place.getName()).thenReturn("빈 자습실");

        PlaceStudentScheduleResponse expectedResponse = PlaceStudentScheduleResponse.builder()
                .placeId(placeId)
                .placeName("빈 자습실")
                .students(List.of())
                .build();

        given(placeRepository.findById(placeId)).willReturn(Optional.of(place));
        given(selfStudyScheduleRepository.getStudentScheduleByPlaceAndDayAndPeriod(placeId, day, period))
                .willReturn(List.of());
        given(additionalSelfStudyScheduleRepository.getStudentScheduleByPlaceAndDayAndPeriod(placeId, day, period))
                .willReturn(List.of());
        given(leaveSeatScheduleRepository.getStudentScheduleByPlaceAndDayAndPeriod(placeId, day, period))
                .willReturn(List.of());
        given(afterSchoolScheduleRepository.getStudentScheduleByPlaceAndDayAndPeriod(placeId, day, period))
                .willReturn(List.of());
        given(afterSchoolScheduleRepository.getReinforcementStudentScheduleByPlaceAndDayAndPeriod(placeId, day, period))
                .willReturn(List.of());
        given(placeStudentScheduleMapper.toPlaceStudentScheduleResponse(eq(place), eq(List.of())))
                .willReturn(expectedResponse);

        // When: 특정 장소의 학생 스케줄을 조회하면
        PlaceStudentScheduleResponse result = placeStudentScheduleService.getStudentsByPlaceId(placeId, day, period);

        // Then: 빈 학생 목록이 반환된다
        assertThat(result).isNotNull();
        assertThat(result.students()).isEmpty();

        verify(placeRepository, times(1)).findById(placeId);
        verify(placeStudentScheduleMapper, times(1))
                .toPlaceStudentScheduleResponse(eq(place), eq(List.of()));
    }

    @Test
    @DisplayName("여러 스케줄 타입의 학생들을 한 번에 조회할 수 있다")
    void shouldGetStudentsWithMultipleScheduleTypes() {
        // Given: 다양한 스케줄 타입의 학생들이 있는 장소가 주어졌을 때
        Long placeId = 1L;
        LocalDate day = LocalDate.of(2024, 1, 15);
        SchoolPeriod period = SchoolPeriod.EIGHT_AND_NINE_PERIOD;

        PlaceEntity place = mock(PlaceEntity.class);
        lenient().when(place.getId()).thenReturn(placeId);
        lenient().when(place.getName()).thenReturn("다목적실");

        StudentScheduleDto selfStudyStudent = new StudentScheduleDto(
                1L, 1, 1, 1, "김학생", day, period, 10L, ScheduleType.SELF_STUDY
        );
        StudentScheduleDto additionalSelfStudyStudent = new StudentScheduleDto(
                2L, 1, 1, 2, "이학생", day, period, 11L, ScheduleType.ADDITIONAL_SELF_STUDY
        );
        StudentScheduleDto leaveSeatStudent = new StudentScheduleDto(
                3L, 2, 1, 1, "박학생", day, period, 12L, ScheduleType.LEAVE_SEAT
        );
        StudentScheduleDto afterSchoolStudent = new StudentScheduleDto(
                4L, 2, 1, 2, "최학생", day, period, 13L, ScheduleType.AFTER_SCHOOL
        );

        StudentScheduleResponse response1 = StudentScheduleResponse.builder()
                .studentId(1L).number(1).name("김학생")
                .state(ScheduleType.SELF_STUDY).scheduleId(10L).build();
        StudentScheduleResponse response2 = StudentScheduleResponse.builder()
                .studentId(2L).number(2).name("이학생")
                .state(ScheduleType.ADDITIONAL_SELF_STUDY).scheduleId(11L).build();
        StudentScheduleResponse response3 = StudentScheduleResponse.builder()
                .studentId(3L).number(1).name("박학생")
                .state(ScheduleType.LEAVE_SEAT).scheduleId(12L).build();
        StudentScheduleResponse response4 = StudentScheduleResponse.builder()
                .studentId(4L).number(2).name("최학생")
                .state(ScheduleType.AFTER_SCHOOL).scheduleId(13L).build();

        PlaceStudentScheduleResponse expectedResponse = PlaceStudentScheduleResponse.builder()
                .placeId(placeId)
                .placeName("다목적실")
                .students(List.of(response1, response2, response3, response4))
                .build();

        given(placeRepository.findById(placeId)).willReturn(Optional.of(place));
        given(selfStudyScheduleRepository.getStudentScheduleByPlaceAndDayAndPeriod(placeId, day, period))
                .willReturn(List.of(selfStudyStudent));
        given(additionalSelfStudyScheduleRepository.getStudentScheduleByPlaceAndDayAndPeriod(placeId, day, period))
                .willReturn(List.of(additionalSelfStudyStudent));
        given(leaveSeatScheduleRepository.getStudentScheduleByPlaceAndDayAndPeriod(placeId, day, period))
                .willReturn(List.of(leaveSeatStudent));
        given(afterSchoolScheduleRepository.getStudentScheduleByPlaceAndDayAndPeriod(placeId, day, period))
                .willReturn(List.of(afterSchoolStudent));
        given(afterSchoolScheduleRepository.getReinforcementStudentScheduleByPlaceAndDayAndPeriod(placeId, day, period))
                .willReturn(List.of());
        lenient().when(studentScheduleMapper.toStudentScheduleResponse(selfStudyStudent)).thenReturn(response1);
        lenient().when(studentScheduleMapper.toStudentScheduleResponse(additionalSelfStudyStudent)).thenReturn(response2);
        lenient().when(studentScheduleMapper.toStudentScheduleResponse(leaveSeatStudent)).thenReturn(response3);
        lenient().when(studentScheduleMapper.toStudentScheduleResponse(afterSchoolStudent)).thenReturn(response4);
        given(placeStudentScheduleMapper.toPlaceStudentScheduleResponse(eq(place), anyList()))
                .willReturn(expectedResponse);

        // When: 장소의 학생 스케줄을 조회하면
        PlaceStudentScheduleResponse result = placeStudentScheduleService.getStudentsByPlaceId(placeId, day, period);

        // Then: 모든 스케줄 타입의 학생들이 반환된다
        assertThat(result).isNotNull();
        assertThat(result.students()).hasSize(4);
        assertThat(result.students()).extracting(StudentScheduleResponse::state)
                .containsExactlyInAnyOrder(
                        ScheduleType.SELF_STUDY,
                        ScheduleType.ADDITIONAL_SELF_STUDY,
                        ScheduleType.LEAVE_SEAT,
                        ScheduleType.AFTER_SCHOOL
                );

        verify(placeRepository, times(1)).findById(placeId);
        verify(selfStudyScheduleRepository, times(1))
                .getStudentScheduleByPlaceAndDayAndPeriod(placeId, day, period);
        verify(additionalSelfStudyScheduleRepository, times(1))
                .getStudentScheduleByPlaceAndDayAndPeriod(placeId, day, period);
        verify(leaveSeatScheduleRepository, times(1))
                .getStudentScheduleByPlaceAndDayAndPeriod(placeId, day, period);
        verify(afterSchoolScheduleRepository, times(1))
                .getStudentScheduleByPlaceAndDayAndPeriod(placeId, day, period);
        verify(afterSchoolScheduleRepository, times(1))
                .getReinforcementStudentScheduleByPlaceAndDayAndPeriod(placeId, day, period);
    }

    @Test
    @DisplayName("방과후 보강 스케줄이 층별 장소 사용 인원에 포함된다")
    void shouldIncludeReinforcementScheduleInFloorCount() {
        // Given: 방과후 보강 스케줄이 있는 날짜와 교시가 주어졌을 때
        LocalDate day = LocalDate.of(2024, 1, 15);
        SchoolPeriod period = SchoolPeriod.ONE_PERIOD;

        ScheduleEntity reinforcementSchedule = mock(ScheduleEntity.class);

        Map<ScheduleType, List<ScheduleEntity>> placeFillScheduleMap = Map.of(
                ScheduleType.SELF_STUDY, List.of(),
                ScheduleType.ADDITIONAL_SELF_STUDY, List.of(),
                ScheduleType.LEAVE_SEAT, List.of(),
                ScheduleType.AFTER_SCHOOL, List.of(),
                ScheduleType.AFTER_SCHOOL_REINFORCEMENT, List.of(reinforcementSchedule)
        );

        Map<Integer, Long> reinforcementCountMap = Map.of(2, 3L, 3, 2L);

        FloorStateResponse floor2Response = FloorStateResponse.builder()
                .floor(2).count(3L).build();
        FloorStateResponse floor3Response = FloorStateResponse.builder()
                .floor(3).count(2L).build();

        given(studentScheduleRepository.findPlaceBasedSchedulesByDayAndPeriodAndTypeIn(eq(day), eq(period), anyList()))
                .willReturn(placeFillScheduleMap);
        given(selfStudyScheduleRepository.getSelfStudyPlaceCount(List.of())).willReturn(Map.of());
        given(additionalSelfStudyScheduleRepository.getAdditionalSelfStudyPlaceCount(List.of())).willReturn(Map.of());
        given(leaveSeatScheduleRepository.getLeaveSeatPlaceCount(List.of())).willReturn(Map.of());
        given(afterSchoolScheduleRepository.getAfterSchoolPlaceCount(List.of())).willReturn(Map.of());
        given(afterSchoolScheduleRepository.getAfterSchoolReinforcementPlaceCount(List.of(reinforcementSchedule)))
                .willReturn(reinforcementCountMap);
        given(placeStudentScheduleMapper.toFloorStateResponses(anyMap()))
                .willReturn(List.of(floor2Response, floor3Response));

        // When: 모든 층의 장소 사용 인원을 조회하면
        List<FloorStateResponse> result = placeStudentScheduleService.getAllFloorsPlaceCount(day, period);

        // Then: 방과후 보강 스케줄이 포함된 결과가 반환된다
        assertThat(result).hasSize(2);

        verify(afterSchoolScheduleRepository, times(1))
                .getAfterSchoolReinforcementPlaceCount(List.of(reinforcementSchedule));
    }

    @Test
    @DisplayName("방과후 보강 장소가 특정 층의 장소 상태 조회에 포함된다")
    void shouldIncludeReinforcementPlaceInFloorState() {
        // Given: 방과후 보강이 있는 층, 날짜, 교시가 주어졌을 때
        Integer floor = 2;
        LocalDate day = LocalDate.of(2024, 1, 15);
        SchoolPeriod period = SchoolPeriod.ONE_PERIOD;

        ScheduleEntity reinforcementSchedule = mock(ScheduleEntity.class);

        Map<ScheduleType, List<ScheduleEntity>> placeFillScheduleMap = Map.of(
                ScheduleType.SELF_STUDY, List.of(),
                ScheduleType.ADDITIONAL_SELF_STUDY, List.of(),
                ScheduleType.LEAVE_SEAT, List.of(),
                ScheduleType.AFTER_SCHOOL, List.of(),
                ScheduleType.AFTER_SCHOOL_REINFORCEMENT, List.of(reinforcementSchedule)
        );

        PlaceEntity reinforcementPlace = mock(PlaceEntity.class);
        PlaceScheduleDto reinforcementPlaceSchedule = new PlaceScheduleDto(reinforcementPlace, ScheduleType.AFTER_SCHOOL_REINFORCEMENT);

        PlaceStateResponse placeState = PlaceStateResponse.builder()
                .placeId(1L).placeName("2층 보강실").state(ScheduleType.AFTER_SCHOOL_REINFORCEMENT).build();

        given(studentScheduleRepository.findPlaceBasedSchedulesByDayAndPeriodAndTypeIn(eq(day), eq(period), anyList()))
                .willReturn(placeFillScheduleMap);
        given(selfStudyScheduleRepository.getPlaceScheduleByFloor(List.of(), floor)).willReturn(List.of());
        given(additionalSelfStudyScheduleRepository.getPlaceScheduleByFloor(List.of(), floor)).willReturn(List.of());
        given(leaveSeatScheduleRepository.getPlaceScheduleByFloor(List.of(), floor)).willReturn(List.of());
        given(afterSchoolScheduleRepository.getPlaceScheduleByFloor(List.of(), floor)).willReturn(List.of());
        given(afterSchoolScheduleRepository.getReinforcementPlaceScheduleByFloor(List.of(reinforcementSchedule), floor))
                .willReturn(List.of(reinforcementPlaceSchedule));
        given(placeStudentScheduleMapper.toPlaceStateResponses(anyList()))
                .willReturn(List.of(placeState));

        // When: 특정 층의 장소 상태를 조회하면
        List<PlaceStateResponse> result = placeStudentScheduleService.getPlaceStatesByFloor(floor, day, period);

        // Then: 방과후 보강 장소가 포함된 결과가 반환된다
        assertThat(result).hasSize(1);
        assertThat(result.get(0).state()).isEqualTo(ScheduleType.AFTER_SCHOOL_REINFORCEMENT);

        verify(afterSchoolScheduleRepository, times(1))
                .getReinforcementPlaceScheduleByFloor(List.of(reinforcementSchedule), floor);
    }

    @Test
    @DisplayName("방과후 보강 학생이 특정 장소의 학생 스케줄 조회에 포함된다")
    void shouldIncludeReinforcementStudentInPlaceSchedule() {
        // Given: 방과후 보강 학생이 있는 장소 ID가 주어졌을 때
        Long placeId = 1L;
        LocalDate day = LocalDate.of(2024, 1, 15);
        SchoolPeriod period = SchoolPeriod.ONE_PERIOD;

        PlaceEntity place = mock(PlaceEntity.class);
        lenient().when(place.getId()).thenReturn(placeId);
        lenient().when(place.getName()).thenReturn("보강실");

        StudentScheduleDto reinforcementStudent = new StudentScheduleDto(
                1L, 1, 1, 1, "김보강", day, period, 10L, ScheduleType.AFTER_SCHOOL_REINFORCEMENT
        );

        StudentScheduleResponse reinforcementResponse = StudentScheduleResponse.builder()
                .studentId(1L).number(1).name("김보강")
                .state(ScheduleType.AFTER_SCHOOL_REINFORCEMENT).scheduleId(10L).build();

        PlaceStudentScheduleResponse expectedResponse = PlaceStudentScheduleResponse.builder()
                .placeId(placeId)
                .placeName("보강실")
                .students(List.of(reinforcementResponse))
                .build();

        given(placeRepository.findById(placeId)).willReturn(Optional.of(place));
        given(selfStudyScheduleRepository.getStudentScheduleByPlaceAndDayAndPeriod(placeId, day, period))
                .willReturn(List.of());
        given(additionalSelfStudyScheduleRepository.getStudentScheduleByPlaceAndDayAndPeriod(placeId, day, period))
                .willReturn(List.of());
        given(leaveSeatScheduleRepository.getStudentScheduleByPlaceAndDayAndPeriod(placeId, day, period))
                .willReturn(List.of());
        given(afterSchoolScheduleRepository.getStudentScheduleByPlaceAndDayAndPeriod(placeId, day, period))
                .willReturn(List.of());
        given(afterSchoolScheduleRepository.getReinforcementStudentScheduleByPlaceAndDayAndPeriod(placeId, day, period))
                .willReturn(List.of(reinforcementStudent));
        lenient().when(studentScheduleMapper.toStudentScheduleResponse(reinforcementStudent)).thenReturn(reinforcementResponse);
        given(placeStudentScheduleMapper.toPlaceStudentScheduleResponse(eq(place), anyList()))
                .willReturn(expectedResponse);

        // When: 특정 장소의 학생 스케줄을 조회하면
        PlaceStudentScheduleResponse result = placeStudentScheduleService.getStudentsByPlaceId(placeId, day, period);

        // Then: 방과후 보강 학생이 포함된 결과가 반환된다
        assertThat(result).isNotNull();
        assertThat(result.students()).hasSize(1);
        assertThat(result.students().get(0).state()).isEqualTo(ScheduleType.AFTER_SCHOOL_REINFORCEMENT);

        verify(afterSchoolScheduleRepository, times(1))
                .getReinforcementStudentScheduleByPlaceAndDayAndPeriod(placeId, day, period);
    }
}
