package solvit.teachmon.domain.leave_seat.application.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import solvit.teachmon.domain.management.student.domain.entity.StudentEntity;
import solvit.teachmon.domain.leave_seat.presentation.dto.response.StudentInfoResponse;
import solvit.teachmon.domain.student_schedule.domain.enums.ScheduleType;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@DisplayName("이석 매퍼 테스트")
class LeaveSeatMapperTest {

    private final LeaveSeatMapper leaveSeatMapper = Mappers.getMapper(LeaveSeatMapper.class);

    @Test
    @DisplayName("아직 학생 스케줄이 생성되지 않아 예약 상태로 대기 중인 학생은 state가 PENDING으로 표시된다")
    void shouldMarkStudentAsPendingWhenScheduleNotYetGenerated() {
        // Given: 학생1은 스케줄이 이미 반영되어 있고, 학생2는 미래 주차라 아직 반영되지 않았을 때
        StudentEntity student1 = createMockStudent(1L, "김철수", 1, 1);
        StudentEntity student2 = createMockStudent(2L, "이영희", 1, 2);

        Map<Long, ScheduleType> studentLastScheduleTypes = Map.of(1L, ScheduleType.LEAVE_SEAT);

        // When: 학생 정보를 매핑하면
        List<StudentInfoResponse> results = leaveSeatMapper.mapStudentInfosWithScheduleTypes(
                List.of(student1, student2), studentLastScheduleTypes
        );

        // Then: 반영된 학생은 실제 스케줄 타입, 아직 반영 안 된 학생은 PENDING으로 표시된다
        assertThat(results)
                .extracting(StudentInfoResponse::id, StudentInfoResponse::state)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(1L, "LEAVE_SEAT"),
                        org.assertj.core.groups.Tuple.tuple(2L, "PENDING")
                );
    }

    private StudentEntity createMockStudent(Long id, String name, Integer grade, Integer classNumber) {
        StudentEntity student = mock(StudentEntity.class);
        given(student.getId()).willReturn(id);
        given(student.getName()).willReturn(name);
        given(student.calculateStudentNumber()).willReturn(grade * 100 + classNumber);
        return student;
    }
}
