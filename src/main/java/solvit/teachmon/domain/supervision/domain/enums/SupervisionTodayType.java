package solvit.teachmon.domain.supervision.domain.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SupervisionTodayType {
    NONE("NONE"),
    SELF_STUDY("SELF_STUDY"),
    LEAVE_SEAT("LEAVE_SEAT"),
    SEVENTH_PERIOD("SEVENTH_PERIOD"),
    ALL("ALL");

    private final String value;

    @JsonValue
    public String getValue() {
        return value;
    }

    public static SupervisionTodayType from(boolean hasSelfStudy, boolean hasLeaveSeat) {
        if (hasSelfStudy && hasLeaveSeat) {
            return ALL;
        } else if (hasSelfStudy) {
            return SELF_STUDY;
        } else if (hasLeaveSeat) {
            return LEAVE_SEAT;
        } else {
            return NONE;
        }
    }

    public static SupervisionTodayType from(boolean hasSelfStudy, boolean hasLeaveSeat, boolean hasSeventhPeriod) {
        int supervisionCount = 0;
        SupervisionTodayType result = NONE;
        
        if (hasSelfStudy) {
            supervisionCount++;
            result = SELF_STUDY;
        }
        if (hasLeaveSeat) {
            supervisionCount++;
            result = LEAVE_SEAT;
        }
        if (hasSeventhPeriod) {
            supervisionCount++;
            result = SEVENTH_PERIOD;
        }
        
        if (supervisionCount > 1) {
            return ALL;
        } else {
            return result;
        }
    }
}