package com.gdut.domain.entity.note;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("ai_usage")
public class AiUsage {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    private Long userId;
    
    private LocalDate usageDate;
    
    private Integer usageCount;
    
    private LocalDateTime updateTime;
}
