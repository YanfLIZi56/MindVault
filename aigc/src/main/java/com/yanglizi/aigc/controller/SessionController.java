package com.yanglizi.aigc.controller;

import com.yanglizi.aigc.Service.SessionService;
import com.yanglizi.aigc.pojo.dto.ChatSessionDetailDto;
import com.yanglizi.aigc.pojo.entry.ChatSession;
import com.yanglizi.common.pojo.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 会话
 */
@RestController
@RequestMapping("/session")
public class SessionController {
    @Autowired
    private SessionService chatService;

    /**
     * 创建会话
     */
    @PostMapping
    public Result createSession(Integer userId){
        return Result.success(chatService.createSession(userId));
    }

    /**
     * 获取会话列表
     */
    @GetMapping
    public Result getSession(Integer userId){
        return Result.success(chatService.getSession(userId));
    }

    /**
     * 获取会话详情
     */
    @GetMapping("/{sessionId}")
    public Result getSessionDetail(@PathVariable String sessionId, Integer userId){
        return Result.success(chatService.getSessionDetail(sessionId, userId));
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/{sessionId}")
    public Result deleteSession(@PathVariable String sessionId, Integer userId){
        int count = chatService.deleteSession(sessionId, userId);
        return Result.success(count);
    }

    /**
     * 更新会话
     */
    @PutMapping("/{sessionId}")
    public Result updateSession(@PathVariable String sessionId, @RequestBody ChatSessionDetailDto dto, Integer userId){
        int count = chatService.updateSessionTitle(dto, sessionId, userId);
        return Result.success(count);
    }
}
