package com.gdut.domain.vo.chat;

import lombok.Data;

@Data
public class GroupUnreadCountVO {
    private Long groupId;
    private Integer count;
}
