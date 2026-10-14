package com.yanglizi.level.mapper;

import com.yanglizi.level.pojo.dto.LevelCacheDto;
import com.yanglizi.level.pojo.entity.Level;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface LevelMapper {

    @Select("select id, code, name, rule_json, icon_url " +
            "from levels where rule_json <= #{i} order by rule_json desc limit 1")
    LevelCacheDto getLevelByRule(int i);

    @Select("select id, code, name, rule_json, icon_url " +
            "from levels")
    List<LevelCacheDto> selectAllLevels();

    @Select("select code from levels where id = #{levelId}")
    Integer getLevelByCode(Integer levelCode);


}
