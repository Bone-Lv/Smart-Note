package com.gdut.domain.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MemberUnreadItem {
    private Long groupId;
    private Long lastReadMsgId;
}
