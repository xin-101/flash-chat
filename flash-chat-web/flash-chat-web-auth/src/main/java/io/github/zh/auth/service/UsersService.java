package io.github.zh.auth.service;

import io.github.zh.common.response.Response;
import io.github.zh.model.auth.bo.LoginUserBO;
import io.github.zh.model.auth.pojo.Users;
public interface UsersService{

    int deleteByPrimaryKey(String id);

    int insert(Users record);

    int insertSelective(Users record);

    Users selectByPrimaryKey(String id);

    int updateByPrimaryKeySelective(Users record);

    int updateByPrimaryKey(Users record);

    Response login(LoginUserBO loginUserBO);

    Response updateFlashChatNum(String userId, String newNum);

    Response updateUserInfo(String userId, io.github.zh.model.auth.bo.UpdateUserBO bo);
}
