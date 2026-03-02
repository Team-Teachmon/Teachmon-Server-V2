package solvit.teachmon.domain.after_school.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import solvit.teachmon.domain.after_school.domain.entity.AfterSchoolEntity;
import solvit.teachmon.domain.after_school.domain.vo.StudentAssignmentResultVo;
import solvit.teachmon.domain.management.student.domain.entity.StudentEntity;
import solvit.teachmon.domain.student_schedule.domain.entity.ScheduleEntity;
import solvit.teachmon.domain.student_schedule.domain.entity.StudentScheduleEntity;
import solvit.teachmon.domain.student_schedule.domain.enums.ScheduleType;
import solvit.teachmon.domain.student_schedule.domain.repository.ScheduleRepository;
import solvit.teachmon.domain.student_schedule.domain.repository.StudentScheduleRepository;
import solvit.teachmon.domain.student_schedule.domain.repository.schedules.AfterSchoolScheduleRepository;
import solvit.teachmon.global.enums.SchoolPeriod;
import solvit.teachmon.global.enums.WeekDay;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AfterSchoolScheduleService 테스트")
class AfterSchoolScheduleServiceTest {

    private final AfterSchoolScheduleRepository afterSchoolScheduleRepository = mock(AfterSchoolScheduleRepository.class);
    private final ScheduleRepository scheduleRepository = mock(ScheduleRepository.class);
    private final StudentScheduleRepository studentScheduleRepository = mock(StudentScheduleRepository.class);

    @Test
    @DisplayName("방과후 교시 종료 이후면 스케줄을 저장하지 않는다")
    void shouldSkipWhenAfterSchoolEnded() {
        AfterSchoolEntity afterSchool = mock(AfterSchoolEntity.class);
        given(afterSchool.getWeekDay()).willReturn(WeekDay.MON);
        given(afterSchool.getPeriod()).willReturn(SchoolPeriod.SEVEN_PERIOD);

        StudentEntity student = mock(StudentEntity.class);
        StudentAssignmentResultVo resultVo = StudentAssignmentResultVo.builder()
                .afterSchool(afterSchool)
                .addedStudents(List.of(student))
                .removedStudents(List.of())
                .build();

        LocalDateTime afterEnd = LocalDateTime.of(2026, 2, 2, 17, 5); // 월요일 7교시 종료 후
        TestableService service = new TestableService(afterEnd);

        service.save(List.of(resultVo));

        verifyNoInteractions(studentScheduleRepository);
        verifyNoInteractions(scheduleRepository);
        verifyNoInteractions(afterSchoolScheduleRepository);
    }

    @Test
    @DisplayName("업데이트 가능 시간이 아니면 스케줄을 저장하지 않는다")
    void shouldSkipWhenOutsideUpdatableWindow() {
        AfterSchoolEntity afterSchool = mock(AfterSchoolEntity.class);
        given(afterSchool.getWeekDay()).willReturn(WeekDay.FRI);

        StudentEntity student = mock(StudentEntity.class);
        StudentAssignmentResultVo resultVo = StudentAssignmentResultVo.builder()
                .afterSchool(afterSchool)
                .addedStudents(List.of(student))
                .removedStudents(List.of())
                .build();

        LocalDateTime fridayNight = LocalDateTime.of(2026, 2, 6, 21, 0); // 금요일 21:00 (윈도우 외)
        TestableService service = new TestableService(fridayNight);

        service.save(List.of(resultVo));

        verifyNoInteractions(studentScheduleRepository);
        verifyNoInteractions(scheduleRepository);
        verifyNoInteractions(afterSchoolScheduleRepository);
    }

    @Test
    @DisplayName("방과후 교시 진행 중이면 스케줄을 저장한다")
    void shouldSaveWhenBeforeEndAndWithinWindow() {
        AfterSchoolEntity afterSchool = mock(AfterSchoolEntity.class);
        given(afterSchool.getWeekDay()).willReturn(WeekDay.MON);
        given(afterSchool.getPeriod()).willReturn(SchoolPeriod.SEVEN_PERIOD);

        StudentEntity student = mock(StudentEntity.class);
        StudentScheduleEntity studentSchedule = mock(StudentScheduleEntity.class);
        given(studentSchedule.getId()).willReturn(1L);

        StudentAssignmentResultVo resultVo = StudentAssignmentResultVo.builder()
                .afterSchool(afterSchool)
                .addedStudents(List.of(student))
                .removedStudents(List.of())
                .build();

        given(studentScheduleRepository.findAllByStudentsAndDayAndPeriod(anyList(), any(LocalDate.class), eq(SchoolPeriod.SEVEN_PERIOD)))
                .willReturn(List.of(studentSchedule));
        given(scheduleRepository.findLastStackOrderByStudentScheduleId(anyLong())).willReturn(0);
        given(scheduleRepository.save(any(ScheduleEntity.class))).willAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime duringClass = LocalDateTime.of(2026, 2, 2, 15, 30); // 월요일 7교시 진행 중
        TestableService service = new TestableService(duringClass);

        service.save(List.of(resultVo));

        verify(studentScheduleRepository).findAllByStudentsAndDayAndPeriod(anyList(), any(LocalDate.class), eq(SchoolPeriod.SEVEN_PERIOD));
        verify(scheduleRepository).findLastStackOrderByStudentScheduleId(anyLong());
        verify(scheduleRepository).save(argThat(schedule -> schedule.getType() == ScheduleType.AFTER_SCHOOL));
        verify(afterSchoolScheduleRepository).save(any());
    }

    private class TestableService extends AfterSchoolScheduleService {
        private final LocalDateTime fixedNow;

        private TestableService(LocalDateTime fixedNow) {
            super(afterSchoolScheduleRepository, scheduleRepository, studentScheduleRepository);
            this.fixedNow = fixedNow;
        }

        @Override
        protected LocalDateTime currentDateTime() {
            return fixedNow;
        }
    }
}
