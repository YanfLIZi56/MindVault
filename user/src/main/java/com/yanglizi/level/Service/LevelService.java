package com.yanglizi.level.Service;

import com.yanglizi.level.pojo.dto.LevelCacheDto;
import com.yanglizi.level.pojo.entity.Level;

public interface LevelService {
    LevelCacheDto getLevelByRule(int i);

    Integer getLevelByCode(Integer levelId);
}
