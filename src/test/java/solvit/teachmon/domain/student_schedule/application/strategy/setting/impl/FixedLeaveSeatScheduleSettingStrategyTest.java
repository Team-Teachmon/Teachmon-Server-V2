package solvit.teachmon.domain.student_schedule.application.strategy.setting.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import solvit.teachmon.domain.leave_seat.domain.entity.FixedLeaveSeatEntity;
import solvit.teachmon.domain.leave_seat.domain.entity.LeaveSeatEntity;
import solvit.teachmon.domain.leave_seat.domain.repository.FixedLeaveSeatRepository;
import solvit.teachmon.domain.leave_seat.domain.repository.FixedLeaveSeatStudentRepository;
import solvit.teachmon.domain.leave_seat.domain.repository.LeaveSeatRepository;
import solvit.teachmon.domain.leave_seat.domain.repository.LeaveSeatStudentRepository;
import solvit.teachmon.domain.management.student.domain.entity.StudentEntity;
import solvit.teachmon.domain.place.domain.entity.PlaceEntity;
import solvit.teachmon.domain.student_schedule.domain.enums.ScheduleType;
import solvit.teachmon.domain.user.domain.entity.TeacherEntity;
import solvit.teachmon.global.enums.SchoolPeriod;
import solvit.teachmon.global.enums.WeekDay;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("고정 이석 스케줄 설정 전략 테스트")
class FixedLeaveSeatScheduleSettingStrategyTest {

    @Mock
    private LeaveSeatRepository leaveSeatRepository;

    @Mock
    private FixedLeaveSeatRepository fixedLeaveSeatRepository;

    @Mock
    private FixedLeaveSeatStudentRepository fixedLeaveSeatStudentRepository;

    @Mock
    private LeaveSeatStudentRepository leaveSeatStudentRepository;

    @InjectMocks
    private FixedLeaveSeatScheduleSettingStrategy strategy;

    @Test
    @DisplayName("전략의 스케줄 타입은 FIXED_LEAVE_SEAT여야 한다")
    void shouldReturnFixedLeaveSeatScheduleType() {
        // When: 스케줄 타입을 가져오면
        ScheduleType scheduleType = strategy.getScheduleType();

        // Then: FIXED_LEAVE_SEAT여야 한다
        assertThat(scheduleType).isEqualTo(ScheduleType.FIXED_LEAVE_SEAT);
    }

    @Test
    @DisplayName("고정 이석 템플릿으로부터 이석 레코드를 생성할 수 있다")
    void shouldCreateLeaveSeatFromFixedTemplate() {
        // Given: 고정 이석 템플릿이 있고, 해당 날짜에 이석 레코드가 없을 때
        LocalDate baseDate = LocalDate.now().plusWeeks(1).with(java.time.DayOfWeek.MONDAY);

        TeacherEntity teacher = createMockTeacher(1L);
        PlaceEntity place = createMockPlace(1L, "도서관");
        FixedLeaveSeatEntity fixedLeaveSeat = createMockFixedLeaveSeat(1L, teacher, place,
                WeekDay.MON, SchoolPeriod.SEVEN_PERIOD, "특별활동");

        StudentEntity student = createMockStudent(1L, 1, 1);

        given(fixedLeaveSeatRepository.findAll()).willReturn(List.of(fixedLeaveSeat));
        given(leaveSeatRepository.findByPlaceAndDayAndPeriod(place, baseDate, SchoolPeriod.SEVEN_PERIOD))
                .willReturn(Optional.empty());
        given(fixedLeaveSeatStudentRepository.findAllByFixedLeaveSeat(fixedLeaveSeat))
                .willReturn(List.of(student));

        // When: 스케줄을 설정하면
        strategy.settingSchedule(baseDate);

        // Then: 이석 레코드가 1개 저장되어야 한다
        verify(leaveSeatRepository, times(1)).save(any(LeaveSeatEntity.class));
    }

    @Test
    @DisplayName("이석 레코드가 이미 존재하면 중복 생성하지 않는다")
    void shouldSkipWhenLeaveSeatAlreadyExists() {
        // Given: 고정 이석 템플릿이 있고, 해당 날짜에 이석 레코드가 이미 있을 때
        LocalDate baseDate = LocalDate.now().plusWeeks(1).with(java.time.DayOfWeek.MONDAY);

        TeacherEntity teacher = createMockTeacher(1L);
        PlaceEntity place = createMockPlace(1L, "도서관");
        FixedLeaveSeatEntity fixedLeaveSeat = createMockFixedLeaveSeat(1L, teacher, place,
                WeekDay.MON, SchoolPeriod.SEVEN_PERIOD, "특별활동");

        LeaveSeatEntity existingLeaveSeat = mock(LeaveSeatEntity.class);

        given(fixedLeaveSeatRepository.findAll()).willReturn(List.of(fixedLeaveSeat));
        given(leaveSeatRepository.findByPlaceAndDayAndPeriod(place, baseDate, SchoolPeriod.SEVEN_PERIOD))
                .willReturn(Optional.of(existingLeaveSeat));

        // When: 스케줄을 설정하면
        strategy.settingSchedule(baseDate);

        // Then: 이석 레코드를 새로 저장하지 않아야 한다
        verify(leaveSeatRepository, never()).save(any(LeaveSeatEntity.class));
    }

