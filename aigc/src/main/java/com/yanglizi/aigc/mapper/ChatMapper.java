package com.yanglizi.aigc.mapper;


import com.yanglizi.aigc.pojo.dto.ChatSessionDetailDto;
import com.yanglizi.aigc.pojo.entry.ChatSession;
import com.yanglizi.aigc.pojo.entry.DiaryConversations;
import com.yanglizi.aigc.pojo.vo.ChatSessionVo;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ChatMapper {
    int createSession(ChatSession build);

    int createConversation(DiaryConversations build1);

    int checkSession(String sessionId, Integer userId);

    Integer getNewRound(String sessionId, Integer userId, LocalDateTime now);

    void saveConversation(DiaryConversations build);

    String getRawFacts(String sessionId, Integer userId);

    ChatSession selectRawFactsLastRound(String sessionId);

    int tryLockRawFactsRound(String sessionId, Integer round);

    List<DiaryConversations> selectConversationByRoundRange(String sessionId, int diff);

    void generateRawFacts(String sessionId, String newRawFacts, Integer round);

    List<DiaryConversations> getChatHistory(String sessionId, Integer cursor, Integer size);

    int hasConversation(String sessionId);

    @Update("update chat_sessions set title = #{title}, updated_at = #{now} where id = #{sessionId} and user_id = #{userId}")
    void saveTitle(String sessionId, String title, Integer userId, LocalDateTime now);

    List<ChatSessionVo> getSessionByUserId(Integer userId);

    @Delete("delete from chat_sessions where id = #{sessionId} and user_id = #{userId}")
    int deleteSession(String sessionId, Integer userId);

    @Delete("delete from diary_conversations where session_id = #{sessionId}")
    void deleteConversationBySessionId(String sessionId);

    @Select("select title from chat_sessions where id = #{sessionId} and user_id = #{userId}")
    String hasTitle(String sessionId, Integer userId);

    ChatSession selectSessionById(String sessionId);



    @Select("select version_group from chat_sessions where id = #{sessionId} and user_id = #{userId}")
    String selectVersionGroup(String sessionId, Integer userId);

    @Update("update chat_sessions set version_group = #{versionGroup} where id = #{sessionId} and user_id = #{userId}")
    void updateVersionGroup(String sessionId, Integer userId, String versionGroup);

    @Select("select id from chat_sessions where user_id = #{userId} and last_round = 0 order by started_at desc limit 1")
    String blankSession(Integer userId);

    int updateChatSession(ChatSessionDetailDto dto, String sessionId, Integer userId);

    ChatSession selectSessionByIdAndUserId(String sessionId, Integer userId);
}
