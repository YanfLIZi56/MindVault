package com.yanglizi.user.Service;

import com.yanglizi.user.pojo.dto.RgisterUserDto;
import com.yanglizi.user.pojo.entity.User;

public interface UserService {
    User register(RgisterUserDto targetUser);

    Integer getUserLevelCode(Integer userId);
}
