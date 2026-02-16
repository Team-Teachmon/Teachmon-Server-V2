package solvit.teachmon.domain.student_schedule.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import solvit.teachmon.domain.management.student.domain.entity.StudentEntity;
import solvit.teachmon.domain.management.student.domain.repository.StudentRepository;
import solvit.teachmon.domain.student_schedule.domain.entity.StudentScheduleEntity;
import solvit.teachmon.domain.student_schedule.domain.repository.StudentScheduleRepository;
import solvit.teachmon.global.enums.SchoolPeriod;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("학생 스케줄 생성기 테스트")
class StudentScheduleGeneratorTest {

    @Mock
    private StudentScheduleRepository studentScheduleRepository;

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private StudentScheduleGenerator studentScheduleGenerator;

    @Test
    @DisplayName("2명의 학생에 대해 주간 스케줄을 생성하면 30개가 저장된다")
    void shouldSave24SchedulesForTwoStudents() {
        // Given: 2명의 학생과 월요일 baseDate
        LocalDate today = LocalDate.now();
        LocalDate nextMonday = today.plusWeeks(1).with(DayOfWeek.MONDAY);

        StudentEntity student1 = createMockStudent(1L);
        StudentEntity student2 = createMockStudent(2L);

        // When: 스케줄 생성
        studentScheduleGenerator.createStudentScheduleByStudents(List.of(student1, student2), nextMonday);

        // Then: 2명 * 5일(월~금) * 3교시 = 30개
        ArgumentCaptor<List<StudentScheduleEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(studentScheduleRepository).saveAll(captor.capture());

        List<StudentScheduleEntity> savedSchedules = captor.getValue();
        assertThat(savedSchedules).hasSize(30);
    }

    @Test
    @DisplayName("생성된 스케줄은 baseDate가 월요일이면 월요일부터 금요일까지여야 한다")
    void shouldCreateSchedulesForMondayToThursday() {
        // Given: 1명의 학생과 월요일 baseDate
        LocalDate today = LocalDate.now();
        LocalDate nextMonday = today.plusWeeks(1).with(DayOfWeek.MONDAY);
        LocalDate nextTuesday = nextMonday.plusDays(1);
        LocalDate nextWednesday = nextMonday.plusDays(2);
        LocalDate nextThursday = nextMonday.plusDays(3);
        LocalDate nextFriday = nextMonday.plusDays(4);

        StudentEntity student = createMockStudent(1L);

        // When
        studentScheduleGenerator.createStudentScheduleByStudents(List.of(student), nextMonday);

        // Then: 날짜는 월~금만 포함
        ArgumentCaptor<List<StudentScheduleEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(studentScheduleRepository).saveAll(captor.capture());

        assertThat(captor.getValue())
                .extracting(StudentScheduleEntity::getDay)
                .containsOnly(nextMonday, nextTuesday, nextWednesday, nextThursday, nextFriday);
    }

    @Test
    @DisplayName("생성된 스케줄은 방과후 활동 시간대(7교시, 8~9교시, 10~11교시)만 포함해야 한다")
    void shouldCreateSchedulesOnlyForAfterSchoolPeriods() {
        // Given: 1명의 학생과 월요일 baseDate
        LocalDate nextMonday = LocalDate.now().plusWeeks(1).with(DayOfWeek.MONDAY);
        StudentEntity student = createMockStudent(1L);

        // When
        studentScheduleGenerator.createStudentScheduleByStudents(List.of(student), nextMonday);

        // Then: 교시는 7교시, 8~9교시, 10~11교시만 포함
        ArgumentCaptor<List<StudentScheduleEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(studentScheduleRepository).saveAll(captor.capture());

        assertThat(captor.getValue())
                .extracting(StudentScheduleEntity::getPeriod)
                .containsOnly(
                        SchoolPeriod.SEVEN_PERIOD,
                        SchoolPeriod.EIGHT_AND_NINE_PERIOD,
                        SchoolPeriod.TEN_AND_ELEVEN_PERIOD
                );
    }

    @Test
    @DisplayName("학생이 없으면 빈 리스트가 저장된다")
    void shouldSaveEmptyListWhenNoStudents() {
        // Given: 빈 학생 목록
        LocalDate nextMonday = LocalDate.now().plusWeeks(1).with(DayOfWeek.MONDAY);

        // When
        studentScheduleGenerator.createStudentScheduleByStudents(List.of(), nextMonday);

        // Then: 빈 리스트가 저장되어야 한다
        ArgumentCaptor<List<StudentScheduleEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(studentScheduleRepository).saveAll(captor.capture());

        assertThat(captor.getValue()).isEmpty();
    }

    @Test
    @DisplayName("baseDate가 수요일이면 수요일부터 금요일까지만 생성된다")
    void shouldCreateSchedulesOnlyFromBaseDate() {
        // Given: 1명의 학생, baseDate = 수요일
        LocalDate nextWednesday = LocalDate.now().plusWeeks(1).with(DayOfWeek.WEDNESDAY);
        LocalDate nextThursday = nextWednesday.plusDays(1);
        LocalDate nextFriday = nextWednesday.plusDays(2);
        StudentEntity student = createMockStudent(1L);

        // When
        studentScheduleGenerator.createStudentScheduleByStudents(List.of(student), nextWednesday);

        // Then: 수요일, 목요일, 금요일만 포함
        ArgumentCaptor<List<StudentScheduleEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(studentScheduleRepository).saveAll(captor.capture());

        assertThat(captor.getValue())
                .extracting(StudentScheduleEntity::getDay)
                .containsOnly(nextWednesday, nextThursday, nextFriday);
    }

    @Test
    @DisplayName("deleteFutureStudentSchedules는 baseDate부터 일요일까지의 스케줄을 삭제한다")
    void shouldDeleteSchedulesFromBaseDateToSunday() {
        // Given: baseDate = 월요일
        LocalDate nextMonday = LocalDate.now().plusWeeks(1).with(DayOfWeek.MONDAY);
        LocalDate nextSunday = nextMonday.with(DayOfWeek.SUNDAY);

        StudentScheduleEntity schedule1 = mock(StudentScheduleEntity.class);
        StudentScheduleEntity schedule2 = mock(StudentScheduleEntity.class);
        given(studentScheduleRepository.findAllByDayBetween(nextMonday, nextSunday))
                .willReturn(List.of(schedule1, schedule2));

        // When
        studentScheduleGenerator.deleteFutureStudentSchedules(nextMonday);

        // Then: 조회 후 삭제
        verify(studentScheduleRepository).findAllByDayBetween(nextMonday, nextSunday);
        verify(studentScheduleRepository).deleteAll(List.of(schedule1, schedule2));
    }

    private StudentEntity createMockStudent(Long id) {
        StudentEntity student = mock(StudentEntity.class);
        given(student.getId()).willReturn(id);
        return student;
    }
}
