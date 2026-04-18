package com.gdut.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gdut.annotation.RequireRole;
import com.gdut.domain.dto.chat.*;
import com.gdut.domain.entity.common.Result;
import com.gdut.domain.vo.chat.*;
import com.gdut.service.MessageService;
import com.gdut.common.util.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/message")
@RequiredArgsConstructor
@Tag(name = "消息管理接口", description = "私聊、群聊、离线消息等功能")
public class MessageController {

    private final MessageService messageService;

    // ==================== 私聊相关接口 ====================

    @PostMapping("/private/send")
    @RequireRole
    @Operation(summary = "发送私聊消息", description = "向好友发送文本或图片消息，支持离线消息")
    public Result<PrivateMessageVO> sendPrivateMessage(@Valid @ModelAttribute PrivateMessageDTO dto) {
        Long senderId = UserContext.getUserId();
        PrivateMessageVO vo = messageService.sendPrivateMessage(senderId, dto);
        return Result.success(vo);
    }

    @GetMapping("/private/history")
    @RequireRole
    @Operation(summary = "获取私聊历史消息", description = "使用游标分页获取与某好友的聊天历史（传入cursor参数）")
    public Result<CursorPageResult<PrivateMessageVO>> getPrivateMessageHistory(MessageQueryDTO queryDTO) {
        Long userId = UserContext.getUserId();
        CursorPageResult<PrivateMessageVO> result = messageService.getPrivateMessageHistory(userId, queryDTO);
        return Result.success(result);
    }

    @GetMapping("/private/offline")
    @RequireRole
    @Operation(summary = "获取离线消息", description = "获取当前用户的所有未读消息")
    public Result<List<PrivateMessageVO>> getOfflineMessages() {
        Long userId = UserContext.getUserId();
        List<PrivateMessageVO> messages = messageService.getOfflineMessages(userId);
        return Result.success(messages);
    }

    @PutMapping("/private/read/{messageId}")
    @RequireRole
    @Operation(summary = "标记消息为已读", description = "将指定消息标记为已读")
    public Result<Void> markMessageAsRead(@PathVariable Long messageId) {
        Long userId = UserContext.getUserId();
        messageService.markMessageAsRead(userId, messageId);
        return Result.success(null);
    }

    @PutMapping("/private/read-all/{friendUserId}")
    @RequireRole
    @Operation(summary = "标记所有消息为已读", description = "将与某好友的所有消息标记为已读")
    public Result<Void> markAllMessagesAsRead(@PathVariable Long friendUserId) {
        Long userId = UserContext.getUserId();
        messageService.markAllMessagesAsRead(userId, friendUserId);
        return Result.success(null);
    }

    @DeleteMapping("/private/clear/{friendUserId}")
    @RequireRole
    @Operation(summary = "清空聊天记录", description = "清空与某好友的聊天对话记录")
    public Result<Void> clearPrivateChatHistory(@PathVariable Long friendUserId) {
        Long userId = UserContext.getUserId();
        messageService.clearPrivateChatHistory(userId, friendUserId);
        return Result.success(null);
    }


    // ==================== 群聊相关接口 ====================

    @PostMapping("/group/create")
    @RequireRole
    @Operation(summary = "创建群聊", description = "创建新的群聊并邀请初始成员")
    public Result<Long> createGroup(@Valid @RequestBody CreateGroupDTO dto) {
        Long userId = UserContext.getUserId();
        Long groupId = messageService.createGroup(userId, dto);
        return Result.success(groupId);
    }

    @PostMapping("/group/send")
    @RequireRole
    @Operation(summary = "发送群聊消息", description = "向群聊发送文本或图片消息")
    public Result<GroupMessageVO> sendGroupMessage(@Valid @ModelAttribute GroupMessageDTO dto) {
        Long senderId = UserContext.getUserId();
        GroupMessageVO vo = messageService.sendGroupMessage(senderId, dto);
        return Result.success(vo);
    }

    @GetMapping("/group/history")
    @RequireRole
    @Operation(summary = "获取群聊历史消息", description = "使用游标分页获取群聊的聊天历史（传入cursor参数）")
    public Result<CursorPageResult<GroupMessageVO>> getGroupMessageHistory(MessageQueryDTO queryDTO) {
        Long userId = UserContext.getUserId();
        CursorPageResult<GroupMessageVO> result = messageService.getGroupMessageHistory(userId, queryDTO);
        return Result.success(result);
    }

    @GetMapping("/group/my-groups")
    @RequireRole
    @Operation(summary = "获取我的群聊列表", description = "获取当前用户加入的所有群聊")
    public Result<List<ChatGroupVO>> getMyGroups() {
        Long userId = UserContext.getUserId();
        List<ChatGroupVO> groups = messageService.getMyGroups(userId);
        return Result.success(groups);
    }

    @PostMapping("/group/join/{groupId}")
    @RequireRole
    @Operation(summary = "申请加入群聊", description = "提交入群申请，等待群主或管理员审批")
    public Result<Void> joinGroup(@PathVariable Long groupId) {
        Long userId = UserContext.getUserId();
        messageService.joinGroup(userId, groupId);
        return Result.success(null);
    }

    @PostMapping("/group/approve")
    @RequireRole
    @Operation(summary = "审批入群申请", description = "群主或管理员审批入群申请")
    public Result<Void> approveGroupJoin(@Valid @RequestBody ApproveGroupJoinDTO dto) {
        Long operatorId = UserContext.getUserId();
        messageService.approveGroupJoin(operatorId, dto);
        return Result.success(null);
    }

