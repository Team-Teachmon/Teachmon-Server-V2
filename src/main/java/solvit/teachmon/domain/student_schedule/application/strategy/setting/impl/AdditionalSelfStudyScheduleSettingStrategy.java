package solvit.teachmon.domain.student_schedule.application.strategy.setting.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import solvit.teachmon.domain.management.student.domain.entity.StudentEntity;
import solvit.teachmon.domain.place.domain.entity.PlaceEntity;
import solvit.teachmon.domain.place.domain.repository.PlaceRepository;
import solvit.teachmon.domain.self_study.domain.entity.AdditionalSelfStudyEntity;
import solvit.teachmon.domain.self_study.domain.repository.AdditionalSelfStudyRepository;
import solvit.teachmon.domain.student_schedule.application.service.StudentScheduleGenerator;
import solvit.teachmon.domain.student_schedule.application.strategy.setting.StudentScheduleSettingStrategy;
import solvit.teachmon.domain.student_schedule.domain.entity.ScheduleEntity;
import solvit.teachmon.domain.student_schedule.domain.entity.StudentScheduleEntity;
import solvit.teachmon.domain.student_schedule.domain.entity.schedules.AdditionalSelfStudyScheduleEntity;
import solvit.teachmon.domain.student_schedule.domain.enums.ScheduleType;
import solvit.teachmon.domain.student_schedule.domain.exception.NoAvailablePlaceException;
import solvit.teachmon.domain.student_schedule.domain.repository.ScheduleRepository;
import solvit.teachmon.domain.student_schedule.domain.repository.schedules.AdditionalSelfStudyScheduleRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static solvit.teachmon.domain.place.domain.entity.PlaceEntity.calculateNextClassNumber;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdditionalSelfStudyScheduleSettingStrategy implements StudentScheduleSettingStrategy {
    private final AdditionalSelfStudyRepository additionalSelfStudyRepository;
    private final ScheduleRepository scheduleRepository;
    private final AdditionalSelfStudyScheduleRepository additionalSelfStudyScheduleRepository;
    private final PlaceRepository placeRepository;
    private final StudentScheduleGenerator studentScheduleGenerator;

    @Override
    public ScheduleType getScheduleType() {
        return ScheduleType.ADDITIONAL_SELF_STUDY;
    }

    @Override
    public void settingSchedule(LocalDate baseDate) {
        List<AdditionalSelfStudyEntity> additionalSelfStudies = findWeeklyAdditionalSelfStudies(baseDate);

        for(AdditionalSelfStudyEntity additionalSelfStudy : additionalSelfStudies) {
            if(isBeforeAdditionalSelfStudy(additionalSelfStudy, baseDate))
                continue;
            List<StudentScheduleEntity> studentSchedules = studentScheduleGenerator.findOrCreateStudentSchedules(
                    additionalSelfStudy.getGrade(),
                    additionalSelfStudy.getDay(),
                    additionalSelfStudy.getPeriod()
            );
            settingAdditionalSelfStudySchedule(studentSchedules, additionalSelfStudy);
        }
    }

    private Boolean isBeforeAdditionalSelfStudy(AdditionalSelfStudyEntity additionalSelfStudy, LocalDate baseDate) {
        return additionalSelfStudy.getDay().isBefore(baseDate);
    }

    private List<AdditionalSelfStudyEntity> findWeeklyAdditionalSelfStudies(LocalDate baseDate) {
        LocalDate startDay = baseDate.with(DayOfWeek.MONDAY);
        LocalDate endDay = baseDate.with(DayOfWeek.SUNDAY);

        return additionalSelfStudyRepository.findAllByDayBetween(startDay, endDay);
    }


    private void settingAdditionalSelfStudySchedule(
            List<StudentScheduleEntity> studentSchedules,
            AdditionalSelfStudyEntity additionalSelfStudy
    ) {
        for(StudentScheduleEntity studentSchedule : studentSchedules) {
            PlaceEntity place;
            try {
                place = findAdditionalSelfStudyPlace(studentSchedule);
            } catch (NoAvailablePlaceException e) {
                // 장소 하나를 못 찾았다고 전체 스케줄 설정이 롤백되지 않도록 해당 건만 건너뜀
                log.warn("추가 자습 장소 배정 실패 - studentScheduleId: {}, day: {}, period: {}",
                        studentSchedule.getId(), studentSchedule.getDay(), studentSchedule.getPeriod());
                continue;
            }
            ScheduleEntity newSchedule = createNewSchedule(studentSchedule);
            createAdditionalSelfStudySchedule(newSchedule, additionalSelfStudy, place);
        }
    }

    private PlaceEntity findAdditionalSelfStudyPlace(StudentScheduleEntity studentSchedule) {
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

    private void createAdditionalSelfStudySchedule(ScheduleEntity schedule, AdditionalSelfStudyEntity additionalSelfStudy, PlaceEntity place) {
        // 자습 스케줄 생성
        AdditionalSelfStudyScheduleEntity additionalSelfStudySchedule = AdditionalSelfStudyScheduleEntity.builder()
                .schedule(schedule)
                .place(place)
                .additionalSelfStudy(additionalSelfStudy)
                .build();

        additionalSelfStudyScheduleRepository.save(additionalSelfStudySchedule);
    }

    private ScheduleEntity createNewSchedule(StudentScheduleEntity studentSchedule) {
        // 새로운 스케줄 생성
        Integer lastStackOrder = scheduleRepository.findLastStackOrderByStudentScheduleId(studentSchedule.getId());
        ScheduleEntity newSchedule = ScheduleEntity.createNewStudentSchedule(studentSchedule, lastStackOrder, ScheduleType.ADDITIONAL_SELF_STUDY);

        scheduleRepository.save(newSchedule);

        return newSchedule;
    }
}
