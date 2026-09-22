package solvit.teachmon.domain.leave_seat.application.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
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

        // 새 place/day/period에 현재와 다른 leaveSeat이 있으면 그쪽으로 합치고, 없으면 기존 leaveSeat 수정
        LeaveSeatEntity leaveSeat = leaveSeatRepository.findByPlaceAndDayAndPeriod(place, request.day(), request.period())
                .filter(found -> !found.getId().equals(leaveSeatId))
                .orElse(currentLeaveSeat);

        // 기존 관련 데이터 삭제
        deleteLeaveSeatRelatedData(leaveSeatId);
        log.info("✓ 기존 이석 관련 데이터 삭제 완료");

        if (leaveSeat != currentLeaveSeat) {
            log.info("→ 다른 LeaveSeat으로 병합 - 기존 LeaveSeat 삭제");
            // 다른 leaveSeat으로 합쳐지는 경우: 기존 leaveSeat 삭제
            leaveSeatRepository.delete(currentLeaveSeat);
        } else {
            log.info("→ LeaveSeat 정보 업데이트");
            // 일반 수정: 기존 leaveSeat 정보 업데이트
            leaveSeat.changeLeaveSeatInfo(teacher, place, request.day(), request.period(), request.cause());
        }

        // 새로운 LeaveSeatStudent, LeaveSeatSchedule, Schedule 저장
        log.info("→ 새로운 이석 관련 데이터 저장");
        saveLeaveSeatRelatedData(leaveSeat, students, request.day(), request.period());

        log.info("=== 이석 수정 완료 ===");
    }

    @Transactional
    public void deleteLeaveSeat(Long leaveSeatId) {
        log.info("=== 이석 삭제 시작 === leaveSeatId: {}", leaveSeatId);

        LeaveSeatEntity leaveSeat = leaveSeatRepository.findById(leaveSeatId)
                .orElseThrow(() -> new LeaveSeatValueInvalidException("이석을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        log.info("✓ 이석 조회 성공: {}", leaveSeat.getId());

        // 관련 데이터 삭제
        log.info("→ 이석 관련 데이터 삭제 시작");
        deleteLeaveSeatRelatedData(leaveSeatId);
        log.info("✓ 이석 관련 데이터 삭제 완료");

        // LeaveSeat 삭제
        log.info("→ 이석 엔티티 삭제 시작");
        leaveSeatRepository.delete(leaveSeat);
        log.info("✓ 이석 엔티티 삭제 완료");

        log.info("=== 이석 삭제 완료 ===");
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
        log.info("  [Step 1] LeaveSeatSchedule 조회 시작 - leaveSeatId: {}", leaveSeatId);

        try {
            List<LeaveSeatScheduleEntity> leaveSeatSchedules = leaveSeatScheduleRepository.findAllByLeaveSeatId(leaveSeatId);
            log.info("  ✓ LeaveSeatSchedule 조회 완료 - 개수: {}", leaveSeatSchedules.size());

            for (LeaveSeatScheduleEntity lss : leaveSeatSchedules) {
                log.info("    - LeaveSeatSchedule ID: {}, Schedule ID: {}", lss.getId(), lss.getSchedule().getId());
            }

            if (!leaveSeatSchedules.isEmpty()) {
                log.info("  [Step 2] LeaveSeatSchedule 개별 삭제 시작 (CASCADE 작동을 위해)");
                // ⭐ 중요: deleteAllInBatch 대신 개별 delete 사용
                // deleteAllInBatch는 CASCADE를 작동시키지 않으므로,
                // 각 엔티티를 개별적으로 삭제해야 JPA의 CASCADE가 작동함
                for (LeaveSeatScheduleEntity lss : leaveSeatSchedules) {
                    log.info("    → Schedule ID: {} 삭제 중...", lss.getSchedule().getId());
                    leaveSeatScheduleRepository.delete(lss);  // ✅ CASCADE가 작동함
                    log.info("    ✓ Schedule ID: {} 삭제 완료", lss.getSchedule().getId());
                }
                log.info("  ✓ LeaveSeatSchedule 개별 삭제 완료");
            } else {
                log.warn("  ⚠ 삭제할 LeaveSeatSchedule이 없습니다");
            }
        } catch (Exception e) {
            log.error("  ✗ LeaveSeatSchedule 삭제 중 에러 발생", e);
            throw e;
        }

        log.info("  [Step 3] LeaveSeatStudent 삭제 시작 - leaveSeatId: {}", leaveSeatId);
        try {
            leaveSeatStudentRepository.deleteAllByLeaveSeatId(leaveSeatId);
            log.info("  ✓ LeaveSeatStudent 삭제 완료");
        } catch (Exception e) {
            log.error("  ✗ LeaveSeatStudent 삭제 중 에러 발생", e);
            throw e;
        }
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
