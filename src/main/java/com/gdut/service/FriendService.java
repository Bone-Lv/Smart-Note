package com.gdut.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gdut.domain.dto.friend.*;
import com.gdut.domain.entity.friend.Friend;
import com.gdut.domain.vo.friend.FriendGroupVO;
import com.gdut.domain.vo.friend.FriendRequestVO;
import com.gdut.domain.vo.friend.FriendVO;
import com.gdut.domain.vo.user.UserPublicVO;

import java.util.List;

public interface FriendService extends IService<Friend> {
    /**
     * 根据账号（邮箱/手机号）查找用户
     */
    UserPublicVO searchUserByAccount(String account);
    
    /**
     * 发送好友申请
     */
    void sendFriendRequest(Long userId, SendFriendRequestDTO dto);
    
    /**
     * 获取我收到的好友申请列表
     */
    List<FriendRequestVO> getReceivedRequests(Long userId);
    
    /**
     * 处理好友申请（同意/拒绝）
     */
    void handleFriendRequest(Long userId, HandleFriendRequestDTO dto);
    
    /**
     * 获取我的好友列表
     */
    List<FriendVO> getFriendList(Long userId, Long groupId);
    
    /**
     * 获取好友分组列表
     */
    List<FriendGroupVO> getFriendGroups(Long userId);
    
    /**
     * 创建好友分组
     */
    void createFriendGroup(Long userId, String groupName);
    
    /**
     * 删除好友分组
     */
    void deleteFriendGroup(Long userId, Long groupId);
    
    /**
     * 更新好友备注
     */
    void updateFriendRemark(Long userId, UpdateFriendRemarkDTO dto);
    
    /**
     * 移动好友到分组
     */
    void moveFriendToGroup(Long userId, MoveFriendToGroupDTO dto);
    
    /**
     * 删除好友
     */
    void deleteFriend(Long userId, Long friendUserId);
    
    /**
     * 获取用户默认分组ID（如果没有则创建）
     */
    Long getDefaultGroupId(Long userId);
}
