package solvit.teachmon.domain.management.student.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import solvit.teachmon.domain.management.student.exception.InvalidStudentInfoException;
import solvit.teachmon.global.entity.BaseEntity;

import java.time.LocalDate;

@Getter
@Entity
@Table(name = "student")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudentEntity extends BaseEntity {
    @Column(name = "`year`", nullable = false)
    private Integer year;

    @Column(name = "grade", nullable = false)
    private Integer grade;

    @Column(name = "class", nullable = false)
    private Integer classNumber;

    @Column(name = "number", nullable = false)
    private Integer number;

    @Column(name = "name", nullable = false)
    private String name;

    @Builder
    public StudentEntity(Integer year, Integer grade, Integer classNumber, Integer number, String name) {
        validateGrade(grade);
        validateClassNumber(classNumber);
        validateNumber(number);
        validateName(name);

        this.year = resolveYear(year);
        this.grade = grade;
        this.classNumber = classNumber;
        this.number = number;
        this.name = name;
    }

    private Integer resolveYear(Integer year) {
        // year 이 유효한지 검사
        return (year != null) ? year : getNowYear();
    }

    private Integer getNowYear() {
        // year 설정 도메인 로직
        return LocalDate.now().getYear();
    }

    public void changeInfo(Integer grade, Integer classNumber, Integer number, String name) {
        validateGrade(grade);
        validateClassNumber(classNumber);
        validateNumber(number);
        validateName(name);

        this.grade = grade;
        this.classNumber = classNumber;
        this.number = number;
        this.name = name;
    }

    private void validateGrade(Integer grade) {
        if (grade == null || grade < 1 || grade > 3) {
            throw new InvalidStudentInfoException("grade(학년)은 1 ~ 3 사이여야 합니다.");
        }
    }

    private void validateClassNumber(Integer classNumber) {
        if (classNumber == null || classNumber < 1) {
            throw new InvalidStudentInfoException("classNumber(반)은 1 이상이어야 합니다.");
        }
    }

    private void validateNumber(Integer number) {
        if (number == null || number < 1) {
            throw new InvalidStudentInfoException("number(번호)는 1 이상이어야 합니다.");
        }
    }

    private void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidStudentInfoException("name(이름)은 필수입니다.");
        }
    }

    public Integer calculateStudentNumber() {
        return Integer.parseInt(
                grade + classNumber + String.format("%02d", number)
        );
    }
}
