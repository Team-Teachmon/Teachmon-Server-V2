package solvit.teachmon.domain.student_schedule.application.strategy.setting.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import solvit.teachmon.domain.branch.domain.entity.BranchEntity;
import solvit.teachmon.domain.branch.domain.repository.BranchRepository;
import solvit.teachmon.domain.management.student.domain.entity.StudentEntity;
import solvit.teachmon.domain.place.domain.entity.PlaceEntity;
import solvit.teachmon.domain.place.domain.repository.PlaceRepository;
import solvit.teachmon.domain.self_study.domain.entity.SelfStudyEntity;
import solvit.teachmon.domain.self_study.domain.repository.SelfStudyRepository;
import solvit.teachmon.domain.student_schedule.domain.entity.StudentScheduleEntity;
import solvit.teachmon.domain.student_schedule.domain.entity.schedules.SelfStudyScheduleEntity;
import solvit.teachmon.domain.student_schedule.domain.repository.ScheduleRepository;
import solvit.teachmon.domain.student_schedule.domain.repository.StudentScheduleRepository;
import solvit.teachmon.domain.student_schedule.domain.repository.schedules.SelfStudyScheduleRepository;
import solvit.teachmon.global.enums.SchoolPeriod;
import solvit.teachmon.global.enums.WeekDay;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("자습 스케줄 설정 전략 테스트")
class SelfStudyScheduleSettingStrategyTest {

    // 2026-10-13 (화)
    private static final LocalDate TUESDAY = LocalDate.of(2026, 10, 13);

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private SelfStudyRepository selfStudyRepository;

    @Mock
    private SelfStudyScheduleRepository selfStudyScheduleRepository;

    @Mock
    private StudentScheduleRepository studentScheduleRepository;

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private PlaceRepository placeRepository;

    @InjectMocks
    private SelfStudyScheduleSettingStrategy strategy;

    @Test
    @DisplayName("분기가 주 중간(화요일)에 시작해도, 그 분기에 속하는 요일의 자습 스케줄은 정상 생성된다")
    void shouldCreateSelfStudyScheduleWhenBranchStartsMidWeek() {
        // Given: 이번 주 월요일(2026-08-24)은 어떤 분기에도 속하지 않고,
        // 새 분기(분기 #3)는 화요일(2026-08-25)부터 시작한다
        LocalDate baseDate = LocalDate.of(2026, 8, 24); // 월요일
        LocalDate wednesday = LocalDate.of(2026, 8, 26); // 자습이 실제 열리는 날

        BranchEntity newBranch = BranchEntity.builder()
                .startDay(LocalDate.of(2026, 8, 25))
                .endDay(LocalDate.of(2026, 10, 22))
                .afterSchoolEndDay(LocalDate.of(2026, 10, 22))
                .year(2026)
                .branch(3)
                .build();

        SelfStudyEntity selfStudy = mock(SelfStudyEntity.class);
        given(selfStudy.getWeekDay()).willReturn(WeekDay.WED);
        given(selfStudy.getPeriod()).willReturn(SchoolPeriod.SEVEN_PERIOD);
        given(selfStudy.getGrade()).willReturn(1);

        StudentEntity student = mock(StudentEntity.class);
        given(student.getGrade()).willReturn(1);
        given(student.getClassNumber()).willReturn(1);

        StudentScheduleEntity studentSchedule = mock(StudentScheduleEntity.class);
        given(studentSchedule.getId()).willReturn(1L);
        given(studentSchedule.getStudent()).willReturn(student);
        given(studentSchedule.getDay()).willReturn(wednesday);
        given(studentSchedule.getPeriod()).willReturn(SchoolPeriod.SEVEN_PERIOD);

        PlaceEntity place = mock(PlaceEntity.class);

        given(branchRepository.findAllOverlapping(baseDate, baseDate.plusDays(6)))
                .willReturn(List.of(newBranch));
        given(selfStudyRepository.findAllByBranch(newBranch))
                .willReturn(List.of(selfStudy));
        given(studentScheduleRepository.findAllByGradeAndDayAndPeriod(1, wednesday, SchoolPeriod.SEVEN_PERIOD))
                .willReturn(List.of(studentSchedule));
        given(scheduleRepository.findLastStackOrderByStudentScheduleId(1L))
                .willReturn(0);
        given(placeRepository.findAllByGradePrefix(1))
                .willReturn(Map.of(1, place));
        given(placeRepository.checkPlaceAvailability(wednesday, SchoolPeriod.SEVEN_PERIOD, place))
                .willReturn(false);

        // When: 스케줄을 설정하면
        strategy.settingSchedule(baseDate);

        // Then: 수요일 자습 스케줄이 생성되어야 한다 (월요일에 분기가 없다고 해서 통째로 실패하면 안 된다)
        verify(selfStudyScheduleRepository, times(1)).save(any(SelfStudyScheduleEntity.class));
    }

