package io.github.zh.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.zh.model.chat.pojo.ConversationMember;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ConversationMemberMapper extends BaseMapper<ConversationMember> {
}
