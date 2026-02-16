package solvit.teachmon.domain.after_school.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

@Builder
public record AfterSchoolAffordableBusinessResponseDto(
    @JsonProperty("dates")
    List<LocalDate> dates
) {}
