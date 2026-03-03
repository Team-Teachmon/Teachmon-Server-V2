package solvit.teachmon.domain.management.student.domain.repository.querydsl.impl;

import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import solvit.teachmon.domain.management.student.domain.repository.querydsl.StudentQueryDslRepository;
import solvit.teachmon.domain.management.student.presentation.dto.response.QStudentSearchResponseDto;
import solvit.teachmon.domain.management.student.presentation.dto.response.StudentSearchResponseDto;

import java.util.List;

import static solvit.teachmon.domain.management.student.domain.entity.QStudentEntity.studentEntity;

@Repository
@RequiredArgsConstructor
public class StudentRepositoryImpl implements StudentQueryDslRepository {
    private final JPAQueryFactory queryFactory;

    @Override
    public List<StudentSearchResponseDto> searchStudentsByKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return queryFactory.select(
                    new QStudentSearchResponseDto(
                            studentEntity.id,
                            studentEntity.grade,
                            studentEntity.classNumber,
                            studentEntity.number,
                            studentEntity.name
                    )
            )
            .from(studentEntity)
            .fetch();
        }
        
        String trimmedKeyword = keyword.trim();
        
        return queryFactory.select(
                new QStudentSearchResponseDto(
                        studentEntity.id,
                        studentEntity.grade,
                        studentEntity.classNumber,
                        studentEntity.number,
                        studentEntity.name
                )
        )
        .from(studentEntity)
        .where(studentEntity.name.containsIgnoreCase(trimmedKeyword)
                .or(studentEntity.grade.stringValue()
                    .concat("-")
                    .concat(studentEntity.classNumber.stringValue())
                    .concat("-")
                    .concat(studentEntity.number.stringValue())
                    .containsIgnoreCase(trimmedKeyword))
                .or(studentEntity.grade.stringValue()
                    .concat(studentEntity.classNumber.stringValue())
                    .concat(Expressions.stringTemplate("LPAD({0}, 2, '0')", studentEntity.number))
                    .containsIgnoreCase(trimmedKeyword)))
        .fetch();
    }
}