package solvit.teachmon.domain.self_study.domain.repository;

import com.querydsl.core.group.GroupBy;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import solvit.teachmon.domain.branch.domain.entity.BranchEntity;
import solvit.teachmon.domain.self_study.domain.entity.QSelfStudyEntity;
import solvit.teachmon.global.enums.SchoolPeriod;
import solvit.teachmon.global.enums.WeekDay;

import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class SelfStudyQueryDslRepositoryImpl implements SelfStudyQueryDslRepository {
    private final JPAQueryFactory queryFactory;

    @Override
    public Map<WeekDay, List<SchoolPeriod>> findGroupedByWeekDay(BranchEntity branch, Integer grade) {
        QSelfStudyEntity selfStudy = QSelfStudyEntity.selfStudyEntity;

        return queryFactory
                .select(selfStudy.weekDay, selfStudy.period)
                .from(selfStudy)
                .where(
                        selfStudy.branch.eq(branch),
                        gradeEq(grade, selfStudy)
                )
                .transform(GroupBy.groupBy(selfStudy.weekDay).as(GroupBy.list(selfStudy.period)));
    }

    private BooleanExpression gradeEq(Integer grade, QSelfStudyEntity selfStudy) {
        return grade != null ? selfStudy.grade.eq(grade) : null;
    }
}
