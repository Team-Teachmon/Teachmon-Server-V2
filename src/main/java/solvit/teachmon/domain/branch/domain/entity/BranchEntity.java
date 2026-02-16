package solvit.teachmon.domain.branch.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import solvit.teachmon.domain.branch.exception.InvalidBranchInfoException;
import solvit.teachmon.global.entity.BaseEntity;

import java.time.LocalDate;

@Getter
@Entity
@Table(name = "branch")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BranchEntity extends BaseEntity {
    @Column(name = "start_day", nullable = false)
    private LocalDate startDay;

    @Column(name = "end_day", nullable = false)
    private LocalDate endDay;

    @Column(name = "after_school_end_day", nullable = false)
    private LocalDate afterSchoolEndDay;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "branch", nullable = false)
    private Integer branch;
    
    @Builder
    public BranchEntity(LocalDate startDay, LocalDate endDay, LocalDate afterSchoolEndDay, Integer year, Integer branch) {
        validateStartDay(startDay);
        validateEndDay(endDay);
        validateAfterSchoolEndDay(afterSchoolEndDay);
        validateYear(year);
        validateBranch(branch);
        validateOrder(startDay, endDay);

        this.startDay = startDay;
        this.endDay = endDay;
        this.afterSchoolEndDay = afterSchoolEndDay;
        this.year = year;
        this.branch = branch;
    }
    
    public void updateBranch(LocalDate startDay, LocalDate endDay, LocalDate afterSchoolEndDay) {
        validateStartDay(startDay);
        validateEndDay(endDay);
        validateOrder(startDay, endDay);
        validateOrder(startDay, afterSchoolEndDay);
        validateOrder(afterSchoolEndDay, endDay);

        this.startDay = startDay;
        this.endDay = endDay;
        this.afterSchoolEndDay = afterSchoolEndDay;
    }

    private void validateAfterSchoolEndDay(LocalDate afterSchoolEndDay) {
        if(afterSchoolEndDay == null) {
            throw new InvalidBranchInfoException("방과후 종료일은 필수입니다.");
        }
    }

    private void validateStartDay(LocalDate startDay) {
        if (startDay == null) {
            throw new InvalidBranchInfoException("분기 시작일은 필수입니다.");
        }
    }

    private void validateEndDay(LocalDate endDay) {
        if (endDay == null) {
            throw new InvalidBranchInfoException("분기 종료일은 필수입니다.");
        }
    }

    private void validateYear(Integer year) {
        if (year == null || year < 2000 || year > 2100) {
            throw new InvalidBranchInfoException("연도는 2000~2100 사이여야 합니다.");
        }
    }

    private void validateBranch(Integer branch) {
        if (branch == null || branch < 1 || branch > 4) {
            throw new InvalidBranchInfoException("분기 번호는 1~4 사이여야 합니다.");
        }
    }

    private void validateOrder(LocalDate startDay, LocalDate endDay) {
        if (startDay != null && endDay != null && startDay.isAfter(endDay)) {
            throw new InvalidBranchInfoException("분기 시작일은 종료일보다 이후일 수 없습니다.");
        }
    }
}
