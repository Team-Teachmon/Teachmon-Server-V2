package solvit.teachmon.domain.supervision.domain.vo;

import java.time.LocalDate;

/**
 * 교사 감독 정보 조회 값 객체
 */
public record TeacherSupervisionInfoVo(
        Long teacherId,
        String teacherName,
        LocalDate lastSupervisionDate,
        Long totalSupervisionCount,
        Long sevenPeriodCount,
        Long eightElevenPeriodCount
) {
}