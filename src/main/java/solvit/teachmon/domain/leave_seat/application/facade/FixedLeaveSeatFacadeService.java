package solvit.teachmon.domain.leave_seat.application.facade;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import solvit.teachmon.domain.leave_seat.domain.entity.FixedLeaveSeatEntity;
import solvit.teachmon.domain.leave_seat.domain.entity.FixedLeaveSeatStudentEntity;
import solvit.teachmon.domain.leave_seat.domain.repository.FixedLeaveSeatRepository;
import solvit.teachmon.domain.leave_seat.domain.repository.FixedLeaveSeatStudentRepository;
import solvit.teachmon.domain.leave_seat.application.mapper.FixedLeaveSeatMapper;
import solvit.teachmon.domain.leave_seat.exception.FixedLeaveSeatNotFoundException;
import solvit.teachmon.domain.leave_seat.presentation.dto.request.FixedLeaveSeatCreateRequest;
import solvit.teachmon.domain.leave_seat.presentation.dto.request.FixedLeaveSeatUpdateRequest;
import solvit.teachmon.domain.leave_seat.presentation.dto.response.FixedLeaveSeatDetailResponse;
import solvit.teachmon.domain.leave_seat.presentation.dto.response.FixedLeaveSeatListResponse;
import solvit.teachmon.domain.management.student.domain.entity.StudentEntity;
import solvit.teachmon.domain.management.student.domain.repository.StudentRepository;
import solvit.teachmon.domain.management.student.exception.StudentNotFoundException;
import solvit.teachmon.domain.place.domain.entity.PlaceEntity;
import solvit.teachmon.domain.place.domain.repository.PlaceRepository;
import solvit.teachmon.domain.place.exception.PlaceNotFoundException;
import solvit.teachmon.domain.user.domain.entity.TeacherEntity;
import solvit.teachmon.domain.student_schedule.application.service.StudentScheduleSettingService;
import solvit.teachmon.global.enums.WeekDay;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FixedLeaveSeatFacadeService {
    private final PlaceRepository placeRepository;
    private final FixedLeaveSeatRepository fixedLeaveSeatRepository;
    private final FixedLeaveSeatStudentRepository fixedLeaveSeatStudentRepository;
    private final StudentRepository studentRepository;
    private final FixedLeaveSeatMapper fixedLeaveSeatMapper;
    private final StudentScheduleSettingService studentScheduleSettingService;

    @Transactional
    public void createStaticLeaveSeat(FixedLeaveSeatCreateRequest request, TeacherEntity teacher) {
        PlaceEntity place = placeRepository.findById(request.placeId())
                .orElseThrow(PlaceNotFoundException::new);

        List<StudentEntity> students = getStudents(request.students());

        // 고정 이석 저장
        FixedLeaveSeatEntity fixedLeaveSeat = saveFixedLeaveSeat(request, teacher, place);

        // 고정 이석 학생들 저장
        saveFixedLeaveSeatStudent(fixedLeaveSeat, students);

        // 요일이 오늘 기준으로 아직 지나지 않았다면 스케줄 세팅
        updateScheduleIfNotPassed(request.weekDay());
    }

    @Transactional(readOnly = true)
    public List<FixedLeaveSeatListResponse> getStaticLeaveSeatList() {
        List<FixedLeaveSeatEntity> fixedLeaveSeats = fixedLeaveSeatRepository.findAllWithFetch();

        return fixedLeaveSeats.stream()
                .map(fixedLeaveSeat -> {
                    List<FixedLeaveSeatStudentEntity> fixedLeaveSeatStudents = fixedLeaveSeatStudentRepository
                            .findAllByFixedLeaveSeatWithFetch(fixedLeaveSeat);
                    return fixedLeaveSeatMapper.toListResponse(fixedLeaveSeat, fixedLeaveSeatStudents);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public FixedLeaveSeatDetailResponse getStaticLeaveSeatDetail(Long fixedLeaveSeatId) {
        FixedLeaveSeatEntity fixedLeaveSeat = fixedLeaveSeatRepository.findByIdWithFetch(fixedLeaveSeatId)
                .orElseThrow(FixedLeaveSeatNotFoundException::new);

        List<FixedLeaveSeatStudentEntity> fixedLeaveSeatStudents = fixedLeaveSeatStudentRepository
                .findAllByFixedLeaveSeatWithFetch(fixedLeaveSeat);

        List<StudentEntity> students = fixedLeaveSeatStudents.stream()
                .map(FixedLeaveSeatStudentEntity::getStudent)
                .toList();

        return fixedLeaveSeatMapper.toDetailResponse(fixedLeaveSeat, students);
    }

    @Transactional
    public void updateStaticLeaveSeat(Long fixedLeaveSeatId, FixedLeaveSeatUpdateRequest request, TeacherEntity teacher) {
        FixedLeaveSeatEntity fixedLeaveSeat = fixedLeaveSeatRepository.findById(fixedLeaveSeatId)
                .orElseThrow(FixedLeaveSeatNotFoundException::new);

        PlaceEntity place = placeRepository.findById(request.place())
                .orElseThrow(PlaceNotFoundException::new);

        List<StudentEntity> students = getStudents(request.students());

        // 기존 학생 관계 삭제
        fixedLeaveSeatStudentRepository.deleteAllByFixedLeaveSeatId(fixedLeaveSeatId);

        // 고정 이석 정보 업데이트
        fixedLeaveSeat.updateFixedLeaveSeatInfo(
                teacher,
                place,
                request.weekDay(),
                request.period(),
                request.cause()
        );

        // 새로운 학생 관계 저장
        saveFixedLeaveSeatStudent(fixedLeaveSeat, students);

        // 요일이 오늘 기준으로 아직 지나지 않았다면 스케줄 세팅
        updateScheduleIfNotPassed(request.weekDay());
    }

    @Transactional
    public void deleteStaticLeaveSeat(Long fixedLeaveSeatId) {
        FixedLeaveSeatEntity fixedLeaveSeat = fixedLeaveSeatRepository.findById(fixedLeaveSeatId)
                .orElseThrow(FixedLeaveSeatNotFoundException::new);

        // 고정 이석 삭제 (연관된 FixedLeaveSeatStudentEntity 도 함께 삭제됨)
        fixedLeaveSeatRepository.delete(fixedLeaveSeat);
    }

    private void saveFixedLeaveSeatStudent(FixedLeaveSeatEntity fixedLeaveSeat, List<StudentEntity> students) {
        List<FixedLeaveSeatStudentEntity> leaveSeatStudents = students.stream()
                .map(student -> FixedLeaveSeatStudentEntity.builder()
                        .fixedLeaveSeat(fixedLeaveSeat)
                        .student(student)
                        .build())
                .toList();

        fixedLeaveSeatStudentRepository.saveAll(leaveSeatStudents);
    }

    private FixedLeaveSeatEntity saveFixedLeaveSeat(FixedLeaveSeatCreateRequest request, TeacherEntity teacher, PlaceEntity place) {
        FixedLeaveSeatEntity fixedLeaveSeat = FixedLeaveSeatEntity.builder()
                .teacher(teacher)
                .place(place)
                .weekDay(request.weekDay())
                .period(request.period())
                .cause(request.cause())
                .build();

        return fixedLeaveSeatRepository.save(fixedLeaveSeat);
    }

    private List<StudentEntity> getStudents(List<Long> studentIds) {
        List<StudentEntity> students = studentRepository.findAllById(studentIds);
        if (students.size() != studentIds.size()) {
            throw new StudentNotFoundException();
        }
        return students;
    }

    private void updateScheduleIfNotPassed(WeekDay weekDay) {
        LocalDate today = LocalDate.now();
        LocalDate targetDate = today.with(weekDay.toDayOfWeek());

        // 해당 요일이 오늘 기준으로 아직 지나지 않았다면 스케줄 세팅
        if (!targetDate.isBefore(today)) {
            studentScheduleSettingService.settingAllTypeSchedule(today);
        }
    }
}
