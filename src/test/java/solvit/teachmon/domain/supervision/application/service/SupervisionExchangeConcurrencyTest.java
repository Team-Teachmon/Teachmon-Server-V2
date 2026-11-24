package solvit.teachmon.domain.supervision.application.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;
import solvit.teachmon.domain.supervision.domain.entity.SupervisionExchangeEntity;
import solvit.teachmon.domain.supervision.domain.entity.SupervisionScheduleEntity;
import solvit.teachmon.domain.supervision.domain.enums.SupervisionExchangeType;
import solvit.teachmon.domain.supervision.domain.enums.SupervisionType;
import solvit.teachmon.domain.supervision.domain.repository.SupervisionExchangeRepository;
import solvit.teachmon.domain.supervision.domain.repository.SupervisionScheduleRepository;
import solvit.teachmon.domain.supervision.exception.InvalidSupervisionExchangeException;
import solvit.teachmon.domain.supervision.presentation.dto.request.SupervisionExchangeAcceptRequestDto;
import solvit.teachmon.domain.user.domain.entity.TeacherEntity;
import solvit.teachmon.domain.user.domain.enums.OAuth2Type;
import solvit.teachmon.domain.user.domain.repository.TeacherRepository;
import solvit.teachmon.global.enums.SchoolPeriod;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 11-19 운영 이슈 재현: 같은 교체 요청에 수락 요청이 거의 동시에 2번 들어오면
 * 담당자 교체가 두 번 실행되어 원상복구되던 문제.
 * 트랜잭션을 실제로 커밋해야 하므로 @Transactional 없이 실행하고 직접 정리한다.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("감독 교체 동시 수락 테스트")
class SupervisionExchangeConcurrencyTest {

    @Autowired
    private SupervisionExchangeService supervisionExchangeService;

    @Autowired
    private SupervisionExchangeRepository supervisionExchangeRepository;

    @Autowired
    private SupervisionScheduleRepository supervisionScheduleRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private Long senderId;
    private Long recipientId;
    private Long senderScheduleId;
    private Long recipientScheduleId;
    private Long exchangeId;

    @BeforeEach
    void setUp() {
        transactionTemplate.executeWithoutResult(status -> {
            TeacherEntity sender = teacherRepository.save(TeacherEntity.builder()
                    .name("송신교사").mail("concurrency-sender@test.com").providerId("c1").oAuth2Type(OAuth2Type.GOOGLE).build());
            TeacherEntity recipient = teacherRepository.save(TeacherEntity.builder()
                    .name("수신교사").mail("concurrency-recipient@test.com").providerId("c2").oAuth2Type(OAuth2Type.GOOGLE).build());
            SupervisionScheduleEntity senderSchedule = supervisionScheduleRepository.save(SupervisionScheduleEntity.builder()
                    .teacher(sender).day(LocalDate.of(2026, 11, 19)).period(SchoolPeriod.EIGHT_AND_NINE_PERIOD)
                    .type(SupervisionType.LEAVE_SEAT_SUPERVISION).build());
            SupervisionScheduleEntity recipientSchedule = supervisionScheduleRepository.save(SupervisionScheduleEntity.builder()
                    .teacher(recipient).day(LocalDate.of(2026, 11, 20)).period(SchoolPeriod.EIGHT_AND_NINE_PERIOD)
                    .type(SupervisionType.LEAVE_SEAT_SUPERVISION).build());
            SupervisionExchangeEntity exchange = supervisionExchangeRepository.save(SupervisionExchangeEntity.builder()
                    .sender(sender).recipient(recipient)
                    .senderSchedule(senderSchedule).recipientSchedule(recipientSchedule)
                    .reason("개인 사유").build());

            senderId = sender.getId();
            recipientId = recipient.getId();
            senderScheduleId = senderSchedule.getId();
            recipientScheduleId = recipientSchedule.getId();
            exchangeId = exchange.getId();
        });
    }

    @AfterEach
    void tearDown() {
        transactionTemplate.executeWithoutResult(status -> {
            supervisionExchangeRepository.deleteById(exchangeId);
            supervisionScheduleRepository.deleteById(senderScheduleId);
            supervisionScheduleRepository.deleteById(recipientScheduleId);
            teacherRepository.deleteById(senderId);
            teacherRepository.deleteById(recipientId);
        });
    }

    @Test
    @DisplayName("같은 요청을 동시에 두 번 수락해도 담당자 교체는 한 번만 실행된다")
    void shouldSwapTeachersOnlyOnceWhenAcceptedConcurrently() throws Exception {
        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);

        Callable<Boolean> accept = () -> {
            ready.countDown();
            start.await();
            try {
                supervisionExchangeService.acceptSupervisionExchange(
                        new SupervisionExchangeAcceptRequestDto(exchangeId), recipientId);
                return true;
            } catch (InvalidSupervisionExchangeException e) {
                return false;
            }
        };

        List<Future<Boolean>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(executor.submit(accept));
        }
        ready.await();
        start.countDown();

        int success = 0;
        for (Future<Boolean> future : futures) {
            if (future.get(30, TimeUnit.SECONDS)) {
                success++;
            }
        }
        executor.shutdown();

        assertThat(success).isEqualTo(1);
        transactionTemplate.executeWithoutResult(status -> {
            assertThat(supervisionExchangeRepository.findById(exchangeId).orElseThrow().getState())
                    .isEqualTo(SupervisionExchangeType.ACCEPTED);
            assertThat(supervisionScheduleRepository.findById(senderScheduleId).orElseThrow().getTeacher().getId())
                    .isEqualTo(recipientId);
            assertThat(supervisionScheduleRepository.findById(recipientScheduleId).orElseThrow().getTeacher().getId())
                    .isEqualTo(senderId);
        });
    }
}
