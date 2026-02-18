package solvit.teachmon.domain.student_schedule.domain.entity.schedules;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import solvit.teachmon.domain.after_school.domain.entity.AfterSchoolEntity;
import solvit.teachmon.domain.student_schedule.domain.entity.ScheduleEntity;
import solvit.teachmon.domain.student_schedule.domain.enums.ScheduleType;
import solvit.teachmon.domain.student_schedule.exception.AfterSchoolScheduleValueInvalidException;
import solvit.teachmon.global.entity.BaseEntity;

@Getter
@Entity
@Table(name = "after_school_schedule")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AfterSchoolScheduleEntity extends BaseEntity {
    @OneToOne(cascade = CascadeType.REMOVE, orphanRemoval = true)
    @MapsId
    @JoinColumn(name = "id")
    private ScheduleEntity schedule;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "after_school_id")
    private AfterSchoolEntity afterSchool;

    @Builder
    public AfterSchoolScheduleEntity(ScheduleEntity schedule, AfterSchoolEntity afterSchool) {
        validateSchedule(schedule);
        validateAfterSchool(afterSchool);

        this.schedule = schedule;
        this.afterSchool = afterSchool;
    }

    private void validateSchedule(ScheduleEntity schedule) {
        if (schedule == null) {
            throw new AfterSchoolScheduleValueInvalidException("schedule(스케줄)은 필수입니다.");
        }
        if (!ScheduleType.AFTER_SCHOOL.equals(schedule.getType())
                && !ScheduleType.AFTER_SCHOOL_REINFORCEMENT.equals(schedule.getType())) {
            throw new AfterSchoolScheduleValueInvalidException("schedule(스케줄)의 타입은 방과후 또는 방과후 보강이어야 합니다.");
        }
    }

    private void validateAfterSchool(AfterSchoolEntity afterSchool) {
        if (afterSchool == null) {
            throw new AfterSchoolScheduleValueInvalidException("afterSchool(방과후)는 필수입니다.");
        }
    }
}
