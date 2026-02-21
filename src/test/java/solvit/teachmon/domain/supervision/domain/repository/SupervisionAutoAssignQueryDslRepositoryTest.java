package solvit.teachmon.domain.supervision.domain.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import solvit.teachmon.domain.management.teacher.domain.entity.SupervisionBanDayEntity;
import solvit.teachmon.domain.management.teacher.domain.repository.SupervisionBanDayRepository;
import solvit.teachmon.domain.supervision.domain.entity.SupervisionScheduleEntity;
import solvit.teachmon.domain.supervision.domain.enums.SupervisionType;
import solvit.teachmon.domain.user.domain.entity.TeacherEntity;
import solvit.teachmon.domain.user.domain.enums.OAuth2Type;
import solvit.teachmon.domain.user.domain.enums.Role;
import solvit.teachmon.domain.user.domain.repository.TeacherRepository;
import solvit.teachmon.global.enums.SchoolPeriod;
import solvit.teachmon.global.enums.WeekDay;
import solvit.teachmon.domain.supervision.domain.vo.TeacherSupervisionInfoVo;
import solvit.teachmon.domain.supervision.domain.vo.SupervisionBanDayVo;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("감독 자동 배정 저장소 테스트")
class SupervisionAutoAssignQueryDslRepositoryTest {

    @Autowired
    private SupervisionAutoAssignQueryDslRepository autoAssignRepository;

    @Autowired
    private SupervisionScheduleRepository scheduleRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private SupervisionBanDayRepository banDayRepository;

    private TeacherEntity teacher1;
    private TeacherEntity teacher2;
    private TeacherEntity teacher3;

    @BeforeEach
    void setUp() {
        // 교사 생성
        teacher1 = createAndSaveTeacher("김선생", "kim@bssm.hs.kr");
        teacher2 = createAndSaveTeacher("이선생", "lee@bssm.hs.kr");
        teacher3 = createAndSaveTeacher("박선생", "park@bssm.hs.kr");
        
        // 기존 감독 이력 생성
        createSupervisionHistory();
        
        // 금지요일 설정 (김선생은 화요일 금지)
        createBanDays();
    }

