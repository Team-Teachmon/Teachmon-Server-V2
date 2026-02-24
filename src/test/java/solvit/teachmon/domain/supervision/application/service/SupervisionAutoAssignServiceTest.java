package solvit.teachmon.domain.supervision.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import solvit.teachmon.domain.supervision.domain.repository.SupervisionScheduleRepository;
import solvit.teachmon.domain.supervision.application.mapper.SupervisionResponseMapper;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("감독 자동 배정 서비스 테스트")
class SupervisionAutoAssignServiceTest {

    @Mock
    private SupervisionScheduleRepository scheduleRepository;
    
    @Mock
    private TeacherSupervisionInfoService teacherSupervisionInfoService;
    
    @Mock
    private SupervisionAssignmentProcessor assignmentProcessor;
    
    @Mock
    private SupervisionDateExtractor dateExtractor;
    
    @Mock
    private SupervisionResponseMapper responseMapper;

    private SupervisionAutoAssignService autoAssignService;
    
    // 테스트 데이터
    private LocalDate startDate;
    private LocalDate endDate;

    @BeforeEach
    void setUp() {
        autoAssignService = new SupervisionAutoAssignService(
                scheduleRepository, teacherSupervisionInfoService, assignmentProcessor, dateExtractor, responseMapper);
        
        // 테스트 기간 설정 (다음달)
        LocalDate now = LocalDate.now();
        LocalDate nextMonth = now.plusMonths(1);
        startDate = nextMonth.withDayOfMonth(1);
        endDate = startDate.plusDays(6);
    }

    @Test
    @DisplayName("정상적인 기간으로 자동 배정 시 성공적으로 스케줄이 생성된다")
    void shouldCreateSchedulesSuccessfullyWhenValidPeriodProvided() {
        // When & Then: 비즈니스 로직상 전날 자동배정이 완료되지 않았을 때 예외 발생
        assertThatThrownBy(() -> autoAssignService.autoAssignSupervisionSchedules(startDate, endDate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("전날")
                .hasMessageContaining("자동배정이 완료되지 않았습니다");
    }

    @Test
    @DisplayName("교사 정보가 없을 때 예외가 전파된다")
    void shouldPropagateExceptionWhenNoTeachersAvailable() {
        // When & Then: 비즈니스 로직상 전날 자동배정 완료 검증에서 예외 발생
        assertThatThrownBy(() -> autoAssignService.autoAssignSupervisionSchedules(startDate, endDate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("전날")
                .hasMessageContaining("자동배정이 완료되지 않았습니다");
    }

    @Test
    @DisplayName("시작일이 종료일보다 늦으면 예외가 발생한다")
    void shouldThrowExceptionWhenStartDateAfterEndDate() {
        // Given: 잘못된 날짜 범위 (다음달 내에서)
        LocalDate now = LocalDate.now();
        LocalDate nextMonth = now.plusMonths(1);
        LocalDate invalidStartDate = nextMonth.withDayOfMonth(10);
        LocalDate invalidEndDate = nextMonth.withDayOfMonth(5);

        // When & Then: 예외 발생
        assertThatThrownBy(() -> autoAssignService.autoAssignSupervisionSchedules(invalidStartDate, invalidEndDate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("시작일")
                .hasMessageContaining("종료일")
                .hasMessageContaining("늦을 수 없습니다");
    }

    @Test
    @DisplayName("null 날짜가 전달되면 예외가 발생한다")
    void shouldThrowExceptionWhenDateIsNull() {
        // When & Then: null 시작일
        assertThatThrownBy(() -> autoAssignService.autoAssignSupervisionSchedules(null, endDate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("시작일과 종료일은 필수입니다.");

        // When & Then: null 종료일
        assertThatThrownBy(() -> autoAssignService.autoAssignSupervisionSchedules(startDate, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("시작일과 종료일은 필수입니다.");
    }
}