    @Test
    @DisplayName("자습 요일이 그 분기의 시작일보다 앞선 날짜로 계산되면 스케줄을 생성하지 않는다")
    void shouldSkipWhenComputedDayIsOutsideBranchPeriod() {
        // Given: 새 분기는 화요일부터 시작하는데, 자습 요일 설정이 월요일이라
        // 계산된 날짜(월요일)가 분기 시작일보다 앞서는 경우
        LocalDate baseDate = LocalDate.of(2026, 8, 24); // 월요일

        BranchEntity newBranch = BranchEntity.builder()
                .startDay(LocalDate.of(2026, 8, 25))
                .endDay(LocalDate.of(2026, 10, 22))
                .afterSchoolEndDay(LocalDate.of(2026, 10, 22))
                .year(2026)
                .branch(3)
                .build();

        SelfStudyEntity selfStudy = mock(SelfStudyEntity.class);
        given(selfStudy.getWeekDay()).willReturn(WeekDay.MON);
        given(selfStudy.getPeriod()).willReturn(SchoolPeriod.SEVEN_PERIOD);

        given(branchRepository.findAllOverlapping(baseDate, baseDate.plusDays(6)))
                .willReturn(List.of(newBranch));
        given(selfStudyRepository.findAllByBranch(newBranch))
                .willReturn(List.of(selfStudy));

        // When: 스케줄을 설정하면
        strategy.settingSchedule(baseDate);

        // Then: 분기 기간 밖의 날짜이므로 스케줄이 생성되지 않아야 한다
        verify(selfStudyScheduleRepository, never()).save(any(SelfStudyScheduleEntity.class));
    }

    @Test
    @DisplayName("배정 가능한 장소가 없으면 해당 학생만 건너뛰고 예외를 던지지 않는다")
    void shouldSkipStudentWhenNoAvailablePlace() {
        givenTuesdaySelfStudy();
        given(placeRepository.checkPlaceAvailability(any(), any(), any())).willReturn(true);

        assertThatCode(() -> strategy.settingSchedule(TUESDAY)).doesNotThrowAnyException();

        verify(scheduleRepository, never()).save(any());
        verify(selfStudyScheduleRepository, never()).save(any());
    }

    private PlaceEntity givenTuesdaySelfStudy() {
        BranchEntity branch = BranchEntity.builder()
                .startDay(LocalDate.of(2026, 8, 3))
                .endDay(LocalDate.of(2026, 10, 25))
                .afterSchoolEndDay(LocalDate.of(2026, 10, 25))
                .year(2026)
                .branch(3)
                .build();
        SelfStudyEntity selfStudy = createMockSelfStudy(WeekDay.TUE, 3, SchoolPeriod.EIGHT_AND_NINE_PERIOD);
        StudentEntity student = createMockStudent(3, 1);
        StudentScheduleEntity studentSchedule = createMockStudentSchedule(1L, student, TUESDAY, SchoolPeriod.EIGHT_AND_NINE_PERIOD);
        PlaceEntity place = mock(PlaceEntity.class);

        given(branchRepository.findAllOverlapping(any(), any())).willReturn(List.of(branch));
        given(selfStudyRepository.findAllByBranch(branch)).willReturn(List.of(selfStudy));
        given(studentScheduleRepository.findAllByGradeAndDayAndPeriod(3, TUESDAY, SchoolPeriod.EIGHT_AND_NINE_PERIOD))
                .willReturn(List.of(studentSchedule));
        given(placeRepository.findAllByGradePrefix(3)).willReturn(Map.of(1, place));
        return place;
    }

    private SelfStudyEntity createMockSelfStudy(WeekDay weekDay, Integer grade, SchoolPeriod period) {
        SelfStudyEntity selfStudy = mock(SelfStudyEntity.class);
        given(selfStudy.getWeekDay()).willReturn(weekDay);
        given(selfStudy.getGrade()).willReturn(grade);
        given(selfStudy.getPeriod()).willReturn(period);
        return selfStudy;
    }

    private StudentEntity createMockStudent(Integer grade, Integer classNumber) {
        StudentEntity student = mock(StudentEntity.class);
        given(student.getGrade()).willReturn(grade);
        given(student.getClassNumber()).willReturn(classNumber);
        return student;
    }

    private StudentScheduleEntity createMockStudentSchedule(Long id, StudentEntity student, LocalDate day, SchoolPeriod period) {
        StudentScheduleEntity studentSchedule = mock(StudentScheduleEntity.class);
        given(studentSchedule.getId()).willReturn(id);
        given(studentSchedule.getStudent()).willReturn(student);
        given(studentSchedule.getDay()).willReturn(day);
        given(studentSchedule.getPeriod()).willReturn(period);
        return studentSchedule;
    }
}
