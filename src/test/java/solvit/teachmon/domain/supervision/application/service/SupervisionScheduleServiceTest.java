package solvit.teachmon.domain.supervision.application.service;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import solvit.teachmon.domain.supervision.domain.entity.SupervisionExchangeEntity;
import solvit.teachmon.domain.supervision.domain.entity.SupervisionScheduleEntity;
import solvit.teachmon.domain.supervision.domain.enums.SupervisionDeleteType;
import solvit.teachmon.domain.supervision.domain.enums.SupervisionExchangeType;
import solvit.teachmon.domain.supervision.domain.enums.SupervisionType;
import solvit.teachmon.domain.supervision.domain.repository.SupervisionExchangeRepository;
import solvit.teachmon.domain.supervision.domain.repository.SupervisionScheduleRepository;
import solvit.teachmon.domain.supervision.exception.InvalidSupervisionExchangeException;
import solvit.teachmon.domain.supervision.presentation.dto.request.SupervisionExchangeAcceptRequestDto;
import solvit.teachmon.domain.supervision.presentation.dto.request.SupervisionExchangeRejectRequestDto;
import solvit.teachmon.domain.supervision.presentation.dto.request.SupervisionScheduleCreateRequestDto;
import solvit.teachmon.domain.supervision.presentation.dto.request.SupervisionScheduleDeleteRequestDto;
import solvit.teachmon.domain.supervision.presentation.dto.request.SupervisionScheduleUpdateRequestDto;
import solvit.teachmon.domain.user.domain.entity.TeacherEntity;
import solvit.teachmon.domain.user.domain.enums.OAuth2Type;
import solvit.teachmon.domain.user.domain.repository.TeacherRepository;
import solvit.teachmon.global.enums.SchoolPeriod;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("감독 일정 수정/삭제 시 교체 신청 유지 테스트")
class SupervisionScheduleServiceTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 20);
    private static final LocalDate OTHER_DAY = LocalDate.of(2026, 11, 25);

    @Autowired
    private SupervisionScheduleService supervisionScheduleService;

    @Autowired
    private SupervisionExchangeService supervisionExchangeService;

    @Autowired
    private SupervisionScheduleRepository supervisionScheduleRepository;

    @Autowired
    private SupervisionExchangeRepository supervisionExchangeRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private EntityManager entityManager;

    // DAY: 자습 A, 이석 B / OTHER_DAY: 자습 C, 이석 D
    private TeacherEntity teacherA;
    private TeacherEntity teacherB;
    private TeacherEntity teacherC;
    private TeacherEntity teacherD;
    private TeacherEntity teacherE;

    @BeforeEach
    void setUp() {
        teacherA = saveTeacher("A");
        teacherB = saveTeacher("B");
        teacherC = saveTeacher("C");
        teacherD = saveTeacher("D");
        teacherE = saveTeacher("E");

        supervisionScheduleService.createSupervisionSchedule(
                new SupervisionScheduleCreateRequestDto(DAY, teacherA.getId(), teacherB.getId(), null));
        supervisionScheduleService.createSupervisionSchedule(
                new SupervisionScheduleCreateRequestDto(OTHER_DAY, teacherC.getId(), teacherD.getId(), null));
        flushAndClear();
    }

    private TeacherEntity saveTeacher(String suffix) {
        return teacherRepository.save(TeacherEntity.builder()
                .name("교사" + suffix)
                .mail("schedule-" + suffix + "@test.com")
                .providerId("schedule-" + suffix)
                .oAuth2Type(OAuth2Type.GOOGLE)
                .build());
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private SupervisionScheduleEntity schedule(LocalDate day, SupervisionType type, SchoolPeriod period) {
        return supervisionScheduleRepository.findAllByDay(day).stream()
                .filter(s -> s.getType() == type && s.getPeriod() == period)
                .findFirst()
                .orElseThrow();
    }

    private Map<String, Long> scheduleIds(LocalDate day) {
        return supervisionScheduleRepository.findAllByDay(day).stream()
                .collect(Collectors.toMap(s -> s.getType() + "/" + s.getPeriod(), SupervisionScheduleEntity::getId));
    }

    /** DAY 의 schedule 담당자와 OTHER_DAY 의 같은 칸 담당자 사이의 교체 신청 */
    private Long requestExchange(SupervisionType type, SchoolPeriod period) {
        SupervisionScheduleEntity senderSchedule = schedule(DAY, type, period);
        SupervisionScheduleEntity recipientSchedule = schedule(OTHER_DAY, type, period);
        Long id = supervisionExchangeRepository.save(SupervisionExchangeEntity.builder()
                .sender(senderSchedule.getTeacher())
                .recipient(recipientSchedule.getTeacher())
                .senderSchedule(senderSchedule)
                .recipientSchedule(recipientSchedule)
                .reason("개인 사유")
                .build()).getId();
        flushAndClear();
        return id;
    }

    private void update(LocalDate day, TeacherEntity selfStudy, TeacherEntity leaveSeat, TeacherEntity seventh) {
        supervisionScheduleService.updateSupervisionSchedule(new SupervisionScheduleUpdateRequestDto(
                day, selfStudy.getId(), leaveSeat.getId(), seventh == null ? null : seventh.getId()));
        flushAndClear();
    }

    @Test
    @DisplayName("한 칸만 바꿔도 그날 다른 일정의 ID 와 교체 신청이 유지된다")
    void shouldKeepOtherSchedulesAndExchangesWhenOneSlotChanged() {
        // Given
        Long exchangeId = requestExchange(SupervisionType.SELF_STUDY_SUPERVISION, SchoolPeriod.EIGHT_AND_NINE_PERIOD);
        Map<String, Long> before = scheduleIds(DAY);

        // When: 이석 감독만 B → E
        update(DAY, teacherA, teacherE, null);

        // Then
        assertThat(scheduleIds(DAY)).isEqualTo(before);
        assertThat(schedule(DAY, SupervisionType.LEAVE_SEAT_SUPERVISION, SchoolPeriod.EIGHT_AND_NINE_PERIOD).getTeacher().getId())
                .isEqualTo(teacherE.getId());
        assertThat(schedule(DAY, SupervisionType.SELF_STUDY_SUPERVISION, SchoolPeriod.EIGHT_AND_NINE_PERIOD).getTeacher().getId())
                .isEqualTo(teacherA.getId());
        SupervisionExchangeEntity exchange = supervisionExchangeRepository.findById(exchangeId).orElseThrow();
        assertThat(exchange.getState()).isEqualTo(SupervisionExchangeType.PENDING);
    }

    @Test
    @DisplayName("다른 날과 맞바꾼 신청은 상대 날짜를 수정해도 유지되고, 수락 기록도 남는다")
    void shouldKeepAcceptedExchangeWhenOtherDayUpdated() {
        // Given
        Long exchangeId = requestExchange(SupervisionType.LEAVE_SEAT_SUPERVISION, SchoolPeriod.TEN_AND_ELEVEN_PERIOD);
        supervisionExchangeService.acceptSupervisionExchange(new SupervisionExchangeAcceptRequestDto(exchangeId), teacherD.getId());
        flushAndClear();

        // When: OTHER_DAY 자습 감독만 C → E
        update(OTHER_DAY, teacherE, teacherD, null);

        // Then
        SupervisionExchangeEntity exchange = supervisionExchangeRepository.findById(exchangeId).orElseThrow();
        assertThat(exchange.getState()).isEqualTo(SupervisionExchangeType.ACCEPTED);
    }

    @Test
    @DisplayName("7교시를 추가하면 새로 만들고 기존 일정은 유지한다")
    void shouldCreateSeventhPeriodWhenAdded() {
        Map<String, Long> before = scheduleIds(DAY);

        update(DAY, teacherA, teacherB, teacherE);

        Map<String, Long> after = scheduleIds(DAY);
        assertThat(after).hasSize(5).containsAllEntriesOf(before);
        assertThat(schedule(DAY, SupervisionType.SEVENTH_PERIOD_SUPERVISION, SchoolPeriod.SEVEN_PERIOD).getTeacher().getId())
                .isEqualTo(teacherE.getId());
    }

    @Test
    @DisplayName("7교시를 빼면 7교시 일정과 그 일정에 걸린 신청만 삭제된다")
    void shouldRemoveOnlySeventhPeriodScheduleAndItsExchanges() {
        // Given: 두 날 모두 7교시 추가 후 7교시 신청과 자습 신청을 각각 만든다
        update(DAY, teacherA, teacherB, teacherA);
        update(OTHER_DAY, teacherC, teacherD, teacherC);
        Long seventhExchangeId = requestExchange(SupervisionType.SEVENTH_PERIOD_SUPERVISION, SchoolPeriod.SEVEN_PERIOD);
        Long selfStudyExchangeId = requestExchange(SupervisionType.SELF_STUDY_SUPERVISION, SchoolPeriod.EIGHT_AND_NINE_PERIOD);

        // When
        update(DAY, teacherA, teacherB, null);

        // Then
        assertThat(scheduleIds(DAY)).hasSize(4).doesNotContainKey("SEVENTH_PERIOD_SUPERVISION/SEVEN_PERIOD");
        assertThat(supervisionExchangeRepository.findById(seventhExchangeId)).isEmpty();
        assertThat(supervisionExchangeRepository.findById(selfStudyExchangeId)).isPresent();
    }

    @Test
    @DisplayName("같은 칸에 중복 일정이 있으면 하나만 남기고, 지워지는 중복 일정의 신청만 삭제된다")
    void shouldRemoveDuplicateSchedules() {
        // Given: 자습 8-9교시에 중복 행을 하나 더 만들고, 원래 행과 중복 행에 각각 신청을 건다
        SupervisionScheduleEntity original = schedule(DAY, SupervisionType.SELF_STUDY_SUPERVISION, SchoolPeriod.EIGHT_AND_NINE_PERIOD);
        Long keptExchangeId = requestExchange(SupervisionType.SELF_STUDY_SUPERVISION, SchoolPeriod.EIGHT_AND_NINE_PERIOD);
        SupervisionScheduleEntity duplicate = supervisionScheduleRepository.save(SupervisionScheduleEntity.builder()
                .teacher(teacherRepository.findById(teacherA.getId()).orElseThrow())
                .day(DAY)
                .period(SchoolPeriod.EIGHT_AND_NINE_PERIOD)
                .type(SupervisionType.SELF_STUDY_SUPERVISION)
                .build());
        Long removedExchangeId = supervisionExchangeRepository.save(SupervisionExchangeEntity.builder()
                .sender(duplicate.getTeacher())
                .recipient(teacherRepository.findById(teacherC.getId()).orElseThrow())
                .senderSchedule(duplicate)
                .recipientSchedule(schedule(OTHER_DAY, SupervisionType.SELF_STUDY_SUPERVISION, SchoolPeriod.EIGHT_AND_NINE_PERIOD))
                .reason("개인 사유")
                .build()).getId();
        flushAndClear();
        Long keptId = Math.min(original.getId(), duplicate.getId());

        // When
        update(DAY, teacherA, teacherB, null);

        // Then
        List<SupervisionScheduleEntity> selfStudy89 = supervisionScheduleRepository.findAllByDay(DAY).stream()
                .filter(s -> s.getType() == SupervisionType.SELF_STUDY_SUPERVISION && s.getPeriod() == SchoolPeriod.EIGHT_AND_NINE_PERIOD)
                .toList();
        assertThat(selfStudy89).extracting(SupervisionScheduleEntity::getId).containsExactly(keptId);
        Long keptExchange = keptId.equals(original.getId()) ? keptExchangeId : removedExchangeId;
        Long removedExchange = keptId.equals(original.getId()) ? removedExchangeId : keptExchangeId;
        assertThat(supervisionExchangeRepository.findById(keptExchange)).isPresent();
        assertThat(supervisionExchangeRepository.findById(removedExchange)).isEmpty();
    }

    @Test
    @DisplayName("관리자가 담당자를 바꾼 일정에 걸린 대기 신청은 수락이 막히고 거절은 된다")
    void shouldBlockAcceptWhenScheduleTeacherChangedByAdmin() {
        // Given
        Long exchangeId = requestExchange(SupervisionType.SELF_STUDY_SUPERVISION, SchoolPeriod.EIGHT_AND_NINE_PERIOD);
        update(DAY, teacherE, teacherB, null);

        // When & Then
        assertThatThrownBy(() -> supervisionExchangeService.acceptSupervisionExchange(
                new SupervisionExchangeAcceptRequestDto(exchangeId), teacherC.getId()))
                .isInstanceOf(InvalidSupervisionExchangeException.class)
                .hasMessage("감독 일정이 변경되어 수락할 수 없습니다.");
        assertThat(schedule(OTHER_DAY, SupervisionType.SELF_STUDY_SUPERVISION, SchoolPeriod.EIGHT_AND_NINE_PERIOD).getTeacher().getId())
                .isEqualTo(teacherC.getId());

        supervisionExchangeService.rejectSupervisionExchange(new SupervisionExchangeRejectRequestDto(exchangeId), teacherC.getId());
        flushAndClear();
        assertThat(supervisionExchangeRepository.findById(exchangeId).orElseThrow().getState())
                .isEqualTo(SupervisionExchangeType.REJECTED);
    }

    @Test
    @DisplayName("타입별 삭제는 그 타입 일정에 걸린 신청만 삭제한다")
    void shouldDeleteOnlyExchangesOfDeletedType() {
        // Given
        Long selfStudyExchangeId = requestExchange(SupervisionType.SELF_STUDY_SUPERVISION, SchoolPeriod.EIGHT_AND_NINE_PERIOD);
        Long leaveSeatExchangeId = requestExchange(SupervisionType.LEAVE_SEAT_SUPERVISION, SchoolPeriod.EIGHT_AND_NINE_PERIOD);

        // When
        supervisionScheduleService.deleteSupervisionSchedule(
                new SupervisionScheduleDeleteRequestDto(DAY, SupervisionDeleteType.SELF_STUDY));
        flushAndClear();

        // Then
        assertThat(supervisionExchangeRepository.findById(selfStudyExchangeId)).isEmpty();
        assertThat(supervisionExchangeRepository.findById(leaveSeatExchangeId)).isPresent();
        assertThat(supervisionScheduleRepository.findAllByDay(DAY))
                .extracting(SupervisionScheduleEntity::getType)
                .containsOnly(SupervisionType.LEAVE_SEAT_SUPERVISION);
    }

    @Test
    @DisplayName("하루 전체 삭제는 그날 일정과 그 일정에 걸린 신청을 모두 삭제한다")
    void shouldDeleteAllSchedulesAndExchangesOfDay() {
        // Given
        Long exchangeId = requestExchange(SupervisionType.LEAVE_SEAT_SUPERVISION, SchoolPeriod.EIGHT_AND_NINE_PERIOD);

        // When
        supervisionScheduleService.deleteSupervisionSchedule(
                new SupervisionScheduleDeleteRequestDto(DAY, SupervisionDeleteType.ALL));
        flushAndClear();

        // Then
        assertThat(supervisionScheduleRepository.findAllByDay(DAY)).isEmpty();
        assertThat(supervisionExchangeRepository.findById(exchangeId)).isEmpty();
        assertThat(supervisionScheduleRepository.findAllByDay(OTHER_DAY)).hasSize(4);
    }
}
