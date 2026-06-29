package io.github.zh.model.auth.bo;

import io.github.zh.common.aspect.validator.annotation.Phone;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString

public class LoginUserBO {

    // 手机号
    @Phone
    private String phone;
    // 验证码
    @Size(min = 6, max = 6, message = "验证码长度为6位")
    @NotBlank
    private String code;
}
