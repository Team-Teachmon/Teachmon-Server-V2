package solvit.teachmon.domain.place.domain.repository;

import solvit.teachmon.domain.place.domain.entity.PlaceEntity;
import solvit.teachmon.domain.place.presentation.dto.response.PlaceSearchResponseDto;
import solvit.teachmon.global.enums.SchoolPeriod;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface PlaceQueryDslRepository {
    Map<Integer, PlaceEntity> findAllByGradePrefix(Integer grade);
    Boolean checkPlaceAvailability(LocalDate day, SchoolPeriod period, PlaceEntity place);
    List<PlaceSearchResponseDto> searchPlacesByKeyword(String keyword);
    Boolean existAfterSchoolPlaceByDayAndPeriodAndPlace(LocalDate day, SchoolPeriod period, PlaceEntity place);
}
