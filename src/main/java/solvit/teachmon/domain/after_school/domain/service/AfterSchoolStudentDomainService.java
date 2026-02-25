package solvit.teachmon.domain.after_school.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import solvit.teachmon.domain.after_school.domain.entity.AfterSchoolEntity;
import solvit.teachmon.domain.after_school.domain.entity.AfterSchoolStudentEntity;
import solvit.teachmon.domain.after_school.domain.vo.StudentAssignmentResultVo;
import solvit.teachmon.domain.after_school.exception.InvalidAfterSchoolInfoException;
import solvit.teachmon.domain.management.student.domain.entity.StudentEntity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class AfterSchoolStudentDomainService {

    public StudentAssignmentResultVo assignStudents(AfterSchoolEntity afterSchool, List<StudentEntity> students) {
        log.info("방과후 학생 배정 시작 - AfterSchool ID: {}, 요청 학생 수: {}", 
                afterSchool.getId(), students != null ? students.size() : 0);
        
        if (students == null) {
            students = List.of();
        }

        List<StudentEntity> newStudents = validateStudents(afterSchool, students);
        List<StudentEntity> currentStudents = afterSchool.getAfterSchoolStudents().stream()
                .map(AfterSchoolStudentEntity::getStudent)
                .toList();

        log.info("현재 등록된 학생 수: {}, 새로 요청된 학생 수: {}", 
                currentStudents.size(), newStudents.size());

        // 추가할 학생들 계산
        List<StudentEntity> addedStudents = newStudents.stream()
                .filter(s -> !currentStudents.contains(s))
                .toList();

        // 삭제할 학생들 계산  
        List<StudentEntity> removedStudents = currentStudents.stream()
                .filter(s -> !newStudents.contains(s))
                .toList();

        log.info("추가할 학생 수: {}, 삭제할 학생 수: {}", addedStudents.size(), removedStudents.size());

        // 삭제할 학생들 개별 제거 (clear() 대신)
        if (!removedStudents.isEmpty()) {
            log.info("학생 삭제 시작 - 삭제 대상: {}", 
                    removedStudents.stream().map(StudentEntity::getName).toList());
            afterSchool.getAfterSchoolStudents().removeIf(
                    as -> removedStudents.contains(as.getStudent())
            );
            log.info("학생 삭제 완료");
        }

        // 새 학생들 추가
        if (!addedStudents.isEmpty()) {
            log.info("학생 추가 시작 - 추가 대상: {}", 
                    addedStudents.stream().map(StudentEntity::getName).toList());
            for (StudentEntity student : addedStudents) {
                afterSchool.getAfterSchoolStudents().add(
                        AfterSchoolStudentEntity.builder()
                                .afterSchool(afterSchool)
                                .student(student)
                                .build()
                );
            }
            log.info("학생 추가 완료");
        }

        log.info("방과후 학생 배정 완료 - 최종 등록 학생 수: {}", 
                afterSchool.getAfterSchoolStudents().size());

        return StudentAssignmentResultVo.builder()
                .afterSchool(afterSchool)
                .addedStudents(addedStudents)
                .removedStudents(removedStudents)
                .build();
    }

    public void deleteAllByAfterSchool(AfterSchoolEntity afterSchool) {
        afterSchool.getAfterSchoolStudents().clear();
    }

    private List<StudentEntity> validateStudents(AfterSchoolEntity afterSchool, List<StudentEntity> students) {
        if (students == null || students.isEmpty()) {
            return List.of();
        }

        Set<StudentEntity> studentSet = new HashSet<>((int) (students.size() / 0.75f) + 1);

        for (StudentEntity student : students) {
            if (!studentSet.add(student)) {
                throw new InvalidAfterSchoolInfoException(
                        String.format("학생 '%s'가 중복 등록되었습니다.", student.getName())
                );
            }

            if (!student.getGrade().equals(afterSchool.getGrade())) {
                throw new InvalidAfterSchoolInfoException(
                        String.format(
                                "학생 '%s'의 학년(%d)이 방과후 대상 학년(%d)과 일치하지 않습니다.",
                                student.getName(),
                                student.getGrade(),
                                afterSchool.getGrade()
                        )
                );
            }
        }

        return studentSet.stream().toList();
    }

}
