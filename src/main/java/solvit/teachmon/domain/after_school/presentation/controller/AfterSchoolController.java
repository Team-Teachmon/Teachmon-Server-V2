package solvit.teachmon.domain.after_school.presentation.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.constraints.NotNull;
import solvit.teachmon.domain.after_school.application.service.AfterSchoolService;
import solvit.teachmon.domain.after_school.presentation.dto.request.AfterSchoolBusinessTripRequestDto;
import solvit.teachmon.domain.after_school.presentation.dto.request.AfterSchoolCreateRequestDto;
import solvit.teachmon.domain.after_school.presentation.dto.request.AfterSchoolDeleteRequestDto;
import solvit.teachmon.domain.after_school.presentation.dto.request.AfterSchoolReinforcementRequestDto;
import solvit.teachmon.domain.after_school.presentation.dto.request.AfterSchoolRequestDto;
import solvit.teachmon.domain.after_school.presentation.dto.request.AfterSchoolSearchRequestDto;
import solvit.teachmon.domain.after_school.presentation.dto.request.AfterSchoolUpdateRequestDto;
import solvit.teachmon.domain.after_school.presentation.dto.response.*;
import solvit.teachmon.global.enums.WeekDay;
import solvit.teachmon.global.security.user.TeachmonUserDetails;

import java.util.List;

@RestController
@RequestMapping("/afterschool")
@RequiredArgsConstructor
@Validated
public class AfterSchoolController {
    private final AfterSchoolService afterSchoolService;

    @GetMapping
    public ResponseEntity<List<AfterSchoolResponseDto>> searchAfterSchools(
            @RequestParam @NotNull(message = "학년은 필수입니다.") Integer grade,
            @RequestParam(name = "branch") @NotNull(message = "분기는 필수입니다.") Integer branch,
            @RequestParam(name = "week_day") @NotNull(message = "요일은 필수입니다.") WeekDay weekDay,
            @RequestParam(name = "start_period") @NotNull(message = "시작 교시는 필수입니다.") Integer startPeriod,
            @RequestParam(name = "end_period") @NotNull(message = "종료 교시는 필수입니다.") Integer endPeriod) {
        AfterSchoolSearchRequestDto searchRequest = new AfterSchoolSearchRequestDto(
                grade, branch, weekDay, startPeriod, endPeriod
        );
        List<AfterSchoolResponseDto> results = afterSchoolService.searchAfterSchools(searchRequest);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/me")
    public ResponseEntity<List<AfterSchoolMyResponseDto>> searchMyAfterSchools(
            @RequestParam(required = false) Integer grade,
            @AuthenticationPrincipal TeachmonUserDetails teachmonUserDetails) {
        Long teacherId = teachmonUserDetails.getId();
        List<AfterSchoolMyResponseDto> results = afterSchoolService.searchMyAfterSchools(teacherId, grade);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/me/today")
    public ResponseEntity<List<AfterSchoolTodayResponseDto>> searchMyTodayAfterSchools(
            @AuthenticationPrincipal TeachmonUserDetails teachmonUserDetails) {
        Long teacherId = teachmonUserDetails.getId();
        List<AfterSchoolTodayResponseDto> results = afterSchoolService.searchMyTodayAfterSchools(teacherId);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/teacher")
    public ResponseEntity<List<AfterSchoolByTeacherResponseDto>> getAfterSchoolsByTeacherId(
            @RequestParam("teacher_id") @NotNull(message = "선생님 ID는 필수입니다.") Long teacherId) {
        List<AfterSchoolByTeacherResponseDto> results = afterSchoolService.getAfterSchoolsByTeacherId(teacherId);
        return ResponseEntity.ok(results);
    }

    @PostMapping
    public ResponseEntity<Void> createAfterSchool(@Valid @RequestBody AfterSchoolCreateRequestDto afterSchoolCreateRequestDto) {
        afterSchoolService.createAfterSchool(afterSchoolCreateRequestDto);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping
    public ResponseEntity<Void> updateAfterSchool(@Valid @RequestBody AfterSchoolUpdateRequestDto afterSchoolUpdateRequestDto) {
        afterSchoolService.updateAfterSchool(afterSchoolUpdateRequestDto);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteAfterSchool(@Valid @RequestBody AfterSchoolDeleteRequestDto requestDto) {
        afterSchoolService.deleteAfterSchool(requestDto.afterSchoolId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/quit")
    public ResponseEntity<Void> quitAfterSchool(@Valid @RequestBody AfterSchoolRequestDto requestDto) {
        afterSchoolService.quitAfterSchool(requestDto.afterSchoolId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/business-trip/affordable/{after_school_id}")
    public ResponseEntity<AfterSchoolAffordableBusinessResponseDto> getAffordableAfterSchool(@PathVariable("after_school_id") Long afterSchoolId) {
        return ResponseEntity.ok(afterSchoolService.getBusinessTrip(afterSchoolId));
    }

    @PostMapping("/business-trip")
    public ResponseEntity<Void> createBusinessTrip(@Valid @RequestBody AfterSchoolBusinessTripRequestDto requestDto) {
        afterSchoolService.createBusinessTrip(requestDto);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reinforcement")
    public ResponseEntity<Void> createReinforcement(@Valid @RequestBody AfterSchoolReinforcementRequestDto requestDto) {
        afterSchoolService.createReinforcement(requestDto);
        return ResponseEntity.noContent().build();
    }
}
