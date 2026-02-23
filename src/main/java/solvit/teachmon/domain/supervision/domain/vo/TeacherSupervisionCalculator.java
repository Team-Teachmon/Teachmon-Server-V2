package solvit.teachmon.domain.supervision.domain.vo;

import solvit.teachmon.domain.supervision.domain.enums.SupervisionType;
import solvit.teachmon.global.enums.WeekDay;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

/**
 * 교사 감독 정보 관련 계산을 담당하는 값 객체
 */
public class TeacherSupervisionCalculator {
    
    private final TeacherSupervisionInfo teacherInfo;
    
    public TeacherSupervisionCalculator(TeacherSupervisionInfo teacherInfo) {
        this.teacherInfo = teacherInfo;
    }
    
    /**
     * 해당 요일이 금지요일인지 확인
     */
    public boolean isBanDay(DayOfWeek dayOfWeek) {
        WeekDay weekDay = WeekDay.fromDayOfWeek(dayOfWeek);
        return teacherInfo.banDays().contains(weekDay);
    }
    
    /**
     * 최근 감독일로부터 경과일 계산
     * 감독한 적이 없으면 365일 (1년, 최고 우선순위)
     */
    public long getDaysSinceLastSupervision(LocalDate targetDate) {
        if (teacherInfo.lastSupervisionDate() == null) {
            return 365L; // 1년에 해당하는 일수로 충분히 높은 우선순위 보장
        }
        return Math.max(0, ChronoUnit.DAYS.between(teacherInfo.lastSupervisionDate(), targetDate));
    }
    
    /**
     * 감독 배정 후 교사 정보 업데이트
     */
    public TeacherSupervisionInfo withUpdatedSupervision(LocalDate newDate, SupervisionType type) {
        Map<SupervisionType, Integer> updatedCounts = new HashMap<>(teacherInfo.supervisionCounts());
        updatedCounts.put(type, updatedCounts.getOrDefault(type, 0) + 1);
        
        return teacherInfo.toBuilder()
                .lastSupervisionDate(newDate)
                .totalSupervisionCount(teacherInfo.totalSupervisionCount() + 1)
                .supervisionCounts(updatedCounts)
                .build();
    }
}