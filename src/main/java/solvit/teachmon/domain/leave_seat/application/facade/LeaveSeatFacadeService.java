package solvit.teachmon.domain.leave_seat.application.facade;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import solvit.teachmon.domain.leave_seat.domain.entity.LeaveSeatEntity;
import solvit.teachmon.domain.leave_seat.domain.entity.LeaveSeatStudentEntity;
import solvit.teachmon.domain.leave_seat.domain.repository.LeaveSeatRepository;
import solvit.teachmon.domain.leave_seat.domain.repository.LeaveSeatStudentRepository;
import solvit.teachmon.domain.leave_seat.exception.LeaveSeatNotFoundException;
import solvit.teachmon.domain.leave_seat.exception.LeaveSeatValueInvalidException;
import solvit.teachmon.domain.leave_seat.application.mapper.LeaveSeatMapper;
import solvit.teachmon.domain.leave_seat.presentation.dto.request.LeaveSeatCreateRequest;
import solvit.teachmon.domain.leave_seat.presentation.dto.request.LeaveSeatUpdateRequest;
import solvit.teachmon.domain.leave_seat.presentation.dto.response.LeaveSeatDetailResponse;
import solvit.teachmon.domain.leave_seat.presentation.dto.response.LeaveSeatListResponse;
import solvit.teachmon.domain.leave_seat.presentation.dto.response.PlaceAvailabilityResponse;
import solvit.teachmon.domain.management.student.domain.entity.StudentEntity;
import solvit.teachmon.domain.management.student.domain.repository.StudentRepository;
import solvit.teachmon.domain.management.student.exception.StudentNotFoundException;
import solvit.teachmon.domain.place.domain.entity.PlaceEntity;
import solvit.teachmon.domain.place.domain.repository.PlaceRepository;
import solvit.teachmon.domain.place.exception.PlaceNotFoundException;
import solvit.teachmon.domain.student_schedule.domain.entity.ScheduleEntity;
import solvit.teachmon.domain.student_schedule.domain.entity.StudentScheduleEntity;
import solvit.teachmon.domain.student_schedule.domain.entity.schedules.LeaveSeatScheduleEntity;
import solvit.teachmon.domain.student_schedule.domain.enums.ScheduleType;
import solvit.teachmon.domain.student_schedule.domain.repository.ScheduleRepository;
import solvit.teachmon.domain.student_schedule.domain.repository.StudentScheduleRepository;
import solvit.teachmon.domain.student_schedule.domain.repository.schedules.LeaveSeatScheduleRepository;
import solvit.teachmon.domain.student_schedule.exception.StudentScheduleNotFoundException;
import solvit.teachmon.domain.user.domain.entity.TeacherEntity;
import solvit.teachmon.global.enums.SchoolPeriod;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LeaveSeatFacadeService {
    private final PlaceRepository placeRepository;
    private final LeaveSeatRepository leaveSeatRepository;
    private final ScheduleRepository scheduleRepository;
    private final StudentScheduleRepository studentScheduleRepository;
    private final LeaveSeatScheduleRepository leaveSeatScheduleRepository;
    private final LeaveSeatStudentRepository leaveSeatStudentRepository;
    private final StudentRepository studentRepository;
    private final LeaveSeatMapper leaveSeatMapper;

    @Transactional
    public void createLeaveSeat(LeaveSeatCreateRequest request, TeacherEntity teacher) {
        PlaceEntity place = placeRepository.findById(request.placeId())
                .orElseThrow(PlaceNotFoundException::new);

        List<StudentEntity> students = getStudents(request.students());

        // 기존 leaveSeat 이 있으면 가져오고, 없으면 새로 생성
        LeaveSeatEntity leaveSeat = leaveSeatRepository.findByPlaceAndDayAndPeriod(place, request.day(), request.period())
                .orElseGet(() -> saveLeaveSeat(request, teacher, place));

        saveLeaveSeatRelatedData(leaveSeat, students, request.day(), request.period());
    }

    // leaveSeat 관련 데이터 저장 메서드
    private void saveLeaveSeatRelatedData(LeaveSeatEntity leaveSeat, List<StudentEntity> students, LocalDate day, SchoolPeriod period) {
        saveLeaveSeatStudent(leaveSeat, students);
        List<StudentScheduleEntity> studentSchedules = getStudentSchedules(students, day, period);
        saveLeaveSeatSchedules(studentSchedules, leaveSeat);
    }

    // LeaveSeatStudent 저장 메서드
    private void saveLeaveSeatStudent(LeaveSeatEntity leaveSeat, List<StudentEntity> students) {
        List<LeaveSeatStudentEntity> leaveSeatStudents = students.stream()
                .map(student -> LeaveSeatStudentEntity.builder()
                        .leaveSeat(leaveSeat)
                        .student(student)
                        .build())
                .toList();

        leaveSeatStudentRepository.saveAll(leaveSeatStudents);
    }

    // LeaveSeat 저장 메서드
    private LeaveSeatEntity saveLeaveSeat(LeaveSeatCreateRequest request, TeacherEntity teacher, PlaceEntity place) {
        LeaveSeatEntity leaveSeat = LeaveSeatEntity.builder()
                .place(place)
                .teacher(teacher)
                .day(request.day())
                .period(request.period())
                .cause(request.cause())
                .build();

        return leaveSeatRepository.save(leaveSeat);
    }

    // LeaveSeatSchedule 및 Schedule 저장 메서드
    private void saveLeaveSeatSchedules(List<StudentScheduleEntity> studentSchedules, LeaveSeatEntity leaveSeat) {
        List<ScheduleEntity> newSchedules = studentSchedules.stream()
                .map(studentSchedule -> {
                    Integer lastStackOrder = scheduleRepository.findLastStackOrderByStudentScheduleId(studentSchedule.getId());
                    return ScheduleEntity.createNewStudentSchedule(studentSchedule, lastStackOrder, ScheduleType.LEAVE_SEAT);
                })
                .toList();
        scheduleRepository.saveAll(newSchedules);

        List<LeaveSeatScheduleEntity> leaveSeatSchedules = newSchedules.stream()
                .map(schedule -> LeaveSeatScheduleEntity.builder()
                        .schedule(schedule)
                        .leaveSeat(leaveSeat)
                        .build())
                .toList();

        leaveSeatScheduleRepository.saveAll(leaveSeatSchedules);
    }

    @Transactional(readOnly = true)
    public List<LeaveSeatListResponse> getLeaveSeatList(LocalDate day, SchoolPeriod period) {
        List<LeaveSeatEntity> leaveSeats = leaveSeatRepository.findAllByDayAndPeriodWithFetch(day, period);

        return leaveSeats.stream()
                .map(leaveSeat -> {
                    List<LeaveSeatStudentEntity> leaveSeatStudents = leaveSeatStudentRepository.findAllByLeaveSeatWithFetch(leaveSeat);
                    return leaveSeatMapper.toListResponse(leaveSeat, leaveSeatStudents);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public LeaveSeatDetailResponse getLeaveSeatDetail(Long leaveSeatId) {
        LeaveSeatEntity leaveSeat = leaveSeatRepository.findByIdWithFetch(leaveSeatId)
                .orElseThrow(LeaveSeatNotFoundException::new);

        List<LeaveSeatStudentEntity> leaveSeatStudents = leaveSeatStudentRepository.findAllByLeaveSeatWithFetch(leaveSeat);
        List<StudentEntity> students = leaveSeatStudents.stream()
                .map(LeaveSeatStudentEntity::getStudent)
                .toList();

        // 이석한 학생들의 상태 조회
        Map<Long, ScheduleType> studentLastScheduleTypes = studentScheduleRepository
                .findLastScheduleTypeByStudentsAndDayAndPeriod(students, leaveSeat.getDay(), leaveSeat.getPeriod());

        return leaveSeatMapper.toDetailResponse(leaveSeat, students, studentLastScheduleTypes);
    }

    @Transactional
    public void updateLeaveSeat(Long leaveSeatId, LeaveSeatUpdateRequest request, TeacherEntity teacher) {
        LeaveSeatEntity currentLeaveSeat = leaveSeatRepository.findById(leaveSeatId)
                .orElseThrow(() -> new LeaveSeatValueInvalidException("이석을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        PlaceEntity place = placeRepository.findById(request.place())
                .orElseThrow(PlaceNotFoundException::new);

        List<StudentEntity> students = getStudents(request.students());

        // 기존 LeaveSeatStudent, LeaveSeatSchedule, Schedule 삭제
        deleteLeaveSeatRelatedData(leaveSeatId);

        // 새 place/day/period에 현재와 다른 leaveSeat이 있으면 그쪽으로 합치고, 없으면 기존 leaveSeat 수정
        LeaveSeatEntity leaveSeat = leaveSeatRepository.findByPlaceAndDayAndPeriod(place, request.day(), request.period())
                .filter(found -> !found.getId().equals(leaveSeatId))
                .orElse(currentLeaveSeat);

        if (leaveSeat != currentLeaveSeat) {
            // 다른 leaveSeat으로 합쳐지는 경우: 기존 leaveSeat 삭제
            leaveSeatRepository.delete(currentLeaveSeat);
        } else {
            // 일반 수정: 기존 leaveSeat 정보 업데이트
            leaveSeat.changeLeaveSeatInfo(teacher, place, request.day(), request.period(), request.cause());
        }

        // 새로운 LeaveSeatStudent, LeaveSeatSchedule, Schedule 저장
        saveLeaveSeatRelatedData(leaveSeat, students, request.day(), request.period());
    }

    @Transactional
    public void deleteLeaveSeat(Long leaveSeatId) {
        LeaveSeatEntity leaveSeat = leaveSeatRepository.findById(leaveSeatId)
                .orElseThrow(() -> new LeaveSeatValueInvalidException("이석을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        // 관련 데이터 삭제
        deleteLeaveSeatRelatedData(leaveSeatId);

        // LeaveSeat 삭제
        leaveSeatRepository.delete(leaveSeat);
    }

    @Transactional(readOnly = true)
    public PlaceAvailabilityResponse checkPlaceAvailability(Long placeId, LocalDate day, SchoolPeriod period) {
        Boolean isEmpty = leaveSeatRepository.isPlaceAvailableForLeaveSeat(placeId, day, period);

        return PlaceAvailabilityResponse.builder()
                .isEmpty(isEmpty)
                .build();
    }


    // leaveSeat 관련 데이터 삭제 메서드
    private void deleteLeaveSeatRelatedData(Long leaveSeatId) {
        // LeaveSeatSchedule 조회
        List<LeaveSeatScheduleEntity> leaveSeatSchedules = leaveSeatScheduleRepository.findAll().stream()
                .filter(ls -> ls.getLeaveSeat().getId().equals(leaveSeatId))
                .toList();

        // Schedule 삭제
        List<ScheduleEntity> schedules = leaveSeatSchedules.stream()
                .map(LeaveSeatScheduleEntity::getSchedule)
                .toList();

        leaveSeatScheduleRepository.deleteAll(leaveSeatSchedules);
        leaveSeatStudentRepository.deleteAllByLeaveSeatId(leaveSeatId);
        scheduleRepository.deleteAll(schedules);
    }

    private List<StudentEntity> getStudents(List<Long> studentIds) {
        List<StudentEntity> students = studentRepository.findAllById(studentIds);
        if (students.size() != studentIds.size()) {
            throw new StudentNotFoundException();
        }
        return students;
    }

    private List<StudentScheduleEntity> getStudentSchedules(List<StudentEntity> students, LocalDate day, SchoolPeriod period) {
        List<StudentScheduleEntity> studentSchedules = studentScheduleRepository.findAllByStudentsAndDayAndPeriod(students, day, period);
        if (studentSchedules.size() != students.size()) {
            throw new StudentScheduleNotFoundException();
        }
        return studentSchedules;
    }
}
