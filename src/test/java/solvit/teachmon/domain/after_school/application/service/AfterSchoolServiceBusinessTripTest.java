package solvit.teachmon.domain.after_school.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import solvit.teachmon.domain.after_school.domain.entity.AfterSchoolBusinessTripEntity;
import solvit.teachmon.domain.after_school.domain.entity.AfterSchoolEntity;
import solvit.teachmon.domain.after_school.domain.repository.AfterSchoolBusinessTripRepository;
import solvit.teachmon.domain.after_school.domain.repository.AfterSchoolReinforcementRepository;
import solvit.teachmon.domain.after_school.domain.repository.AfterSchoolRepository;
import solvit.teachmon.domain.after_school.domain.service.AfterSchoolStudentDomainService;
import solvit.teachmon.domain.after_school.exception.AfterSchoolBusinessTripScheduleNotFoundException;
import solvit.teachmon.domain.after_school.exception.AfterSchoolNotFoundException;
import solvit.teachmon.domain.after_school.presentation.dto.request.AfterSchoolBusinessTripRequestDto;
import solvit.teachmon.domain.after_school.presentation.dto.response.AfterSchoolAffordableBusinessResponseDto;
import solvit.teachmon.domain.branch.domain.entity.BranchEntity;
import solvit.teachmon.domain.branch.domain.repository.BranchRepository;
import solvit.teachmon.domain.branch.exception.BranchNotFoundException;
import solvit.teachmon.domain.management.student.domain.repository.StudentRepository;
import solvit.teachmon.domain.management.teacher.domain.repository.SupervisionBanDayRepository;
import solvit.teachmon.domain.place.domain.repository.PlaceRepository;
import solvit.teachmon.domain.student_schedule.domain.entity.StudentScheduleEntity;
import solvit.teachmon.domain.student_schedule.domain.repository.StudentScheduleRepository;
import solvit.teachmon.domain.student_schedule.domain.repository.ScheduleRepository;
import solvit.teachmon.domain.student_schedule.domain.repository.schedules.AfterSchoolScheduleRepository;
import solvit.teachmon.domain.user.domain.repository.TeacherRepository;
import solvit.teachmon.global.enums.SchoolPeriod;
import solvit.teachmon.global.enums.WeekDay;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@DisplayName("방과후 출장 서비스 테스트")
class AfterSchoolServiceBusinessTripTest {

    @Mock
    private AfterSchoolStudentDomainService afterSchoolStudentDomainService;
    @Mock
    private AfterSchoolScheduleService afterSchoolScheduleService;
    @Mock
    private SupervisionBanDayRepository supervisionBanDayRepository;
    @Mock
    private AfterSchoolRepository afterSchoolRepository;
    @Mock
    private AfterSchoolBusinessTripRepository afterSchoolBusinessTripRepository;
    @Mock
    private AfterSchoolReinforcementRepository afterSchoolReinforcementRepository;
    @Mock
    private TeacherRepository teacherRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private BranchRepository branchRepository;
    @Mock
    private PlaceRepository placeRepository;
    @Mock
    private StudentScheduleRepository studentScheduleRepository;
    @Mock
    private ScheduleRepository scheduleRepository;
    @Mock
    private AfterSchoolScheduleRepository afterSchoolScheduleRepository;

    private AfterSchoolService afterSchoolService;
    private AfterSchoolEntity afterSchool;
    private AfterSchoolBusinessTripRequestDto businessTripRequest;

    @BeforeEach
    void setUp() {
        afterSchoolService = new AfterSchoolService(
                afterSchoolStudentDomainService,
                supervisionBanDayRepository,
                afterSchoolRepository,
                afterSchoolBusinessTripRepository,
                afterSchoolReinforcementRepository,
                teacherRepository,
                studentRepository,
                branchRepository,
                placeRepository,
                studentScheduleRepository,
                scheduleRepository,
                afterSchoolScheduleService,
                afterSchoolScheduleRepository
        );

        // Mock을 사용해서 AfterSchoolEntity 생성
        afterSchool = mock(AfterSchoolEntity.class);

        businessTripRequest = new AfterSchoolBusinessTripRequestDto(
                LocalDate.now().plusDays(10), // 현재 날짜보다 10일 후
                1L
        );
    }

