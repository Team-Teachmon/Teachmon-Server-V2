package solvit.teachmon.domain.after_school.exception;

import org.springframework.http.HttpStatus;
import solvit.teachmon.global.exception.TeachmonBusinessException;

public class AfterSchoolBusinessTripScheduleNotFoundException extends TeachmonBusinessException {
    
    public AfterSchoolBusinessTripScheduleNotFoundException(String afterSchoolName) {
        super(afterSchoolName + " 방과후의 출장 처리할 스케줄을 찾을 수 없습니다", HttpStatus.NOT_FOUND);
    }
}