package com.yanglizi.user.mapper;

import com.yanglizi.user.pojo.dto.LoginUserDto;
import com.yanglizi.user.pojo.entity.User;
import com.yanglizi.user.pojo.vo.UserLoginVo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper {

    int existAccount(String account);

    int createUser(User user);

    int existId(Integer userId);

    UserLoginVo selectLoginUser(LoginUserDto loginUserDto);

    Integer getUserLevelId(Integer userId);

    void updateLoginTime(Integer id, String loginAt);
}
