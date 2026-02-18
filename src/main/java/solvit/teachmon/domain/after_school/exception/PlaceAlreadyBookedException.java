package solvit.teachmon.domain.after_school.exception;

import org.springframework.http.HttpStatus;
import solvit.teachmon.global.exception.TeachmonBusinessException;

public class PlaceAlreadyBookedException extends TeachmonBusinessException {
    public PlaceAlreadyBookedException() {
        super("이미 해당 날짜/교시에 사용 중인 장소입니다.", HttpStatus.CONFLICT);
    }
}
