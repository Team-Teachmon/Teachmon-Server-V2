package solvit.teachmon.domain.student_schedule.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import solvit.teachmon.domain.management.student.domain.entity.StudentEntity;
import solvit.teachmon.domain.management.student.domain.repository.StudentRepository;
import solvit.teachmon.domain.student_schedule.application.strategy.setting.StudentScheduleSettingStrategy;
import solvit.teachmon.domain.student_schedule.application.strategy.setting.StudentScheduleSettingStrategyComposite;
import solvit.teachmon.domain.student_schedule.domain.entity.ScheduleEntity;
import solvit.teachmon.domain.student_schedule.domain.entity.StudentScheduleEntity;
import solvit.teachmon.domain.student_schedule.domain.enums.ScheduleType;
import solvit.teachmon.domain.student_schedule.domain.repository.ScheduleRepository;
import solvit.teachmon.domain.student_schedule.domain.repository.StudentScheduleRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentScheduleSettingService {
    private final StudentScheduleSettingStrategyComposite studentScheduleSettingStrategyComposite;
    private final StudentRepository studentRepository;
    private final StudentScheduleGenerator studentScheduleGenerator;
    private final StudentScheduleRepository studentScheduleRepository;
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

        // 삭제 범위를 '오늘(LocalDate.now()) ~ baseDate 주의 일요일'로 변경하여
        // 오늘 이전의 과거 스케줄은 보존하도록 함
        LocalDate startDay = LocalDate.now();
        LocalDate endDay = baseDate.with(DayOfWeek.SUNDAY);

        List<StudentScheduleEntity> weekStudentSchedules;
        if (endDay.isBefore(startDay)) {
            // 대상 기간이 유효하지 않은 경우(예: baseDate가 오늘보다 이전) 삭제 대상이 없음
            weekStudentSchedules = List.of();
        } else {
            weekStudentSchedules = studentScheduleRepository.findAllByDayBetween(startDay, endDay);
        }

        // 기존 스케줄(type별) 삭제: 중복 생성을 방지하기 위해 각 전략의 타입에 해당하는 기존 스케줄을 엔티티 삭제로 제거
        if (!weekStudentSchedules.isEmpty()) {
            for (StudentScheduleSettingStrategy settingStrategy : settingStrategies) {
                ScheduleType type = settingStrategy.getScheduleType();
                for (StudentScheduleEntity studentSchedule : weekStudentSchedules) {
                    // bulk delete가 아닌 엔티티 삭제를 사용하여 Cascade REMOVE가 동작하도록 함
                    scheduleRepository.findByStudentScheduleIdAndType(studentSchedule.getId(), type)
                            .ifPresent(scheduleRepository::delete);
                }
            }
        }

        // 이제 각 전략을 실행하여 새로운 스케줄을 생성
        for(StudentScheduleSettingStrategy settingStrategy : settingStrategies) {
            settingStrategy.settingSchedule(baseDate);
        }
    }
}
