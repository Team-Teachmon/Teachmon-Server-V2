package solvit.teachmon.domain.supervision.domain.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import solvit.teachmon.domain.supervision.domain.enums.SupervisionExchangeType;
import solvit.teachmon.domain.supervision.domain.enums.SupervisionType;
import solvit.teachmon.domain.user.domain.entity.TeacherEntity;
import solvit.teachmon.domain.user.domain.enums.OAuth2Type;
import solvit.teachmon.global.enums.SchoolPeriod;

import java.time.LocalDate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;
import solvit.teachmon.domain.supervision.exception.InvalidSupervisionExchangeException;

@DisplayName("감독 교체 엔티티 테스트")
class SupervisionExchangeEntityTest {

    private TeacherEntity senderTeacher;
    private TeacherEntity recipientTeacher;
    private SupervisionScheduleEntity senderSchedule;
    private SupervisionScheduleEntity recipientSchedule;

    @BeforeEach
    void setUp() {
        senderTeacher = TeacherEntity.builder()
                .name("송혜정")
                .mail("song@test.com")
                .providerId("1")
                .oAuth2Type(OAuth2Type.GOOGLE)
                .build();
        ReflectionTestUtils.setField(senderTeacher, "id", 1L);

        recipientTeacher = TeacherEntity.builder()
                .name("김선생")
                .mail("kim@test.com")
                .providerId("2")
                .oAuth2Type(OAuth2Type.GOOGLE)
                .build();
        ReflectionTestUtils.setField(recipientTeacher, "id", 2L);

        senderSchedule = SupervisionScheduleEntity.builder()
                .teacher(senderTeacher)
                .day(LocalDate.of(2025, 3, 2))
                .period(SchoolPeriod.SEVEN_PERIOD)
                .type(SupervisionType.SELF_STUDY_SUPERVISION)
                .build();

        recipientSchedule = SupervisionScheduleEntity.builder()
                .teacher(recipientTeacher)
                .day(LocalDate.of(2025, 3, 3))
                .period(SchoolPeriod.EIGHT_AND_NINE_PERIOD)
                .type(SupervisionType.LEAVE_SEAT_SUPERVISION)
                .build();
    }

    @Test
    @DisplayName("감독 교체 엔티티가 PENDING 상태로 생성된다")
    void shouldCreateExchangeWithPendingStatus() {
        // Given
        String reason = "개인 사유로 교체 요청드립니다";

        // When
        SupervisionExchangeEntity exchange = SupervisionExchangeEntity.builder()
                .sender(senderTeacher)
                .recipient(recipientTeacher)
                .senderSchedule(senderSchedule)
                .recipientSchedule(recipientSchedule)
                .reason(reason)
                .build();

        // Then
        assertThat(exchange.getSender()).isEqualTo(senderTeacher);
        assertThat(exchange.getRecipient()).isEqualTo(recipientTeacher);
        assertThat(exchange.getSenderSchedule()).isEqualTo(senderSchedule);
        assertThat(exchange.getRecipientSchedule()).isEqualTo(recipientSchedule);
        assertThat(exchange.getReason()).isEqualTo(reason);
        assertThat(exchange.getState()).isEqualTo(SupervisionExchangeType.PENDING);
    }

    @Test
    @DisplayName("교체 요청을 수락할 수 있다")
    void shouldAcceptExchangeRequest() {
        // Given
        SupervisionExchangeEntity exchange = SupervisionExchangeEntity.builder()
                .sender(senderTeacher)
                .recipient(recipientTeacher)
                .senderSchedule(senderSchedule)
                .recipientSchedule(recipientSchedule)
                .reason("개인 사유")
                .build();

        // When
        exchange.accept();

        // Then
        assertThat(exchange.getState()).isEqualTo(SupervisionExchangeType.ACCEPTED);
    }

    @Test
    @DisplayName("교체 요청을 거절할 수 있다")
    void shouldRejectExchangeRequest() {
        // Given
        SupervisionExchangeEntity exchange = SupervisionExchangeEntity.builder()
                .sender(senderTeacher)
                .recipient(recipientTeacher)
                .senderSchedule(senderSchedule)
                .recipientSchedule(recipientSchedule)
                .reason("개인 사유")
                .build();

        // When
        exchange.reject();

        // Then
        assertThat(exchange.getState()).isEqualTo(SupervisionExchangeType.REJECTED);
    }

