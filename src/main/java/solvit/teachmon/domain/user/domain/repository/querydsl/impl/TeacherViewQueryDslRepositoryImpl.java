package solvit.teachmon.domain.user.domain.repository.querydsl.impl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import solvit.teachmon.domain.user.domain.entity.QTeacherEntity;
import solvit.teachmon.domain.user.domain.repository.querydsl.TeacherQueryDslRepository;
import solvit.teachmon.domain.user.presentation.dto.response.QTeacherProfileResponseDto;
import solvit.teachmon.domain.user.presentation.dto.response.QTeacherSearchResponseDto;
import solvit.teachmon.domain.user.presentation.dto.response.TeacherProfileResponseDto;
import solvit.teachmon.domain.user.presentation.dto.response.TeacherSearchResponseDto;

import java.util.List;
import java.util.Optional;

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
                                QTeacherEntity.teacherEntity.profile
                        )
                )
                .from(QTeacherEntity.teacherEntity)
                .where(QTeacherEntity.teacherEntity.id.eq(id))
                .fetchOne()
        );
    }

    @Override
    public List<TeacherSearchResponseDto> queryTeachersByName(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }

        BooleanBuilder builder = new BooleanBuilder();
        
        builder.or(QTeacherEntity.teacherEntity.name.like("%" + keyword + "%"));
        
        return queryFactory.select(
                new QTeacherSearchResponseDto(
                        QTeacherEntity.teacherEntity.id,
                        QTeacherEntity.teacherEntity.name
                )
        )
        .from(QTeacherEntity.teacherEntity)
        .where(builder)
        .fetch();
    }
}