    @Test
    @DisplayName("유효한 요청으로 출장을 성공적으로 생성한다")
    void shouldCreateBusinessTripSuccessfully() {
        // Given
        given(afterSchoolRepository.findWithAllRelations(1L))
                .willReturn(Optional.of(afterSchool));
        given(afterSchoolBusinessTripRepository.save(any(AfterSchoolBusinessTripEntity.class)))
                .willReturn(any(AfterSchoolBusinessTripEntity.class));

        // When
        afterSchoolService.createBusinessTrip(businessTripRequest);

        // Then
        verify(afterSchoolRepository).findWithAllRelations(1L);
        verify(afterSchoolBusinessTripRepository).save(any(AfterSchoolBusinessTripEntity.class));
    }

    @Test
    @DisplayName("존재하지 않는 방과후 ID로 출장 생성 시 예외가 발생한다")
    void shouldThrowExceptionWhenAfterSchoolNotExists() {
        // Given
        given(afterSchoolRepository.findWithAllRelations(1L))
                .willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> afterSchoolService.createBusinessTrip(businessTripRequest))
                .isInstanceOf(AfterSchoolNotFoundException.class);

        verify(afterSchoolRepository).findWithAllRelations(1L);
        verify(afterSchoolBusinessTripRepository, never()).save(any());
    }

    @Test
    @DisplayName("출장 가능한 날짜 목록을 성공적으로 조회한다")
    void shouldGetBusinessTripDatesSuccessfully() {
        // Given
        Long afterSchoolId = 1L;
        LocalDate startDay = LocalDate.of(2025, 3, 3); // 월요일
        LocalDate afterSchoolEndDay = LocalDate.of(2025, 3, 31);
        
        BranchEntity branch = mock(BranchEntity.class);
        given(branch.getStartDay()).willReturn(startDay);
        given(branch.getAfterSchoolEndDay()).willReturn(afterSchoolEndDay);
        
        given(branchRepository.findCurrentBranch(any(LocalDate.class)))
                .willReturn(Optional.of(branch));
        given(afterSchoolRepository.findWithAllRelations(afterSchoolId))
                .willReturn(Optional.of(afterSchool));
        given(afterSchool.getWeekDay()).willReturn(WeekDay.MON);
        
        // 배치 쿼리로 빈 목록 반환 (출장이 없음을 의미)
        given(afterSchoolBusinessTripRepository.findBusinessTripDatesByAfterSchoolAndDateRange(
                eq(afterSchool), eq(startDay), eq(afterSchoolEndDay)))
                .willReturn(List.of());

        // When
        AfterSchoolAffordableBusinessResponseDto result = afterSchoolService.getBusinessTrip(afterSchoolId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.dates()).isNotEmpty();
        
        // 모든 날짜가 월요일인지 확인
        result.dates().forEach(date -> 
            assertThat(date.getDayOfWeek()).isEqualTo(WeekDay.MON.toDayOfWeek())
        );
        
        // 날짜가 범위 내에 있는지 확인
        result.dates().forEach(date -> {
            assertThat(date).isAfterOrEqualTo(startDay);
            assertThat(date).isBefore(afterSchoolEndDay);
        });

        verify(branchRepository).findCurrentBranch(any(LocalDate.class));
        verify(afterSchoolRepository).findWithAllRelations(afterSchoolId);
        verify(afterSchoolBusinessTripRepository).findBusinessTripDatesByAfterSchoolAndDateRange(
                eq(afterSchool), eq(startDay), eq(afterSchoolEndDay));
    }

    @Test
    @DisplayName("현재 분기를 찾을 수 없을 때 예외가 발생한다")
    void shouldThrowExceptionWhenBranchNotFound() {
        // Given
        Long afterSchoolId = 1L;
        given(branchRepository.findCurrentBranch(any(LocalDate.class)))
                .willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> afterSchoolService.getBusinessTrip(afterSchoolId))
                .isInstanceOf(BranchNotFoundException.class);

        verify(branchRepository).findCurrentBranch(any(LocalDate.class));
        verify(afterSchoolRepository, never()).findWithAllRelations(any());
    }

    @Test
    @DisplayName("방과후를 찾을 수 없을 때 예외가 발생한다")
    void shouldThrowExceptionWhenAfterSchoolNotFoundForBusinessTrip() {
        // Given
        Long afterSchoolId = 1L;
        BranchEntity branch = mock(BranchEntity.class);
        given(branchRepository.findCurrentBranch(any(LocalDate.class)))
                .willReturn(Optional.of(branch));
        given(afterSchoolRepository.findWithAllRelations(afterSchoolId))
                .willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> afterSchoolService.getBusinessTrip(afterSchoolId))
                .isInstanceOf(AfterSchoolNotFoundException.class);

        verify(branchRepository).findCurrentBranch(any(LocalDate.class));
        verify(afterSchoolRepository).findWithAllRelations(afterSchoolId);
    }

    @Test
    @DisplayName("화요일 방과후의 출장 가능 날짜를 정확히 조회한다")
    void shouldGetBusinessTripDatesForTuesday() {
        // Given
        Long afterSchoolId = 1L;
        LocalDate startDay = LocalDate.of(2025, 3, 3); // 월요일
        LocalDate afterSchoolEndDay = LocalDate.of(2025, 3, 31);
        
        BranchEntity branch = mock(BranchEntity.class);
        given(branch.getStartDay()).willReturn(startDay);
        given(branch.getAfterSchoolEndDay()).willReturn(afterSchoolEndDay);
        
        given(branchRepository.findCurrentBranch(any(LocalDate.class)))
                .willReturn(Optional.of(branch));
        given(afterSchoolRepository.findWithAllRelations(afterSchoolId))
                .willReturn(Optional.of(afterSchool));
        given(afterSchool.getWeekDay()).willReturn(WeekDay.TUE);
        
        // 배치 쿼리로 빈 목록 반환 (출장이 없음을 의미)
        given(afterSchoolBusinessTripRepository.findBusinessTripDatesByAfterSchoolAndDateRange(
                eq(afterSchool), eq(startDay), eq(afterSchoolEndDay)))
                .willReturn(List.of());

        // When
        AfterSchoolAffordableBusinessResponseDto result = afterSchoolService.getBusinessTrip(afterSchoolId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.dates()).isNotEmpty();
        
        // 모든 날짜가 화요일인지 확인
        result.dates().forEach(date -> 
            assertThat(date.getDayOfWeek()).isEqualTo(WeekDay.TUE.toDayOfWeek())
        );
        
        // 첫 번째 날짜가 3월 4일(화요일)인지 확인
        assertThat(result.dates().get(0)).isEqualTo(LocalDate.of(2025, 3, 4));
    }

    @Test
    @DisplayName("이번주 출장 등록 시 스케줄이 삭제된다")
    void shouldDeleteSchedulesWhenBusinessTripIsCurrentWeek() {
        // Given
        LocalDate currentWeekDate = LocalDate.now();
        AfterSchoolBusinessTripRequestDto currentWeekRequest = new AfterSchoolBusinessTripRequestDto(
                currentWeekDate, 1L
        );
        
        StudentScheduleEntity schedule1 = mock(StudentScheduleEntity.class);
        StudentScheduleEntity schedule2 = mock(StudentScheduleEntity.class);
        List<StudentScheduleEntity> schedules = List.of(schedule1, schedule2);
        
        given(afterSchoolRepository.findWithAllRelations(1L)).willReturn(Optional.of(afterSchool));
        given(afterSchool.getPeriod()).willReturn(SchoolPeriod.ONE_PERIOD);
        given(studentScheduleRepository.findAllByAfterSchoolAndDayAndPeriod(
                afterSchool, currentWeekDate, SchoolPeriod.ONE_PERIOD)).willReturn(schedules);
        given(schedule1.getId()).willReturn(1L);
        given(schedule2.getId()).willReturn(2L);

        // When
        afterSchoolService.createBusinessTrip(currentWeekRequest);

        // Then
        verify(afterSchoolBusinessTripRepository).save(any(AfterSchoolBusinessTripEntity.class));
        verify(studentScheduleRepository).findAllByAfterSchoolAndDayAndPeriod(
                afterSchool, currentWeekDate, SchoolPeriod.ONE_PERIOD);
        verify(scheduleRepository).deleteTopSchedulesByStudentScheduleIds(List.of(1L, 2L));
    }

    @Test
    @DisplayName("다음주 출장 등록 시 스케줄이 삭제되지 않는다")
    void shouldNotDeleteSchedulesWhenBusinessTripIsNextWeek() {
        // Given
        LocalDate nextWeekDate = LocalDate.now().plusWeeks(1);
        AfterSchoolBusinessTripRequestDto nextWeekRequest = new AfterSchoolBusinessTripRequestDto(
                nextWeekDate, 1L
        );
        
        given(afterSchoolRepository.findWithAllRelations(1L)).willReturn(Optional.of(afterSchool));

        // When
        afterSchoolService.createBusinessTrip(nextWeekRequest);

        // Then
        verify(afterSchoolBusinessTripRepository).save(any(AfterSchoolBusinessTripEntity.class));
        verify(studentScheduleRepository, never()).findAllByAfterSchoolAndDayAndPeriod(any(), any(), any());
        verify(scheduleRepository, never()).deleteTopSchedulesByStudentScheduleIds(any());
    }

    @Test
    @DisplayName("이번주 출장 등록 시 스케줄이 없으면 예외 발생")
    void shouldThrowExceptionWhenNoSchedulesFoundForCurrentWeekBusinessTrip() {
        // Given
        LocalDate currentWeekDate = LocalDate.now();
        AfterSchoolBusinessTripRequestDto currentWeekRequest = new AfterSchoolBusinessTripRequestDto(
                currentWeekDate, 1L
        );
        
        given(afterSchoolRepository.findWithAllRelations(1L)).willReturn(Optional.of(afterSchool));
        given(afterSchool.getName()).willReturn("테스트 방과후");
        given(afterSchool.getPeriod()).willReturn(SchoolPeriod.ONE_PERIOD);
        given(studentScheduleRepository.findAllByAfterSchoolAndDayAndPeriod(
                afterSchool, currentWeekDate, SchoolPeriod.ONE_PERIOD)).willReturn(Collections.emptyList());

        // When & Then
        assertThatThrownBy(() -> afterSchoolService.createBusinessTrip(currentWeekRequest))
                .isInstanceOf(AfterSchoolBusinessTripScheduleNotFoundException.class)
                .hasMessageContaining("테스트 방과후");

        verify(afterSchoolBusinessTripRepository).save(any(AfterSchoolBusinessTripEntity.class));
        verify(scheduleRepository, never()).deleteTopSchedulesByStudentScheduleIds(any());
    }

    @Test
    @DisplayName("출장 엔티티가 올바른 정보로 저장된다")
    void shouldSaveBusinessTripEntityWithCorrectInfo() {
        // Given
        LocalDate businessTripDate = LocalDate.now().plusDays(30);
        AfterSchoolBusinessTripRequestDto request = new AfterSchoolBusinessTripRequestDto(
                businessTripDate, 1L
        );
        
        given(afterSchoolRepository.findWithAllRelations(1L)).willReturn(Optional.of(afterSchool));

        // When
        afterSchoolService.createBusinessTrip(request);

        // Then
        verify(afterSchoolBusinessTripRepository).save(argThat(businessTrip -> 
                businessTrip.getDay().equals(businessTripDate) &&
                businessTrip.getAfterSchool().equals(afterSchool)
        ));
    }
  
    @Test
    @DisplayName("이미 출장이 등록된 날짜는 출장 가능한 날짜 목록에서 제외된다")
    void shouldExcludeAlreadyScheduledBusinessTripDates() {
        // Given
        Long afterSchoolId = 1L;
        LocalDate startDay = LocalDate.of(2025, 3, 3); // 월요일
        LocalDate afterSchoolEndDay = LocalDate.of(2025, 3, 31);
        LocalDate existingBusinessTripDate = LocalDate.of(2025, 3, 10); // 월요일
        
        BranchEntity branch = mock(BranchEntity.class);
        given(branch.getStartDay()).willReturn(startDay);
        given(branch.getAfterSchoolEndDay()).willReturn(afterSchoolEndDay);
        
        given(branchRepository.findCurrentBranch(any(LocalDate.class)))
                .willReturn(Optional.of(branch));
        given(afterSchoolRepository.findWithAllRelations(afterSchoolId))
                .willReturn(Optional.of(afterSchool));
        given(afterSchool.getWeekDay()).willReturn(WeekDay.MON);
        
        // 배치 쿼리로 이미 등록된 출장 날짜 목록 반환
        given(afterSchoolBusinessTripRepository.findBusinessTripDatesByAfterSchoolAndDateRange(
                eq(afterSchool), eq(startDay), eq(afterSchoolEndDay)))
                .willReturn(List.of(existingBusinessTripDate));

        // When
        AfterSchoolAffordableBusinessResponseDto result = afterSchoolService.getBusinessTrip(afterSchoolId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.dates()).isNotEmpty();
        
        // 이미 출장이 등록된 날짜는 목록에 포함되지 않아야 함
        assertThat(result.dates()).doesNotContain(existingBusinessTripDate);
        
        // 모든 날짜가 월요일인지 확인
        result.dates().forEach(date -> 
            assertThat(date.getDayOfWeek()).isEqualTo(WeekDay.MON.toDayOfWeek())
        );

        verify(branchRepository).findCurrentBranch(any(LocalDate.class));
        verify(afterSchoolRepository).findWithAllRelations(afterSchoolId);
        verify(afterSchoolBusinessTripRepository).findBusinessTripDatesByAfterSchoolAndDateRange(
                eq(afterSchool), eq(startDay), eq(afterSchoolEndDay));
    }
}
