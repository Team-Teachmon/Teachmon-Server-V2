package solvit.teachmon.domain.leave_seat.presentation.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import solvit.teachmon.domain.leave_seat.application.facade.LeaveSeatFacadeService;
import solvit.teachmon.domain.leave_seat.presentation.dto.request.LeaveSeatCreateRequest;
import solvit.teachmon.domain.leave_seat.presentation.dto.request.LeaveSeatUpdateRequest;
import solvit.teachmon.domain.leave_seat.presentation.dto.response.LeaveSeatDetailResponse;
import solvit.teachmon.domain.leave_seat.presentation.dto.response.LeaveSeatListResponse;
import solvit.teachmon.domain.leave_seat.presentation.dto.response.PlaceAvailabilityResponse;
import solvit.teachmon.domain.user.domain.entity.TeacherEntity;
import solvit.teachmon.global.enums.SchoolPeriod;
import solvit.teachmon.global.security.user.TeachmonUserDetails;

import java.time.LocalDate;
import java.util.List;

@Validated
@RestController
@RequestMapping("/leaveseat")
@RequiredArgsConstructor
public class LeaveSeatController {
    private final LeaveSeatFacadeService leaveSeatFacadeService;

    @PostMapping
    public ResponseEntity<String> createLeaveSeat(
            @Valid @RequestBody LeaveSeatCreateRequest request,
            @AuthenticationPrincipal TeachmonUserDetails teachmonUserDetails
    ) {
        TeacherEntity teacher = teachmonUserDetails.teacherEntity();

        leaveSeatFacadeService.createLeaveSeat(request, teacher);

        return ResponseEntity
                .ok()
                .body("이석을 작성하였습니다.");
    }

    @GetMapping
    public ResponseEntity<List<LeaveSeatListResponse>> getLeaveSeatList(
            @RequestParam @NotNull(message = "이석 조회에서 day(날짜)는 필수입니다.") LocalDate day,
            @RequestParam @NotNull(message = "이석 조회에서 period(교시)는 필수입니다.") SchoolPeriod period
    ) {
        List<LeaveSeatListResponse> results = leaveSeatFacadeService.getLeaveSeatList(day, period);

        return ResponseEntity
                .ok()
                .body(results);
    }

    @GetMapping("/{leaveseat_id}")
    public ResponseEntity<LeaveSeatDetailResponse> getLeaveSeatDetail(
            @PathVariable("leaveseat_id") @Positive(message = "이석 상세 조회에서 leaveseat_id(이석 ID)는 양수여야 합니다.") Long leaveseatId
    ) {
        LeaveSeatDetailResponse result = leaveSeatFacadeService.getLeaveSeatDetail(leaveseatId);

        return ResponseEntity
                .ok()
                .body(result);
    }

    @PatchMapping("/{leaveseat_id}")
    public ResponseEntity<String> updateLeaveSeat(
            @PathVariable("leaveseat_id") @Positive(message = "이석 수정에서 leaveseat_id(이석 ID)는 양수여야 합니다.") Long leaveSeatId,
            @Valid @RequestBody LeaveSeatUpdateRequest request,
            @AuthenticationPrincipal TeachmonUserDetails teachmonUserDetails
    ) {
        TeacherEntity teacher = teachmonUserDetails.teacherEntity();

        leaveSeatFacadeService.updateLeaveSeat(leaveSeatId, request, teacher);

        return ResponseEntity
                .ok()
                .body("이석 수정이 완료되었습니다");
    }

    @DeleteMapping("/{leaveseat_id}")
    public ResponseEntity<String> deleteLeaveSeat(
            @PathVariable("leaveseat_id") @Positive(message = "이석 삭제에서 leaveseat_id(이석 ID)는 양수여야 합니다.") Long leaveSeatId
    ) {
        leaveSeatFacadeService.deleteLeaveSeat(leaveSeatId);

        return ResponseEntity
                .ok()
                .body("이석 삭제가 완료되었습니다");
    }

    @GetMapping("/place/{place_id}/availability")
    public ResponseEntity<PlaceAvailabilityResponse> checkPlaceAvailability(
            @PathVariable("place_id") @Positive(message = "장소 가능 여부 조회에서 place_id(장소 ID)는 양수여야 합니다.") Long placeId,
            @RequestParam @NotNull(message = "장소 가능 여부 조회에서 day(날짜)는 필수입니다.") LocalDate day,
            @RequestParam @NotNull(message = "장소 가능 여부 조회에서 period(교시)는 필수입니다.") SchoolPeriod period
    ) {
        PlaceAvailabilityResponse result = leaveSeatFacadeService.checkPlaceAvailability(placeId, day, period);

        return ResponseEntity
                .ok()
                .body(result);
    }
}
