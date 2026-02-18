package solvit.teachmon.domain.self_study.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import solvit.teachmon.domain.management.student.domain.entity.StudentEntity;
import solvit.teachmon.domain.place.domain.entity.PlaceEntity;
import solvit.teachmon.domain.place.domain.repository.PlaceRepository;
import solvit.teachmon.domain.self_study.application.mapper.AdditionalSelfStudyMapper;
import solvit.teachmon.domain.self_study.domain.entity.AdditionalSelfStudyEntity;
import solvit.teachmon.domain.self_study.domain.repository.AdditionalSelfStudyRepository;
import solvit.teachmon.domain.self_study.exception.AdditionalSelfStudyNotFoundException;
import solvit.teachmon.domain.self_study.presentation.dto.request.AdditionalSelfStudySetRequest;
import solvit.teachmon.domain.student_schedule.domain.entity.ScheduleEntity;
import solvit.teachmon.domain.student_schedule.domain.entity.StudentScheduleEntity;
import solvit.teachmon.domain.student_schedule.domain.entity.schedules.AdditionalSelfStudyScheduleEntity;
import solvit.teachmon.domain.student_schedule.domain.enums.ScheduleType;
import solvit.teachmon.domain.student_schedule.application.service.StudentScheduleGenerator;
import solvit.teachmon.domain.student_schedule.domain.repository.ScheduleRepository;
import solvit.teachmon.domain.student_schedule.domain.repository.StudentScheduleRepository;
import solvit.teachmon.domain.student_schedule.domain.repository.schedules.AdditionalSelfStudyScheduleRepository;
import solvit.teachmon.global.enums.SchoolPeriod;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("추가 자습 서비스 테스트")
class AdditionalSelfStudyServiceTest {

    @Mock
    private AdditionalSelfStudyRepository additionalSelfStudyRepository;

    @Mock
    private AdditionalSelfStudyMapper additionalSelfStudyMapper;

    @Mock
    private StudentScheduleRepository studentScheduleRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private AdditionalSelfStudyScheduleRepository additionalSelfStudyScheduleRepository;

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private StudentScheduleGenerator studentScheduleGenerator;

    @InjectMocks
    private AdditionalSelfStudyService additionalSelfStudyService;

    @Test
    @DisplayName("현재 주의 날짜로 추가 자습을 설정하면 즉시 스케줄이 생성된다")
    void shouldApplySchedulesImmediatelyWhenSettingForCurrentWeek() {
        // Given: 현재 주의 날짜로 추가 자습 요청이 있을 때
        LocalDate currentWeekDate = LocalDate.now().with(DayOfWeek.WEDNESDAY);
        AdditionalSelfStudySetRequest request = new AdditionalSelfStudySetRequest(
                currentWeekDate,
                1,
                List.of(SchoolPeriod.SEVEN_PERIOD)
        );

        AdditionalSelfStudyEntity additionalSelfStudy = createMockAdditionalSelfStudy(
                1L, currentWeekDate, SchoolPeriod.SEVEN_PERIOD, 1
        );
        given(additionalSelfStudyMapper.toEntities(request))
                .willReturn(List.of(additionalSelfStudy));

        StudentScheduleEntity studentSchedule1 = createMockStudentSchedule(
                1L, currentWeekDate, SchoolPeriod.SEVEN_PERIOD, 1, 1
        );
        StudentScheduleEntity studentSchedule2 = createMockStudentSchedule(
                2L, currentWeekDate, SchoolPeriod.SEVEN_PERIOD, 1, 2
        );
        given(studentScheduleGenerator.findOrCreateStudentSchedules(1, currentWeekDate, SchoolPeriod.SEVEN_PERIOD))
                .willReturn(List.of(studentSchedule1, studentSchedule2));

        given(scheduleRepository.findLastStackOrderByStudentScheduleId(anyLong()))
                .willReturn(0);

        ScheduleEntity mockSchedule = mock(ScheduleEntity.class);
        given(mockSchedule.getType()).willReturn(ScheduleType.ADDITIONAL_SELF_STUDY);
        given(scheduleRepository.save(any(ScheduleEntity.class)))
                .willReturn(mockSchedule);

        PlaceEntity mockPlace = mock(PlaceEntity.class);
        given(placeRepository.findAllByGradePrefix(1))
                .willReturn(Map.of(1, mockPlace, 2, mockPlace));
        given(placeRepository.checkPlaceAvailability(any(), any(), any()))
                .willReturn(false);

        // When: 추가 자습을 설정하면
        additionalSelfStudyService.setAdditionalSelfStudy(request);

        // Then: 추가 자습이 저장되고, 스케줄이 즉시 생성되어야 한다
        verify(additionalSelfStudyRepository).saveAll(List.of(additionalSelfStudy));
        verify(studentScheduleGenerator).findOrCreateStudentSchedules(1, currentWeekDate, SchoolPeriod.SEVEN_PERIOD);
        verify(scheduleRepository, times(2)).save(any(ScheduleEntity.class));
        verify(additionalSelfStudyScheduleRepository, times(2)).save(any(AdditionalSelfStudyScheduleEntity.class));
    }

