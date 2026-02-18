package solvit.teachmon.domain.place.domain.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import solvit.teachmon.domain.after_school.domain.entity.QAfterSchoolEntity;
import solvit.teachmon.domain.after_school.domain.entity.QAfterSchoolReinforcementEntity;
import solvit.teachmon.domain.leave_seat.domain.entity.QLeaveSeatEntity;
import solvit.teachmon.domain.place.domain.entity.PlaceEntity;
import solvit.teachmon.domain.place.domain.entity.QPlaceEntity;
import solvit.teachmon.domain.place.presentation.dto.response.PlaceSearchResponseDto;
import solvit.teachmon.domain.place.presentation.dto.response.QPlaceSearchResponseDto;
import solvit.teachmon.global.enums.SchoolPeriod;
import solvit.teachmon.global.enums.WeekDay;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class PlaceQueryDslRepositoryImpl implements PlaceQueryDslRepository{
    private final JPAQueryFactory queryFactory;

    @Override
    public Map<Integer, PlaceEntity> findAllByGradePrefix(Integer grade) {
        QPlaceEntity place = QPlaceEntity.placeEntity;

        return queryFactory
                .selectFrom(place)
                .where(place.name.startsWith(grade + "-"))
                .fetch()
                .stream()
                .collect(Collectors.toMap(
                        p -> extractClassNumber(p.getName()),
                        Function.identity()
                ));
    }

    @Override
    public Boolean checkPlaceAvailability(LocalDate day, SchoolPeriod period, PlaceEntity place) {
        Boolean afterSchoolExist = existAfterSchoolPlaceByDayAndPeriodAndPlace(day, period, place);
        Boolean leaveSeatExist = existLeaveSeatPlaceByDayAndPeriodAndPlace(day, period, place);
        Boolean afterSchoolReinforcementExist = existAfterSchoolReinforcementPlaceByDayAndPeriodAndPlace(day, period, place);

        return afterSchoolExist || leaveSeatExist || afterSchoolReinforcementExist;
    }

    @Override
    public Boolean existAfterSchoolPlaceByDayAndPeriodAndPlace(LocalDate day, SchoolPeriod period, PlaceEntity place) {
        QAfterSchoolEntity afterSchool = QAfterSchoolEntity.afterSchoolEntity;
        WeekDay weekDay = WeekDay.fromLocalDate(day);

        return queryFactory
                .selectOne()
                .from(afterSchool)
                .where(
                        afterSchool.weekDay.eq(weekDay),
                        afterSchool.period.eq(period),
                        afterSchool.place.eq(place)
                )
                .fetchFirst() != null;
    }

    private Boolean existAfterSchoolReinforcementPlaceByDayAndPeriodAndPlace(LocalDate day, SchoolPeriod period, PlaceEntity place) {
        QAfterSchoolReinforcementEntity afterSchoolReinforcement = QAfterSchoolReinforcementEntity.afterSchoolReinforcementEntity;

        return queryFactory
                .selectOne()
                .from(afterSchoolReinforcement)
                .where(
                        afterSchoolReinforcement.changeDay.eq(day),
                        afterSchoolReinforcement.changePeriod.eq(period),
                        afterSchoolReinforcement.place.eq(place)
                )
                .fetchFirst() != null;
    }

    private Boolean existLeaveSeatPlaceByDayAndPeriodAndPlace(LocalDate day, SchoolPeriod period, PlaceEntity place) {
        QLeaveSeatEntity leaveSeat = QLeaveSeatEntity.leaveSeatEntity;

         return queryFactory
                 .selectOne()
                 .from(leaveSeat)
                 .where(
                         leaveSeat.day.eq(day),
                         leaveSeat.period.eq(period),
                         leaveSeat.place.eq(place)
                 )
                 .fetchFirst() != null;
    }

    @Override
    public List<PlaceSearchResponseDto> searchPlacesByKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }

        QPlaceEntity place = QPlaceEntity.placeEntity;
        BooleanBuilder builder = new BooleanBuilder();
        
        // 이름으로 검색
        builder.or(place.name.containsIgnoreCase(keyword));
        
        // 층수로 검색 (숫자인 경우)
        try {
            Integer floor = Integer.valueOf(keyword.trim());
            builder.or(place.floor.eq(floor));
        } catch (NumberFormatException e) {
            // 숫자가 아닌 경우 이름 검색만 수행
        }
        
        return queryFactory.select(
                new QPlaceSearchResponseDto(
                        place.id,
                        place.name,
                        place.floor
                )
        )
        .from(place)
        .where(builder)
        .fetch();
    }

    private Integer extractClassNumber(String name) {
        // 반 추출하기
        return Integer.parseInt(name.substring(name.indexOf('-') + 1));
    }

}
