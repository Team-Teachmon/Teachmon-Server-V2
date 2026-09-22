package solvit.teachmon.domain.student_schedule.application.strategy.setting.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import solvit.teachmon.domain.leave_seat.domain.entity.LeaveSeatEntity;
import solvit.teachmon.domain.leave_seat.domain.entity.LeaveSeatStudentEntity;
import solvit.teachmon.domain.leave_seat.domain.repository.LeaveSeatRepository;
import solvit.teachmon.domain.management.student.domain.entity.StudentEntity;
import solvit.teachmon.domain.student_schedule.domain.entity.StudentScheduleEntity;
import solvit.teachmon.domain.student_schedule.domain.entity.schedules.LeaveSeatScheduleEntity;
import solvit.teachmon.domain.student_schedule.domain.enums.ScheduleType;
import solvit.teachmon.domain.student_schedule.domain.repository.ScheduleRepository;
import solvit.teachmon.domain.student_schedule.domain.repository.StudentScheduleRepository;
import solvit.teachmon.domain.student_schedule.domain.repository.schedules.LeaveSeatScheduleRepository;
import solvit.teachmon.global.enums.SchoolPeriod;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("이석 스케줄 설정 전략 테스트")
class LeaveSeatScheduleSettingStrategyTest {

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private StudentScheduleRepository studentScheduleRepository;

    @Mock
    private LeaveSeatRepository leaveSeatRepository;

    @Mock
    private LeaveSeatScheduleRepository leaveSeatScheduleRepository;

    @InjectMocks
    private LeaveSeatScheduleSettingStrategy strategy;

    @Test
    @DisplayName("전략의 스케줄 타입은 LEAVE_SEAT여야 한다")
    void shouldReturnLeaveSeatScheduleType() {
        // When: 스케줄 타입을 가져오면
        ScheduleType scheduleType = strategy.getScheduleType();

        // Then: LEAVE_SEAT여야 한다
        assertThat(scheduleType).isEqualTo(ScheduleType.LEAVE_SEAT);
    }

    @Test
    @DisplayName("baseDate 이후의 이석 레코드에 대한 스케줄을 설정할 수 있다")
    void shouldSettingLeaveSeatScheduleForExistingRecords() {
        // Given: baseDate 이후에 이석 레코드가 2명의 학생으로 존재할 때
        LocalDate baseDate = LocalDate.now().plusWeeks(1).with(java.time.DayOfWeek.MONDAY);

        StudentEntity student1 = createMockStudent(1L, 1, 1);
        StudentEntity student2 = createMockStudent(2L, 1, 2);

        LeaveSeatStudentEntity leaveSeatStudent1 = createMockLeaveSeatStudent(student1);
        LeaveSeatStudentEntity leaveSeatStudent2 = createMockLeaveSeatStudent(student2);

        LeaveSeatEntity leaveSeat = createMockLeaveSeat(1L, baseDate, SchoolPeriod.SEVEN_PERIOD,
                List.of(leaveSeatStudent1, leaveSeatStudent2));

        StudentScheduleEntity studentSchedule1 = createMockStudentSchedule(1L, student1, baseDate, SchoolPeriod.SEVEN_PERIOD);
        StudentScheduleEntity studentSchedule2 = createMockStudentSchedule(2L, student2, baseDate, SchoolPeriod.SEVEN_PERIOD);

        given(leaveSeatRepository.findAllFromDate(baseDate)).willReturn(List.of(leaveSeat));
        given(studentScheduleRepository.findAllByStudentsAndDayAndPeriod(
                List.of(student1, student2), baseDate, SchoolPeriod.SEVEN_PERIOD))
                .willReturn(List.of(studentSchedule1, studentSchedule2));
        given(scheduleRepository.findLastStackOrderByStudentScheduleId(any())).willReturn(0);

        // When: 스케줄을 설정하면
        strategy.settingSchedule(baseDate);

        // Then: 이석 스케줄 링크가 학생 수만큼 생성되어야 한다
        verify(leaveSeatScheduleRepository, times(2)).save(any(LeaveSeatScheduleEntity.class));
    }

