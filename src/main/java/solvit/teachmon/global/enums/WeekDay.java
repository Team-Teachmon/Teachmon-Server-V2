package solvit.teachmon.global.enums;

import java.time.DayOfWeek;
import java.time.LocalDate;

public enum WeekDay {
    MON,
    TUE,
    WED,
    THU,
    FRI;

    public String toKorean() {
        return switch (this) {
            case MON -> "월";
            case TUE -> "화";
            case WED -> "수";
            case THU -> "목";
            case FRI -> "금";
        };
    }

    public String toKoreanFull() {
        return switch (this) {
            case MON -> "월요일";
            case TUE -> "화요일";
            case WED -> "수요일";
            case THU -> "목요일";
            case FRI -> "금요일";
        };
    }

    public static WeekDay fromKorean(String value) {
        return switch (value) {
            case "월요일", "월" -> MON;
            case "화요일", "화" -> TUE;
            case "수요일", "수" -> WED;
            case "목요일", "목" -> THU;
            case "금요일", "금" -> FRI;
            default -> throw new IllegalArgumentException("Invalid WeekDay: " + value);
        };
    }

    public static String convertWeekDayToKorean(WeekDay weekDay) {
        return weekDay.toKoreanFull();
    }
    public static WeekDay fromLocalDate(LocalDate date) {
        return switch (date.getDayOfWeek()) {
            case MONDAY -> MON;
            case TUESDAY -> TUE;
            case WEDNESDAY -> WED;
            case THURSDAY -> THU;
            case FRIDAY -> FRI;
            default -> throw new IllegalArgumentException("서비스에서 지원하는 요일이 아닙니다.");
        };
    }

    public static WeekDay fromDayOfWeek(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> MON;
            case TUESDAY -> TUE;
            case WEDNESDAY -> WED;
            case THURSDAY -> THU;
            case FRIDAY -> FRI;
            default -> throw new IllegalArgumentException("서비스에서 지원하는 요일이 아닙니다.");
        };
    }

    public DayOfWeek toDayOfWeek() {
        return switch (this) {
            case MON -> DayOfWeek.MONDAY;
            case TUE -> DayOfWeek.TUESDAY;
            case WED -> DayOfWeek.WEDNESDAY;
            case THU -> DayOfWeek.THURSDAY;
            case FRI -> DayOfWeek.FRIDAY;
        };
    }
}
