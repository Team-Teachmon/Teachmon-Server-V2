package solvit.teachmon.domain.place.domain.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;
import solvit.teachmon.domain.after_school.domain.entity.AfterSchoolBusinessTripEntity;
import solvit.teachmon.domain.after_school.domain.entity.AfterSchoolEntity;
import solvit.teachmon.domain.after_school.domain.repository.AfterSchoolBusinessTripRepository;
import solvit.teachmon.domain.after_school.domain.repository.AfterSchoolRepository;
import solvit.teachmon.domain.branch.domain.entity.BranchEntity;
import solvit.teachmon.domain.branch.domain.repository.BranchRepository;
import solvit.teachmon.domain.place.domain.entity.PlaceEntity;
import solvit.teachmon.domain.user.domain.entity.TeacherEntity;
import solvit.teachmon.domain.user.domain.enums.OAuth2Type;
import solvit.teachmon.domain.user.domain.repository.TeacherRepository;
import solvit.teachmon.global.enums.SchoolPeriod;
import solvit.teachmon.global.enums.WeekDay;

import java.lang.reflect.Constructor;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("방과후 장소 점유 조회 테스트")
class PlaceAfterSchoolOccupancyTest {

    // 2026-10-13 (화)
    private static final LocalDate TUESDAY = LocalDate.of(2026, 10, 13);

    @Autowired
    private PlaceRepository placeRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private AfterSchoolRepository afterSchoolRepository;

    @Autowired
    private AfterSchoolBusinessTripRepository afterSchoolBusinessTripRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    private TeacherEntity teacher;
    private PlaceEntity place;
    private BranchEntity currentBranch;
    private BranchEntity nextBranch;

    @BeforeEach
    void setUp() {
        teacher = teacherRepository.save(TeacherEntity.builder()
                .name("김선생")
                .mail("kim@test.com")
                .providerId("provider_kim")
                .oAuth2Type(OAuth2Type.GOOGLE)
                .build());
        place = placeRepository.save(createPlace("3-1", 3));
        currentBranch = branchRepository.save(createBranch(3,
                LocalDate.of(2026, 8, 3), LocalDate.of(2026, 10, 25)));
        nextBranch = branchRepository.save(createBranch(4,
                LocalDate.of(2026, 10, 26), LocalDate.of(2027, 2, 28)));
    }

    @Test
    @DisplayName("아직 시작하지 않은 분기의 방과후는 장소를 점유하지 않는다")
    void notStartedBranchAfterSchoolDoesNotOccupyPlace() {
        afterSchoolRepository.save(createAfterSchool(nextBranch));

        assertThat(placeRepository.existAfterSchoolPlaceByDayAndPeriodAndPlace(
                TUESDAY, SchoolPeriod.EIGHT_AND_NINE_PERIOD, place)).isFalse();
    }

    @Test
    @DisplayName("현재 분기의 방과후는 장소를 점유한다")
    void currentBranchAfterSchoolOccupiesPlace() {
        afterSchoolRepository.save(createAfterSchool(currentBranch));

        assertThat(placeRepository.existAfterSchoolPlaceByDayAndPeriodAndPlace(
                TUESDAY, SchoolPeriod.EIGHT_AND_NINE_PERIOD, place)).isTrue();
    }

    @Test
    @DisplayName("종료된 방과후는 장소를 점유하지 않는다")
    void endedAfterSchoolDoesNotOccupyPlace() {
        AfterSchoolEntity afterSchool = createAfterSchool(currentBranch);
        afterSchool.endAfterSchool();
        afterSchoolRepository.save(afterSchool);

        assertThat(placeRepository.existAfterSchoolPlaceByDayAndPeriodAndPlace(
                TUESDAY, SchoolPeriod.EIGHT_AND_NINE_PERIOD, place)).isFalse();
    }

    @Test
    @DisplayName("이날 출장인 방과후는 장소를 점유하지 않는다")
    void businessTripAfterSchoolDoesNotOccupyPlace() {
        AfterSchoolEntity afterSchool = afterSchoolRepository.save(createAfterSchool(currentBranch));
        AfterSchoolBusinessTripEntity businessTrip = AfterSchoolBusinessTripEntity.builder()
                .day(LocalDate.now().plusDays(1))
                .afterSchool(afterSchool)
                .build();
        // 출장 날짜는 과거로 생성할 수 없으므로 생성 후 조회 날짜로 변경
        ReflectionTestUtils.setField(businessTrip, "day", TUESDAY);
        afterSchoolBusinessTripRepository.save(businessTrip);

        assertThat(placeRepository.existAfterSchoolPlaceByDayAndPeriodAndPlace(
                TUESDAY, SchoolPeriod.EIGHT_AND_NINE_PERIOD, place)).isFalse();
    }

    private AfterSchoolEntity createAfterSchool(BranchEntity branch) {
        return AfterSchoolEntity.builder()
                .teacher(teacher)
                .branch(branch)
                .place(place)
                .weekDay(WeekDay.TUE)
                .period(SchoolPeriod.EIGHT_AND_NINE_PERIOD)
                .year(2026)
                .name("방과후")
                .grade(3)
                .build();
    }

    private BranchEntity createBranch(Integer branch, LocalDate startDay, LocalDate endDay) {
        return BranchEntity.builder()
                .year(2026)
                .branch(branch)
                .startDay(startDay)
                .endDay(endDay)
                .afterSchoolEndDay(endDay)
                .build();
    }

    private PlaceEntity createPlace(String name, Integer floor) {
        try {
            Constructor<PlaceEntity> constructor = PlaceEntity.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            PlaceEntity newPlace = constructor.newInstance();
            ReflectionTestUtils.setField(newPlace, "name", name);
            ReflectionTestUtils.setField(newPlace, "floor", floor);
            return newPlace;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
