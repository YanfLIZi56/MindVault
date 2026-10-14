package com.yanglizi.aigc.Service.impl;

import com.yanglizi.aigc.Service.SessionService;
import com.yanglizi.aigc.exception.AiException;
import com.yanglizi.aigc.mapper.ChatMapper;
import com.yanglizi.aigc.pojo.dto.ChatSessionDetailDto;
import com.yanglizi.aigc.pojo.entry.ChatSession;
import com.yanglizi.aigc.pojo.entry.DiaryConversations;
import com.yanglizi.aigc.pojo.vo.ChatSessionVo;
import com.yanglizi.aigc.pojo.vo.CreateSessionVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.yanglizi.aigc.constants.ChatConstant.AI_SPEAKER;
import static com.yanglizi.aigc.constants.ChatConstant.CHATTING;

@Service
public class SessionServiceImpl implements SessionService {
    @Value("${ai.greeting}")
    private String greeting;
    @Autowired
    private ChatMapper chatMapper;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public CreateSessionVo createSession(Integer userId) {
        // 检查用户是否已存在未对话的会话, 如果有则复用
        String blankSession = chatMapper.blankSession(userId);
        if (blankSession != null) {
            return new CreateSessionVo(blankSession, greeting);
        }

        // 生成不带 '-' 的32位UUID
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        ChatSession build = ChatSession.builder()
                .id(sessionId)
                .userId(userId)
                .status(CHATTING)
                .build();
        chatMapper.createSession(build);

        DiaryConversations build1 = DiaryConversations.builder()
                .sessionId(sessionId)
                .content(greeting)
                .speaker(AI_SPEAKER)
                .round(0)
                .build();
        chatMapper.createConversation(build1);

        return new CreateSessionVo(sessionId, greeting);
    }

    @Override
    public List<ChatSessionVo> getSession(Integer userId) {
        return chatMapper.getSessionByUserId(userId);
    }

    @Override
    public int deleteSession(String sessionId, Integer userId) {
        int result = chatMapper.deleteSession(sessionId, userId);
        if (result == 0) {
            throw new AiException("会话不存在");
        }
        chatMapper.deleteConversationBySessionId(sessionId);
        return result;
    }

    @Override
    public int updateSessionTitle(ChatSessionDetailDto dto, String sessionId, Integer userId) {
        int i = chatMapper.updateChatSession(dto, sessionId, userId);
        if (i == 0) {
            throw new AiException("会话不存在");
        }
        return i;
    }

    @Override
    public ChatSession getSessionDetail(String sessionId, Integer userId) {
        return chatMapper.selectSessionByIdAndUserId(sessionId, userId);
    }
}
