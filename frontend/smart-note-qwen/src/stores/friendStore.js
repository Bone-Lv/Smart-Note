import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { 
  searchUserApi,
  sendFriendRequestApi,
  getReceivedRequestsApi,
  handleFriendRequestApi,
  getFriendListApi,
  getFriendGroupsApi,
  createFriendGroupApi,
  deleteFriendGroupApi,
  updateFriendRemarkApi,
  moveFriendToGroupApi,
  deleteFriendApi
} from '../api/friend.js';
import { ElMessage, ElMessageBox } from 'element-plus';

export const useFriendStore = defineStore('friend', () => {
  // 状态
  const friends = ref([]); // 好友列表
  const groups = ref([]); // 好友分组列表
  const receivedRequests = ref([]); // 收到的好友申请
  const selectedGroupId = ref(null); // 当前选中的分组ID
  const loading = ref(false); // 加载状态

  // 计算属性
  const totalFriends = computed(() => friends.value.length);
  
  const groupedFriends = computed(() => {
    const result = {};
    
    // 初始化所有分组
    groups.value.forEach(group => {
      result[group.id] = [];
    });
    
    // 将好友分配到对应分组
    friends.value.forEach(friend => {
      const groupId = friend.groupId || 'default';
      if (!result[groupId]) {
        result[groupId] = [];
      }
      result[groupId].push(friend);
    });
    
    return result;
  });

  const pendingRequestsCount = computed(() => receivedRequests.value.length);

  // ==================== 数据获取 ====================

  /**
   * 设置好友列表（用于从外部更新）
   */
  const setFriends = (friendsList) => {
    friends.value = friendsList || [];
  };

  /**
   * 设置好友分组列表（用于从外部更新）
   */
  const setGroups = (groupsList) => {
    groups.value = groupsList || [];
  };

  /**
   * 设置收到的好友申请（用于从外部更新）
   */
  const setReceivedRequests = (requestsList) => {
    receivedRequests.value = requestsList || [];
  };

  /**
   * 获取好友列表
   */
  const fetchFriends = async (groupId = null) => {
    try {
      loading.value = true;
      const result = await getFriendListApi(groupId);
      
      // 后端已在好友列表接口中返回onlineStatus字段
      // 格式可能是: 1=在线, 0=离线 或 'online'/'offline'
      friends.value = (result.data.data || []).map(friend => ({
        ...friend,
        // 统一转换为字符串格式，便于前端判断
        onlineStatus: friend.onlineStatus === 1 || friend.onlineStatus === 'online' 
          ? 'online' 
          : 'offline'
      }));
      
      console.log(' 好友列表加载完成:', friends.value.length, '个好友');
      console.log('  在线人数:', friends.value.filter(f => f.onlineStatus === 'online').length);
    } catch (error) {
      console.error('获取好友列表失败:', error);
      ElMessage.error('获取好友列表失败');
    } finally {
      loading.value = false;
    }
  };

  /**
   * 获取好友分组列表
   */
  const fetchGroups = async () => {
    try {
      loading.value = true;
      const result = await getFriendGroupsApi();
      groups.value = result.data.data || [];
    } catch (error) {
      console.error('获取好友分组失败:', error);
      ElMessage.error('获取好友分组失败');
    } finally {
      loading.value = false;
    }
  };

  /**
   * 获取收到的好友申请
   */
  const fetchReceivedRequests = async () => {
    try {
      loading.value = true;
      const result = await getReceivedRequestsApi();
      receivedRequests.value = result.data.data || [];
    } catch (error) {

    } finally {
      loading.value = false;
    }
  };

  /**
   * 刷新所有数据
   */
  const refreshAll = async () => {
    await Promise.all([
      fetchFriends(),
      fetchGroups(),
      fetchReceivedRequests()
    ]);
  };

  // ==================== 搜索与添加好友 ====================

  /**
   * 搜索用户
   */
  const searchUser = async (account) => {
    try {
      loading.value = true;
      const result = await searchUserApi(account);
      return result.data;
    } catch (error) {
      console.error('搜索用户失败:', error);
      ElMessage.error(error.response?.data?.msg || '搜索失败');
      throw error;
    } finally {
      loading.value = false;
    }
  };

  /**
   * 发送好友申请
   */
  const sendFriendRequest = async (targetUserId, message = '') => {
    try {
      loading.value = true;
      
      await sendFriendRequestApi({ targetUserId, message });
      
      ElMessage.success('好友申请已发送');
    } catch (error) {
      console.error('发送好友申请失败:', error);
      ElMessage.error(error.response?.data?.msg || '发送失败');
      throw error;
    } finally {
      loading.value = false;
    }
  };

  // ==================== 处理好友申请 ====================

  /**
   * 同意好友申请
   */
  const acceptRequest = async (requestId) => {
    try {
      loading.value = true;
      
      await handleFriendRequestApi({ requestId, approved: true });
      
      // 从申请列表中移除
      receivedRequests.value = receivedRequests.value.filter(r => r.id !== requestId);
      
      // 刷新好友列表
      await fetchFriends();
      
      ElMessage.success('已同意好友申请');
    } catch (error) {
      console.error('处理好友申请失败:', error);
      ElMessage.error(error.response?.data?.msg || '操作失败');
      throw error;
    } finally {
      loading.value = false;
    }
  };

  /**
   * 拒绝好友申请
   */
  const rejectRequest = async (requestId) => {
    try {
      loading.value = true;
      
      await handleFriendRequestApi({ requestId, approved: false });
      
      // 从申请列表中移除
      receivedRequests.value = receivedRequests.value.filter(r => r.id !== requestId);
      
      ElMessage.success('已拒绝好友申请');
    } catch (error) {
      console.error('处理好友申请失败:', error);
      ElMessage.error(error.response?.data?.msg || '操作失败');
      throw error;
    } finally {
      loading.value = false;
    }
  };

  // ==================== 好友分组管理 ====================

  /**
   * 创建好友分组
   */
  const createGroup = async (groupName) => {
    try {
      loading.value = true;
      
      await createFriendGroupApi(groupName);
      
      // 刷新分组列表
      await fetchGroups();
      
      ElMessage.success('分组创建成功');
    } catch (error) {
      console.error('创建分组失败:', error);
      ElMessage.error(error.response?.data?.msg || '创建失败');
      throw error;
    } finally {
      loading.value = false;
    }
  };

  /**
   * 删除好友分组
   */
  const deleteGroup = async (groupId) => {
    try {
      // 二次确认
      await ElMessageBox.confirm(
        '删除分组后，该分组下的好友将移到默认分组，确定继续吗？',
        '删除分组',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }
      );
      
      loading.value = true;
      
      await deleteFriendGroupApi(groupId);
      
      // 刷新分组和好友列表
      await Promise.all([fetchGroups(), fetchFriends()]);
      
      ElMessage.success('分组已删除');
    } catch (error) {
      if (error === 'cancel' || error === 'close') {
        return;
      }
      
      console.error('删除分组失败:', error);
      ElMessage.error(error.response?.data?.msg || '删除失败');
      throw error;
    } finally {
      loading.value = false;
    }
  };

  /**
   * 设置选中的分组
   */
  const setSelectedGroup = (groupId) => {
    selectedGroupId.value = groupId;
  };

  // ==================== 好友操作 ====================

  /**
   * 更新好友备注
   */
  const updateRemark = async (friendUserId, remark) => {
    try {
      loading.value = true;
      
      await updateFriendRemarkApi({ friendUserId, remark });
      
      // 更新本地列表
      const friend = friends.value.find(f => f.id === friendUserId);
      if (friend) {
        friend.remark = remark;
      }
      
      ElMessage.success('备注更新成功');
    } catch (error) {
      console.error('更新备注失败:', error);
      ElMessage.error(error.response?.data?.msg || '更新失败');
      throw error;
    } finally {
      loading.value = false;
    }
  };

  /**
   * 移动好友到分组
   */
  const moveFriend = async (friendUserId, groupId) => {
    try {
      loading.value = true;
      
      await moveFriendToGroupApi({ friendUserId, groupId });
      
      // 更新本地列表
      const friend = friends.value.find(f => f.id === friendUserId);
      if (friend) {
        friend.groupId = groupId;
      }
      
      ElMessage.success('移动成功');
    } catch (error) {
      console.error('移动好友失败:', error);
      ElMessage.error(error.response?.data?.msg || '移动失败');
      throw error;
    } finally {
      loading.value = false;
    }
  };

  /**
   * 删除好友
   */
  const deleteFriend = async (friendUserId) => {
    try {
      // 二次确认
      await ElMessageBox.confirm(
        '确定要删除该好友吗？删除后将无法恢复。',
        '删除好友',
        {
          confirmButtonText: '确定删除',
          cancelButtonText: '取消',
          type: 'warning',
          confirmButtonClass: 'el-button--danger'
        }
      );
      
      loading.value = true;
      
      await deleteFriendApi(friendUserId);
      
      // 从本地列表移除
      friends.value = friends.value.filter(f => f.id !== friendUserId);
      
      ElMessage.success('好友已删除');
    } catch (error) {
      if (error === 'cancel' || error === 'close') {
        return;
      }
      
      console.error('删除好友失败:', error);
      ElMessage.error(error.response?.data?.msg || '删除失败');
      throw error;
    } finally {
      loading.value = false;
    }
  };

  /**
   * 更新好友在线状态（WebSocket 推送）
   * @param {number|string} friendUserId - 好友用户ID
   * @param {string} status - 在线状态：'online' 或 'offline'
   */
  const updateFriendOnlineStatus = (friendUserId, status) => {
    console.log('📍 FriendStore.updateFriendOnlineStatus 被调用');
    console.log('   参数 friendUserId:', friendUserId, typeof friendUserId);
    console.log('   参数 status:', status);
    console.log('   当前好友总数:', friends.value.length);
    
    const friend = friends.value.find(f => f.friendUserId === friendUserId);
    
    if (friend) {
      console.log('   ✅ 找到好友:', friend.friendUsername || friend.remark);
      console.log('   更新前 onlineStatus:', friend.onlineStatus);
      friend.onlineStatus = status;
      console.log('   更新后 onlineStatus:', friend.onlineStatus);
      console.log(`🔄 更新好友在线状态: ${friend.friendUsername || friend.remark} -> ${status}`);
    } else {
      console.warn('   ❌ 未找到好友，friendUserId:', friendUserId);
      console.log('   当前所有好友的 friendUserId:', friends.value.map(f => f.friendUserId));
    }
  };

  /**
   * 批量更新好友在线状态（用于初始化）
   * @param {Array} statusList - 好友状态列表 [{friendUserId, onlineStatus}]
   */
  const batchUpdateOnlineStatus = (statusList) => {
    if (!statusList || !Array.isArray(statusList)) return;
    
    statusList.forEach(({ friendUserId, onlineStatus }) => {
      const friend = friends.value.find(f => f.friendUserId === friendUserId);
      if (friend) {
        friend.onlineStatus = onlineStatus === 1 || onlineStatus === 'online' 
          ? 'online' 
          : 'offline';
      }
    });
    
    console.log(`🔄 批量更新 ${statusList.length} 个好友的在线状态`);
  };

  return {
    // 状态
    friends,
    groups,
    receivedRequests,
    selectedGroupId,
    loading,
    totalFriends,
    groupedFriends,
    pendingRequestsCount,
    
    // 方法
    setFriends,
    setGroups,
    setReceivedRequests,
    fetchFriends,
    fetchGroups,
    fetchReceivedRequests,
    refreshAll,
    searchUser,
    sendFriendRequest,
    acceptRequest,
    rejectRequest,
    createGroup,
    deleteGroup,
    setSelectedGroup,
    updateRemark,
    moveFriend,
    deleteFriend,
    updateFriendOnlineStatus,
    batchUpdateOnlineStatus
  };
});
