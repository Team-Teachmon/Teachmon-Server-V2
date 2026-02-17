package solvit.teachmon.domain.student_schedule.application.facade;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import solvit.teachmon.domain.place.exception.PlaceNotFoundException;
import solvit.teachmon.domain.place.domain.entity.PlaceEntity;
import solvit.teachmon.domain.place.domain.repository.PlaceRepository;
import solvit.teachmon.domain.student_schedule.application.dto.PlaceScheduleDto;
import solvit.teachmon.domain.student_schedule.application.mapper.PlaceStudentScheduleMapper;
import solvit.teachmon.domain.student_schedule.application.mapper.StudentScheduleMapper;
import solvit.teachmon.domain.student_schedule.domain.entity.ScheduleEntity;
import solvit.teachmon.domain.student_schedule.domain.enums.ScheduleType;
import solvit.teachmon.domain.student_schedule.domain.repository.StudentScheduleRepository;
import solvit.teachmon.domain.student_schedule.domain.repository.schedules.AdditionalSelfStudyScheduleRepository;
import solvit.teachmon.domain.student_schedule.domain.repository.schedules.AfterSchoolScheduleRepository;
import solvit.teachmon.domain.student_schedule.domain.repository.schedules.LeaveSeatScheduleRepository;
import solvit.teachmon.domain.student_schedule.domain.repository.schedules.SelfStudyScheduleRepository;
import solvit.teachmon.domain.student_schedule.presentation.dto.response.FloorStateResponse;
import solvit.teachmon.domain.student_schedule.presentation.dto.response.PlaceStateResponse;
import solvit.teachmon.domain.student_schedule.presentation.dto.response.PlaceStudentScheduleResponse;
import solvit.teachmon.domain.student_schedule.presentation.dto.response.StudentScheduleResponse;
import solvit.teachmon.global.enums.SchoolPeriod;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class PlaceStudentScheduleService {
    private final StudentScheduleRepository studentScheduleRepository;
    private final SelfStudyScheduleRepository selfStudyScheduleRepository;
    private final AdditionalSelfStudyScheduleRepository additionalSelfStudyScheduleRepository;
    private final LeaveSeatScheduleRepository leaveSeatScheduleRepository;
    private final AfterSchoolScheduleRepository afterSchoolScheduleRepository;
    private final PlaceRepository placeRepository;
    private final PlaceStudentScheduleMapper placeStudentScheduleMapper;
    private final StudentScheduleMapper studentScheduleMapper;
    // 장소 사용 스케줄
    private final List<ScheduleType> placeScheduleType = List.of(
            ScheduleType.SELF_STUDY,
            ScheduleType.ADDITIONAL_SELF_STUDY,
            ScheduleType.LEAVE_SEAT,
            ScheduleType.AFTER_SCHOOL,
            ScheduleType.AFTER_SCHOOL_REINFORCEMENT
    );

    @Transactional(readOnly = true)
    public List<FloorStateResponse> getAllFloorsPlaceCount(LocalDate day, SchoolPeriod period) {
        // 해당 시간의 장소를 사용하고 있는 스케줄 조회 (EXIT/AWAY 처리 포함)
        Map<ScheduleType, List<ScheduleEntity>> placeFillScheduleMap = studentScheduleRepository.findPlaceBasedSchedulesByDayAndPeriodAndTypeIn(day, period, placeScheduleType);
        // 방어: repository가 null을 반환하는 경우 빈 맵으로 대체
        if (placeFillScheduleMap == null) placeFillScheduleMap = Map.of();

        // 각 스케줄 타입별로 층별 장소 사용 인원 조회
        Map<Integer, Long> selfStudyCountMap = selfStudyScheduleRepository.getSelfStudyPlaceCount(
                placeFillScheduleMap.getOrDefault(ScheduleType.SELF_STUDY, List.of())
        );
        Map<Integer, Long> additionalSelfStudyCountMap = additionalSelfStudyScheduleRepository.getAdditionalSelfStudyPlaceCount(
                placeFillScheduleMap.getOrDefault(ScheduleType.ADDITIONAL_SELF_STUDY, List.of())
        );
        Map<Integer, Long> leaveSeatCountMap = leaveSeatScheduleRepository.getLeaveSeatPlaceCount(
                placeFillScheduleMap.getOrDefault(ScheduleType.LEAVE_SEAT, List.of())
        );
        Map<Integer, Long> afterSchoolCountMap = afterSchoolScheduleRepository.getAfterSchoolPlaceCount(
                placeFillScheduleMap.getOrDefault(ScheduleType.AFTER_SCHOOL, List.of())
        );
        Map<Integer, Long> afterSchoolReinfocementCountMap = afterSchoolScheduleRepository.getAfterSchoolReinforcementPlaceCount(
                placeFillScheduleMap.getOrDefault(ScheduleType.AFTER_SCHOOL_REINFORCEMENT, List.of())
        );

        // 각 층별로 장소 사용 인원 합산
        Map<Integer, Long> result = Stream.of(
                        selfStudyCountMap,
                        additionalSelfStudyCountMap,
                        leaveSeatCountMap,
                        afterSchoolCountMap,
                        afterSchoolReinfocementCountMap
                ).flatMap(m -> m.entrySet().stream())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        Long::sum
                ));


        return placeStudentScheduleMapper.toFloorStateResponses(result);
    }

    @Transactional(readOnly = true)
    public List<PlaceStateResponse> getPlaceStatesByFloor(Integer floor, LocalDate day, SchoolPeriod period) {
        // 해당 시간의 장소를 사용하고 있는 스케줄 조회 (EXIT/AWAY 처리 포함)
        Map<ScheduleType, List<ScheduleEntity>> placeFillScheduleMap = studentScheduleRepository.findPlaceBasedSchedulesByDayAndPeriodAndTypeIn(day, period, placeScheduleType);
        // 방어: repository가 null을 반환하는 경우 빈 맵으로 대체
        if (placeFillScheduleMap == null) placeFillScheduleMap = Map.of();

        // 각 스케줄별 장소 추출
        List<PlaceScheduleDto> placeSchedules = Stream.of(
                selfStudyScheduleRepository.getPlaceScheduleByFloor(placeFillScheduleMap.getOrDefault(ScheduleType.SELF_STUDY, List.of()), floor),
                additionalSelfStudyScheduleRepository.getPlaceScheduleByFloor(placeFillScheduleMap.getOrDefault(ScheduleType.ADDITIONAL_SELF_STUDY, List.of()), floor),
                leaveSeatScheduleRepository.getPlaceScheduleByFloor(placeFillScheduleMap.getOrDefault(ScheduleType.LEAVE_SEAT, List.of()), floor),
                afterSchoolScheduleRepository.getPlaceScheduleByFloor(placeFillScheduleMap.getOrDefault(ScheduleType.AFTER_SCHOOL, List.of()), floor),
                afterSchoolScheduleRepository.getReinforcementPlaceScheduleByFloor(placeFillScheduleMap.getOrDefault(ScheduleType.AFTER_SCHOOL_REINFORCEMENT, List.of()), floor)
        ).flatMap(List::stream)
                .toList();

        return placeStudentScheduleMapper.toPlaceStateResponses(placeSchedules);
    }

    @Transactional(readOnly = true)
    public PlaceStudentScheduleResponse getStudentsByPlaceId(Long placeId, LocalDate day, SchoolPeriod period) {
        PlaceEntity place = placeRepository.findById(placeId)
                .orElseThrow(PlaceNotFoundException::new);

        // 해당 장소의 학생 스케줄 조회
        List<StudentScheduleResponse> studentScheduleResponses = Stream.of(
                selfStudyScheduleRepository.getStudentScheduleByPlaceAndDayAndPeriod(placeId, day, period),
                additionalSelfStudyScheduleRepository.getStudentScheduleByPlaceAndDayAndPeriod(placeId, day, period),
                leaveSeatScheduleRepository.getStudentScheduleByPlaceAndDayAndPeriod(placeId, day, period),
                afterSchoolScheduleRepository.getStudentScheduleByPlaceAndDayAndPeriod(placeId, day, period),
                afterSchoolScheduleRepository.getReinforcementStudentScheduleByPlaceAndDayAndPeriod(placeId, day, period)
        ).flatMap(List::stream)
                .map(studentScheduleMapper::toStudentScheduleResponse)
                .toList();

        return placeStudentScheduleMapper.toPlaceStudentScheduleResponse(place, studentScheduleResponses);
    }
}
