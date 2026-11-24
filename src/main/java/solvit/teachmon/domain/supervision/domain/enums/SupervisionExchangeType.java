package solvit.teachmon.domain.supervision.domain.enums;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum SupervisionExchangeType {
    PENDING("대기"),
    ACCEPTED("수락됨"),
    REJECTED("거절됨"),
    /**
     * @deprecated 확인 여부는 SupervisionExchangeEntity 의 senderChecked / recipientChecked 로 관리한다.
     * 기존 데이터 마이그레이션 후 제거 예정이며 새로 저장하지 않는다.
     */
    @Deprecated
    CHECKED("확인됨");

    private final String value;
}