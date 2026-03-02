package solvit.teachmon.domain.after_school.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import solvit.teachmon.domain.after_school.domain.entity.AfterSchoolEntity;
import solvit.teachmon.domain.after_school.domain.repository.AfterSchoolBusinessTripRepository;
import solvit.teachmon.domain.after_school.domain.repository.AfterSchoolReinforcementRepository;
import solvit.teachmon.domain.after_school.domain.repository.AfterSchoolRepository;
import solvit.teachmon.domain.after_school.domain.service.AfterSchoolStudentDomainService;
import solvit.teachmon.domain.after_school.presentation.dto.request.AfterSchoolCreateRequestDto;
import solvit.teachmon.domain.after_school.presentation.dto.request.AfterSchoolUpdateRequestDto;
import solvit.teachmon.domain.branch.domain.entity.BranchEntity;
import solvit.teachmon.domain.branch.domain.repository.BranchRepository;
import solvit.teachmon.domain.management.student.domain.repository.StudentRepository;
import solvit.teachmon.domain.management.teacher.domain.entity.SupervisionBanDayEntity;
import solvit.teachmon.domain.management.teacher.domain.repository.SupervisionBanDayRepository;
import solvit.teachmon.domain.place.domain.entity.PlaceEntity;
import solvit.teachmon.domain.place.domain.repository.PlaceRepository;
import solvit.teachmon.domain.student_schedule.domain.repository.ScheduleRepository;
import solvit.teachmon.domain.student_schedule.domain.repository.StudentScheduleRepository;
import solvit.teachmon.domain.student_schedule.domain.repository.schedules.AfterSchoolScheduleRepository;
import solvit.teachmon.domain.user.domain.entity.TeacherEntity;
import solvit.teachmon.domain.user.domain.repository.TeacherRepository;
import solvit.teachmon.global.enums.SchoolPeriod;
import solvit.teachmon.global.enums.WeekDay;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("방과후 감독 금지 요일 연동 테스트")
class AfterSchoolServiceBanDayTest {

    @Mock
    private AfterSchoolStudentDomainService afterSchoolStudentDomainService;
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
    private AfterSchoolScheduleService afterSchoolScheduleService;
    @Mock
    private StudentScheduleRepository studentScheduleRepository;
    @Mock
    private ScheduleRepository scheduleRepository;
    @Mock
    private AfterSchoolScheduleRepository afterSchoolScheduleRepository;

    private AfterSchoolService afterSchoolService;

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
    }

    @Test
    @DisplayName("방과후 생성 시 감독 금지 요일을 저장한다")
    void createAfterSchoolSavesBanDay() {
        TeacherEntity teacher = mock(TeacherEntity.class);
        PlaceEntity place = mock(PlaceEntity.class);

        given(teacherRepository.findById(1L)).willReturn(Optional.of(teacher));
        given(placeRepository.findById(2L)).willReturn(Optional.of(place));
        given(branchRepository.findByYearAndDate(anyInt(), any(LocalDate.class))).willReturn(Optional.of(mock(BranchEntity.class)));
        given(studentRepository.findAllById(anyList())).willReturn(List.of());
        given(afterSchoolStudentDomainService.assignStudents(any(), anyList())).willReturn(mock(solvit.teachmon.domain.after_school.domain.vo.StudentAssignmentResultVo.class));
        given(afterSchoolRepository.save(any())).willReturn(mock(AfterSchoolEntity.class));

        AfterSchoolCreateRequestDto request = new AfterSchoolCreateRequestDto(
                2024,
                1,
                WeekDay.MON,
                SchoolPeriod.SEVEN_PERIOD,
                1L,
                2L,
                "Math",
                List.of()
        );

        afterSchoolService.createAfterSchool(request);

        verify(supervisionBanDayRepository).save(any(SupervisionBanDayEntity.class));
    }

    @Test
    @DisplayName("방과후 수정 시 기존 감독 금지 요일을 삭제하고 새로 저장한다")
    void updateAfterSchoolRefreshesBanDay() {
        TeacherEntity originalTeacher = mock(TeacherEntity.class);
        TeacherEntity newTeacher = mock(TeacherEntity.class);
        PlaceEntity newPlace = mock(PlaceEntity.class);
        AfterSchoolEntity afterSchool = mock(AfterSchoolEntity.class);

        given(afterSchoolRepository.findWithAllRelations(1L)).willReturn(Optional.of(afterSchool));
        given(afterSchool.getTeacher()).willReturn(originalTeacher);
        given(originalTeacher.getId()).willReturn(10L);
        given(afterSchool.getWeekDay()).willReturn(WeekDay.MON);
        given(teacherRepository.findById(2L)).willReturn(Optional.of(newTeacher));
        given(placeRepository.findById(3L)).willReturn(Optional.of(newPlace));

        AfterSchoolUpdateRequestDto request = new AfterSchoolUpdateRequestDto(
                1L,
                2024,
                1,
                WeekDay.TUE,
                SchoolPeriod.EIGHT_AND_NINE_PERIOD,
                2L,
                3L,
                "New",
                null
        );

        afterSchoolService.updateAfterSchool(request);

        verify(supervisionBanDayRepository).deleteAfterSchoolBanDay(10L, WeekDay.MON);
        verify(supervisionBanDayRepository).save(any(SupervisionBanDayEntity.class));

        verify(afterSchool).updateAfterSchool(
                newTeacher,
                newPlace,
                WeekDay.TUE,
                SchoolPeriod.EIGHT_AND_NINE_PERIOD,
                2024,
                "New",
                1
        );
    }

    @Test
    @DisplayName("방과후 삭제 시 감독 금지 요일을 함께 삭제한다")
    void deleteAfterSchoolRemovesBanDay() {
        TeacherEntity teacher = mock(TeacherEntity.class);
        AfterSchoolEntity afterSchool = mock(AfterSchoolEntity.class);
        given(afterSchoolRepository.findById(1L)).willReturn(Optional.of(afterSchool));
        given(afterSchool.getTeacher()).willReturn(teacher);
        given(teacher.getId()).willReturn(5L);
        given(afterSchool.getWeekDay()).willReturn(WeekDay.WED);

        afterSchoolService.deleteAfterSchool(1L);

        verify(supervisionBanDayRepository).deleteAfterSchoolBanDay(5L, WeekDay.WED);
        verify(afterSchoolRepository).delete(afterSchool);
    }

    @Test
    @DisplayName("방과후 종료 시 감독 금지 요일을 함께 삭제한다")
    void quitAfterSchoolRemovesBanDay() {
        TeacherEntity teacher = mock(TeacherEntity.class);
        AfterSchoolEntity afterSchool = mock(AfterSchoolEntity.class);
        given(afterSchoolRepository.findById(1L)).willReturn(Optional.of(afterSchool));
        given(afterSchool.getTeacher()).willReturn(teacher);
        given(teacher.getId()).willReturn(7L);
        given(afterSchool.getWeekDay()).willReturn(WeekDay.THU);

        afterSchoolService.quitAfterSchool(1L);

        verify(supervisionBanDayRepository).deleteAfterSchoolBanDay(7L, WeekDay.THU);
        verify(afterSchool).endAfterSchool();
    }
}
