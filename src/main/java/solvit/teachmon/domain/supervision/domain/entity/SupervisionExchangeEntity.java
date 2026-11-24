package solvit.teachmon.domain.supervision.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import solvit.teachmon.domain.supervision.domain.enums.SupervisionExchangeType;
import solvit.teachmon.domain.supervision.exception.InvalidSupervisionExchangeException;
import solvit.teachmon.domain.user.domain.entity.TeacherEntity;
import solvit.teachmon.global.entity.BaseEntity;

@Entity
@Getter
@Table(name = "supervision_exchange")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SupervisionExchangeEntity extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private TeacherEntity sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private TeacherEntity recipient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_schedule_id", nullable = false)
    private SupervisionScheduleEntity senderSchedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_schedule_id", nullable = false)
    private SupervisionScheduleEntity recipientSchedule;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private SupervisionExchangeType state;

    @Column(name = "reason", nullable = false)
    private String reason;

    @ColumnDefault("false")
    @Column(name = "sender_checked", nullable = false)
    private boolean senderChecked;

    @ColumnDefault("false")
    @Column(name = "recipient_checked", nullable = false)
    private boolean recipientChecked;

    @Builder
    public SupervisionExchangeEntity(TeacherEntity sender, TeacherEntity recipient,
                                   SupervisionScheduleEntity senderSchedule,
                                   SupervisionScheduleEntity recipientSchedule,
                                   String reason) {
        validateSender(sender);
        validateRecipient(recipient);
        validateSenderSchedule(senderSchedule);
        validateRecipientSchedule(recipientSchedule);
        validateReason(reason);
        validateDifferentTeachers(sender, recipient);

        this.sender = sender;
        this.recipient = recipient;
        this.senderSchedule = senderSchedule;
        this.recipientSchedule = recipientSchedule;
        this.reason = reason;
        this.state = SupervisionExchangeType.PENDING;
    }

    public void accept() {
        validatePending("대기 중인 교체 요청만 수락할 수 있습니다.");
        this.state = SupervisionExchangeType.ACCEPTED;
    }

    public void reject() {
        validatePending("대기 중인 교체 요청만 거절할 수 있습니다.");
        this.state = SupervisionExchangeType.REJECTED;
    }

    /**
     * 처리 결과(수락/거절)를 확인했음을 호출한 사람 쪽에만 표시한다.
     * 송신자와 수신자는 각자 확인하며, 한 사람이 확인해도 상대방 목록에서는 사라지지 않는다.
     * 같은 사람이 여러 번 확인해도 예외 없이 그대로 둔다.
     */
    public void check(Long teacherId) {
        if (state == SupervisionExchangeType.PENDING) {
            throw new InvalidSupervisionExchangeException("처리가 완료된 교체 요청만 확인할 수 있습니다.");
        }
        if (isSender(teacherId)) {
            this.senderChecked = true;
        }
        if (isRecipient(teacherId)) {
            this.recipientChecked = true;
        }
    }

    public boolean isSender(Long teacherId) {
        return sender.getId().equals(teacherId);
    }

    public boolean isRecipient(Long teacherId) {
        return recipient.getId().equals(teacherId);
    }

    private void validatePending(String message) {
        if (state != SupervisionExchangeType.PENDING) {
            throw new InvalidSupervisionExchangeException(message);
        }
    }

    private void validateSender(TeacherEntity sender) {
        if (sender == null) {
            throw new InvalidSupervisionExchangeException("교체 요청자는 필수입니다.");
        }
    }

    private void validateRecipient(TeacherEntity recipient) {
        if (recipient == null) {
            throw new InvalidSupervisionExchangeException("교체 대상자는 필수입니다.");
        }
    }

    private void validateSenderSchedule(SupervisionScheduleEntity senderSchedule) {
        if (senderSchedule == null) {
            throw new InvalidSupervisionExchangeException("요청자의 감독 일정은 필수입니다.");
        }
    }

    private void validateRecipientSchedule(SupervisionScheduleEntity recipientSchedule) {
        if (recipientSchedule == null) {
            throw new InvalidSupervisionExchangeException("대상자의 감독 일정은 필수입니다.");
        }
    }

    private void validateReason(String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new InvalidSupervisionExchangeException("교체 요청 사유는 필수입니다.");
        }
        if (reason.length() > 500) {
            throw new InvalidSupervisionExchangeException("교체 요청 사유는 500자 이하로 입력해주세요.");
        }
    }

    private void validateDifferentTeachers(TeacherEntity sender, TeacherEntity recipient) {
        if (sender != null && recipient != null && sender.getId().equals(recipient.getId())) {
            throw new InvalidSupervisionExchangeException("자신에게는 교체 요청을 할 수 없습니다.");
        }
    }
}
