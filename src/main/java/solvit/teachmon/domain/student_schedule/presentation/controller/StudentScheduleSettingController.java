package solvit.teachmon.domain.student_schedule.presentation.controller;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import solvit.teachmon.domain.student_schedule.application.service.StudentScheduleSettingService;

import java.time.LocalDate;

@Validated
@RestController
@RequestMapping("/student-schedule/setting")
@RequiredArgsConstructor
public class StudentScheduleSettingController {
    private final StudentScheduleSettingService studentScheduleSettingService;

    @PostMapping("/weekly")
    public ResponseEntity<String> settingWeeklyStudentSchedule(
            @RequestParam("base_day") @NotNull(message = "주간 학생 스케줄 설정시 base_day(기준 날)는 필수입니다.") LocalDate baseDay
    ) {
        studentScheduleSettingService.createNewStudentSchedule(baseDay);
        studentScheduleSettingService.settingAllTypeSchedule(baseDay);

        return ResponseEntity
                .ok()
                .body("주간 학생 스케줄 설정이 완료되었습니다.");
    }

    // 2026-10-11 스케줄러 실패로 누락된 자습 스케줄 복구용 (복구 완료 후 제거)
    @PostMapping("/self-study/restore")
    public ResponseEntity<String> restoreSelfStudySchedule(
            @RequestParam("from_day") @NotNull(message = "자습 스케줄 복구시 from_day(복구 시작 날)는 필수입니다.") LocalDate fromDay
    ) {
        int restoredCount = studentScheduleSettingService.restoreSelfStudySchedule(fromDay);

        return ResponseEntity
                .ok()
                .body("자습 스케줄 " + restoredCount + "건을 복구했습니다.");
    }
}
