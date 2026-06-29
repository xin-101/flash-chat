package io.github.zh.common.aspect.validator;

import cn.hutool.core.lang.Validator;
import io.github.zh.common.aspect.validator.annotation.Phone;
import io.github.zh.common.exception.BizException;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Objects;

public class PhoneValidator implements ConstraintValidator<Phone, String> {

    // 定义注解对象
    private Phone phone;
    // 初始化注解对象
    @Override
    public void initialize(Phone phone) {
        this.phone = phone;
    }

    @Override
    public boolean isValid(String s, ConstraintValidatorContext ctx) {
        if (Objects.isNull(s)){
            throw new BizException("common-10000","手机号码不能为空");
        }

        // 正则表达式 工具类
        boolean mobile = Validator.isMobile(s);

        return mobile;
    }

}
