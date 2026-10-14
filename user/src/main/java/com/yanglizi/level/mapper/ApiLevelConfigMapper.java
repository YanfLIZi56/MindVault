package com.yanglizi.level.mapper;

import com.yanglizi.level.pojo.entity.ApiLevelConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ApiLevelConfigMapper {
    @Select("select * from api_level_config")
    List<ApiLevelConfig> selectAll();
}
