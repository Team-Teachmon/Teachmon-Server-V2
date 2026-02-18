package solvit.teachmon.domain.student_schedule.domain.repository;

import java.util.List;

public interface ScheduleQueryDslRepository {
    void deleteTopSchedulesByStudentScheduleIds(List<Long> studentScheduleIds);
}