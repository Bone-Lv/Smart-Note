package com.gdut.common.util;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gdut.domain.entity.note.Note;
import com.gdut.service.NoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AITools {

    private final NoteService noteService;

    /**
     * 读取笔记工具：搜索并返回笔记内容（包含 ID，用于后续写入）
     */
    @Tool(description = "根据标题、内容关键词或标签搜索笔记。返回格式：ID|标题|内容")
    public String readNote(
            @ToolParam(description = "笔记标题关键词（可选）") String titleKeyword,
            @ToolParam(description = "笔记内容关键词（可选，模糊搜索）") String contentKeyword,
            @ToolParam(description = "笔记标签（可选）") String tag) {
        
        // 至少需要一个搜索条件
        if (titleKeyword == null && contentKeyword == null && tag == null) {
            return "错误：请至少提供一个搜索条件（标题、内容或标签）";
        }

        LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(titleKeyword != null, Note::getTitle, titleKeyword)
                .like(contentKeyword != null, Note::getContent, contentKeyword)
                .eq(tag != null, Note::getTags, tag)
                .last("LIMIT 1"); // 限制只返回最匹配的一篇

        Note note = noteService.getOne(wrapper);
        if (note == null) {
            return "未找到符合条件的笔记";
        }
        // 返回结构化数据，方便 AI 提取 ID
        return String.format("笔记ID:%d\n标题:%s\n内容:%s", note.getId(), note.getTitle(), note.getContent());
    }

    /**
     * 写入笔记工具：将总结追加到指定笔记末尾
     */
    @Tool(description = "将总结内容追加到指定笔记的末尾。需要提供笔记ID和总结内容。")
    public String writeNote(
            @ToolParam(description = "目标笔记ID") Long noteId,
            @ToolParam(description = "要追加的总结内容") String summary) {
        try {
            Note note = noteService.getById(noteId);
            if (note == null) {
                return "错误：笔记不存在，无法写入";
            }
            
            // 追加总结，添加分隔线
            String updatedContent = note.getContent() + "\n\n--- AI 智能总结 ---\n" + summary;
            
            noteService.lambdaUpdate()
                    .eq(Note::getId, noteId)
                    .set(Note::getContent, updatedContent)
                    .update();
            
            return "成功：已将总结保存到笔记末尾";
        } catch (Exception e) {
            return "错误：写入失败，" + e.getMessage();
        }
    }

}
