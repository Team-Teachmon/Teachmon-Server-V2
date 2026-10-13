package solvit.teachmon.domain.student_schedule.application.strategy.setting.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import solvit.teachmon.domain.student_schedule.domain.entity.ScheduleEntity;
import solvit.teachmon.domain.student_schedule.domain.entity.StudentScheduleEntity;
import solvit.teachmon.domain.student_schedule.domain.entity.schedules.SelfStudyScheduleEntity;
import solvit.teachmon.domain.student_schedule.domain.enums.ScheduleType;
import solvit.teachmon.domain.student_schedule.domain.repository.ScheduleRepository;
import solvit.teachmon.domain.student_schedule.domain.repository.StudentScheduleRepository;
import solvit.teachmon.domain.student_schedule.domain.repository.schedules.SelfStudyScheduleRepository;
import solvit.teachmon.global.enums.SchoolPeriod;
import solvit.teachmon.global.enums.WeekDay;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

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

    private PlaceEntity place;
    private StudentScheduleEntity studentSchedule;

    @BeforeEach
    void setUp() {
        BranchEntity branch = mock(BranchEntity.class);
        SelfStudyEntity selfStudy = createMockSelfStudy(WeekDay.TUE, 3, SchoolPeriod.EIGHT_AND_NINE_PERIOD);
        StudentEntity student = createMockStudent(3, 1);
        studentSchedule = createMockStudentSchedule(1L, student, TUESDAY, SchoolPeriod.EIGHT_AND_NINE_PERIOD);
        place = mock(PlaceEntity.class);

        given(branchRepository.findByDay(any())).willReturn(Optional.of(branch));
        given(selfStudyRepository.findAllByBranch(branch)).willReturn(List.of(selfStudy));
        given(studentScheduleRepository.findAllByGradeAndDayAndPeriod(3, TUESDAY, SchoolPeriod.EIGHT_AND_NINE_PERIOD))
                .willReturn(List.of(studentSchedule));
        given(placeRepository.findAllByGradePrefix(3)).willReturn(Map.of(1, place));
    }

    @Test
    @DisplayName("배정 가능한 장소가 없으면 해당 학생만 건너뛰고 예외를 던지지 않는다")
    void shouldSkipStudentWhenNoAvailablePlace() {
        given(placeRepository.checkPlaceAvailability(any(), any(), any())).willReturn(true);

        assertThatCode(() -> strategy.settingSchedule(TUESDAY)).doesNotThrowAnyException();

        verify(scheduleRepository, never()).save(any());
        verify(selfStudyScheduleRepository, never()).save(any());
    }

    @Test
    @DisplayName("자습 복구시 기존 스케줄을 덮지 않도록 스택 맨 아래에 넣는다")
    void shouldRestoreSelfStudyAtBottomOfStack() {
        given(placeRepository.checkPlaceAvailability(TUESDAY, SchoolPeriod.EIGHT_AND_NINE_PERIOD, place)).willReturn(false);
        given(scheduleRepository.existsByStudentScheduleIdAndType(1L, ScheduleType.SELF_STUDY)).willReturn(false);
        // 이미 이석 스케줄이 stackOrder 1로 들어가 있음
        given(scheduleRepository.findFirstStackOrderByStudentScheduleId(1L)).willReturn(1);

        int restoredCount = strategy.restoreSchedule(TUESDAY);

        ArgumentCaptor<ScheduleEntity> scheduleCaptor = ArgumentCaptor.forClass(ScheduleEntity.class);
        verify(scheduleRepository).save(scheduleCaptor.capture());
        assertThat(scheduleCaptor.getValue().getStackOrder()).isEqualTo(0);
        assertThat(scheduleCaptor.getValue().getType()).isEqualTo(ScheduleType.SELF_STUDY);
        verify(selfStudyScheduleRepository).save(any(SelfStudyScheduleEntity.class));
        assertThat(restoredCount).isEqualTo(1);
    }

    @Test
    @DisplayName("이미 자습 스케줄이 있는 학생은 복구하지 않는다")
    void shouldNotRestoreWhenSelfStudyAlreadyExists() {
        given(scheduleRepository.existsByStudentScheduleIdAndType(1L, ScheduleType.SELF_STUDY)).willReturn(true);

        int restoredCount = strategy.restoreSchedule(TUESDAY);

        verify(scheduleRepository, never()).save(any());
        verify(selfStudyScheduleRepository, never()).save(any());
        assertThat(restoredCount).isZero();
    }

    @Test
    @DisplayName("복구 시작일 이전 날짜의 자습은 복구하지 않는다")
    void shouldNotRestoreBeforeFromDate() {
        LocalDate wednesday = TUESDAY.plusDays(1);

        int restoredCount = strategy.restoreSchedule(wednesday);

        verify(studentScheduleRepository, never()).findAllByGradeAndDayAndPeriod(eq(3), eq(TUESDAY), any());
        verify(scheduleRepository, never()).save(any());
        assertThat(restoredCount).isZero();
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