    @Test
    @DisplayName("이석 레코드가 없으면 아무것도 생성하지 않는다")
    void shouldNotCreateSchedulesWhenNoLeaveSeatRecords() {
        // Given: baseDate 이후에 이석 레코드가 없을 때
        LocalDate baseDate = LocalDate.now().plusWeeks(1).with(java.time.DayOfWeek.MONDAY);

        given(leaveSeatRepository.findAllFromDate(baseDate)).willReturn(List.of());

        // When: 스케줄을 설정하면
        strategy.settingSchedule(baseDate);

        // Then: 아무것도 생성되지 않아야 한다
        verify(leaveSeatScheduleRepository, never()).save(any());
        verify(scheduleRepository, never()).save(any());
    }

    @Test
    @DisplayName("이석 학생의 학생 스케줄이 존재하지 않으면 해당 학생은 건너뛴다")
    void shouldSkipWhenNoStudentSchedulesExist() {
        // Given: 이석 레코드는 있지만, 매칭되는 학생 스케줄이 없을 때
        LocalDate baseDate = LocalDate.now().plusWeeks(1).with(java.time.DayOfWeek.MONDAY);

        StudentEntity student = createMockStudent(1L, 1, 1);
        LeaveSeatStudentEntity leaveSeatStudent = createMockLeaveSeatStudent(student);
        LeaveSeatEntity leaveSeat = createMockLeaveSeat(1L, baseDate, SchoolPeriod.SEVEN_PERIOD,
                List.of(leaveSeatStudent));

        given(leaveSeatRepository.findAllFromDate(baseDate)).willReturn(List.of(leaveSeat));
        given(studentScheduleRepository.findAllByStudentsAndDayAndPeriod(
                List.of(student), baseDate, SchoolPeriod.SEVEN_PERIOD))
                .willReturn(List.of());

        // When: 스케줄을 설정하면
        strategy.settingSchedule(baseDate);

        // Then: 스케줄 링크가 생성되지 않아야 한다
        verify(leaveSeatScheduleRepository, never()).save(any());
    }

    @Test
    @DisplayName("여러 이석 레코드가 있을 때 각각 독립적으로 처리되어야 한다")
    void shouldHandleMultipleLeaveSeatRecordsIndependently() {
        // Given: baseDate 이후에 2개의 이석 레코드가 있을 때
        LocalDate baseDate = LocalDate.now().plusWeeks(1).with(java.time.DayOfWeek.MONDAY);
        LocalDate nextTuesday = baseDate.plusDays(1);

        StudentEntity student1 = createMockStudent(1L, 1, 1);
        StudentEntity student2 = createMockStudent(2L, 1, 2);

        LeaveSeatStudentEntity leaveSeatStudent1 = createMockLeaveSeatStudent(student1);
        LeaveSeatStudentEntity leaveSeatStudent2 = createMockLeaveSeatStudent(student2);

        LeaveSeatEntity leaveSeat1 = createMockLeaveSeat(1L, baseDate, SchoolPeriod.SEVEN_PERIOD,
                List.of(leaveSeatStudent1));
        LeaveSeatEntity leaveSeat2 = createMockLeaveSeat(2L, nextTuesday, SchoolPeriod.SEVEN_PERIOD,
                List.of(leaveSeatStudent2));

        StudentScheduleEntity studentSchedule1 = createMockStudentSchedule(1L, student1, baseDate, SchoolPeriod.SEVEN_PERIOD);
        StudentScheduleEntity studentSchedule2 = createMockStudentSchedule(2L, student2, nextTuesday, SchoolPeriod.SEVEN_PERIOD);

        given(leaveSeatRepository.findAllFromDate(baseDate)).willReturn(List.of(leaveSeat1, leaveSeat2));
        given(studentScheduleRepository.findAllByStudentsAndDayAndPeriod(
                List.of(student1), baseDate, SchoolPeriod.SEVEN_PERIOD))
                .willReturn(List.of(studentSchedule1));
        given(studentScheduleRepository.findAllByStudentsAndDayAndPeriod(
                List.of(student2), nextTuesday, SchoolPeriod.SEVEN_PERIOD))
                .willReturn(List.of(studentSchedule2));
        given(scheduleRepository.findLastStackOrderByStudentScheduleId(any())).willReturn(0);

        // When: 스케줄을 설정하면
        strategy.settingSchedule(baseDate);

        // Then: 각 이석 레코드별로 스케줄 링크가 생성되어야 한다
        verify(leaveSeatScheduleRepository, times(2)).save(any(LeaveSeatScheduleEntity.class));
    }

