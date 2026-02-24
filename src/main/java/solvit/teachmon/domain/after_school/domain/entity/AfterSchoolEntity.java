package solvit.teachmon.domain.after_school.domain.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import solvit.teachmon.domain.after_school.exception.InvalidAfterSchoolInfoException;
import solvit.teachmon.domain.branch.domain.entity.BranchEntity;
import solvit.teachmon.domain.place.domain.entity.PlaceEntity;
import solvit.teachmon.domain.student_schedule.domain.entity.schedules.AfterSchoolScheduleEntity;
import solvit.teachmon.domain.user.domain.entity.TeacherEntity;
import solvit.teachmon.global.entity.BaseEntity;
import solvit.teachmon.global.enums.SchoolPeriod;
import solvit.teachmon.global.enums.WeekDay;

import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "after_school")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AfterSchoolEntity extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_id")
    private TeacherEntity teacher;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id")
    private BranchEntity branch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "place_id")
    private PlaceEntity place;

    @Enumerated(EnumType.STRING)
    @Column(name = "week_day", nullable = false)
    private WeekDay weekDay;

    @Enumerated(EnumType.STRING)
    @Column(name = "period", nullable = false)
    private SchoolPeriod period;

    @Column(name = "`year`", nullable = false)
    private Integer year;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "grade", nullable = false)
    private Integer grade;

    @Column(name = "is_end", nullable = false)
    private Boolean isEnd;

    @OneToMany(mappedBy = "afterSchool", cascade = CascadeType.ALL)
    private List<AfterSchoolScheduleEntity> afterSchoolSchedules = new ArrayList<>();

    @OneToMany(mappedBy = "afterSchool", cascade = CascadeType.ALL)
    private List<AfterSchoolStudentEntity> afterSchoolStudents = new ArrayList<>();

    @Builder
    public AfterSchoolEntity(TeacherEntity teacher, BranchEntity branch, PlaceEntity place,
                            WeekDay weekDay, SchoolPeriod period, Integer year, String name, Integer grade) {
        validateTeacher(teacher);
        validateBranch(branch);
        validatePlace(place);
        validateWeekDay(weekDay);
        validatePeriod(period);
        validateYear(year);
        validateName(name);
        validateGrade(grade);

        this.teacher = teacher;
        this.branch = branch;
        this.place = place;
        this.weekDay = weekDay;
        this.period = period;
        this.year = year;
        this.name = name;
        this.grade = grade;
        this.afterSchoolSchedules = new ArrayList<>();
        this.isEnd = false;
    }

    public void updateAfterSchool(TeacherEntity teacher, PlaceEntity place, WeekDay weekDay,
                                 SchoolPeriod period, Integer year, String name, Integer grade) {
        validateTeacher(teacher);
        validatePlace(place);
        validateWeekDay(weekDay);
        validatePeriod(period);
        validateYear(year);
        validateName(name);
        validateGrade(grade);

        this.teacher = teacher;
        this.place = place;
        this.weekDay = weekDay;
        this.period = period;
        this.year = year;
        this.name = name;
        this.grade = grade;
    }

    public void endAfterSchool() {
        this.isEnd = true;
    }

    public void resumeAfterSchool() {
        this.isEnd = false;
    }

    private void validateTeacher(TeacherEntity teacher) {
        if (teacher == null) {
            throw new InvalidAfterSchoolInfoException("담당 선생님은 필수입니다.");
        }
    }

    private void validateBranch(BranchEntity branch) {
        if (branch == null) {
            throw new InvalidAfterSchoolInfoException("분기는 필수입니다.");
        }
    }

    private void validatePlace(PlaceEntity place) {
        if (place == null) {
            throw new InvalidAfterSchoolInfoException("장소는 필수입니다.");
        }
    }

    private void validateWeekDay(WeekDay weekDay) {
        if (weekDay == null) {
            throw new InvalidAfterSchoolInfoException("요일은 필수입니다.");
        }
    }

    private void validatePeriod(SchoolPeriod period) {
        if (period == null) {
            throw new InvalidAfterSchoolInfoException("교시는 필수입니다.");
        }
    }

    private void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidAfterSchoolInfoException("방과후 이름은 비어 있을 수 없습니다.");
        }
    }

    private void validateGrade(Integer grade) {
        if (grade == null || grade < 1 || grade > 3) {
            throw new InvalidAfterSchoolInfoException("학년은 1 ~ 3 사이여야 합니다.");
        }
    }

    private void validateYear(Integer year) {
        if (year == null || year < 2000 || year > 2100) {
            throw new InvalidAfterSchoolInfoException("연도는 2000 ~ 2100 사이여야 합니다.");
        }
    }

}
