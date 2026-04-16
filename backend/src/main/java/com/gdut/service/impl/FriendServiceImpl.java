package com.gdut.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gdut.common.exception.BusinessException;
import com.gdut.common.enums.ResultCode;
import com.gdut.domain.dto.friend.*;
import com.gdut.domain.entity.friend.Friend;
import com.gdut.domain.entity.friend.FriendGroup;
import com.gdut.domain.entity.user.User;
import com.gdut.domain.vo.friend.FriendGroupVO;
import com.gdut.domain.vo.friend.FriendRequestVO;
import com.gdut.domain.vo.friend.FriendVO;
import com.gdut.domain.vo.user.UserPublicVO;
import com.gdut.common.enums.FriendStatus;
import com.gdut.mapper.FriendMapper;
import com.gdut.mapper.FriendGroupMapper;
import com.gdut.mapper.UserMapper;
import com.gdut.service.FriendService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FriendServiceImpl extends ServiceImpl<FriendMapper, Friend> implements FriendService {

    private final FriendGroupMapper friendGroupMapper;
    private final UserMapper userMapper;

    @Override
    public UserPublicVO searchUserByAccount(String account) {
        // 根据邮箱或手机号查找用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getEmail, account)
                .or()
                .eq(User::getPhone, account);
        User user = userMapper.selectOne(wrapper);
        
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        
        // 使用 Hutool 工具类进行属性复制，自动忽略目标类中不存在的字段（如邮箱、手机号）
        return BeanUtil.copyProperties(user, UserPublicVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendFriendRequest(Long userId, SendFriendRequestDTO dto) {
        // 1. 查找目标用户
        User targetUser = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, dto.getAccount())
                .or()
                .eq(User::getPhone, dto.getAccount()));
        
        if (targetUser == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        
        if (targetUser.getId().equals(userId)) {
            throw new BusinessException(ResultCode.SELF_FRIEND_FORBIDDEN);
        }
        
        // 2. 检查是否已经是好友或已有申请
        Friend existingFriend = getOne(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getUserId, userId)
                .eq(Friend::getFriendUserId, targetUser.getId()));
        
        if (existingFriend != null) {
            if (existingFriend.getStatus() == FriendStatus.ACCEPTED) {
                throw new BusinessException(ResultCode.FRIEND_ALREADY_EXISTS);
            } else if (existingFriend.getStatus() == FriendStatus.PENDING) {
                throw new BusinessException(ResultCode.FRIEND_REQUEST_PENDING);
            }
        }
        
        // 3. 获取或创建默认分组
        Long groupId = dto.getGroupId() != null ? dto.getGroupId() : getDefaultGroupId(userId);
        
        // 4. 如果之前有被拒绝的记录，更新状态；否则创建新记录
        if (existingFriend != null && existingFriend.getStatus() == FriendStatus.REJECTED) {
            existingFriend.setStatus(FriendStatus.PENDING);
            existingFriend.setGroupId(groupId);
            existingFriend.setRemark(dto.getRemark());
            existingFriend.setApplyMessage(dto.getApplyMessage());
            updateById(existingFriend);
        } else {
            // 5. 创建好友申请记录（双向记录）
            Friend friend1 = new Friend();
            friend1.setUserId(userId);
            friend1.setFriendUserId(targetUser.getId());
            friend1.setGroupId(groupId);
            friend1.setRemark(dto.getRemark());
            friend1.setStatus(FriendStatus.PENDING);
            friend1.setApplyMessage(dto.getApplyMessage());
            save(friend1);
            
            Friend friend2 = new Friend();
            friend2.setUserId(targetUser.getId());
            friend2.setFriendUserId(userId);
            friend2.setGroupId(null);
            friend2.setRemark(null);
            friend2.setStatus(FriendStatus.PENDING);
            friend2.setApplyMessage(dto.getApplyMessage());
            save(friend2);
        }
    }

    @Override
    public List<FriendRequestVO> getReceivedRequests(Long userId) {
        // 查询收到的好友申请
        List<Friend> friends = list(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getFriendUserId, userId)
                .eq(Friend::getStatus, FriendStatus.PENDING)
                .orderByDesc(Friend::getCreateTime));
        
        if (friends.isEmpty()) {
            return List.of();
        }
        
        // 获取申请人信息
        List<Long> userIds = friends.stream()
                .map(Friend::getUserId)
                .collect(Collectors.toList());
        
        List<User> users = userMapper.selectBatchIds(userIds);
        Map<Long, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        
        // 转换为VO
        return friends.stream().map(f -> {

            FriendRequestVO vo = BeanUtil.copyProperties(f, FriendRequestVO.class);
            
            User applicant = userMap.get(f.getUserId());
            if (applicant != null) {
                vo.setApplicantUsername(applicant.getUsername());
                vo.setApplicantAvatar(applicant.getAvatar());
            }
            
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleFriendRequest(Long userId, HandleFriendRequestDTO dto) {
        // 1. 查询好友申请记录
        Friend friend = getById(dto.getFriendId());
        if (friend == null) {
            throw new BusinessException(ResultCode.FRIEND_REQUEST_NOT_FOUND);
        }
        
        // 2. 校验是否是接收方
        if (!friend.getFriendUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权处理该申请");
        }
        
        // 3. 校验状态
        if (friend.getStatus() != FriendStatus.PENDING) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该申请已处理");
        }
        
        // 4. 查找反向记录
        Friend reverseFriend = getOne(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getUserId, friend.getFriendUserId())
                .eq(Friend::getFriendUserId, friend.getUserId()));
        
        if (dto.getAccept()) {
            // 同意好友申请
            friend.setStatus(FriendStatus.ACCEPTED);
            
            // 设置备注（如果传了的话）
            if (dto.getRemark() != null) {
                friend.setRemark(dto.getRemark());
            }
            
            // 设置分组（如果传了的话，否则使用默认分组）
            if (dto.getGroupId() != null) {
                friend.setGroupId(dto.getGroupId());
            } else if (friend.getGroupId() == null) {
                // 如果之前也没分组，给一个默认分组
                friend.setGroupId(getDefaultGroupId(userId));
            }
            
            updateById(friend);
            
            if (reverseFriend != null) {
                reverseFriend.setStatus(FriendStatus.ACCEPTED);
                // 反向记录也设置分组（如果传了的话）
                if (dto.getGroupId() != null) {
                    reverseFriend.setGroupId(getDefaultGroupId(friend.getFriendUserId()));
                }
                updateById(reverseFriend);
            }
        } else {
            // 拒绝好友申请
            friend.setStatus(FriendStatus.REJECTED);
            updateById(friend);
            
            if (reverseFriend != null) {
                reverseFriend.setStatus(FriendStatus.REJECTED);
                updateById(reverseFriend);
            }
        }
    }

    @Override
    public List<FriendVO> getFriendList(Long userId, Long groupId) {
        // 查询已通过的好友
        LambdaQueryWrapper<Friend> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Friend::getUserId, userId)
                .eq(Friend::getStatus, FriendStatus.ACCEPTED);
        
        if (groupId != null) {
            wrapper.eq(Friend::getGroupId, groupId);
        }
        
        List<Friend> friends = list(wrapper);
        
        if (friends.isEmpty()) {
            return List.of();
        }
        
        // 获取好友用户信息
        List<Long> friendUserIds = friends.stream()
                .map(Friend::getFriendUserId)
                .collect(Collectors.toList());
        // 把好友的用户信息存入map中
        List<User> users = userMapper.selectBatchIds(friendUserIds);
        var userMap = users.stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        
        // 获取分组信息
        List<Long> groupIds = friends.stream()
                .map(Friend::getGroupId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        // 批量获取分组信息
        Map<Long, FriendGroup> groupMap = groupIds.isEmpty() ? new HashMap<>() : 
                friendGroupMapper.selectBatchIds(groupIds).stream()
                        .collect(Collectors.toMap(FriendGroup::getId, g -> g));
        
        // 转换为VO
        return friends.stream().map(f -> {

            FriendVO vo = BeanUtil.copyProperties(f, FriendVO.class);
            // 获取集合里每一个朋友的user对象,把他们的用户名绑定出来
            User friendUser = userMap.get(f.getFriendUserId());
            if (friendUser != null) {
                vo.setFriendUsername(friendUser.getUsername());
                vo.setFriendAvatar(friendUser.getAvatar());
                vo.setFriendMotto(friendUser.getMotto());
            }
            
            if (f.getGroupId() != null) {
                FriendGroup group = groupMap.get(f.getGroupId());
                if (group != null) {
                    vo.setGroupName(group.getGroupName());
                }
            }
            
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<FriendGroupVO> getFriendGroups(Long userId) {
        // 获取当前用户的所有分组对象
        List<FriendGroup> groups = friendGroupMapper.selectList(
                new LambdaQueryWrapper<FriendGroup>()
                        .eq(FriendGroup::getUserId, userId)
                        .orderByAsc(FriendGroup::getSortOrder)
        );
        
        if (groups.isEmpty()) {
            return List.of();
        }
        
        // 统计每个分组的好友数量
        List<Friend> allFriends = list(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getUserId, userId)
                .eq(Friend::getStatus, FriendStatus.ACCEPTED));
        // 获取每个分组的好友数量
        var groupCountMap = allFriends.stream()
                .filter(f -> f.getGroupId() != null)
                .collect(Collectors.groupingBy(Friend::getGroupId, Collectors.counting()));
        
        // 转换为VO
        return groups.stream().map(g -> {
            FriendGroupVO vo = BeanUtil.copyProperties(g, FriendGroupVO.class);
            vo.setFriendCount(groupCountMap.getOrDefault(g.getId(), 0L).intValue());
            vo.setCreateTime(g.getCreateTime());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createFriendGroup(Long userId, String groupName) {
        // 检查分组名是否重复
        long count = friendGroupMapper.selectCount(
                new LambdaQueryWrapper<FriendGroup>()
                        .eq(FriendGroup::getUserId, userId)
                        .eq(FriendGroup::getGroupName, groupName)
        );
        
        if (count > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "分组名称已存在");
        }
        
        // 获取最大排序号
        FriendGroup maxSortGroup = friendGroupMapper.selectOne(
                new LambdaQueryWrapper<FriendGroup>()
                        .eq(FriendGroup::getUserId, userId)
                        .orderByDesc(FriendGroup::getSortOrder)
                        .last("LIMIT 1")
        );
        
        int sortOrder = maxSortGroup != null ? maxSortGroup.getSortOrder() + 1 : 1;
        
        FriendGroup group = new FriendGroup();
        group.setUserId(userId);
        group.setGroupName(groupName);
        group.setSortOrder(sortOrder);
        friendGroupMapper.insert(group);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFriendGroup(Long userId, Long groupId) {
        FriendGroup group = friendGroupMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException(ResultCode.FRIEND_GROUP_NOT_EXIST);
        }
        
        if (!group.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权删除该分组");
        }
        
        // 将分组下的好友移到默认分组
        Long defaultGroupId = getDefaultGroupId(userId);
        lambdaUpdate()
                .eq(Friend::getUserId, userId)
                .eq(Friend::getGroupId, groupId)
                .set(Friend::getGroupId, defaultGroupId)
                .update();
        
        // 删除分组
        friendGroupMapper.deleteById(groupId);
    }

    @Override
    public void updateFriendRemark(Long userId, UpdateFriendRemarkDTO dto) {
        // 检查好友是否存在
        Friend friend = getOne(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getUserId, userId)
                .eq(Friend::getFriendUserId, dto.getFriendUserId())
                .eq(Friend::getStatus, FriendStatus.ACCEPTED.getCode()));
        
        if (friend == null) {
            throw new BusinessException(ResultCode.FRIEND_NOT_EXIST);
        }
        // 更新好友备注
        lambdaUpdate()
                .eq(Friend::getId, friend.getId())
                .set(Friend::getRemark, dto.getRemark())
                .update();
    }

    @Override
    public void moveFriendToGroup(Long userId, MoveFriendToGroupDTO dto) {
        // 获取当前好友对象
        Friend friend = getOne(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getUserId, userId)
                .eq(Friend::getFriendUserId, dto.getFriendUserId())
                .eq(Friend::getStatus, FriendStatus.ACCEPTED.getCode()));
        
        if (friend == null) {
            throw new BusinessException(ResultCode.FRIEND_NOT_EXIST);
        }
        
        // 如果指定了分组，校验分组是否存在且属于当前用户
        if (dto.getGroupId() != null) {
            FriendGroup group = friendGroupMapper.selectById(dto.getGroupId());
            if (group == null || !group.getUserId().equals(userId)) {
                throw new BusinessException(ResultCode.FRIEND_GROUP_NOT_EXIST);
            }
        }
        // 更新好友分组
        lambdaUpdate()
                .eq(Friend::getId, friend.getId())
                .set(Friend::getGroupId, dto.getGroupId())
                .update();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFriend(Long userId, Long friendUserId) {
        // 查找当前用户的好友记录
        Friend friend = getOne(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getUserId, userId)
                .eq(Friend::getFriendUserId, friendUserId)
                .eq(Friend::getStatus, FriendStatus.ACCEPTED));
        
        if (friend == null) {
            throw new BusinessException(ResultCode.FRIEND_NOT_EXIST);
        }
        
        // 删除双向好友关系
        friend.setStatus(FriendStatus.DELETED);
        updateById(friend);
        
        // 查找并更新反向记录
        Friend reverseFriend = getOne(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getUserId, friendUserId)
                .eq(Friend::getFriendUserId, userId)
                .eq(Friend::getStatus, FriendStatus.ACCEPTED));
        
        if (reverseFriend != null) {
            reverseFriend.setStatus(FriendStatus.DELETED);
            updateById(reverseFriend);
        }
    }

    @Override
    public Long getDefaultGroupId(Long userId) {
        // 查找默认分组
        FriendGroup defaultGroup = friendGroupMapper.selectOne(
                new LambdaQueryWrapper<FriendGroup>()
                        .eq(FriendGroup::getUserId, userId)
                        .eq(FriendGroup::getGroupName, "默认")
        );
        
        if (defaultGroup == null) {
            // 创建默认分组
            defaultGroup = new FriendGroup();
            defaultGroup.setUserId(userId);
            defaultGroup.setGroupName("默认");
            defaultGroup.setSortOrder(0);
            friendGroupMapper.insert(defaultGroup);
        }
        
        return defaultGroup.getId();
    }
}