    @GetMapping("/group/pending/{groupId}")
    @RequireRole
    @Operation(summary = "获取待审核的入群申请", description = "群主或管理员查看待审核的入群申请列表")
    public Result<List<PendingGroupJoinVO>> getPendingJoinRequests(@PathVariable Long groupId) {
        Long operatorId = UserContext.getUserId();
        List<PendingGroupJoinVO> requests = messageService.getPendingJoinRequests(operatorId, groupId);
        return Result.success(requests);
    }

    @PostMapping("/group/leave/{groupId}")
    @RequireRole
    @Operation(summary = "退出群聊", description = "退出指定群聊")
    public Result<Void> leaveGroup(@PathVariable Long groupId) {
        Long userId = UserContext.getUserId();
        messageService.leaveGroup(userId, groupId);
        return Result.success(null);
    }

    @GetMapping("/group/detail/{groupId}")
    @RequireRole
    @Operation(summary = "获取群聊详情", description = "获取指定群聊的详细信息")
    public Result<ChatGroupVO> getGroupDetail(@PathVariable Long groupId) {
        Long userId = UserContext.getUserId();
        ChatGroupVO vo = messageService.getGroupDetail(userId, groupId);
        return Result.success(vo);
    }

    @PutMapping("/group/read-all/{groupId}")
    @RequireRole
    @Operation(summary = "标记群聊所有消息为已读", description = "用户点开群聊时自动调用，将所有未读消息标记为已读")
    public Result<Void> markAllGroupMessagesAsRead(@PathVariable Long groupId) {
        Long userId = UserContext.getUserId();
        messageService.markAllGroupMessagesAsRead(userId, groupId);
        return Result.success(null);
    }

    @DeleteMapping("/group/clear/{groupId}")
    @RequireRole
    @Operation(summary = "清空群聊历史记录", description = "仅自己不可见，不影响其他成员")
    public Result<Void> clearGroupChatHistory(@PathVariable Long groupId) {
        Long userId = UserContext.getUserId();
        messageService.clearGroupChatHistory(userId, groupId);
        return Result.success(null);
    }

    @GetMapping("/group/members/{groupId}")
    @RequireRole
    @Operation(summary = "获取群聊成员列表", description = "获取指定群聊的所有成员（按角色排序：群主、管理员、普通成员）")
    public Result<List<GroupMemberVO>> getGroupMembers(@PathVariable Long groupId) {
        Long userId = UserContext.getUserId();
        List<GroupMemberVO> members = messageService.getGroupMembers(userId, groupId);
        return Result.success(members);
    }

    @PutMapping("/group/{groupId}/name")
    @RequireRole
    @Operation(summary = "重命名群聊", description = "仅群主可以修改群名称")
    public Result<Void> renameGroup(@PathVariable Long groupId, 
                                     @Valid @RequestBody UpdateGroupDTO dto) {
        Long userId = UserContext.getUserId();
        messageService.renameGroup(userId, groupId, dto.getGroupName());
        return Result.success(null);
    }

    @PostMapping("/group/{groupId}/transfer")
    @RequireRole
    @Operation(summary = "转让群主", description = "仅群主可以将群主转让给其他成员")
    public Result<Void> transferGroupOwner(@PathVariable Long groupId,
                                            @Valid @RequestBody TransferGroupOwnerDTO dto) {
        Long userId = UserContext.getUserId();
        messageService.transferGroupOwner(userId, groupId, dto.getNewOwnerId());
        return Result.success(null);
    }

    @PutMapping("/group/{groupId}/admin")
    @RequireRole
    @Operation(summary = "设置或取消管理员", description = "仅群主可以设置或取消管理员")
    public Result<Void> setGroupAdmin(@PathVariable Long groupId,
                                       @Valid @RequestBody SetGroupAdminDTO dto) {
        Long userId = UserContext.getUserId();
        messageService.setGroupAdmin(userId, groupId, dto.getUserId(), dto.getIsAdmin());
        return Result.success(null);
    }

    @DeleteMapping("/group/{groupId}/member")
    @RequireRole
    @Operation(summary = "移除群成员", description = "群主或管理员可以移除普通成员")
    public Result<Void> removeGroupMember(@PathVariable Long groupId,
                                           @Valid @RequestBody RemoveGroupMemberDTO dto) {
        Long userId = UserContext.getUserId();
        messageService.removeGroupMember(userId, groupId, dto.getUserId());
        return Result.success(null);
    }

    @DeleteMapping("/group/disband/{groupId}")
    @RequireRole
    @Operation(summary = "解散群聊", description = "仅群主可以解散群聊，解散后所有成员将被移除")
    public Result<Void> disbandGroup(@PathVariable Long groupId) {
        Long userId = UserContext.getUserId();
        messageService.disbandGroup(userId, groupId);
        return Result.success(null);
    }

    // ==================== 会话列表 ====================

    @GetMapping("/conversations")
    @RequireRole
    @Operation(summary = "获取会话列表", description = "获取所有会话（私聊+群聊）的列表，按最后消息时间排序")
    public Result<List<ConversationSessionVO>> getConversationList() {
        Long userId = UserContext.getUserId();
        List<ConversationSessionVO> sessions = messageService.getConversationList(userId);
        return Result.success(sessions);
    }
}