    @Test
    @DisplayName("감독 가능한 활성 교사들의 감독 정보를 조회할 수 있다")
    void shouldFindEligibleTeacherSupervisionInfo() {
        // Given: 교사들과 감독 이력이 준비됨

        // When: 교사 감독 정보 조회
        List<TeacherSupervisionInfoVo> result = 
                autoAssignRepository.findEligibleTeacherSupervisionInfo();

        // Then: VIEWER가 아니고 @bssm.hs.kr 메일을 가진 활성 교사 3명 조회됨
        assertThat(result).hasSize(3); // 모든 교사가 조건에 맞음
        
        // 김선생 확인
        var kimTeacher = result.stream()
                .filter(t -> t.teacherName().equals("김선생"))
                .findFirst()
                .orElseThrow();
        
        assertThat(kimTeacher.teacherId()).isEqualTo(teacher1.getId());
        assertThat(kimTeacher.teacherName()).isEqualTo("김선생");
        assertThat(kimTeacher.lastSupervisionDate()).isEqualTo(LocalDate.of(2025, 1, 20));
        assertThat(kimTeacher.totalSupervisionCount()).isEqualTo(2L);
        assertThat(kimTeacher.sevenPeriodCount()).isEqualTo(1L);
        assertThat(kimTeacher.eightElevenPeriodCount()).isEqualTo(0L);
        
        // 이선생 확인
        var leeTeacher = result.stream()
                .filter(t -> t.teacherName().equals("이선생"))
                .findFirst()
                .orElseThrow();
        
        assertThat(leeTeacher.teacherId()).isEqualTo(teacher2.getId());
        assertThat(leeTeacher.teacherName()).isEqualTo("이선생");
        assertThat(leeTeacher.lastSupervisionDate()).isEqualTo(LocalDate.of(2025, 1, 15));
        assertThat(leeTeacher.totalSupervisionCount()).isEqualTo(1L);
        assertThat(leeTeacher.sevenPeriodCount()).isEqualTo(1L);
        assertThat(leeTeacher.eightElevenPeriodCount()).isEqualTo(0L);
        
        // 박선생 확인 (감독 이력 없음)
        var parkTeacher = result.stream()
                .filter(t -> t.teacherName().equals("박선생"))
                .findFirst()
                .orElseThrow();
        
        assertThat(parkTeacher.teacherId()).isEqualTo(teacher3.getId());
        assertThat(parkTeacher.teacherName()).isEqualTo("박선생");
        assertThat(parkTeacher.lastSupervisionDate()).isNull();
        assertThat(parkTeacher.totalSupervisionCount()).isEqualTo(0L);
        assertThat(parkTeacher.sevenPeriodCount()).isEqualTo(0L);
        assertThat(parkTeacher.eightElevenPeriodCount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("교사 ID 목록으로 금지요일 정보를 조회할 수 있다")
    void shouldFindBanDaysByTeacherIds() {
        // Given: 교사들의 금지요일이 설정됨
        List<Long> teacherIds = List.of(teacher1.getId(), teacher2.getId());

        // When: 금지요일 정보 조회
        List<SupervisionBanDayVo> result = 
                autoAssignRepository.findBanDaysByTeacherIds(teacherIds);

        // Then: 김선생의 화요일 금지요일만 조회됨
        assertThat(result).hasSize(1);
        
        var banDay = result.getFirst();
        assertThat(banDay.teacherId()).isEqualTo(teacher1.getId());
        assertThat(banDay.weekDay()).isEqualTo(WeekDay.TUE);
    }

    @Test
    @DisplayName("존재하지 않는 교사 ID로 금지요일을 조회하면 빈 결과가 반환된다")
    void shouldReturnEmptyWhenNonExistentTeacherIds() {
        // Given: 존재하지 않는 교사 ID
        List<Long> nonExistentIds = List.of(999L, 1000L);

        // When: 금지요일 정보 조회
        List<SupervisionBanDayVo> result = 
                autoAssignRepository.findBanDaysByTeacherIds(nonExistentIds);

        // Then: 빈 결과 반환
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("특정 날짜에 스케줄이 존재하는지 확인할 수 있다")
    void shouldCheckIfScheduleExistsByDate() {
        // Given: 특정 날짜에 스케줄 생성
        LocalDate targetDate = LocalDate.of(2025, 2, 10);
        SupervisionScheduleEntity schedule = SupervisionScheduleEntity.builder()
                .teacher(teacher1)
                .day(targetDate)
                .period(SchoolPeriod.SEVEN_PERIOD)
                .type(SupervisionType.SELF_STUDY_SUPERVISION)
                .build();
        scheduleRepository.save(schedule);

        // When: 해당 날짜 스케줄 존재 확인
        boolean exists = autoAssignRepository.existsScheduleByDate(targetDate);
        boolean notExists = autoAssignRepository.existsScheduleByDate(LocalDate.of(2025, 2, 11));

        // Then: 존재하는 날짜는 true, 없는 날짜는 false
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("감독 이력이 없는 교사는 총 횟수가 0으로 조회된다")
    void shouldReturnZeroCountForTeachersWithNoHistory() {
        // Given: 감독 이력이 없는 새 교사
        createAndSaveTeacher("정선생", "jung@bssm.hs.kr");

        // When: 교사 감독 정보 조회
        List<TeacherSupervisionInfoVo> result = 
                autoAssignRepository.findEligibleTeacherSupervisionInfo();

        // Then: 새 교사는 총 횟수 0, 최근 날짜 null
        var newTeacherInfo = result.stream()
                .filter(t -> t.teacherName().equals("정선생"))
                .findFirst()
                .orElseThrow();
        
        assertThat(newTeacherInfo.totalSupervisionCount()).isEqualTo(0L);
        assertThat(newTeacherInfo.lastSupervisionDate()).isNull();
        assertThat(newTeacherInfo.sevenPeriodCount()).isEqualTo(0L);
        assertThat(newTeacherInfo.eightElevenPeriodCount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("VIEWER 역할이거나 @bssm.hs.kr 메일이 아닌 교사는 조회되지 않는다")
    void shouldNotFindViewerOrNonBssmTeachers() {
        // Given: VIEWER 역할 교사와 다른 도메인 메일 교사
        TeacherEntity viewerTeacher = TeacherEntity.builder()
                .name("뷰어선생")
                .mail("viewer@bssm.hs.kr")
                .providerId("viewer_provider")
                .oAuth2Type(OAuth2Type.GOOGLE)
                .build();
        viewerTeacher.changeRole(Role.VIEWER);
        teacherRepository.save(viewerTeacher);

        // When: 교사 감독 정보 조회
        List<TeacherSupervisionInfoVo> result = 
                autoAssignRepository.findEligibleTeacherSupervisionInfo();

        // Then: VIEWER나 다른 도메인 교사는 조회되지 않음
        assertThat(result).noneMatch(t -> t.teacherName().equals("뷰어선생"));
        assertThat(result).noneMatch(t -> t.teacherName().equals("타도메인선생"));
        
        // 기존 3명만 조회됨 (각 테스트는 독립적으로 실행됨)
        assertThat(result).hasSize(3); // 기존 3명
    }

    @Test
    @DisplayName("8-11교시 감독은 8-9교시와 10-11교시를 합쳐서 1회로 계산한다")
    void shouldCount8To11PeriodSupervisionAsOneWhenBothPeriodsExist() {
        // Given: 교사가 8-9교시와 10-11교시 둘 다 감독한 경우
        SupervisionScheduleEntity eightNine = SupervisionScheduleEntity.builder()
                .teacher(teacher1)
                .day(LocalDate.of(2025, 1, 25))
                .period(SchoolPeriod.EIGHT_AND_NINE_PERIOD)
                .type(SupervisionType.SELF_STUDY_SUPERVISION)
                .build();
        
        SupervisionScheduleEntity tenEleven = SupervisionScheduleEntity.builder()
                .teacher(teacher1)
                .day(LocalDate.of(2025, 1, 25))
                .period(SchoolPeriod.TEN_AND_ELEVEN_PERIOD)
                .type(SupervisionType.SELF_STUDY_SUPERVISION)
                .build();
        
        scheduleRepository.saveAll(List.of(eightNine, tenEleven));

        // When: 교사 감독 정보 조회
        List<TeacherSupervisionInfoVo> result = 
                autoAssignRepository.findEligibleTeacherSupervisionInfo();

        // Then: 김선생의 8-11교시 감독 횟수가 1회로 계산됨
        var kimTeacher = result.stream()
                .filter(t -> t.teacherName().equals("김선생"))
                .findFirst()
                .orElseThrow();
        
        assertThat(kimTeacher.eightElevenPeriodCount()).isEqualTo(1L); // (2개 레코드 / 2) = 1회
        assertThat(kimTeacher.sevenPeriodCount()).isEqualTo(1L); // 기존 7교시 1회
        assertThat(kimTeacher.totalSupervisionCount()).isEqualTo(4L); // 기존 2회 + 새로 추가된 2회
    }

    // Helper methods
    private TeacherEntity createAndSaveTeacher(String name, String email) {
        TeacherEntity teacher = TeacherEntity.builder()
                .name(name)
                .mail(email)
                .providerId("provider_" + name)
                .oAuth2Type(OAuth2Type.GOOGLE)
                .build(); // isActive는 자동으로 true로 설정됨
        return teacherRepository.save(teacher);
    }

    private void createSupervisionHistory() {
        // 김선생 감독 이력 (2회)
        SupervisionScheduleEntity kim1 = SupervisionScheduleEntity.builder()
                .teacher(teacher1)
                .day(LocalDate.of(2025, 1, 10))
                .period(SchoolPeriod.SEVEN_PERIOD)
                .type(SupervisionType.SELF_STUDY_SUPERVISION)
                .build();
        
        SupervisionScheduleEntity kim2 = SupervisionScheduleEntity.builder()
                .teacher(teacher1)
                .day(LocalDate.of(2025, 1, 20))
                .period(SchoolPeriod.EIGHT_AND_NINE_PERIOD)
                .type(SupervisionType.LEAVE_SEAT_SUPERVISION)
                .build();
        
        // 이선생 감독 이력 (1회)
        SupervisionScheduleEntity lee1 = SupervisionScheduleEntity.builder()
                .teacher(teacher2)
                .day(LocalDate.of(2025, 1, 15))
                .period(SchoolPeriod.SEVEN_PERIOD)
                .type(SupervisionType.SELF_STUDY_SUPERVISION)
                .build();
        
        scheduleRepository.saveAll(List.of(kim1, kim2, lee1));
    }

    private void createBanDays() {
        // 김선생은 화요일 금지
        SupervisionBanDayEntity kimBanDay = SupervisionBanDayEntity.builder()
                .teacher(teacher1)
                .weekDay(WeekDay.TUE)
                .isAfterschool(false)
                .build();
        
        banDayRepository.save(kimBanDay);
    }
}