    @Test
    @DisplayName("다음 주의 날짜로 추가 자습을 설정하면 스케줄이 즉시 생성되지 않는다")
    void shouldNotApplySchedulesImmediatelyWhenSettingForNextWeek() {
        // Given: 다음 주의 날짜로 추가 자습 요청이 있을 때
        LocalDate nextWeekDate = LocalDate.now().plusWeeks(1).with(DayOfWeek.MONDAY);
        AdditionalSelfStudySetRequest request = new AdditionalSelfStudySetRequest(
                nextWeekDate,
                1,
                List.of(SchoolPeriod.SEVEN_PERIOD)
        );

        AdditionalSelfStudyEntity additionalSelfStudy = createMockAdditionalSelfStudy(
                1L, nextWeekDate, SchoolPeriod.SEVEN_PERIOD, 1
        );
        given(additionalSelfStudyMapper.toEntities(request))
                .willReturn(List.of(additionalSelfStudy));

        // When: 추가 자습을 설정하면
        additionalSelfStudyService.setAdditionalSelfStudy(request);

        // Then: 추가 자습만 저장되고, 스케줄은 즉시 생성되지 않아야 한다
        verify(additionalSelfStudyRepository).saveAll(List.of(additionalSelfStudy));
        verify(studentScheduleRepository, never()).findAllByGradeAndDayAndPeriod(any(), any(), any());
        verify(scheduleRepository, never()).save(any(ScheduleEntity.class));
        verify(additionalSelfStudyScheduleRepository, never()).save(any(AdditionalSelfStudyScheduleEntity.class));
    }

    @Test
    @DisplayName("추가 자습을 삭제하면 엔티티가 제거된다")
    void shouldRemoveSchedulesImmediatelyWhenDeletingForCurrentWeek() {
        // Given: 추가 자습이 있을 때
        LocalDate currentWeekDate = LocalDate.now().with(DayOfWeek.WEDNESDAY);
        AdditionalSelfStudyEntity additionalSelfStudy = createMockAdditionalSelfStudy(
                1L, currentWeekDate, SchoolPeriod.SEVEN_PERIOD, 1
        );
        given(additionalSelfStudyRepository.findById(1L))
                .willReturn(Optional.of(additionalSelfStudy));

        // When: 추가 자습을 삭제하면
        additionalSelfStudyService.deleteAdditionalSelfStudy(1L);

        // Then: 추가 자습 엔티티가 삭제되어야 한다
        verify(additionalSelfStudyRepository).delete(additionalSelfStudy);
        verify(studentScheduleRepository, never()).findAllByGradeAndDayAndPeriod(any(), any(), any());
        verify(scheduleRepository, never()).deleteByStudentScheduleIdAndType(any(), any());
    }

    @Test
    @DisplayName("다음 주의 추가 자습을 삭제하면 스케줄이 즉시 제거되지 않는다")
    void shouldNotRemoveSchedulesImmediatelyWhenDeletingForNextWeek() {
        // Given: 다음 주의 추가 자습이 있을 때
        LocalDate nextWeekDate = LocalDate.now().plusWeeks(1).with(DayOfWeek.MONDAY);
        AdditionalSelfStudyEntity additionalSelfStudy = createMockAdditionalSelfStudy(
                1L, nextWeekDate, SchoolPeriod.SEVEN_PERIOD, 1
        );
        given(additionalSelfStudyRepository.findById(1L))
                .willReturn(Optional.of(additionalSelfStudy));

        // When: 추가 자습을 삭제하면
        additionalSelfStudyService.deleteAdditionalSelfStudy(1L);

        // Then: 스케줄은 즉시 제거되지 않고, 추가 자습 엔티티만 삭제되어야 한다
        verify(additionalSelfStudyRepository).delete(additionalSelfStudy);
        verify(studentScheduleRepository, never()).findAllByGradeAndDayAndPeriod(any(), any(), any());
        verify(scheduleRepository, never()).deleteByStudentScheduleIdAndType(any(), any());
    }