    @Test
    @DisplayName("동일한 LeaveSeat이 여러 LeaveSeatSchedule에 공유되어야 한다")
    void shouldShareSameLeaveSeatAcrossSchedules() {
        // Given: 이석 레코드에 3명의 학생이 있을 때
        LocalDate baseDate = LocalDate.now().plusWeeks(1).with(java.time.DayOfWeek.MONDAY);

        StudentEntity student1 = createMockStudent(1L, 1, 1);
        StudentEntity student2 = createMockStudent(2L, 1, 2);
        StudentEntity student3 = createMockStudent(3L, 1, 3);

        LeaveSeatStudentEntity ls1 = createMockLeaveSeatStudent(student1);
        LeaveSeatStudentEntity ls2 = createMockLeaveSeatStudent(student2);
        LeaveSeatStudentEntity ls3 = createMockLeaveSeatStudent(student3);

        LeaveSeatEntity leaveSeat = createMockLeaveSeat(1L, baseDate, SchoolPeriod.SEVEN_PERIOD,
                List.of(ls1, ls2, ls3));

        StudentScheduleEntity ss1 = createMockStudentSchedule(1L, student1, baseDate, SchoolPeriod.SEVEN_PERIOD);
        StudentScheduleEntity ss2 = createMockStudentSchedule(2L, student2, baseDate, SchoolPeriod.SEVEN_PERIOD);
        StudentScheduleEntity ss3 = createMockStudentSchedule(3L, student3, baseDate, SchoolPeriod.SEVEN_PERIOD);

        given(leaveSeatRepository.findAllFromDate(baseDate)).willReturn(List.of(leaveSeat));
        given(studentScheduleRepository.findAllByStudentsAndDayAndPeriod(
                List.of(student1, student2, student3), baseDate, SchoolPeriod.SEVEN_PERIOD))
                .willReturn(List.of(ss1, ss2, ss3));
        given(scheduleRepository.findLastStackOrderByStudentScheduleId(any())).willReturn(0);

        // When: 스케줄을 설정하면
        strategy.settingSchedule(baseDate);

        // Then: 3개의 LeaveSeatSchedule이 동일한 LeaveSeat을 참조해야 한다
        org.mockito.ArgumentCaptor<LeaveSeatScheduleEntity> captor =
                org.mockito.ArgumentCaptor.forClass(LeaveSeatScheduleEntity.class);
        verify(leaveSeatScheduleRepository, times(3)).save(captor.capture());

        List<LeaveSeatScheduleEntity> savedSchedules = captor.getAllValues();
        assertThat(savedSchedules)
                .extracting(LeaveSeatScheduleEntity::getLeaveSeat)
                .containsOnly(leaveSeat);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private StudentEntity createMockStudent(Long id, Integer grade, Integer classNumber) {
        StudentEntity student = mock(StudentEntity.class);
        given(student.getId()).willReturn(id);
        given(student.getGrade()).willReturn(grade);
        given(student.getClassNumber()).willReturn(classNumber);
        return student;
    }

    private LeaveSeatStudentEntity createMockLeaveSeatStudent(StudentEntity student) {
        LeaveSeatStudentEntity leaveSeatStudent = mock(LeaveSeatStudentEntity.class);
        given(leaveSeatStudent.getStudent()).willReturn(student);
        return leaveSeatStudent;
    }

    private LeaveSeatEntity createMockLeaveSeat(Long id, LocalDate day, SchoolPeriod period,
                                                List<LeaveSeatStudentEntity> students) {
        LeaveSeatEntity leaveSeat = mock(LeaveSeatEntity.class);
        given(leaveSeat.getId()).willReturn(id);
        given(leaveSeat.getDay()).willReturn(day);
        given(leaveSeat.getPeriod()).willReturn(period);
        given(leaveSeat.getLeaveSeatStudents()).willReturn(students);
        return leaveSeat;
    }

    private StudentScheduleEntity createMockStudentSchedule(Long id, StudentEntity student,
                                                             LocalDate day, SchoolPeriod period) {
        StudentScheduleEntity studentSchedule = mock(StudentScheduleEntity.class);
        given(studentSchedule.getId()).willReturn(id);
        given(studentSchedule.getStudent()).willReturn(student);
        given(studentSchedule.getDay()).willReturn(day);
        given(studentSchedule.getPeriod()).willReturn(period);
        return studentSchedule;
    }
}
