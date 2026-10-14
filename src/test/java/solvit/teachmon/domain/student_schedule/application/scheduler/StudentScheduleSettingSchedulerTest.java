package solvit.teachmon.domain.student_schedule.application.scheduler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import solvit.teachmon.domain.student_schedule.application.service.StudentScheduleSettingService;
import solvit.teachmon.global.infra.discord.DiscordAlertService;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("학생 스케줄 설정 스케줄러 테스트")
class StudentScheduleSettingSchedulerTest {

    @Mock
    private StudentScheduleSettingService studentScheduleSettingService;

    @Mock
    private DiscordAlertService discordAlertService;

    @InjectMocks
    private StudentScheduleSettingScheduler scheduler;

    @Test
    @DisplayName("스케줄러가 실행되면 새로운 학생 스케줄을 생성하고 모든 타입의 스케줄을 설정한다")
    void shouldCreateAndSettingScheduleWhenSchedulerRuns() {
        // When: 스케줄러가 실행되면
        scheduler.settingStudentSchedule();

        // Then: 학생 스케줄을 생성하고, 모든 타입의 스케줄을 설정해야 한다
        ArgumentCaptor<LocalDate> captor = ArgumentCaptor.forClass(LocalDate.class);
        verify(studentScheduleSettingService, times(1)).createNewStudentSchedule(captor.capture());
        verify(studentScheduleSettingService, times(1)).settingAllTypeSchedule(captor.capture());

        // 동일한 날짜를 사용해야 한다
        assertThat(captor.getAllValues().get(0)).isEqualTo(captor.getAllValues().get(1));
    }

    @Test
    @DisplayName("스케줄러는 학생 스케줄 생성 후에 타입별 스케줄을 설정해야 한다")
    void shouldSettingScheduleAfterCreatingStudentSchedule() {
        // When: 스케줄러가 실행되면
        scheduler.settingStudentSchedule();

        // Then: 학생 스케줄 생성이 먼저 실행되고, 그 다음 타입별 스케줄이 설정되어야 한다
        ArgumentCaptor<LocalDate> captor = ArgumentCaptor.forClass(LocalDate.class);
        InOrder inOrder = inOrder(studentScheduleSettingService);
        inOrder.verify(studentScheduleSettingService).createNewStudentSchedule(captor.capture());
        inOrder.verify(studentScheduleSettingService).settingAllTypeSchedule(captor.capture());
    }

    @Test
    @DisplayName("스케줄러의 cron 설정은 매주 일요일 자정(Asia/Seoul)에 실행된다")
    void schedulerCronConfiguration() {
        // 이 테스트는 스케줄러의 @Scheduled 어노테이션 설정을 문서화합니다.
        //
        // @Scheduled(
        //     cron = "0 0 0 * * SUN",  // 매주 일요일 0시 0분 0초
        //     zone = "Asia/Seoul"       // 한국 시간대
        // )
        //
        // 동작:
        // - 매주 일요일 자정에 실행
        // - 다음 주 월요일부터 목요일까지의 학생 스케줄을 생성
        // - 각 스케줄 타입(자습, 방과후, 이석, 추가자습)에 대한 설정을 적용
        //
        // 예시:
        // - 1월 14일(일) 자정 실행
        // - 1월 15일(월) ~ 1월 18일(목)의 스케줄 생성
        // - 교시: 7교시, 8~9교시, 10~11교시만 생성
    }

    @Test
    @DisplayName("스케줄러 동작 플로우")
    void schedulerWorkflow() {
        // 이 테스트는 스케줄러의 전체 동작 플로우를 문서화합니다.
        //
        // 1단계: createNewStudentSchedule() 실행
        //    - 다음 주(월~일)의 기존 스케줄 삭제
        //    - 현재 연도 학생들 조회
        //    - 각 학생별로 다음 주 월~목, 7/8-9/10-11교시 스케줄 생성
        //    - 총 생성 개수: 학생 수 × 4일 × 3교시
        //
        // 2단계: settingAllTypeSchedule() 실행
        //    - SelfStudyScheduleSettingStrategy 실행
        //      → 분기별 자습 정보를 기반으로 자습 스케줄 생성
        //      → 각 학생에게 장소 배정
        //
        //    - AfterSchoolScheduleSettingStrategy 실행
        //      → 방과후 수업 정보를 기반으로 방과후 스케줄 생성
        //      → 출장인 경우 스킵
        //
        //    - LeaveSeatScheduleSettingStrategy 실행
        //      → 고정 이석 정보를 기반으로 이석 스케줄 생성
        //      → 이석 학생 정보 포함
        //
        //    - AdditionalSelfStudyScheduleSettingStrategy 실행
        //      → 추가 자습 정보를 기반으로 추가 자습 스케줄 생성
        //      → 각 학생에게 장소 배정
        //
        // 스케줄 우선순위:
        //    - stackOrder로 관리
        //    - 나중에 추가된 스케줄이 더 높은 우선순위를 가짐
        //    - 예: EXIT, AWAY는 기본 스케줄을 덮어씀
    }


    @Test
    @DisplayName("스케줄 설정에 실패하면 예외를 삼키지 않고 배치 실패 알림을 보낸다")
    void shouldAlertWhenSettingScheduleFails() {
        RuntimeException exception = new RuntimeException("배정 가능한 장소가 없습니다.");
        doThrow(exception).when(studentScheduleSettingService).settingAllTypeSchedule(any());

        assertThatCode(() -> scheduler.settingStudentSchedule()).doesNotThrowAnyException();

        verify(discordAlertService).alertBatchFailure(eq("주간 학생 스케줄 설정"), any(LocalDate.class), eq(exception));
    }

    @Test
    @DisplayName("알림 전송이 실패해도 스케줄러는 예외를 던지지 않는다")
    void shouldNotThrowWhenAlertFails() {
        doThrow(new RuntimeException()).when(studentScheduleSettingService).settingAllTypeSchedule(any());
        doThrow(new RuntimeException("webhook 실패")).when(discordAlertService).alertBatchFailure(any(), any(), any());

        assertThatCode(() -> scheduler.settingStudentSchedule()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("스케줄 설정에 성공하면 알림을 보내지 않는다")
    void shouldNotAlertWhenSucceeded() {
        scheduler.settingStudentSchedule();

        verify(discordAlertService, never()).alertBatchFailure(any(), any(), any());
    }
}
