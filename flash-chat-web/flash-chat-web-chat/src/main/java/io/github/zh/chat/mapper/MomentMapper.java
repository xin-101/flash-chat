package io.github.zh.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.zh.model.chat.pojo.Moment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MomentMapper extends BaseMapper<Moment> {
}
