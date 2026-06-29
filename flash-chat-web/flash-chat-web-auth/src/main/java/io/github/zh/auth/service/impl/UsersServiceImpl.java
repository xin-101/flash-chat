package io.github.zh.auth.service.impl;

import cn.hutool.core.lang.UUID;
import io.github.zh.auth.constants.RedisKeyConstants;
import io.github.zh.auth.exception.ResponseEnum;
import io.github.zh.auth.service.UsersService;
import io.github.zh.common.constants.BizConstants;
import io.github.zh.common.constants.DateConstants;
import io.github.zh.common.enums.SexEnum;
import io.github.zh.common.exception.BizException;
import io.github.zh.common.response.Response;
import io.github.zh.model.auth.bo.LoginUserBO;
import io.github.zh.model.auth.bo.UpdateUserBO;
import io.github.zh.model.auth.vo.UserVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import org.springframework.beans.factory.annotation.Autowired;
import io.github.zh.common.utils.redis.RedisUtil;
import io.github.zh.auth.mapper.UsersMapper;
import io.github.zh.model.auth.pojo.Users;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static io.github.zh.auth.constants.RedisKeyConstants.FLASH_CHAT_NUM_UPDATE_KEY;

@Service
@Slf4j
public class UsersServiceImpl implements UsersService {

    @Autowired
    private UsersMapper usersMapper;
    @Resource
    private RedisUtil redisUtil;

    @Override
    public int deleteByPrimaryKey(String id) {
        return usersMapper.deleteByPrimaryKey(id);
    }

    @Override
    public int insert(Users record) {
        return usersMapper.insert(record);
    }

    @Override
    public int insertSelective(Users record) {
        return usersMapper.insertSelective(record);
    }

    @Override
    public Users selectByPrimaryKey(String id) {
        return usersMapper.selectByPrimaryKey(id);
    }

    @Override
    public int updateByPrimaryKeySelective(Users record) {
        return usersMapper.updateByPrimaryKeySelective(record);
    }

    @Override
    public int updateByPrimaryKey(Users record) {
        return usersMapper.updateByPrimaryKey(record);
    }

    @Override
    public Response login(LoginUserBO loginUserBO) {
        // 两步 第一步 需要判断code
        String code = loginUserBO.getCode();
        String phone = loginUserBO.getPhone();

        String codeInRedis = redisUtil.get(RedisKeyConstants.SMS_CODE_KEY + phone);
        if (!code.equals(codeInRedis)) {
            throw new BizException(ResponseEnum.SMS_CODE_ERROR);
        }

        Users users = usersMapper.selectByPhone(phone);

        if (users == null) {
            // 创建用户
            users = new Users();
            users.setId(UUID.fastUUID().toString(true));
            users.setMobile(phone);
            users.setNickname("用户" + phone.substring(7));
            users.setFlashChatNum(UUID.fastUUID().toString(true));
            users.setFlashChatNumImg("https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=" + users.getFlashChatNum());
            users.setRealName("测试用户");
            users.setFace(BizConstants.DEFAULT_AVATAR);
            users.setSex(SexEnum.SECRET.getCode());

            users.setCreatedTime(LocalDateTime.now());
            users.setUpdatedTime(LocalDateTime.now());
            usersMapper.insert(users);
        }

        // 兼容旧数据：更新占位符二维码
        if (users.getFlashChatNumImg() == null || users.getFlashChatNumImg().equals("1111")) {
            users.setFlashChatNumImg("https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=" + (users.getFlashChatNum() != null ? users.getFlashChatNum() : users.getId()));
            usersMapper.updateByPrimaryKeySelective(users);
        }

        log.info("用户登录成功，用户手机号为{}，用户id为：{}", phone, users.getId());

        // 登录成功 1.移除Redis中的验证码
        redisUtil.del(RedisKeyConstants.SMS_CODE_KEY + phone);

        // 2.生成token
        String token = UUID.fastUUID().toString(true);
        String userTokenKey = RedisKeyConstants.USER_TOKEN_KEY + users.getId();

        redisUtil.set(userTokenKey, token, DateConstants.ONE_WEEK);
        // token反向映射，用于WebSocket验证
        redisUtil.set(RedisKeyConstants.USER_TOKEN_REVERSE_KEY + token, String.valueOf(users.getId()), DateConstants.ONE_WEEK);

        // 创建返回对象
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(users, userVO);
        userVO.setToken(token);


        return Response.success(userVO);
    }

