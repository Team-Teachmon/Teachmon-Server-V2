package solvit.teachmon.domain.student_schedule.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import solvit.teachmon.domain.management.student.domain.entity.StudentEntity;
import solvit.teachmon.domain.management.student.domain.repository.StudentRepository;
import solvit.teachmon.domain.student_schedule.application.strategy.setting.StudentScheduleSettingStrategy;
import solvit.teachmon.domain.student_schedule.application.strategy.setting.StudentScheduleSettingStrategyComposite;
import solvit.teachmon.domain.student_schedule.domain.repository.ScheduleRepository;
import solvit.teachmon.domain.student_schedule.domain.repository.StudentScheduleRepository;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("학생 스케줄 설정 서비스 테스트")
class StudentScheduleSettingServiceTest {

    @Mock
    private StudentScheduleSettingStrategyComposite studentScheduleSettingStrategyComposite;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private StudentScheduleGenerator studentScheduleGenerator;

    // 새로 추가된 의존성들을 모킹
    @Mock
    private StudentScheduleRepository studentScheduleRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    @InjectMocks
    private StudentScheduleSettingService studentScheduleSettingService;

    @Mock
    private StudentScheduleSettingStrategy mockStrategy1;

    @Mock
    private StudentScheduleSettingStrategy mockStrategy2;

    @BeforeEach
    void setUp() {
        // 기본적으로 주간 조회는 빈 리스트를 반환하도록 설정하여 기존 테스트의 기대 동작과 충돌하지 않도록 함
        given(studentScheduleRepository.findAllByDayBetween(any(LocalDate.class), any(LocalDate.class)))
                .willReturn(List.of());
    }

    @Test
    @DisplayName("새로운 학생 스케줄을 생성할 수 있다")
    void shouldCreateNewStudentSchedule() {
        // Given: 현재 연도의 학생들이 있을 때
        LocalDate today = LocalDate.now();
        Integer currentYear = today.getYear();

        StudentEntity student1 = createMockStudent(1L, currentYear, 1, 1);
        StudentEntity student2 = createMockStudent(2L, currentYear, 1, 2);
        List<StudentEntity> students = List.of(student1, student2);

        LocalDate nextWeek = today.plusWeeks(1);
        given(studentRepository.findByYear(nextWeek.getYear()))
                .willReturn(students);

        // When: 새로운 학생 스케줄을 생성하면
        studentScheduleSettingService.createNewStudentSchedule(nextWeek);

        // Then: Generator를 통해 삭제 후 생성이 수행되어야 한다
        verify(studentScheduleGenerator).deleteFutureStudentSchedules(nextWeek);
        verify(studentScheduleGenerator).createStudentScheduleByStudents(students, nextWeek);
    }

    @Test
    @DisplayName("현재 연도의 학생이 없으면 빈 리스트로 스케줄 생성을 요청한다")
    void shouldCallGeneratorWithEmptyListWhenNoStudents() {
        // Given: 현재 연도의 학생이 없을 때
        LocalDate today = LocalDate.now();
        LocalDate nextWeek = today.plusWeeks(1);

        given(studentRepository.findByYear(nextWeek.getYear()))
                .willReturn(List.of());

        // When: 새로운 학생 스케줄을 생성하면
        studentScheduleSettingService.createNewStudentSchedule(nextWeek);

        // Then: 빈 리스트로 Generator가 호출되어야 한다
        verify(studentScheduleGenerator).createStudentScheduleByStudents(List.of(), nextWeek);
    }

    @Test
    @DisplayName("모든 타입의 스케줄을 설정할 수 있다")
    void shouldSettingAllTypeSchedule() {
        // Given: 여러 개의 설정 전략이 있을 때
        LocalDate today = LocalDate.now();
        given(studentScheduleSettingStrategyComposite.getAllStrategies())
                .willReturn(List.of(mockStrategy1, mockStrategy2));

        // When: 모든 타입의 스케줄을 설정하면
        LocalDate nextWeek = today.plusWeeks(1);
        studentScheduleSettingService.settingAllTypeSchedule(nextWeek);

        // Then: 모든 전략의 settingSchedule이 호출되어야 한다
        verify(mockStrategy1, times(1)).settingSchedule(nextWeek);
        verify(mockStrategy2, times(1)).settingSchedule(nextWeek);
    }

    @Test
    @DisplayName("전략이 없으면 아무것도 설정하지 않는다")
    void shouldDoNothingWhenNoStrategies() {
        // Given: 설정 전략이 없을 때
        LocalDate today = LocalDate.now();
        given(studentScheduleSettingStrategyComposite.getAllStrategies())
                .willReturn(List.of());

        // When: 모든 타입의 스케줄을 설정하면
        LocalDate nextWeek = today.plusWeeks(1);
        studentScheduleSettingService.settingAllTypeSchedule(nextWeek);

        // Then: 아무런 전략도 호출되지 않아야 한다
        verifyNoInteractions(mockStrategy1, mockStrategy2);
    }

    private StudentEntity createMockStudent(Long id, Integer year, Integer grade, Integer classNumber) {
        StudentEntity student = mock(StudentEntity.class);
        given(student.getId()).willReturn(id);
        given(student.getYear()).willReturn(year);
        given(student.getGrade()).willReturn(grade);
        given(student.getClassNumber()).willReturn(classNumber);
        return student;
    }
}
