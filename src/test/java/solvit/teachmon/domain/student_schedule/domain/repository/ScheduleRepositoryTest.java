package solvit.teachmon.domain.student_schedule.domain.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import solvit.teachmon.domain.student_schedule.domain.entity.ScheduleEntity;
import solvit.teachmon.domain.student_schedule.domain.entity.StudentScheduleEntity;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Schedule Repository 테스트")
class ScheduleRepositoryTest {

    @Mock
    private ScheduleRepository scheduleRepository;

    private StudentScheduleEntity studentSchedule1;
    private StudentScheduleEntity studentSchedule2;
    private LocalDate monday;
    private LocalDate sunday;
    private ScheduleEntity schedule1;
    private ScheduleEntity schedule2;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 설정
        monday = LocalDate.now().with(DayOfWeek.MONDAY);
        sunday = monday.with(DayOfWeek.SUNDAY);

        studentSchedule1 = mock(StudentScheduleEntity.class);
        given(studentSchedule1.getDay()).willReturn(monday);

        studentSchedule2 = mock(StudentScheduleEntity.class);
        given(studentSchedule2.getDay()).willReturn(monday.plusDays(1));

        schedule1 = mock(ScheduleEntity.class);
        given(schedule1.getStudentSchedule()).willReturn(studentSchedule1);

        schedule2 = mock(ScheduleEntity.class);
        given(schedule2.getStudentSchedule()).willReturn(studentSchedule2);
    }

    @Test
    @DisplayName("날짜 범위로 Schedule을 조회할 수 있다")
    void shouldFindSchedulesByDateRange() {
        // Given: 날짜 범위에 Schedule들이 있을 때
        List<ScheduleEntity> schedules = List.of(schedule1, schedule2);
        given(scheduleRepository.findAllByDateRange(monday, sunday))
                .willReturn(schedules);

        // When: 날짜 범위로 조회하면
        List<ScheduleEntity> found = scheduleRepository.findAllByDateRange(monday, sunday);

        // Then: 해당 범위의 모든 Schedule을 조회해야 한다
        assertThat(found).hasSize(2);
        assertThat(found).containsExactlyInAnyOrder(schedule1, schedule2);

        verify(scheduleRepository).findAllByDateRange(monday, sunday);
    }

    @Test
    @DisplayName("범위 외의 Schedule은 조회되지 않는다")
    void shouldNotFindSchedulesOutOfRange() {
        // Given: 날짜 범위 외의 Schedule이 있을 때
        List<ScheduleEntity> inRangeSchedules = List.of(schedule1);
        given(scheduleRepository.findAllByDateRange(monday, sunday))
                .willReturn(inRangeSchedules);

        // When: 날짜 범위로 조회하면
        List<ScheduleEntity> found = scheduleRepository.findAllByDateRange(monday, sunday);

        // Then: 범위 내의 Schedule만 조회되어야 한다
        assertThat(found).hasSize(1);
        assertThat(found).contains(schedule1);
    }

    @Test
    @DisplayName("Schedule 삭제 시 CASCADE로 인해 관련 LeaveSeatSchedule도 삭제된다")
    void shouldDeleteLeaveSeatScheduleWhenScheduleIsDeleted() {
        // Given: Schedule과 LeaveSeatSchedule이 연결되어 있을 때
        List<ScheduleEntity> schedules = List.of(schedule1);

        // When: Schedule을 삭제하면
        scheduleRepository.deleteAll(schedules);

        // Then: deleteAll이 호출되었는지 확인
        verify(scheduleRepository).deleteAll(schedules);
    }

    @Test
    @DisplayName("여러 Schedule을 한 번에 조회하고 삭제할 수 있다")
    void shouldFindAndDeleteMultipleSchedulesInDateRange() {
        // Given: 날짜 범위에 여러 Schedule이 있을 때
        ScheduleEntity schedule3 = mock(ScheduleEntity.class);
        StudentScheduleEntity studentSchedule3 = mock(StudentScheduleEntity.class);
        given(studentSchedule3.getDay()).willReturn(monday.plusDays(2));
        given(schedule3.getStudentSchedule()).willReturn(studentSchedule3);

        List<ScheduleEntity> schedules = List.of(schedule1, schedule2, schedule3);
        given(scheduleRepository.findAllByDateRange(monday, sunday))
                .willReturn(schedules);

        // When: 날짜 범위로 조회하고 삭제하면
        List<ScheduleEntity> found = scheduleRepository.findAllByDateRange(monday, sunday);
        assertThat(found).hasSize(3);

        scheduleRepository.deleteAll(found);

        // Then: deleteAll이 호출되었는지 확인
        verify(scheduleRepository).findAllByDateRange(monday, sunday);
        verify(scheduleRepository).deleteAll(found);
    }

    @Test
    @DisplayName("정확한 날짜 범위 경계를 처리한다")
    void shouldHandleExactDateBoundaries() {
        // Given: 월요일부터 일요일까지의 Schedule
        ScheduleEntity sundaySchedule = mock(ScheduleEntity.class);
        StudentScheduleEntity studentScheduleSunday = mock(StudentScheduleEntity.class);
        given(studentScheduleSunday.getDay()).willReturn(sunday);
        given(sundaySchedule.getStudentSchedule()).willReturn(studentScheduleSunday);

        List<ScheduleEntity> schedules = List.of(schedule1, sundaySchedule);
        given(scheduleRepository.findAllByDateRange(monday, sunday))
                .willReturn(schedules);

        // When: 월요일부터 일요일까지 조회하면
        List<ScheduleEntity> found = scheduleRepository.findAllByDateRange(monday, sunday);

        // Then: 경계의 양 끝 모두 포함되어야 한다
        assertThat(found).hasSize(2);
        verify(scheduleRepository).findAllByDateRange(monday, sunday);
    }

    @Test
    @DisplayName("빈 리스트로 스케줄을 삭제할 수 있다 (CASCADE 테스트)")
    void shouldHandleEmptyListDeletion() {
        // Given: 삭제할 Schedule이 없을 때
        List<ScheduleEntity> emptyList = List.of();

        // When: 빈 리스트로 deleteAll을 호출하면
        scheduleRepository.deleteAll(emptyList);

        // Then: deleteAll이 호출되어야 한다
        verify(scheduleRepository).deleteAll(emptyList);
    }
}