    private SupervisionExchangeEntity createExchange() {
        return SupervisionExchangeEntity.builder()
                .sender(senderTeacher)
                .recipient(recipientTeacher)
                .senderSchedule(senderSchedule)
                .recipientSchedule(recipientSchedule)
                .reason("개인 사유")
                .build();
    }

    @Test
    @DisplayName("생성 시 송신자/수신자 모두 확인하지 않은 상태다")
    void shouldBeUncheckedOnCreate() {
        SupervisionExchangeEntity exchange = createExchange();

        assertThat(exchange.isSenderChecked()).isFalse();
        assertThat(exchange.isRecipientChecked()).isFalse();
    }

    @Test
    @DisplayName("PENDING 상태의 교체 요청은 확인할 수 없다")
    void shouldNotCheckPendingExchange() {
        SupervisionExchangeEntity exchange = createExchange();

        assertThatThrownBy(() -> exchange.check(1L))
                .isInstanceOf(InvalidSupervisionExchangeException.class)
                .hasMessage("처리가 완료된 교체 요청만 확인할 수 있습니다.");
        assertThat(exchange.getState()).isEqualTo(SupervisionExchangeType.PENDING);
        assertThat(exchange.isSenderChecked()).isFalse();
        assertThat(exchange.isRecipientChecked()).isFalse();
    }

    @Test
    @DisplayName("송신자가 확인하면 송신자 플래그만 바뀌고 상태는 유지된다")
    void shouldCheckOnlySenderFlag() {
        SupervisionExchangeEntity exchange = createExchange();
        exchange.reject();

        exchange.check(1L);

        assertThat(exchange.isSenderChecked()).isTrue();
        assertThat(exchange.isRecipientChecked()).isFalse();
        assertThat(exchange.getState()).isEqualTo(SupervisionExchangeType.REJECTED);
    }

    @Test
    @DisplayName("수신자가 확인하면 수신자 플래그만 바뀌고 상태는 유지된다")
    void shouldCheckOnlyRecipientFlag() {
        SupervisionExchangeEntity exchange = createExchange();
        exchange.accept();

        exchange.check(2L);

        assertThat(exchange.isRecipientChecked()).isTrue();
        assertThat(exchange.isSenderChecked()).isFalse();
        assertThat(exchange.getState()).isEqualTo(SupervisionExchangeType.ACCEPTED);
    }

    @Test
    @DisplayName("같은 사람이 여러 번 확인해도 예외가 발생하지 않는다")
    void shouldAllowRepeatedCheck() {
        SupervisionExchangeEntity exchange = createExchange();
        exchange.accept();
        exchange.check(1L);

        assertThatCode(() -> exchange.check(1L)).doesNotThrowAnyException();
        assertThat(exchange.isSenderChecked()).isTrue();
        assertThat(exchange.isRecipientChecked()).isFalse();
    }

    @Test
    @DisplayName("당사자가 아닌 사람이 확인하면 아무 플래그도 바뀌지 않는다")
    void shouldNotChangeFlagsForThirdParty() {
        SupervisionExchangeEntity exchange = createExchange();
        exchange.accept();

        exchange.check(999L);

        assertThat(exchange.isSenderChecked()).isFalse();
        assertThat(exchange.isRecipientChecked()).isFalse();
    }

    @Test
    @DisplayName("이미 수락된 교체 요청은 다시 수락할 수 없다")
    void shouldNotAcceptTwice() {
        SupervisionExchangeEntity exchange = createExchange();
        exchange.accept();

        assertThatThrownBy(exchange::accept)
                .isInstanceOf(InvalidSupervisionExchangeException.class)
                .hasMessage("대기 중인 교체 요청만 수락할 수 있습니다.");
        assertThat(exchange.getState()).isEqualTo(SupervisionExchangeType.ACCEPTED);
    }

    @Test
    @DisplayName("이미 처리된 교체 요청은 다시 수락하거나 거절할 수 없다")
    void shouldNotAcceptOrRejectNonPendingExchange() {
        SupervisionExchangeEntity exchange = createExchange();
        exchange.reject();

        assertThatThrownBy(exchange::accept)
                .isInstanceOf(InvalidSupervisionExchangeException.class)
                .hasMessage("대기 중인 교체 요청만 수락할 수 있습니다.");
        assertThatThrownBy(exchange::reject)
                .isInstanceOf(InvalidSupervisionExchangeException.class)
                .hasMessage("대기 중인 교체 요청만 거절할 수 있습니다.");
        assertThat(exchange.getState()).isEqualTo(SupervisionExchangeType.REJECTED);
    }
}
