package solvit.teachmon.domain.supervision.domain.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;
import solvit.teachmon.domain.supervision.domain.entity.SupervisionExchangeEntity;
import solvit.teachmon.domain.supervision.domain.entity.SupervisionScheduleEntity;
import solvit.teachmon.domain.supervision.domain.enums.SupervisionExchangeType;
import solvit.teachmon.domain.supervision.domain.enums.SupervisionType;
import solvit.teachmon.domain.user.domain.entity.TeacherEntity;
import solvit.teachmon.domain.user.domain.enums.OAuth2Type;
import solvit.teachmon.domain.user.domain.repository.TeacherRepository;
import solvit.teachmon.global.enums.SchoolPeriod;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("감독 교체 저장소 테스트")
class SupervisionExchangeRepositoryTest {

    @Autowired
    private SupervisionExchangeRepository supervisionExchangeRepository;

    @Autowired
    private SupervisionScheduleRepository supervisionScheduleRepository;

    @Autowired
    private TeacherRepository teacherRepository;

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
        teacherRepository.save(senderTeacher);

        recipientTeacher = TeacherEntity.builder()
                .name("김선생")
                .mail("kim@test.com")
                .providerId("2")
                .oAuth2Type(OAuth2Type.GOOGLE)
                .build();
        teacherRepository.save(recipientTeacher);

        senderSchedule = SupervisionScheduleEntity.builder()
                .teacher(senderTeacher)
                .day(LocalDate.of(2025, 3, 2))
                .period(SchoolPeriod.SEVEN_PERIOD)
                .type(SupervisionType.SELF_STUDY_SUPERVISION)
                .build();
        supervisionScheduleRepository.save(senderSchedule);

