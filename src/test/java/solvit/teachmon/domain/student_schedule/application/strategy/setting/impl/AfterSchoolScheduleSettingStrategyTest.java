package solvit.teachmon.domain.student_schedule.application.strategy.setting.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import solvit.teachmon.domain.after_school.domain.entity.AfterSchoolEntity;
import solvit.teachmon.domain.after_school.domain.repository.AfterSchoolBusinessTripRepository;
import solvit.teachmon.domain.after_school.domain.repository.AfterSchoolRepository;
import solvit.teachmon.domain.branch.domain.entity.BranchEntity;
import solvit.teachmon.domain.branch.domain.repository.BranchRepository;
import solvit.teachmon.domain.management.student.domain.entity.StudentEntity;
import solvit.teachmon.domain.student_schedule.domain.entity.StudentScheduleEntity;
import solvit.teachmon.domain.student_schedule.domain.entity.schedules.AfterSchoolScheduleEntity;
import solvit.teachmon.domain.student_schedule.domain.repository.ScheduleRepository;
import solvit.teachmon.domain.student_schedule.domain.repository.StudentScheduleRepository;
import solvit.teachmon.domain.student_schedule.domain.repository.schedules.AfterSchoolScheduleRepository;
import solvit.teachmon.global.enums.SchoolPeriod;
import solvit.teachmon.global.enums.WeekDay;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("방과후 스케줄 설정 전략 테스트")
class AfterSchoolScheduleSettingStrategyTest {

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private AfterSchoolRepository afterSchoolRepository;

    @Mock
    private AfterSchoolScheduleRepository afterSchoolScheduleRepository;

    @Mock
    private StudentScheduleRepository studentScheduleRepository;

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private AfterSchoolBusinessTripRepository afterSchoolBusinessTripRepository;

    @InjectMocks
    private AfterSchoolScheduleSettingStrategy strategy;

    @Test
    @DisplayName("분기가 주 중간(화요일)에 시작해도, 그 분기에 속하는 요일의 방과후 스케줄은 정상 생성된다")
    void shouldCreateAfterSchoolScheduleWhenBranchStartsMidWeek() {
        // Given: 이번 주 월요일(2026-08-24)은 어떤 분기에도 속하지 않고,
        // 새 분기(분기 #3)는 화요일(2026-08-25)부터 시작한다
        LocalDate baseDate = LocalDate.of(2026, 8, 24); // 월요일
        LocalDate wednesday = LocalDate.of(2026, 8, 26); // 방과후가 실제 열리는 날

        BranchEntity newBranch = BranchEntity.builder()
                .startDay(LocalDate.of(2026, 8, 25))
                .endDay(LocalDate.of(2026, 10, 22))
                .afterSchoolEndDay(LocalDate.of(2026, 10, 22))
                .year(2026)
                .branch(3)
                .build();

        AfterSchoolEntity afterSchool = mock(AfterSchoolEntity.class);
        given(afterSchool.getIsEnd()).willReturn(false);
        given(afterSchool.getWeekDay()).willReturn(WeekDay.WED);
        given(afterSchool.getPeriod()).willReturn(SchoolPeriod.EIGHT_AND_NINE_PERIOD);

        StudentEntity student = mock(StudentEntity.class);
        StudentScheduleEntity studentSchedule = mock(StudentScheduleEntity.class);
        given(studentSchedule.getId()).willReturn(1L);
        given(studentSchedule.getStudent()).willReturn(student);

        given(branchRepository.findAllOverlapping(baseDate, baseDate.plusDays(6)))
                .willReturn(List.of(newBranch));
        given(afterSchoolRepository.findAllByBranch(newBranch))
                .willReturn(List.of(afterSchool));
        given(afterSchoolBusinessTripRepository.existsByAfterSchoolAndDay(afterSchool, wednesday))
                .willReturn(false);
        given(studentScheduleRepository.findAllByAfterSchoolAndDayAndPeriod(afterSchool, wednesday, SchoolPeriod.EIGHT_AND_NINE_PERIOD))
                .willReturn(List.of(studentSchedule));
        given(scheduleRepository.findLastStackOrderByStudentScheduleId(1L))
                .willReturn(0);

        // When: 스케줄을 설정하면
        strategy.settingSchedule(baseDate);

        // Then: 수요일 방과후 스케줄이 생성되어야 한다 (월요일에 분기가 없다고 해서 통째로 실패하면 안 된다)
        verify(afterSchoolScheduleRepository, times(1)).save(any(AfterSchoolScheduleEntity.class));
    }

    @Test
    @DisplayName("방과후 요일이 그 분기의 시작일보다 앞선 날짜로 계산되면 스케줄을 생성하지 않는다")
    void shouldSkipWhenComputedDayIsOutsideBranchPeriod() {
        // Given: 새 분기는 화요일부터 시작하는데, 방과후 요일 설정이 월요일이라
        // 계산된 날짜(월요일)가 분기 시작일보다 앞서는 경우
        LocalDate baseDate = LocalDate.of(2026, 8, 24); // 월요일

        BranchEntity newBranch = BranchEntity.builder()
                .startDay(LocalDate.of(2026, 8, 25))
                .endDay(LocalDate.of(2026, 10, 22))
                .afterSchoolEndDay(LocalDate.of(2026, 10, 22))
                .year(2026)
                .branch(3)
                .build();

        AfterSchoolEntity afterSchool = mock(AfterSchoolEntity.class);
        given(afterSchool.getIsEnd()).willReturn(false);
        given(afterSchool.getWeekDay()).willReturn(WeekDay.MON);
        given(afterSchool.getPeriod()).willReturn(SchoolPeriod.EIGHT_AND_NINE_PERIOD);

        given(branchRepository.findAllOverlapping(baseDate, baseDate.plusDays(6)))
                .willReturn(List.of(newBranch));
        given(afterSchoolRepository.findAllByBranch(newBranch))
                .willReturn(List.of(afterSchool));

        // When: 스케줄을 설정하면
        strategy.settingSchedule(baseDate);

        // Then: 분기 기간 밖의 날짜이므로 스케줄이 생성되지 않아야 한다
        verify(afterSchoolScheduleRepository, never()).save(any(AfterSchoolScheduleEntity.class));
    }
}
