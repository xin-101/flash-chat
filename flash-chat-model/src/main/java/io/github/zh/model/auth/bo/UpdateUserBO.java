package io.github.zh.model.auth.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserBO {
    private String nickname;
    private Integer sex;
    private String face;
    private String signature;
    private String friendCircleBg;
    private String chatBg;
    private String birthday;
    private String email;
    private String country;
    private String province;
    private String city;
    private String district;
}
