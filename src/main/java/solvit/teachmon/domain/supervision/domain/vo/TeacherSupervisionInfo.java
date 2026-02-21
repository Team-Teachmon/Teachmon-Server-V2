package solvit.teachmon.domain.supervision.domain.vo;

import lombok.Builder;
import solvit.teachmon.domain.supervision.domain.enums.SupervisionType;
import solvit.teachmon.global.enums.WeekDay;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

/**
 * 교사별 감독 정보를 담는 값 객체 (순수 데이터)
 */
@Builder(toBuilder = true)
public record TeacherSupervisionInfo(
        Long teacherId,
        String teacherName,
        Set<WeekDay> banDays,                           // 금지 요일들
        LocalDate lastSupervisionDate,                  // 최근 감독 날짜
        int totalSupervisionCount,                      // 총 감독 횟수
        int sevenPeriodCount,                          // 7교시 감독 횟수
        int eightElevenPeriodCount,                    // 8~11교시 감독 횟수
        Map<SupervisionType, Integer> supervisionCounts // 감독 타입별 횟수
) {
}