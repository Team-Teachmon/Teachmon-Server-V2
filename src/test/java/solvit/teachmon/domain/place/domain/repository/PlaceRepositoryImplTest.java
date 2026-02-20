package solvit.teachmon.domain.place.domain.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import solvit.teachmon.domain.after_school.domain.repository.AfterSchoolBusinessTripRepository;
import solvit.teachmon.domain.place.presentation.dto.response.PlaceSearchResponseDto;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("장소 저장소 구현체 테스트")
class PlaceRepositoryImplTest {

    @Mock
    private JPAQueryFactory queryFactory;
    
    @Mock
    private AfterSchoolBusinessTripRepository afterSchoolBusinessTripRepository;

    private PlaceQueryDslRepositoryImpl placeRepositoryImpl;

    @BeforeEach
    void setUp() {
        placeRepositoryImpl = new PlaceQueryDslRepositoryImpl(queryFactory, afterSchoolBusinessTripRepository);
    }

    @Test
    @DisplayName("null 검색어를 처리할 수 있다")
    void shouldHandleNullQuery() {
        // When: null 검색어로 검색하면
        List<PlaceSearchResponseDto> results = placeRepositoryImpl.searchPlacesByKeyword(null);

        // Then: 빈 결과가 반환된다 (예외가 발생하지 않음)
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("빈 검색어를 처리할 수 있다")
    void shouldHandleEmptyQuery() {
        // When: 빈 검색어로 검색하면
        List<PlaceSearchResponseDto> results = placeRepositoryImpl.searchPlacesByKeyword("");

        // Then: 빈 결과가 반환된다
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("공백만 있는 검색어를 처리할 수 있다")
    void shouldHandleWhitespaceQuery() {
        // When: 공백만 있는 검색어로 검색하면
        List<PlaceSearchResponseDto> results = placeRepositoryImpl.searchPlacesByKeyword("   ");

        // Then: 빈 결과가 반환된다
        assertThat(results).isEmpty();
    }
}