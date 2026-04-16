package com.gdut.domain.vo.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 游标分页响应结果
 * 用于解决深分页性能问题
 *
 * @param <T> 数据类型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "游标分页响应结果")
public class CursorPageResult<T> {
    
    @Schema(description = "数据列表")
    private List<T> records;
    
    @Schema(description = "下一页游标（为null表示没有更多数据）")
    private Long nextCursor;
    
    @Schema(description = "是否有下一页")
    private Boolean hasNext;
    
    @Schema(description = "每页大小")
    private Integer pageSize;
    
    /**
     * 创建空结果
     */
    public static <T> CursorPageResult<T> empty(Integer pageSize) {
        return CursorPageResult.<T>builder()
                .records(List.of())
                .nextCursor(null)
                .hasNext(false)
                .pageSize(pageSize)
                .build();
    }
}
