package com.yanglizi.aigc.controller;

import com.yanglizi.aigc.Service.ChatService;
import com.yanglizi.aigc.pojo.dto.ChatDto;
import com.yanglizi.aigc.pojo.vo.ChatVo;
import com.yanglizi.common.pojo.dto.CursorDto;
import com.yanglizi.common.pojo.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * 对话
 */
@RestController
@RequestMapping("/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    /**
     * 对话测试
     */
    @PostMapping("/test")
    public String testChat(@RequestBody ChatDto question) {
        return chatService.testChat(question.getQuestion(), "test");
    }

    /**
     * 对话
     */
    @PostMapping(value = "/{sessionId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Object>> chat(@PathVariable String sessionId, @RequestBody ChatDto question, Integer userId) {
        return chatService.chat(question.getQuestion(), sessionId, userId);
    }

    /**
     * 停止对话
     */
    @PostMapping(value = "/{sessionId}/stop")
    public Result stop(@PathVariable String sessionId, Integer userId) {
        chatService.stop(sessionId, userId);
        return Result.success();
    }

    /**
     * 获取对话历史
     */
    @GetMapping("/{sessionId}")
    public Result getSession(@PathVariable String sessionId, Integer userId, CursorDto cursorDto) {
        return Result.success(chatService.getChatHistory(sessionId, userId, cursorDto));
    }
}
