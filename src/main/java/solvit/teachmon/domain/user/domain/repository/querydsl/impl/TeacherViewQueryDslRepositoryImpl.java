package solvit.teachmon.domain.user.domain.repository.querydsl.impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import solvit.teachmon.domain.user.domain.entity.QTeacherEntity;
import solvit.teachmon.domain.user.domain.repository.querydsl.TeacherQueryDslRepository;
import solvit.teachmon.domain.user.presentation.dto.response.QTeacherProfileResponseDto;
import solvit.teachmon.domain.user.presentation.dto.response.TeacherProfileResponseDto;

import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class TeacherViewQueryDslRepositoryImpl implements TeacherQueryDslRepository {
    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<TeacherProfileResponseDto> findUserProfileById(Long id) {
        return Optional.ofNullable(
                queryFactory.select(
                        new QTeacherProfileResponseDto(
                                QTeacherEntity.teacherEntity.id,
                                QTeacherEntity.teacherEntity.name,
                                QTeacherEntity.teacherEntity.profile,
                                QTeacherEntity.teacherEntity.role
                        )
                )
                .from(QTeacherEntity.teacherEntity)
                .where(QTeacherEntity.teacherEntity.id.eq(id))
                .fetchOne()
        );
    }
}
