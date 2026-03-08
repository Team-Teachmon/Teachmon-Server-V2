package solvit.teachmon.domain.supervision.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import solvit.teachmon.domain.supervision.application.mapper.SupervisionExchangeResponseMapper;
import solvit.teachmon.domain.supervision.domain.entity.SupervisionExchangeEntity;
import solvit.teachmon.domain.supervision.domain.entity.SupervisionScheduleEntity;
import solvit.teachmon.domain.supervision.domain.repository.SupervisionExchangeRepository;
import solvit.teachmon.domain.supervision.domain.repository.SupervisionScheduleRepository;
import solvit.teachmon.domain.supervision.exception.SupervisionExchangeNotFoundException;
import solvit.teachmon.domain.supervision.exception.SupervisionScheduleNotFoundException;
import solvit.teachmon.domain.supervision.exception.UnauthorizedSupervisionAccessException;
import solvit.teachmon.domain.supervision.presentation.dto.request.SupervisionExchangeAcceptRequestDto;
import solvit.teachmon.domain.supervision.presentation.dto.request.SupervisionExchangeRejectRequestDto;
import solvit.teachmon.domain.supervision.presentation.dto.request.SupervisionExchangeRequestDto;
import solvit.teachmon.domain.supervision.presentation.dto.response.SupervisionExchangeResponseDto;
import solvit.teachmon.domain.user.domain.entity.TeacherEntity;
import solvit.teachmon.domain.user.domain.repository.TeacherRepository;
import solvit.teachmon.domain.user.exception.TeacherNotFoundException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SupervisionExchangeService {

    private final SupervisionExchangeRepository supervisionExchangeRepository;
    private final SupervisionScheduleRepository supervisionScheduleRepository;
    private final TeacherRepository teacherRepository;
    private final SupervisionExchangeResponseMapper mapper;

    @Transactional
    public void createSupervisionExchangeRequest(SupervisionExchangeRequestDto requestDto, Long requesterId) {
        // 감독 일정 조회
        SupervisionScheduleEntity requestorSchedule = supervisionScheduleRepository.findById(requestDto.requestorSupervisionId())
                .orElseThrow(SupervisionScheduleNotFoundException::new);
        
        SupervisionScheduleEntity changeSchedule = supervisionScheduleRepository.findById(requestDto.changeSupervisionId())
                .orElseThrow(SupervisionScheduleNotFoundException::new);

        // 요청자 소유권 검증 - 현재 사용자가 해당 감독 일정의 담당자인지 확인
        if (!requestorSchedule.getTeacher().getId().equals(requesterId)) {
            throw new UnauthorizedSupervisionAccessException("본인의 감독 일정에 대해서만 교체 요청을 할 수 있습니다.");
        }

        // 교사 조회
        TeacherEntity requester = teacherRepository.findById(requesterId)
                .orElseThrow(TeacherNotFoundException::new);
        
        TeacherEntity recipient = changeSchedule.getTeacher();

        // 감독 교체 요청 생성
        SupervisionExchangeEntity exchangeEntity = SupervisionExchangeEntity.builder()
                .sender(requester)
                .recipient(recipient)
                .senderSchedule(requestorSchedule)
                .recipientSchedule(changeSchedule)
                .reason(requestDto.reason())
                .build();

        supervisionExchangeRepository.save(exchangeEntity);
    }

    @Transactional
    public void acceptSupervisionExchange(SupervisionExchangeAcceptRequestDto requestDto, Long currentUserId) {
        // 교체 요청 조회
        SupervisionExchangeEntity exchangeEntity = supervisionExchangeRepository.findById(requestDto.exchangeRequestId())
                .orElseThrow(SupervisionExchangeNotFoundException::new);

        // 수신자 권한 확인 - 현재 사용자가 수신자인지 검증
        if (!exchangeEntity.getRecipient().getId().equals(currentUserId)) {
            throw new UnauthorizedSupervisionAccessException("해당 교체 요청의 수신자만 수락할 수 있습니다.");
        }

        // 교체 요청 수락
        exchangeEntity.accept();

        // 스케줄 담당자 교체
        SupervisionScheduleEntity senderSchedule = exchangeEntity.getSenderSchedule();
        SupervisionScheduleEntity recipientSchedule = exchangeEntity.getRecipientSchedule();
        
        TeacherEntity originalSenderTeacher = senderSchedule.getTeacher();
        TeacherEntity originalRecipientTeacher = recipientSchedule.getTeacher();
        
        senderSchedule.changeTeacher(originalRecipientTeacher);
        recipientSchedule.changeTeacher(originalSenderTeacher);
    }

    @Transactional
    public void rejectSupervisionExchange(SupervisionExchangeRejectRequestDto requestDto, Long currentUserId) {
        // 교체 요청 조회
        SupervisionExchangeEntity exchangeEntity = supervisionExchangeRepository.findById(requestDto.exchangeRequestId())
                .orElseThrow(SupervisionExchangeNotFoundException::new);

        // 수신자 권한 확인 - 현재 사용자가 수신자인지 검증
        if (!exchangeEntity.getRecipient().getId().equals(currentUserId)) {
            throw new UnauthorizedSupervisionAccessException("해당 교체 요청의 수신자만 거절할 수 있습니다.");
        }

        // 교체 요청 거절
        exchangeEntity.reject();
    }

    @Transactional(readOnly = true)
    public List<SupervisionExchangeResponseDto> getSupervisionExchanges(Long currentUserId) {
        List<SupervisionExchangeEntity> exchanges = supervisionExchangeRepository.findByRecipientIdOrSenderIdAndExchangeTypeNotChecked(currentUserId);
        
        return exchanges.stream()
                .map(mapper::toResponseDto)
                .toList();
    }

    @Transactional
    public void checkSupervisionExchange(Long exchangeSupervisionId, Long currentUserId) {
        // 교체 요청 조회
        SupervisionExchangeEntity exchangeEntity = supervisionExchangeRepository.findById(exchangeSupervisionId)
                .orElseThrow(SupervisionExchangeNotFoundException::new);

        // 수신자 권한 확인 - 현재 사용자가 수신자인지 검증
        if (!exchangeEntity.getRecipient().getId().equals(currentUserId)) {
            throw new UnauthorizedSupervisionAccessException("해당 교체 요청의 수신자만 확인할 수 있습니다.");
        }

        exchangeEntity.check();
    }
}