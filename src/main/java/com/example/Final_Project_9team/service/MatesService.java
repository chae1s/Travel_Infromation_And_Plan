package com.example.Final_Project_9team.service;

import com.example.Final_Project_9team.dto.InvitationResponseDto;
import com.example.Final_Project_9team.dto.ResponseDto;
import com.example.Final_Project_9team.entity.Mates;
import com.example.Final_Project_9team.entity.Schedule;
import com.example.Final_Project_9team.entity.User;
import com.example.Final_Project_9team.exception.CustomException;
import com.example.Final_Project_9team.exception.ErrorCode;
import com.example.Final_Project_9team.repository.MatesRepository;
import com.example.Final_Project_9team.repository.ScheduleRepository;
import com.example.Final_Project_9team.repository.UserRepository;
import com.example.Final_Project_9team.utils.UserUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatesService {
    private final MatesRepository matesRepository;
//    private final UserRepository userRepository;
    private final ScheduleRepository scheduleRepository;
    private final UserUtils userUtils;

    // 유저가 다른 유저를 초대
    public ResponseEntity<ResponseDto> inviteUserToSchedule(String userEmail, String invitedUsername, Long scheduleId) {
        User invitedUser = userUtils.getUser(invitedUsername);
//        User invitedUser = userRepository.findByNickname(invitedUsername).orElseThrow(
//                () -> new CustomException(ErrorCode.USER_NOT_FOUND));
        Schedule schedule = scheduleRepository.findById(scheduleId).orElseThrow(
                () -> new CustomException(ErrorCode.SCHEDULE_NOT_FOUND));

        Optional<Mates> optionalMates = matesRepository.findByScheduleIdAndUserId(scheduleId, invitedUser.getId());
        // 이미 메이트가 존재하는 경우(이전에 초대된 적이 있다.)
        if (optionalMates.isPresent()) {
            Mates mates = optionalMates.get();
            if (mates.getIsDeleted() == true) { // 1. 탈퇴했던 경우
                mates.setDeleted(false);
                matesRepository.save(mates);
            } else { // 2. 이미 초대한 경우
                throw new CustomException(ErrorCode.ALREADY_INVITED_MATES);
            }
        } else {
            Mates mate = Mates.builder()
                    .user(invitedUser)
                    .schedule(schedule)
                    .isDeleted(false)
                    .isAccepted(false)
                    .isHost(false)
                    .build();
            matesRepository.save(mate);
        }

        return ResponseEntity.ok(ResponseDto.getMessage("초대가 정상적으로 되었습니다."));
    }
    // 초대 리스트 조회(수락대기중인 리스트)
    public ResponseEntity<List<InvitationResponseDto>> readInvitations(String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElseThrow(
                () -> new CustomException(ErrorCode.USER_NOT_FOUND));
        List<Mates> matesList = matesRepository.findAllByUserIdAndIsAcceptedFalseAndIsDeletedFalse(user.getId());

        List<InvitationResponseDto> invitationResponseDtos = new ArrayList<>();

        for (Mates mates : matesList) {
            InvitationResponseDto invitationResponseDto = InvitationResponseDto.builder()
                    .invitingUser(mates.getSchedule().getUser().getNickname())
                    .invitedTime(mates.getCreatedAt().toString())
                    .scheduleTitle(mates.getSchedule().getTitle())
                    .build();
            invitationResponseDtos.add(invitationResponseDto);
        }
        if(invitationResponseDtos.isEmpty())
            throw new CustomException(ErrorCode.INVITATION_NOT_FOUND);

        return ResponseEntity.ok(invitationResponseDtos);
    }

    // 초대 승낙 시
    public ResponseEntity<ResponseDto> acceptInvitation(String userEmail, Long scheduleId, Long matesId) {
        User invitedUser = userUtils.getUser(userEmail);
//        User invitedUser = userRepository.findByEmail(userEmail).orElseThrow(
//                () -> new CustomException(ErrorCode.USER_NOT_FOUND));
        Mates mates = matesRepository.findById(matesId).orElseThrow(
                () -> new CustomException(ErrorCode.MATES_NOT_FOUND));
        Schedule schedule = scheduleRepository.findById(scheduleId).orElseThrow(
                () -> new CustomException(ErrorCode.SCHEDULE_NOT_FOUND));

        // matesId의 유저정보와 맞는지 확인하기
        isMatchedUserAndMates(invitedUser, mates);

        // 이미 초대를 수락한 경우
        isAcceptedMates(mates);
        // 이미 탈퇴한 경우
        isLeftMates(mates);

        mates.setAccepted(true);
        matesRepository.save(mates);

        return ResponseEntity.ok(ResponseDto.getMessage("초대가 정상적으로 수락되었습니다."));
    }

    // 초대 거절 시
    public ResponseEntity<ResponseDto> rejectInvitation(String userEmail, Long scheduleId, Long matesId) {
        log.info("rejectInvitation() 실행");
        User invitedUser = userUtils.getUser(userEmail);
//        User invitedUser = userRepository.findByEmail(userEmail).orElseThrow(
//                () -> new CustomException(ErrorCode.USER_NOT_FOUND));
        Mates mates = matesRepository.findById(matesId).orElseThrow(
                () -> new CustomException(ErrorCode.MATES_NOT_FOUND));
        Schedule schedule = scheduleRepository.findById(scheduleId).orElseThrow(
                () -> new CustomException(ErrorCode.SCHEDULE_NOT_FOUND));

        isMatchedUserAndMates(invitedUser, mates);
        isAcceptedMates(mates);
        isLeftMates(mates);

        matesRepository.delete(mates);
        return ResponseEntity.ok(ResponseDto.getMessage("초대가 정상적으로 거절되었습니다."));

    }

    // 소속된 일정(메이트)에서 나가기(탈퇴)
    public ResponseEntity<ResponseDto> leaveMates(String userEmail, Long scheduleId, Long matesId) {
        User user = userUtils.getUser(userEmail);
//        User user = userRepository.findByEmail(userEmail).orElseThrow(
//                () -> new CustomException(ErrorCode.USER_NOT_FOUND));
        Mates mates = matesRepository.findById(matesId).orElseThrow(
                () -> new CustomException(ErrorCode.MATES_NOT_FOUND));
        Schedule schedule = scheduleRepository.findById(scheduleId).orElseThrow(
                () -> new CustomException(ErrorCode.SCHEDULE_NOT_FOUND));

        isMatchedUserAndMates(user, mates);
        isLeftMates(mates);

        mates.setDeleted(true);
        mates.setAccepted(false);
        matesRepository.save(mates);

        return ResponseEntity.ok(ResponseDto.getMessage("해당 일정에서 나갔습니다."));
    }

    public void isMatchedUserAndMates(User invitedUser, Mates mates) {
        if (!mates.getUser().equals(invitedUser))
            throw new CustomException(ErrorCode.MATES_NOT_MATCHED_USER);
    }

    public void isAcceptedMates(Mates mates) {
        if (mates.getIsAccepted() == true)
            throw new CustomException(ErrorCode.ALREADY_ACCEPTED_MATES);
    }

    public void isLeftMates(Mates mates) {
        log.info(mates.getIsDeleted().toString());
        if (mates.getIsDeleted() == true)
            throw new CustomException(ErrorCode.ALREADY_LEFT_MATES);
    }

    // 현재 회원이 포함된 메이트 모두 삭제
    public void deleteMateAll(User user) {
        // isDeleted == false인 일정 찾아오기
        List<Mates> matesList = matesRepository.findAllByUserIdAndIsDeletedIsFalse(user.getId());
        for (Mates mates : matesList) {
            mates.setDeleted(true);
            matesRepository.save(mates);
        }
    }
}
