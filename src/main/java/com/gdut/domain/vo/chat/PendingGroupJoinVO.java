package com.gdut.domain.vo.chat;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PendingGroupJoinVO {
    private Long memberId;
    private Long userId;
    private String username;
    private String avatar;
    private LocalDateTime joinTime;
}
