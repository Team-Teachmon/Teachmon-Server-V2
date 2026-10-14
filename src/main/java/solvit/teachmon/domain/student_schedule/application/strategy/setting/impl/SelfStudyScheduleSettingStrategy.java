package solvit.teachmon.domain.student_schedule.application.strategy.setting.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import solvit.teachmon.domain.branch.domain.entity.BranchEntity;
import solvit.teachmon.domain.branch.domain.repository.BranchRepository;
import solvit.teachmon.domain.management.student.domain.entity.StudentEntity;
import solvit.teachmon.domain.place.domain.entity.PlaceEntity;
import solvit.teachmon.domain.place.domain.repository.PlaceRepository;
import solvit.teachmon.domain.self_study.domain.entity.SelfStudyEntity;
import solvit.teachmon.domain.self_study.domain.repository.SelfStudyRepository;
import solvit.teachmon.domain.student_schedule.application.strategy.setting.StudentScheduleSettingStrategy;
import solvit.teachmon.domain.student_schedule.domain.entity.ScheduleEntity;
import solvit.teachmon.domain.student_schedule.domain.entity.StudentScheduleEntity;
import solvit.teachmon.domain.student_schedule.domain.entity.schedules.SelfStudyScheduleEntity;
import solvit.teachmon.domain.student_schedule.domain.enums.ScheduleType;
import solvit.teachmon.domain.student_schedule.domain.exception.NoAvailablePlaceException;
import solvit.teachmon.domain.student_schedule.domain.repository.ScheduleRepository;
import solvit.teachmon.domain.student_schedule.domain.repository.StudentScheduleRepository;
import solvit.teachmon.domain.student_schedule.domain.repository.schedules.SelfStudyScheduleRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static solvit.teachmon.domain.place.domain.entity.PlaceEntity.calculateNextClassNumber;

@Slf4j
@Component
@RequiredArgsConstructor
public class SelfStudyScheduleSettingStrategy implements StudentScheduleSettingStrategy {
    private final ScheduleRepository scheduleRepository;
    private final SelfStudyRepository selfStudyRepository;
    private final SelfStudyScheduleRepository selfStudyScheduleRepository;
    private final StudentScheduleRepository studentScheduleRepository;
    private final BranchRepository branchRepository;
    private final PlaceRepository placeRepository;

    @Override
    public ScheduleType getScheduleType() {
        return ScheduleType.SELF_STUDY;
    }

    @Override
    public void settingSchedule(LocalDate baseDate) {
        // baseDate(그 주의 월요일)가 속한 분기가 아니라, baseDate ~ 일요일까지 걸쳐있는
        // 모든 분기를 가져온다. 분기 전환 주에는 이 기간에 분기가 2개 걸릴 수 있다.
        List<BranchEntity> branches = branchRepository.findAllOverlapping(baseDate, baseDate.plusDays(6));

        for (BranchEntity branch : branches) {
            // 해당 분기의 모든 자습 가져오기
            List<SelfStudyEntity> selfStudies = selfStudyRepository.findAllByBranch(branch);

            for (SelfStudyEntity selfStudy : selfStudies) {
                LocalDate selfStudyDay = calculateSelfStudyDay(selfStudy, baseDate);

                // 이번 주에 해당하는 날짜가 이 분기 기간 밖이면 넘어가기
                if (isOutOfBranchPeriod(selfStudyDay, branch))
                    continue;
                // 이전 날짜면 넘어가기
                if (isBeforeSelfStudy(selfStudy, baseDate))
                    continue;
                // 각 자습별 학년들의 student schedule 가져오기
                List<StudentScheduleEntity> studentSchedules = findStudentScheduleBySelfStudy(selfStudy, baseDate);

                // 각 student schedule 별로 self study 설정해주기
                settingSelfStudySchedule(studentSchedules, selfStudy);
            }
        }
    }

    private boolean isOutOfBranchPeriod(LocalDate day, BranchEntity branch) {
        return day.isBefore(branch.getStartDay()) || day.isAfter(branch.getEndDay());
    }

    private Boolean isBeforeSelfStudy(SelfStudyEntity selfStudy, LocalDate baseDate) {
        LocalDate selfStudyDay = calculateSelfStudyDay(selfStudy, baseDate);
        return selfStudyDay.isBefore(baseDate);
    }

    private List<StudentScheduleEntity> findStudentScheduleBySelfStudy(SelfStudyEntity selfStudy, LocalDate baseDate) {
        return studentScheduleRepository.findAllByGradeAndDayAndPeriod(
                selfStudy.getGrade(), calculateSelfStudyDay(selfStudy, baseDate), selfStudy.getPeriod()
        );
    }

    private LocalDate calculateSelfStudyDay(SelfStudyEntity selfStudy, LocalDate baseDate) {
        return baseDate.with(selfStudy.getWeekDay().toDayOfWeek());
    }

    private void settingSelfStudySchedule(List<StudentScheduleEntity> studentSchedules, SelfStudyEntity selfStudy) {
        for(StudentScheduleEntity studentSchedule : studentSchedules) {
            PlaceEntity place;
            try {
                place = findSelfStudyPlace(studentSchedule);
            } catch (NoAvailablePlaceException e) {
                // 장소 하나를 못 찾았다고 전체 스케줄 설정이 롤백되지 않도록 해당 건만 건너뜀
                log.warn("자습 장소 배정 실패 - studentScheduleId: {}, day: {}, period: {}",
                        studentSchedule.getId(), studentSchedule.getDay(), studentSchedule.getPeriod());
                continue;
            }
            ScheduleEntity newSchedule = createNewSchedule(studentSchedule);
            createSelfStudySchedule(newSchedule, selfStudy, place);
        }
    }

    private PlaceEntity findSelfStudyPlace(StudentScheduleEntity studentSchedule) {
        StudentEntity student = studentSchedule.getStudent();
        Map<Integer, PlaceEntity> placesMap = placeRepository.findAllByGradePrefix(student.getGrade());

        Integer targetPoint = student.getClassNumber();
        for(int count = 0; count < 4; count++) {
            PlaceEntity place = placesMap.get(targetPoint);

            // 해당 반 교실이 존재하지 않으면 다음 반으로 넘어감
            if(place == null) {
                targetPoint = calculateNextClassNumber(targetPoint);
                continue;
            }

            if(!placeRepository.checkPlaceAvailability(
                    studentSchedule.getDay(), studentSchedule.getPeriod(), place
            ))
                return place;
            targetPoint = calculateNextClassNumber(targetPoint);
        }

        throw new NoAvailablePlaceException();
    }

    private void createSelfStudySchedule(ScheduleEntity schedule, SelfStudyEntity selfStudy, PlaceEntity place) {
        // 자습 스케줄 생성
        SelfStudyScheduleEntity selfStudySchedule = SelfStudyScheduleEntity.builder()
                .schedule(schedule)
                .place(place)
                .selfStudy(selfStudy)
                .build();

        selfStudyScheduleRepository.save(selfStudySchedule);
    }

    private ScheduleEntity createNewSchedule(StudentScheduleEntity studentSchedule) {
        // 새로운 스케줄 생성
        Integer lastStackOrder = scheduleRepository.findLastStackOrderByStudentScheduleId(studentSchedule.getId());
        ScheduleEntity newSchedule = ScheduleEntity.createNewStudentSchedule(studentSchedule, lastStackOrder, ScheduleType.SELF_STUDY);

        scheduleRepository.save(newSchedule);

        return newSchedule;
    }
}
