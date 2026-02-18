package solvit.teachmon.domain.user.presentation.controller;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import solvit.teachmon.domain.user.application.service.SearchTeacherService;
import solvit.teachmon.domain.user.presentation.dto.response.TeacherSearchResponseDto;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("/teacher")
@RequiredArgsConstructor
public class SearchTeacherController {
    private final SearchTeacherService searchTeacherService;

    @GetMapping("/search")
    public ResponseEntity<List<TeacherSearchResponseDto>> searchTeacher(@RequestParam @NotNull(message = "검색어는 필수입니다.") String query) {
        log.info(query);
        return ResponseEntity.ok(searchTeacherService.searchTeacherByQuery(query));
    }
}