package solvit.teachmon.domain.supervision.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import solvit.teachmon.domain.supervision.domain.enums.SupervisionType;
import solvit.teachmon.domain.supervision.exception.InvalidSupervisionScheduleException;
import solvit.teachmon.domain.user.domain.entity.TeacherEntity;
import solvit.teachmon.global.entity.BaseEntity;
import solvit.teachmon.global.enums.SchoolPeriod;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "supervision_schedule")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SupervisionScheduleEntity extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_id")
    private TeacherEntity teacher;

    @Column(name = "`day`", nullable = false)
    private LocalDate day;

    @Enumerated(EnumType.STRING)
    @Column(name = "period", nullable = false)
    private SchoolPeriod period;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private SupervisionType type;

    @OneToMany(mappedBy = "senderSchedule", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SupervisionExchangeEntity> senderExchanges = new ArrayList<>();

    @OneToMany(mappedBy = "recipientSchedule", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SupervisionExchangeEntity> recipientExchanges = new ArrayList<>();

    @Builder
    public SupervisionScheduleEntity(TeacherEntity teacher, LocalDate day, SchoolPeriod period, SupervisionType type) {
        validateTeacher(teacher);
        validateDay(day);
        validatePeriod(period);
        validateType(type);
        
        this.teacher = teacher;
        this.day = day;
        this.period = period;
        this.type = type;
    }
    
    private void validateTeacher(TeacherEntity teacher) {
        if (teacher == null) {
            throw new InvalidSupervisionScheduleException("감독 교사는 필수입니다.");
        }
    }
    
    private void validateDay(LocalDate day) {
        if (day == null) {
            throw new InvalidSupervisionScheduleException("감독 날짜는 필수입니다.");
        }
    }
    
    private void validatePeriod(SchoolPeriod period) {
        if (period == null) {
            throw new InvalidSupervisionScheduleException("감독 교시는 필수입니다.");
        }
    }
    
    private void validateType(SupervisionType type) {
        if (type == null) {
            throw new InvalidSupervisionScheduleException("감독 타입은 필수입니다.");
        }
    }
}
