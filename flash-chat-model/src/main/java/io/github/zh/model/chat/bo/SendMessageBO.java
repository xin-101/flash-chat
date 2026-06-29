package io.github.zh.model.chat.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class SendMessageBO {
    private String conversationId;
    private String content;
    private Integer type;
}
