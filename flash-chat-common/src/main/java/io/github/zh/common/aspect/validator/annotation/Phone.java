package io.github.zh.common.aspect.validator.annotation;

import io.github.zh.common.aspect.validator.PhoneValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {PhoneValidator.class})
public @interface Phone {

    // 默认错误消息
    String message() default "手机号格式错误";
    // 指定分组 让校验采取不同形式
    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
