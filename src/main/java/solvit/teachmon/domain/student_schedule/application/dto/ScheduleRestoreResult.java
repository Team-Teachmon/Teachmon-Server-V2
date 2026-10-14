package solvit.teachmon.domain.student_schedule.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ScheduleRestoreResult(
        @JsonProperty("restored_count")
        int restoredCount,
        @JsonProperty("skipped_student_schedule_ids")
        List<Long> skippedStudentScheduleIds
) {}