    @Test
    @DisplayName("존재하지 않는 추가 자습을 삭제하면 예외가 발생한다")
    void shouldThrowExceptionWhenDeletingNonExistentAdditionalSelfStudy() {
        // Given: 존재하지 않는 추가 자습 ID가 주어졌을 때
        given(additionalSelfStudyRepository.findById(999L))
                .willReturn(Optional.empty());

        // When & Then: 삭제를 시도하면 예외가 발생해야 한다
        assertThatThrownBy(() -> additionalSelfStudyService.deleteAdditionalSelfStudy(999L))
                .isInstanceOf(AdditionalSelfStudyNotFoundException.class)
                .hasMessage("추가 자습을 찾을 수 없습니다.");

        verify(additionalSelfStudyRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("여러 교시의 추가 자습을 설정하면 모든 교시에 대해 스케줄이 생성된다")
    void shouldCreateSchedulesForAllPeriodsWhenSettingMultiplePeriods() {
        // Given: 현재 주의 여러 교시로 추가 자습 요청이 있을 때
        LocalDate currentWeekDate = LocalDate.now().with(DayOfWeek.WEDNESDAY);
        AdditionalSelfStudySetRequest request = new AdditionalSelfStudySetRequest(
                currentWeekDate,
                1,
                List.of(SchoolPeriod.SEVEN_PERIOD, SchoolPeriod.EIGHT_AND_NINE_PERIOD)
        );

        AdditionalSelfStudyEntity additionalSelfStudy1 = createMockAdditionalSelfStudy(
                1L, currentWeekDate, SchoolPeriod.SEVEN_PERIOD, 1
        );
        AdditionalSelfStudyEntity additionalSelfStudy2 = createMockAdditionalSelfStudy(
                2L, currentWeekDate, SchoolPeriod.EIGHT_AND_NINE_PERIOD, 1
        );
        given(additionalSelfStudyMapper.toEntities(request))
                .willReturn(List.of(additionalSelfStudy1, additionalSelfStudy2));

        StudentScheduleEntity studentSchedule1 = createMockStudentSchedule(
                1L, currentWeekDate, SchoolPeriod.SEVEN_PERIOD, 1, 1
        );
        StudentScheduleEntity studentSchedule2 = createMockStudentSchedule(
                2L, currentWeekDate, SchoolPeriod.EIGHT_AND_NINE_PERIOD, 1, 1
        );

        given(studentScheduleGenerator.findOrCreateStudentSchedules(1, currentWeekDate, SchoolPeriod.SEVEN_PERIOD))
                .willReturn(List.of(studentSchedule1));
        given(studentScheduleGenerator.findOrCreateStudentSchedules(1, currentWeekDate, SchoolPeriod.EIGHT_AND_NINE_PERIOD))
                .willReturn(List.of(studentSchedule2));

        given(scheduleRepository.findLastStackOrderByStudentScheduleId(anyLong()))
                .willReturn(0);

        ScheduleEntity mockSchedule = mock(ScheduleEntity.class);
        given(mockSchedule.getType()).willReturn(ScheduleType.ADDITIONAL_SELF_STUDY);
        given(scheduleRepository.save(any(ScheduleEntity.class)))
                .willReturn(mockSchedule);

        PlaceEntity mockPlace = mock(PlaceEntity.class);
        given(placeRepository.findAllByGradePrefix(1))
                .willReturn(Map.of(1, mockPlace));
        given(placeRepository.checkPlaceAvailability(any(), any(), any()))
                .willReturn(false);

        // When: 추가 자습을 설정하면
        additionalSelfStudyService.setAdditionalSelfStudy(request);

        // Then: 모든 교시에 대해 스케줄이 생성되어야 한다
        verify(additionalSelfStudyRepository).saveAll(List.of(additionalSelfStudy1, additionalSelfStudy2));
        verify(studentScheduleGenerator).findOrCreateStudentSchedules(1, currentWeekDate, SchoolPeriod.SEVEN_PERIOD);
        verify(studentScheduleGenerator).findOrCreateStudentSchedules(1, currentWeekDate, SchoolPeriod.EIGHT_AND_NINE_PERIOD);
        verify(scheduleRepository, times(2)).save(any(ScheduleEntity.class));
        verify(additionalSelfStudyScheduleRepository, times(2)).save(any(AdditionalSelfStudyScheduleEntity.class));
    }

    @Test
    @DisplayName("현재 주의 월요일 날짜는 현재 주로 판단된다")
    void shouldRecognizeMondayOfCurrentWeekAsCurrentWeek() {
        // Given: 현재 주의 월요일 날짜로 추가 자습 요청이 있을 때
        LocalDate currentMonday = LocalDate.now().with(DayOfWeek.MONDAY);
        AdditionalSelfStudySetRequest request = new AdditionalSelfStudySetRequest(
                currentMonday,
                1,
                List.of(SchoolPeriod.SEVEN_PERIOD)
        );

        AdditionalSelfStudyEntity additionalSelfStudy = createMockAdditionalSelfStudy(
                1L, currentMonday, SchoolPeriod.SEVEN_PERIOD, 1
        );
        given(additionalSelfStudyMapper.toEntities(request))
                .willReturn(List.of(additionalSelfStudy));

        StudentScheduleEntity studentSchedule = createMockStudentSchedule(
                1L, currentMonday, SchoolPeriod.SEVEN_PERIOD, 1, 1
        );
        given(studentScheduleGenerator.findOrCreateStudentSchedules(1, currentMonday, SchoolPeriod.SEVEN_PERIOD))
                .willReturn(List.of(studentSchedule));

        given(scheduleRepository.findLastStackOrderByStudentScheduleId(anyLong()))
                .willReturn(0);

        ScheduleEntity mockSchedule = mock(ScheduleEntity.class);
        given(mockSchedule.getType()).willReturn(ScheduleType.ADDITIONAL_SELF_STUDY);
        given(scheduleRepository.save(any(ScheduleEntity.class)))
                .willReturn(mockSchedule);

        PlaceEntity mockPlace = mock(PlaceEntity.class);
        given(placeRepository.findAllByGradePrefix(1))
                .willReturn(Map.of(1, mockPlace));
        given(placeRepository.checkPlaceAvailability(any(), any(), any()))
                .willReturn(false);

        // When: 추가 자습을 설정하면
        additionalSelfStudyService.setAdditionalSelfStudy(request);

        // Then: 스케줄이 즉시 생성되어야 한다
        verify(studentScheduleGenerator).findOrCreateStudentSchedules(1, currentMonday, SchoolPeriod.SEVEN_PERIOD);
        verify(scheduleRepository).save(any(ScheduleEntity.class));
    }

    @Test
    @DisplayName("현재 주의 일요일 날짜는 현재 주로 판단된다")
    void shouldRecognizeSundayOfCurrentWeekAsCurrentWeek() {
        // Given: 현재 주의 일요일 날짜로 추가 자습 요청이 있을 때
        LocalDate currentSunday = LocalDate.now().with(DayOfWeek.SUNDAY);
        AdditionalSelfStudySetRequest request = new AdditionalSelfStudySetRequest(
                currentSunday,
                1,
                List.of(SchoolPeriod.SEVEN_PERIOD)
        );

        AdditionalSelfStudyEntity additionalSelfStudy = createMockAdditionalSelfStudy(
                1L, currentSunday, SchoolPeriod.SEVEN_PERIOD, 1
        );
        given(additionalSelfStudyMapper.toEntities(request))
                .willReturn(List.of(additionalSelfStudy));

        StudentScheduleEntity studentSchedule = createMockStudentSchedule(
                1L, currentSunday, SchoolPeriod.SEVEN_PERIOD, 1, 1
        );
        given(studentScheduleGenerator.findOrCreateStudentSchedules(1, currentSunday, SchoolPeriod.SEVEN_PERIOD))
                .willReturn(List.of(studentSchedule));

        given(scheduleRepository.findLastStackOrderByStudentScheduleId(anyLong()))
                .willReturn(0);

        ScheduleEntity mockSchedule = mock(ScheduleEntity.class);
        given(mockSchedule.getType()).willReturn(ScheduleType.ADDITIONAL_SELF_STUDY);
        given(scheduleRepository.save(any(ScheduleEntity.class)))
                .willReturn(mockSchedule);

        PlaceEntity mockPlace = mock(PlaceEntity.class);
        given(placeRepository.findAllByGradePrefix(1))
                .willReturn(Map.of(1, mockPlace));
        given(placeRepository.checkPlaceAvailability(any(), any(), any()))
                .willReturn(false);

        // When: 추가 자습을 설정하면
        additionalSelfStudyService.setAdditionalSelfStudy(request);

        // Then: 스케줄이 즉시 생성되어야 한다
        verify(studentScheduleGenerator).findOrCreateStudentSchedules(1, currentSunday, SchoolPeriod.SEVEN_PERIOD);
        verify(scheduleRepository).save(any(ScheduleEntity.class));
    }

    private AdditionalSelfStudyEntity createMockAdditionalSelfStudy(
            Long id, LocalDate day, SchoolPeriod period, Integer grade
    ) {
        AdditionalSelfStudyEntity entity = mock(AdditionalSelfStudyEntity.class);
        given(entity.getId()).willReturn(id);
        given(entity.getDay()).willReturn(day);
        given(entity.getPeriod()).willReturn(period);
        given(entity.getGrade()).willReturn(grade);
        return entity;
    }

    private StudentScheduleEntity createMockStudentSchedule(
            Long id, LocalDate day, SchoolPeriod period, Integer grade, Integer classNumber
    ) {
        StudentScheduleEntity entity = mock(StudentScheduleEntity.class);
        StudentEntity student = mock(StudentEntity.class);

        given(entity.getId()).willReturn(id);
        given(entity.getDay()).willReturn(day);
        given(entity.getPeriod()).willReturn(period);
        given(entity.getStudent()).willReturn(student);

        given(student.getGrade()).willReturn(grade);
        given(student.getClassNumber()).willReturn(classNumber);

        return entity;
    }
}
