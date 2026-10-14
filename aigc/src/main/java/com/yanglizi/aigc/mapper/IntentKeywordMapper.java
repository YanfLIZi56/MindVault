package com.yanglizi.aigc.mapper;

import com.yanglizi.aigc.pojo.entry.IntentKeywordConfig;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface IntentKeywordMapper {

    @Select("SELECT keyword FROM intent_keyword_config " +
            "WHERE intent_type = #{intentType} AND enabled = true " +
            "ORDER BY sort_order ")
    List<String> selectKeywordsByType(@Param("intentType") String intentType);

    @Select("SELECT * FROM intent_keyword_config WHERE enabled = true")
    List<IntentKeywordConfig> selectAllEnabled();

    @Insert("INSERT INTO intent_keyword_config (intent_type, keyword, enabled, sort_order) " +
            "VALUES (#{intentType}, #{keyword}, #{enabled}, #{sortOrder})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(IntentKeywordConfig config);

    @Update("UPDATE intent_keyword_config SET keyword = #{keyword}, enabled = #{enabled}, " +
            "sort_order = #{sortOrder}, updated_at = NOW() WHERE id = #{id}")
    int update(IntentKeywordConfig config);

    @Delete("DELETE FROM intent_keyword_config WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    @Update("UPDATE intent_keyword_config SET enabled = #{enabled}, updated_at = NOW() " +
            "WHERE id = #{id}")
    int updateEnabled(@Param("id") Long id, @Param("enabled") Boolean enabled);
}