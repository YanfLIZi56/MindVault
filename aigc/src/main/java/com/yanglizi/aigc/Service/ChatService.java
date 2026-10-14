package com.yanglizi.aigc.Service;

import com.yanglizi.aigc.pojo.entry.DiaryConversations;
import com.yanglizi.aigc.pojo.vo.ChatHistoryCursorVO;
import com.yanglizi.common.pojo.dto.CursorDto;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

public interface ChatService {

    Flux<ServerSentEvent<Object>> chat(String question, String sessionId, Integer userId);

    void stop(String sessionId, Integer userId);

    ChatHistoryCursorVO<DiaryConversations> getChatHistory(String sessionId, Integer userId, CursorDto cursorDto);

    String testChat(String question, String test);
}
