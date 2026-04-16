package com.gdut.controller;

import com.gdut.annotation.RequireRole;
import com.gdut.domain.dto.friend.*;
import com.gdut.domain.entity.common.Result;
import com.gdut.domain.vo.friend.FriendGroupVO;
import com.gdut.domain.vo.friend.FriendRequestVO;
import com.gdut.domain.vo.friend.FriendVO;
import com.gdut.domain.vo.user.UserPublicVO;
import com.gdut.service.FriendService;
import com.gdut.common.util.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/friend")
@RequiredArgsConstructor
@Tag(name = "好友管理接口", description = "好友申请、好友列表、分组管理等功能")
public class FriendController {

    private final FriendService friendService;

    @GetMapping("/search")
    @RequireRole
    @Operation(summary = "查找用户", description = "通过邮箱或手机号查找用户")
    public Result<UserPublicVO> searchUser(@RequestParam String account) {
        return Result.success(friendService.searchUserByAccount(account));
    }

    @PostMapping("/request")
    @RequireRole
    @Operation(summary = "发送好友申请", description = "向指定用户发送好友申请")
    public Result<Void> sendFriendRequest(@Valid @RequestBody SendFriendRequestDTO dto) {
        friendService.sendFriendRequest(UserContext.getUserId(), dto);
        return Result.success(null);
    }

    @GetMapping("/requests/received")
    @RequireRole
    @Operation(summary = "获取收到的好友申请", description = "查看收到的好友申请列表")
    public Result<List<FriendRequestVO>> getReceivedRequests() {
        return Result.success(friendService.getReceivedRequests(UserContext.getUserId()));
    }

    @PutMapping("/request/handle")
    @RequireRole
    @Operation(summary = "处理好友申请", description = "同意或拒绝好友申请")
    public Result<Void> handleFriendRequest(@Valid @RequestBody HandleFriendRequestDTO dto) {
        friendService.handleFriendRequest(UserContext.getUserId(), dto);
        return Result.success(null);
    }

    @GetMapping("/list")
    @RequireRole
    @Operation(summary = "获取好友列表", description = "获取当前用户的好友列表，可按分组筛选")
    public Result<List<FriendVO>> getFriendList(@RequestParam(required = false) Long groupId) {
        return Result.success(friendService.getFriendList(UserContext.getUserId(), groupId));
    }

    @GetMapping("/groups")
    @RequireRole
    @Operation(summary = "获取好友分组列表", description = "获取当前用户的所有好友分组")
    public Result<List<FriendGroupVO>> getFriendGroups() {
        return Result.success(friendService.getFriendGroups(UserContext.getUserId()));
    }

    @PostMapping("/group")
    @RequireRole
    @Operation(summary = "创建好友分组", description = "创建新的好友分组")
    public Result<Void> createFriendGroup(@RequestParam @NotBlank(message = "分组名称不能为空") 
                                          @Size(max = 50, message = "分组名称不能超过50个字符") String groupName) {
        friendService.createFriendGroup(UserContext.getUserId(), groupName);
        return Result.success(null);
    }

    @DeleteMapping("/group/{groupId}")
    @RequireRole
    @Operation(summary = "删除好友分组", description = "删除指定分组，分组下的好友会移到默认分组")
    public Result<Void> deleteFriendGroup(@PathVariable Long groupId) {
        friendService.deleteFriendGroup(UserContext.getUserId(), groupId);
        return Result.success(null);
    }

    @PutMapping("/remark")
    @RequireRole
    @Operation(summary = "更新好友备注", description = "修改好友的备注名")
    public Result<Void> updateFriendRemark(@Valid @RequestBody UpdateFriendRemarkDTO dto) {
        friendService.updateFriendRemark(UserContext.getUserId(), dto);
        return Result.success(null);
    }

    @PutMapping("/move")
    @RequireRole
    @Operation(summary = "移动好友到分组", description = "将好友移动到指定分组")
    public Result<Void> moveFriendToGroup(@Valid @RequestBody MoveFriendToGroupDTO dto) {
        friendService.moveFriendToGroup(UserContext.getUserId(), dto);
        return Result.success(null);
    }

    @DeleteMapping("/{friendUserId}")
    @RequireRole
    @Operation(summary = "删除好友", description = "删除指定好友")
    public Result<Void> deleteFriend(@PathVariable Long friendUserId) {
        friendService.deleteFriend(UserContext.getUserId(), friendUserId);
        return Result.success(null);
    }
}
