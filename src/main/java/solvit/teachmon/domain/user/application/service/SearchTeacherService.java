package solvit.teachmon.domain.user.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import solvit.teachmon.domain.user.domain.repository.TeacherRepository;
import solvit.teachmon.domain.user.presentation.dto.response.TeacherSearchResponseDto;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchTeacherService {
    private final TeacherRepository teacherRepository;

    @Transactional(readOnly = true)
    public List<TeacherSearchResponseDto> searchTeacherByQuery(String query) {
        log.info("service" + query);
        return teacherRepository.queryTeachersByName(query)
                .stream()
                .map(teacher -> TeacherSearchResponseDto.builder()
                        .id(teacher.getId())
                        .name(teacher.getName())
                        .build())
                .toList();
    }
}