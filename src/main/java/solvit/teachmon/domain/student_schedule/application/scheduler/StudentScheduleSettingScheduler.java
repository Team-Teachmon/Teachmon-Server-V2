package solvit.teachmon.domain.student_schedule.application.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import solvit.teachmon.domain.student_schedule.application.service.StudentScheduleSettingService;
import solvit.teachmon.global.infra.discord.DiscordAlertService;

import java.time.DayOfWeek;
import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class StudentScheduleSettingScheduler {
    private final StudentScheduleSettingService studentScheduleSettingService;
    private final DiscordAlertService discordAlertService;

    @Scheduled(
            cron = "0 0 0 * * SUN",
            zone = "Asia/Seoul"
    )
    public void settingStudentSchedule() {
        LocalDate baseDate = LocalDate.now().with(DayOfWeek.MONDAY).plusWeeks(1);
        try {
            studentScheduleSettingService.createNewStudentSchedule(baseDate);
            studentScheduleSettingService.settingAllTypeSchedule(baseDate);
        } catch (Exception e) {
            // @Scheduled는 예외를 로그만 남기고 삼키므로, 실패를 놓치지 않도록 알림을 보낸다
            log.error("주간 학생 스케줄 설정 실패 - baseDate: {}", baseDate, e);
            alertFailure(baseDate, e);
        }
    }

    private void alertFailure(LocalDate baseDate, Exception e) {
        try {
            discordAlertService.alertBatchFailure("주간 학생 스케줄 설정", baseDate, e);
        } catch (Exception alertException) {
            log.error("배치 실패 알림 전송 실패", alertException);
        }
    }
}
