package solvit.teachmon.domain.student_schedule.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import solvit.teachmon.domain.management.student.domain.entity.StudentEntity;
import solvit.teachmon.domain.management.student.domain.repository.StudentRepository;
import solvit.teachmon.domain.student_schedule.application.strategy.setting.StudentScheduleSettingStrategy;
import solvit.teachmon.domain.student_schedule.application.strategy.setting.StudentScheduleSettingStrategyComposite;
import solvit.teachmon.domain.student_schedule.domain.entity.ScheduleEntity;
import solvit.teachmon.domain.student_schedule.domain.repository.ScheduleRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentScheduleSettingService {
    private final StudentScheduleSettingStrategyComposite studentScheduleSettingStrategyComposite;
    private final StudentRepository studentRepository;
    private final StudentScheduleGenerator studentScheduleGenerator;
    private final ScheduleRepository scheduleRepository;

    @Transactional
    public void createNewStudentSchedule(LocalDate baseDate) {
        List<StudentEntity> students = getNowStudents(baseDate);

        // baseDate 이후(포함) 스케줄 삭제 (과거 데이터는 보존)
        studentScheduleGenerator.deleteFutureStudentSchedules(baseDate);

        // 새로운 학생 스케줄 생성
        studentScheduleGenerator.createStudentScheduleByStudents(students, baseDate);
    }

    private List<StudentEntity> getNowStudents(LocalDate baseDate) {
        Integer nowYear = baseDate.getYear();
        return studentRepository.findByYear(nowYear);
    }

    @Transactional
    public void settingAllTypeSchedule(LocalDate baseDate) {
        List<StudentScheduleSettingStrategy> settingStrategies = studentScheduleSettingStrategyComposite.getAllStrategies();

        // baseDate부터 일요일까지의 범위에서 기존 스케줄들을 먼저 삭제
        deleteFutureSchedulesByDate(baseDate);

        // 새로운 스케줄 설정
        for(StudentScheduleSettingStrategy settingStrategy : settingStrategies) {
            settingStrategy.settingSchedule(baseDate);
        }
    }

    /**
     * baseDate부터 그 주 일요일까지의 모든 스케줄(AFTER_SCHOOL, SELF_STUDY, LEAVE_SEAT 등)과
     * 그에 연관된 AfterSchoolScheduleEntity, LeaveSeatScheduleEntity, SelfStudyScheduleEntity 등을 삭제합니다.
     * CASCADE 설정이 작동하도록 엔티티를 조회한 후 삭제합니다.
     * @param baseDate 삭제 시작 날짜
     */
    private void deleteFutureSchedulesByDate(LocalDate baseDate) {
        LocalDate endDay = baseDate.with(DayOfWeek.SUNDAY);
        // 날짜 범위의 모든 Schedule을 조회
        List<ScheduleEntity> schedulesToDelete = scheduleRepository.findAllByDateRange(baseDate, endDay);

        // 엔티티 기반 삭제로 CASCADE가 작동하도록 함
        // ScheduleEntity의 CascadeType.REMOVE가 작동하여
        // LeaveSeatScheduleEntity, AfterSchoolScheduleEntity, SelfStudyScheduleEntity 등이 자동 삭제됨
        scheduleRepository.deleteAll(schedulesToDelete);
    }
}