        recipientSchedule = SupervisionScheduleEntity.builder()
                .teacher(recipientTeacher)
                .day(LocalDate.of(2025, 3, 3))
                .period(SchoolPeriod.EIGHT_AND_NINE_PERIOD)
                .type(SupervisionType.LEAVE_SEAT_SUPERVISION)
                .build();
        supervisionScheduleRepository.save(recipientSchedule);
    }

    @Test
    @DisplayName("감독 교체 요청을 저장할 수 있다")
    void shouldSaveSupervisionExchange() {
        // Given
        SupervisionExchangeEntity exchange = SupervisionExchangeEntity.builder()
                .sender(senderTeacher)
                .recipient(recipientTeacher)
                .senderSchedule(senderSchedule)
                .recipientSchedule(recipientSchedule)
                .reason("개인 사유로 교체 요청드립니다")
                .build();

        // When
        SupervisionExchangeEntity savedExchange = supervisionExchangeRepository.save(exchange);

        // Then
        assertThat(savedExchange.getId()).isNotNull();
        assertThat(savedExchange.getSender().getName()).isEqualTo("송혜정");
        assertThat(savedExchange.getRecipient().getName()).isEqualTo("김선생");
        assertThat(savedExchange.getReason()).isEqualTo("개인 사유로 교체 요청드립니다");
        assertThat(savedExchange.getState()).isEqualTo(SupervisionExchangeType.PENDING);
    }

    @Test
    @DisplayName("ID로 감독 교체 요청을 찾을 수 있다")
    void shouldFindSupervisionExchangeById() {
        // Given
        SupervisionExchangeEntity exchange = SupervisionExchangeEntity.builder()
                .sender(senderTeacher)
                .recipient(recipientTeacher)
                .senderSchedule(senderSchedule)
                .recipientSchedule(recipientSchedule)
                .reason("개인 사유")
                .build();
        SupervisionExchangeEntity savedExchange = supervisionExchangeRepository.save(exchange);

        // When
        Optional<SupervisionExchangeEntity> result = supervisionExchangeRepository.findById(savedExchange.getId());

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getSender().getName()).isEqualTo("송혜정");
        assertThat(result.get().getRecipient().getName()).isEqualTo("김선생");
        assertThat(result.get().getReason()).isEqualTo("개인 사유");
    }

    @Test
    @DisplayName("존재하지 않는 ID로 찾으면 빈 결과가 반환된다")
    void shouldReturnEmptyWhenExchangeNotExists() {
        // Given
        Long nonExistentId = 999L;

        // When
        Optional<SupervisionExchangeEntity> result = supervisionExchangeRepository.findById(nonExistentId);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("모든 감독 교체 요청을 조회할 수 있다")
    void shouldFindAllSupervisionExchanges() {
        // Given
        SupervisionExchangeEntity exchange1 = SupervisionExchangeEntity.builder()
                .sender(senderTeacher)
                .recipient(recipientTeacher)
                .senderSchedule(senderSchedule)
                .recipientSchedule(recipientSchedule)
                .reason("첫 번째 요청")
                .build();

        SupervisionExchangeEntity exchange2 = SupervisionExchangeEntity.builder()
                .sender(recipientTeacher)
                .recipient(senderTeacher)
                .senderSchedule(recipientSchedule)
                .recipientSchedule(senderSchedule)
                .reason("두 번째 요청")
                .build();

        supervisionExchangeRepository.save(exchange1);
        supervisionExchangeRepository.save(exchange2);

        // When
        List<SupervisionExchangeEntity> result = supervisionExchangeRepository.findAll();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting("reason")
                .containsExactlyInAnyOrder("첫 번째 요청", "두 번째 요청");
    }

    @Test
    @DisplayName("감독 교체 요청의 상태를 변경할 수 있다")
    void shouldUpdateSupervisionExchangeState() {
        // Given
        SupervisionExchangeEntity exchange = SupervisionExchangeEntity.builder()
                .sender(senderTeacher)
                .recipient(recipientTeacher)
                .senderSchedule(senderSchedule)
                .recipientSchedule(recipientSchedule)
                .reason("개인 사유")
                .build();
        SupervisionExchangeEntity savedExchange = supervisionExchangeRepository.save(exchange);

        // When
        savedExchange.accept();
        supervisionExchangeRepository.save(savedExchange);

        // Then
        Optional<SupervisionExchangeEntity> result = supervisionExchangeRepository.findById(savedExchange.getId());
        assertThat(result).isPresent();
        assertThat(result.get().getState()).isEqualTo(SupervisionExchangeType.ACCEPTED);
    }

    @Autowired
    private EntityManager entityManager;

    private SupervisionExchangeEntity saveExchange(String reason) {
        return supervisionExchangeRepository.save(SupervisionExchangeEntity.builder()
                .sender(senderTeacher)
                .recipient(recipientTeacher)
                .senderSchedule(senderSchedule)
                .recipientSchedule(recipientSchedule)
                .reason(reason)
                .build());
    }

    private List<String> uncheckedReasons(Long teacherId) {
        entityManager.flush();
        entityManager.clear();
        return supervisionExchangeRepository.findUncheckedByTeacherId(teacherId).stream()
                .map(SupervisionExchangeEntity::getReason)
                .toList();
    }

    @Test
    @DisplayName("송신자가 확인한 요청은 송신자 목록에서만 빠지고 수신자 목록에는 남는다")
    void shouldHideFromSenderOnlyWhenSenderChecked() {
        // Given
        SupervisionExchangeEntity exchange = saveExchange("거절된 요청");
        exchange.reject();
        exchange.check(senderTeacher.getId());

        // When & Then
        assertThat(uncheckedReasons(senderTeacher.getId())).isEmpty();
        assertThat(uncheckedReasons(recipientTeacher.getId())).containsExactly("거절된 요청");
    }

    @Test
    @DisplayName("수신자가 확인한 요청은 수신자 목록에서만 빠지고 송신자 목록에는 남는다")
    void shouldHideFromRecipientOnlyWhenRecipientChecked() {
        // Given: 11-19 운영 케이스 - 거절 후 수신자가 확인
        SupervisionExchangeEntity exchange = saveExchange("거절된 요청");
        exchange.reject();
        exchange.check(recipientTeacher.getId());

        // When & Then
        assertThat(uncheckedReasons(recipientTeacher.getId())).isEmpty();
        assertThat(uncheckedReasons(senderTeacher.getId())).containsExactly("거절된 요청");
    }

    @Test
    @DisplayName("양쪽 모두 확인한 요청은 어느 목록에도 나오지 않는다")
    void shouldHideFromBothWhenBothChecked() {
        // Given
        SupervisionExchangeEntity checked = saveExchange("양쪽 확인");
        checked.accept();
        checked.check(senderTeacher.getId());
        checked.check(recipientTeacher.getId());
        saveExchange("대기 중");

        // When & Then
        assertThat(uncheckedReasons(senderTeacher.getId())).containsExactly("대기 중");
        assertThat(uncheckedReasons(recipientTeacher.getId())).containsExactly("대기 중");
    }

    @Test
    @DisplayName("당사자가 아닌 교사의 목록에는 나오지 않는다")
    void shouldNotReturnOtherTeachersExchanges() {
        // Given
        saveExchange("다른 사람 요청");
        TeacherEntity other = teacherRepository.save(TeacherEntity.builder()
                .name("이선생").mail("lee@test.com").providerId("3").oAuth2Type(OAuth2Type.GOOGLE).build());

        // When & Then
        assertThat(uncheckedReasons(other.getId())).isEmpty();
    }

    @Test
    @DisplayName("확인 플래그는 저장 후 다시 조회해도 유지된다")
    void shouldPersistCheckFlags() {
        // Given
        SupervisionExchangeEntity exchange = saveExchange("개인 사유");
        exchange.accept();
        exchange.check(senderTeacher.getId());
        entityManager.flush();
        entityManager.clear();

        // When
        SupervisionExchangeEntity result = supervisionExchangeRepository.findByIdForUpdate(exchange.getId()).orElseThrow();

        // Then
        assertThat(result.isSenderChecked()).isTrue();
        assertThat(result.isRecipientChecked()).isFalse();
        assertThat(result.getState()).isEqualTo(SupervisionExchangeType.ACCEPTED);
    }

    @Test
    @DisplayName("이전 방식으로 CHECKED 처리된 데이터는 마이그레이션 전에도 목록에 나오지 않는다")
    void shouldHideLegacyCheckedExchanges() {
        // Given
        SupervisionExchangeEntity legacy = saveExchange("이전 확인 건");
        ReflectionTestUtils.setField(legacy, "state", SupervisionExchangeType.CHECKED);

        // When & Then
        assertThat(uncheckedReasons(senderTeacher.getId())).isEmpty();
        assertThat(uncheckedReasons(recipientTeacher.getId())).isEmpty();
    }
}