    @Test
    @DisplayName("고정 이석의 날짜가 baseDate 이전이면 건너뛴다")
    void shouldSkipWhenFixedLeaveSeatDayIsBeforeBaseDate() {
        // Given: 고정 이석 템플릿의 날짜가 baseDate 이전인 경우 (수요일 baseDate, 월요일 고정 이석)
        LocalDate baseDate = LocalDate.now().plusWeeks(1).with(java.time.DayOfWeek.WEDNESDAY);

        TeacherEntity teacher = createMockTeacher(1L);
        PlaceEntity place = createMockPlace(1L, "도서관");
        FixedLeaveSeatEntity fixedLeaveSeat = createMockFixedLeaveSeat(1L, teacher, place,
                WeekDay.MON, SchoolPeriod.SEVEN_PERIOD, "특별활동");

        given(fixedLeaveSeatRepository.findAll()).willReturn(List.of(fixedLeaveSeat));

        // When: 스케줄을 설정하면
        strategy.settingSchedule(baseDate);

        // Then: 이석 레코드가 생성되지 않아야 한다
        verify(leaveSeatRepository, never()).save(any(LeaveSeatEntity.class));
    }

    @Test
    @DisplayName("고정 이석 템플릿이 없으면 아무것도 생성하지 않는다")
    void shouldNotCreateAnythingWhenNoFixedLeaveSeats() {
        // Given: 고정 이석 템플릿이 없을 때
        LocalDate baseDate = LocalDate.now().plusWeeks(1).with(java.time.DayOfWeek.MONDAY);

        given(fixedLeaveSeatRepository.findAll()).willReturn(List.of());

        // When: 스케줄을 설정하면
        strategy.settingSchedule(baseDate);

        // Then: 아무것도 생성되지 않아야 한다
        verify(leaveSeatRepository, never()).save(any());
    }

    @Test
    @DisplayName("여러 고정 이석 템플릿이 있을 때 각각 독립적으로 처리된다")
    void shouldHandleMultipleFixedLeaveSeatsIndependently() {
        // Given: 2개의 고정 이석 템플릿이 있고, 모두 이석 레코드가 없을 때
        LocalDate baseDate = LocalDate.now().plusWeeks(1).with(java.time.DayOfWeek.MONDAY);
        LocalDate nextTuesday = baseDate.plusDays(1);

        TeacherEntity teacher = createMockTeacher(1L);
        PlaceEntity place1 = createMockPlace(1L, "도서관");
        PlaceEntity place2 = createMockPlace(2L, "컴퓨터실");

        FixedLeaveSeatEntity fixedLeaveSeat1 = createMockFixedLeaveSeat(1L, teacher, place1,
                WeekDay.MON, SchoolPeriod.SEVEN_PERIOD, "특별활동1");
        FixedLeaveSeatEntity fixedLeaveSeat2 = createMockFixedLeaveSeat(2L, teacher, place2,
                WeekDay.TUE, SchoolPeriod.SEVEN_PERIOD, "특별활동2");

        given(fixedLeaveSeatRepository.findAll()).willReturn(List.of(fixedLeaveSeat1, fixedLeaveSeat2));
        given(leaveSeatRepository.findByPlaceAndDayAndPeriod(place1, baseDate, SchoolPeriod.SEVEN_PERIOD))
                .willReturn(Optional.empty());
        given(leaveSeatRepository.findByPlaceAndDayAndPeriod(place2, nextTuesday, SchoolPeriod.SEVEN_PERIOD))
                .willReturn(Optional.empty());
        given(fixedLeaveSeatStudentRepository.findAllByFixedLeaveSeat(any()))
                .willReturn(List.of());

        // When: 스케줄을 설정하면
        strategy.settingSchedule(baseDate);

        // Then: 각 고정 이석 템플릿에 대해 이석 레코드가 생성되어야 한다
        verify(leaveSeatRepository, times(2)).save(any(LeaveSeatEntity.class));
    }

