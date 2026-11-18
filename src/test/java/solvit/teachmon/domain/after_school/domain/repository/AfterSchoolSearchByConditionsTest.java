package solvit.teachmon.domain.after_school.domain.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;
import solvit.teachmon.domain.after_school.domain.entity.AfterSchoolEntity;
import solvit.teachmon.domain.after_school.presentation.dto.request.AfterSchoolSearchRequestDto;
import solvit.teachmon.domain.after_school.presentation.dto.response.AfterSchoolResponseDto;
import solvit.teachmon.domain.branch.domain.entity.BranchEntity;
import solvit.teachmon.domain.branch.domain.repository.BranchRepository;
import solvit.teachmon.domain.place.domain.entity.PlaceEntity;
import solvit.teachmon.domain.place.domain.repository.PlaceRepository;
import solvit.teachmon.domain.user.domain.entity.TeacherEntity;
import solvit.teachmon.domain.user.domain.enums.OAuth2Type;
import solvit.teachmon.domain.user.domain.repository.TeacherRepository;
import solvit.teachmon.global.enums.SchoolPeriod;
import solvit.teachmon.global.enums.WeekDay;

import java.lang.reflect.Constructor;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("방과후 조건 조회 테스트")
class AfterSchoolSearchByConditionsTest {

    private static final LocalDate TODAY = LocalDate.now();
    private static final int THIS_YEAR = TODAY.getYear();

    @Autowired
    private AfterSchoolRepository afterSchoolRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private PlaceRepository placeRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    private TeacherEntity teacher;
    private PlaceEntity place;

    @BeforeEach
    void setUp() {
        teacher = teacherRepository.save(TeacherEntity.builder()
                .name("김선생")
                .mail("kim@test.com")
                .providerId("provider_kim")
                .oAuth2Type(OAuth2Type.GOOGLE)
                .build());
        place = placeRepository.save(createPlace("3-1", 3));
    }

    @Test
    @DisplayName("이미 끝난 분기는 종료 처리된 방과후도 조회된다")
    void finishedBranchIncludesEndedAfterSchools() {
        BranchEntity finished = branchRepository.save(createBranch(THIS_YEAR, 2,
                TODAY.minusDays(60), TODAY.minusDays(1)));
        AfterSchoolEntity ended = createAfterSchool(finished, "종료된 방과후");
        ended.endAfterSchool();
        afterSchoolRepository.save(ended);
        afterSchoolRepository.save(createAfterSchool(finished, "진행 기록 방과후"));

        List<AfterSchoolResponseDto> results = afterSchoolRepository.findAfterSchoolsByConditions(search(THIS_YEAR, 2));

        assertThat(results).extracting(AfterSchoolResponseDto::name)
                .containsExactlyInAnyOrder("종료된 방과후", "진행 기록 방과후");
    }

    @Test
    @DisplayName("진행 중인 분기는 종료(폐강)된 방과후가 제외된다")
    void ongoingBranchExcludesEndedAfterSchools() {
        BranchEntity ongoing = branchRepository.save(createBranch(THIS_YEAR, 3,
                TODAY.minusDays(10), TODAY.plusDays(30)));
        AfterSchoolEntity quit = createAfterSchool(ongoing, "폐강된 방과후");
        quit.endAfterSchool();
        afterSchoolRepository.save(quit);
        afterSchoolRepository.save(createAfterSchool(ongoing, "진행 중 방과후"));

        List<AfterSchoolResponseDto> results = afterSchoolRepository.findAfterSchoolsByConditions(search(THIS_YEAR, 3));

        assertThat(results).extracting(AfterSchoolResponseDto::name)
                .containsExactly("진행 중 방과후");
    }

    @Test
    @DisplayName("연도가 다른 같은 분기 번호의 방과후는 섞이지 않는다")
    void sameBranchNumberOfDifferentYearIsNotMixed() {
        BranchEntity lastYear = branchRepository.save(createBranch(THIS_YEAR - 1, 2,
                TODAY.minusYears(1).minusDays(60), TODAY.minusYears(1).minusDays(1)));
        BranchEntity thisYear = branchRepository.save(createBranch(THIS_YEAR, 2,
                TODAY.minusDays(60), TODAY.minusDays(1)));
        afterSchoolRepository.save(createAfterSchool(lastYear, "작년 방과후"));
        afterSchoolRepository.save(createAfterSchool(thisYear, "올해 방과후"));

        assertThat(afterSchoolRepository.findAfterSchoolsByConditions(search(THIS_YEAR, 2)))
                .extracting(AfterSchoolResponseDto::name)
                .containsExactly("올해 방과후");
        assertThat(afterSchoolRepository.findAfterSchoolsByConditions(search(THIS_YEAR - 1, 2)))
                .extracting(AfterSchoolResponseDto::name)
                .containsExactly("작년 방과후");
    }

    private AfterSchoolSearchRequestDto search(int year, int branch) {
        return new AfterSchoolSearchRequestDto(3, year, branch, WeekDay.TUE, 8, 9);
    }

    private AfterSchoolEntity createAfterSchool(BranchEntity branch, String name) {
        return AfterSchoolEntity.builder()
                .teacher(teacher)
                .branch(branch)
                .place(place)
                .weekDay(WeekDay.TUE)
                .period(SchoolPeriod.EIGHT_AND_NINE_PERIOD)
                .year(branch.getYear())
                .name(name)
                .grade(3)
                .build();
    }

    private BranchEntity createBranch(Integer year, Integer branch, LocalDate startDay, LocalDate endDay) {
        return BranchEntity.builder()
                .year(year)
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
