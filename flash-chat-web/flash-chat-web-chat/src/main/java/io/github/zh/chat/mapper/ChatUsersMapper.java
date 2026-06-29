package io.github.zh.chat.mapper;

import io.github.zh.model.auth.pojo.Users;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChatUsersMapper {
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

    List<Users> searchFriends(@Param("userId") String userId, @Param("keyword") String keyword);
}