    @Override
    public Response updateFlashChatNum(String userId, String newNum) {
        if (newNum == null || newNum.trim().isEmpty()) {
            throw new BizException(ResponseEnum.PARAM_ERROR);
        }
        newNum = newNum.trim();

        Users user = usersMapper.selectByPrimaryKey(userId);
        if (user == null) {
            throw new BizException(ResponseEnum.PARAM_ERROR);
        }

        if (newNum.equals(user.getFlashChatNum())) {
            throw new BizException(ResponseEnum.PARAM_ERROR.getErrorCode(), "新闪聊号与当前相同");
        }

        // 检查30天限制
        String updateKey = FLASH_CHAT_NUM_UPDATE_KEY + userId;
        String lastUpdate = redisUtil.get(updateKey);
        if (lastUpdate != null) {
            throw new BizException(ResponseEnum.PARAM_ERROR.getErrorCode(), "闪聊号一个月只能修改一次");
        }

        // 检查是否被占用
        Users existing = usersMapper.selectByFlashChatNum(newNum);
        if (existing != null && !existing.getId().equals(userId)) {
            throw new BizException(ResponseEnum.PARAM_ERROR.getErrorCode(), "该闪聊号已被占用");
        }

        // 更新闪聊号和二维码
        user.setFlashChatNum(newNum);
        user.setFlashChatNumImg("https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=" + newNum);
        user.setUpdatedTime(LocalDateTime.now());
        usersMapper.updateByPrimaryKeySelective(user);

        // 记录修改时间（30天过期）
        redisUtil.set(updateKey, LocalDateTime.now().toString(), 30 * 24 * 60 * 60);

        log.info("闪聊号修改成功: userId={}, newNum={}", userId, newNum);

        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return Response.success(userVO);
    }

    @Override
    public Response updateUserInfo(String userId, UpdateUserBO bo) {
        Users user = usersMapper.selectByPrimaryKey(userId);
        if (user == null) {
            throw new BizException(ResponseEnum.PARAM_ERROR);
        }

        if (bo.getNickname() != null) user.setNickname(bo.getNickname());
        if (bo.getSex() != null) user.setSex(bo.getSex());
        if (bo.getFace() != null) user.setFace(bo.getFace());
        if (bo.getSignature() != null) user.setSignature(bo.getSignature());
        if (bo.getFriendCircleBg() != null) user.setFriendCircleBg(bo.getFriendCircleBg());
        if (bo.getChatBg() != null) user.setChatBg(bo.getChatBg());
        if (bo.getEmail() != null) user.setEmail(bo.getEmail());
        if (bo.getCountry() != null) user.setCountry(bo.getCountry());
        if (bo.getProvince() != null) user.setProvince(bo.getProvince());
        if (bo.getCity() != null) user.setCity(bo.getCity());
        if (bo.getDistrict() != null) user.setDistrict(bo.getDistrict());
        if (bo.getBirthday() != null) {
            try {
                user.setBirthday(LocalDate.parse(bo.getBirthday()));
            } catch (Exception e) {
                throw new BizException(ResponseEnum.PARAM_ERROR.getErrorCode(), "日期格式错误，请使用 yyyy-MM-dd");
            }
        }

        user.setUpdatedTime(LocalDateTime.now());
        usersMapper.updateByPrimaryKeySelective(user);

        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return Response.success(userVO);
    }

}