    @Test
    @DisplayName("고정 이석 전략은 학생 스케줄 링크를 생성하지 않는다 (LeaveSeatScheduleSettingStrategy 의 역할)")
    void shouldNotCreateScheduleLinks() {
        // Given: 고정 이석 템플릿이 있을 때
        LocalDate baseDate = LocalDate.now().plusWeeks(1).with(java.time.DayOfWeek.MONDAY);

        TeacherEntity teacher = createMockTeacher(1L);
        PlaceEntity place = createMockPlace(1L, "도서관");
        FixedLeaveSeatEntity fixedLeaveSeat = createMockFixedLeaveSeat(1L, teacher, place,
                WeekDay.MON, SchoolPeriod.SEVEN_PERIOD, "특별활동");

        given(fixedLeaveSeatRepository.findAll()).willReturn(List.of(fixedLeaveSeat));
        given(leaveSeatRepository.findByPlaceAndDayAndPeriod(place, baseDate, SchoolPeriod.SEVEN_PERIOD))
                .willReturn(Optional.empty());
        given(fixedLeaveSeatStudentRepository.findAllByFixedLeaveSeat(fixedLeaveSeat))
                .willReturn(List.of());

        // When: 스케줄을 설정하면
        strategy.settingSchedule(baseDate);

        // Then: LeaveSeat 레코드만 저장되고, 학생들이 조회되어야 한다 (학생 스케줄 링크는 LeaveSeatScheduleSettingStrategy 의 역할)
        verify(leaveSeatRepository, times(1)).save(any(LeaveSeatEntity.class));
        verify(fixedLeaveSeatStudentRepository, times(1)).findAllByFixedLeaveSeat(fixedLeaveSeat);
    }

    @Test
    @DisplayName("이석이 이미 존재할 때 누락된 학생만 추가한다 (N+1 쿼리 최적화)")
    void shouldAddOnlyMissingStudentsWhenLeaveSeatExists() {
        // Given: 고정 이석에는 3명의 학생이 있고, 기존 이석에는 1명만 등록되어 있을 때
        LocalDate baseDate = LocalDate.now().plusWeeks(1).with(java.time.DayOfWeek.MONDAY);

        TeacherEntity teacher = createMockTeacher(1L);
        PlaceEntity place = createMockPlace(1L, "도서관");
        FixedLeaveSeatEntity fixedLeaveSeat = createMockFixedLeaveSeat(1L, teacher, place,
                WeekDay.MON, SchoolPeriod.SEVEN_PERIOD, "특별활동");

        StudentEntity student1 = createMockStudent(1L, 1, 1);
        StudentEntity student2 = createMockStudent(2L, 1, 2);
        StudentEntity student3 = createMockStudent(3L, 1, 3);

        LeaveSeatEntity existingLeaveSeat = mock(LeaveSeatEntity.class);

        given(fixedLeaveSeatRepository.findAll()).willReturn(List.of(fixedLeaveSeat));
        given(leaveSeatRepository.findByPlaceAndDayAndPeriod(place, baseDate, SchoolPeriod.SEVEN_PERIOD))
                .willReturn(Optional.of(existingLeaveSeat));
        given(fixedLeaveSeatStudentRepository.findAllByFixedLeaveSeat(fixedLeaveSeat))
                .willReturn(List.of(student1, student2, student3));

        // 기존 이석에는 학생1만 등록되어 있음 (1번 쿼리)
        given(leaveSeatStudentRepository.findStudentIdsByLeaveSeat(existingLeaveSeat))
                .willReturn(List.of(1L));

        // When: 스케줄을 설정하면
        strategy.settingSchedule(baseDate);

        // Then:
        // 1. 새 LeaveSeat는 저장되지 않음
        verify(leaveSeatRepository, never()).save(any(LeaveSeatEntity.class));

        // 2. 기존 이석에 등록된 학생 ID 배치 조회 (1번 쿼리)
        verify(leaveSeatStudentRepository, times(1)).findStudentIdsByLeaveSeat(existingLeaveSeat);

        // 3. 누락된 학생 2명(student2, student3)만 추가됨 (1번 쿼리로 배치 처리)
        verify(leaveSeatStudentRepository, times(1)).saveAll(any());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private TeacherEntity createMockTeacher(Long id) {
        TeacherEntity teacher = mock(TeacherEntity.class);
        given(teacher.getId()).willReturn(id);
        given(teacher.hasStudentScheduleChangeAuthority()).willReturn(true);
        return teacher;
    }

    private PlaceEntity createMockPlace(Long id, String name) {
        PlaceEntity place = mock(PlaceEntity.class);
        given(place.getId()).willReturn(id);
        given(place.getName()).willReturn(name);
        return place;
    }

    private FixedLeaveSeatEntity createMockFixedLeaveSeat(Long id, TeacherEntity teacher,
                                                           PlaceEntity place, WeekDay weekDay,
                                                           SchoolPeriod period, String cause) {
        FixedLeaveSeatEntity fixedLeaveSeat = mock(FixedLeaveSeatEntity.class);
        given(fixedLeaveSeat.getId()).willReturn(id);
        given(fixedLeaveSeat.getTeacher()).willReturn(teacher);
        given(fixedLeaveSeat.getPlace()).willReturn(place);
        given(fixedLeaveSeat.getWeekDay()).willReturn(weekDay);
        given(fixedLeaveSeat.getPeriod()).willReturn(period);
        given(fixedLeaveSeat.getCause()).willReturn(cause);
        return fixedLeaveSeat;
    }

    private StudentEntity createMockStudent(Long id, Integer grade, Integer classNumber) {
        StudentEntity student = mock(StudentEntity.class);
        given(student.getId()).willReturn(id);
        given(student.getGrade()).willReturn(grade);
        given(student.getClassNumber()).willReturn(classNumber);
        return student;
    }
}
