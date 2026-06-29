package io.github.zh.auth.mapper;

import io.github.zh.model.auth.pojo.Users;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UsersMapper {
    int deleteByPrimaryKey(String id);

    int insert(Users record);

    int insertSelective(Users record);

    Users selectByPrimaryKey(String id);

    int updateByPrimaryKeySelective(Users record);

    int updateByPrimaryKey(Users record);

    Users selectByPhone(@Param("phone") String phone);

    Users selectById(String otherUserId);

    List<Users> selectByIds(@Param("ids") List<String> ids);

    List<Users> searchByKeyword(@Param("keyword") String keyword);

    Users selectByFlashChatNum(@Param("flashChatNum") String flashChatNum);
}