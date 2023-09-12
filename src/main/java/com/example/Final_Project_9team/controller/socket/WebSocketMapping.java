package com.example.Final_Project_9team.controller.socket;

import com.example.Final_Project_9team.dto.ChatMessageDto;
import com.example.Final_Project_9team.dto.ChatRoomDto;
import com.example.Final_Project_9team.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;


@Slf4j
@Controller
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:8800")
public class WebSocketMapping {
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final ChatService chatService;
    @MessageMapping("/chat")
    public void sendChat(
            @Payload ChatMessageDto chatMessageDto,
            @Headers Map<String, Object> headers,
            @Header("nativeHeaders") Map<String, String> nativeHeaders
    ){
        log.info(chatMessageDto.toString());
        log.info(headers.toString());
        log.info(nativeHeaders.toString());
        if (!chatMessageDto.getMessage().equals("")) { // 입력되지 않은 메시지는 전송되지 않도록
//            String time = new SimpleDateFormat("HH:mm").format(new Date());
//            chatMessageDto.setTime(time);
//            chatService.saveChatMessage(chatMessageDto);
            simpMessagingTemplate.convertAndSend(
                    String.format("/topic/%s", chatMessageDto.getRoomId()),
                    chatMessageDto
            );
        }
    }
    @SubscribeMapping("/{roomId}") // 누군가가 구독할때 실행하는 메소드
    public List<ChatMessageDto> sendGreet(
            @DestinationVariable("roomId") Long roomId
    ) {
        log.info("구독 메서드 실행");
        return chatService.getChatMessages(roomId);
    }
}
