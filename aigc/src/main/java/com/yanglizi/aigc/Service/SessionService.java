package com.yanglizi.aigc.Service;

import com.yanglizi.aigc.pojo.dto.ChatSessionDetailDto;
import com.yanglizi.aigc.pojo.entry.ChatSession;
import com.yanglizi.aigc.pojo.vo.ChatSessionVo;
import com.yanglizi.aigc.pojo.vo.CreateSessionVo;

import java.util.List;

public interface SessionService {
    CreateSessionVo createSession(Integer userId);

    List<ChatSessionVo> getSession(Integer userId);

    int deleteSession(String sessionId, Integer userId);

    int updateSessionTitle(ChatSessionDetailDto dto, String sessionId, Integer userId);

    ChatSession getSessionDetail(String sessionId, Integer userId);
}
