<template>
  <div class="friend-list">
    <!-- 头部区域 -->
    <div class="list-header">
      <h2>好友列表</h2>
      <div class="header-actions">
        <el-button @click="showRequestsModal = true">
          <i class="fas fa-user-friends"></i>
          好友申请
          <span v-if="receivedRequests.length > 0" class="badge">{{ receivedRequests.length }}</span>
        </el-button>
        <el-button type="primary" @click="showAddFriendModal = true">
          <i class="fas fa-user-plus"></i>
          添加好友
        </el-button>
        <el-button @click="loadFriends" :loading="loading">
          <i class="fas fa-sync-alt"></i>
          刷新
        </el-button>
      </div>
    </div>

    <!-- 搜索区域 -->
    <div class="search-section">
      <div class="search-box">
        <i class="fas fa-search"></i>
        <input 
          v-model="searchKeyword" 
          type="text" 
          placeholder="搜索好友..."
          @input="searchFriends"
        />
      </div>
    </div>

    <!-- 内容区域 -->
    <div class="friends-content">
      <!-- 左侧分组列表 -->
      <div class="groups-section">
        <div class="section-header">
          <h3>好友分组</h3>
          <button class="add-group-btn" @click="showCreateGroupModal = true" title="创建分组">+</button>
        </div>
        <div class="group-list">
          <!-- 全部分组 -->
          <div 
            class="group-item" 
            :class="{ active: selectedGroupId === null }"
            @click="selectGroup(null)"
          >
            <i class="fas fa-users"></i>
            <span class="group-name">全部好友</span>
            <span class="friend-count">{{ totalFriendsCount }}</span>
          </div>
          
          <!-- 自定义分组 -->
          <div 
            v-for="group in friendGroups" 
            :key="group.id"
            class="group-item"
            :class="{ active: selectedGroupId === group.id }"
            @click="selectGroup(group.id)"
            @contextmenu.prevent="showGroupOptions($event, group)"
          >
            <i class="fas fa-folder"></i>
            <span class="group-name">{{ group.groupName }}</span>
            <span class="friend-count">{{ getGroupFriendCount(group.id) }}</span>
          </div>
        </div>
      </div>

      <!-- 右侧好友列表 -->
      <div class="friends-list">
        <!-- 加载状态 -->
        <div v-if="loading" class="loading-state">
          <div class="loading-spinner"></div>
          <p>加载中...</p>
        </div>

        <!-- 空状态 -->
        <div v-else-if="shouldShowEmpty" class="empty-state">
          <i class="fas fa-user-friends"></i>
          <p>暂无好友</p>
          <button class="add-first-btn" @click="showAddFriendModal = true">添加好友</button>
        </div>

        <!-- 好友列表 -->
        <div v-else class="friend-items">
          <div 
            v-for="friend in filteredFriends" 
            :key="friend.friendUserId"
            class="friend-item"
            @contextmenu.prevent="showFriendOptions($event, friend)"
          >
            <img 
              :src="friend.friendAvatar || defaultAvatar" 
              :alt="friend.remark || friend.friendUsername"
              class="friend-avatar"
              @error="handleAvatarError"
            />
            <div class="friend-info">
              <div class="friend-details">
                <span class="friend-name">{{ friend.remark || friend.friendUsername }}</span>
                <div class="friend-status">
                  <span 
                    class="status-dot" 
                    :class="friend.onlineStatus === 'online' ? 'online' : 'offline'"
                  ></span>
                  <span>{{ friend.onlineStatus === 'online' ? '在线' : '离线' }}</span>
                </div>
              </div>
              <div class="friend-motto" v-if="friend.friendMotto">{{ friend.friendMotto }}</div>
            </div>
            <div class="friend-actions">
              <button class="action-btn" @click.stop="startChat(friend)" title="发消息">
                <i class="fas fa-comment"></i>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 添加好友对话框 -->
    <el-dialog 
      v-model="showAddFriendModal" 
      title="添加好友" 
      width="450px"
      :close-on-click-modal="false"
    >
      <el-form label-width="80px">
        <el-form-item label="搜索">
          <div class="search-input-group">
            <input 
              v-model="searchAccount" 
              placeholder="请输入邮箱或手机号"
              @keyup.enter="searchUser"
            />
            <el-button type="primary" @click="searchUser" :loading="sendingRequest">
              搜索
            </el-button>
          </div>
        </el-form-item>

        <!-- 搜索结果 -->
        <div v-if="searchedUser" class="searched-user-info">
          <div class="user-card">
            <img 
              :src="searchedUser.avatar || defaultAvatar" 
              class="user-avatar"
              @error="handleAvatarError"
            />
            <div class="user-details">
              <h4>{{ searchedUser.username }}</h4>
              <p class="motto">{{ searchedUser.motto || '这个人很懒，什么都没写~' }}</p>
            </div>
          </div>
          <el-form-item label="备注">
            <input v-model="friendRemark" placeholder="请输入备注（可选）" />
          </el-form-item>
          <el-form-item label="分组">
            <select v-model="friendGroupId">
              <option :value="null">默认分组</option>
              <option v-for="group in friendGroups" :key="group.id" :value="group.id">
                {{ group.groupName }}
              </option>
            </select>
          </el-form-item>
          <el-form-item label="验证">
            <textarea 
              v-model="applyMessage" 
              placeholder="请输入验证消息（可选）"
              maxlength="50"
              rows="3"
            ></textarea>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="sendFriendRequest" :loading="sendingRequest">
              发送申请
            </el-button>
            <el-button @click="clearSearch">取消</el-button>
          </el-form-item>
        </div>
      </el-form>
    </el-dialog>

    <!-- 创建分组对话框 -->
    <el-dialog 
      v-model="showCreateGroupModal" 
      title="创建分组" 
      width="400px"
      :close-on-click-modal="false"
    >
      <el-form label-width="80px">
        <el-form-item label="分组名称">
          <input v-model="newGroupName" placeholder="请输入分组名称" @keyup.enter="createFriendGroup" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="createFriendGroup" :loading="creatingGroup">
            创建
          </el-button>
          <el-button @click="showCreateGroupModal = false">取消</el-button>
        </el-form-item>
      </el-form>
    </el-dialog>

    <!-- 好友申请对话框 -->
    <el-dialog 
      v-model="showRequestsModal" 
      title="好友申请" 
      width="600px"
      :close-on-click-modal="false"
    >
      <div class="requests-list">
        <div v-if="receivedRequests.length === 0" class="no-requests">
          <i class="fas fa-inbox"></i>
          <p>暂无好友申请</p>
        </div>
        <div v-else v-for="request in receivedRequests" :key="request.id" class="request-item">
          <div class="request-info">
            <img 
              :src="request.avatar || defaultAvatar" 
              class="request-avatar"
              @error="handleAvatarError"
            />
            <div class="request-details">
              <h4>{{ request.username }}</h4>
              <p>{{ request.message || '请求添加您为好友' }}</p>
              <span class="request-time">{{ request.applyTime }}</span>
            </div>
          </div>
          <div class="request-actions">
            <el-button type="primary" size="small" @click="handleFriendRequest(request.id, true)">
              同意
            </el-button>
            <el-button size="small" @click="handleFriendRequest(request.id, false)">
              拒绝
            </el-button>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- 右键菜单：好友选项 -->
    <div 
      v-if="showFriendOptionsPanel" 
      class="options-panel"
      :style="optionsPanelStyle"
      @click.stop
    >
      <div class="options-item" @click="showFriendDetail">
        <i class="fas fa-info-circle"></i>
        查看详情
      </div>
      <div class="options-item" @click="startChat(currentFriend)">
        <i class="fas fa-comment"></i>
        发消息
      </div>
      <div class="options-item" @click="showRemarkDialog">
        <i class="fas fa-edit"></i>
        修改备注
      </div>
      <div class="options-item" @click="showMoveGroupDialog">
        <i class="fas fa-folder-open"></i>
        移动分组
      </div>
      <div class="options-item danger" @click="removeFriend">
        <i class="fas fa-trash-alt"></i>
        删除好友
      </div>
    </div>

    <!-- 右键菜单：分组选项 -->
    <div 
      v-if="showGroupOptionsPanel" 
      class="options-panel"
      :style="groupOptionsPanelStyle"
      @click.stop
    >
      <div class="options-item" @click="renameGroup">
        <i class="fas fa-edit"></i>
        重命名
      </div>
      <div class="options-item danger" @click="deleteGroup">
        <i class="fas fa-trash-alt"></i>
        删除分组
      </div>
    </div>

    <!-- 遮罩层 -->
    <div 
      v-if="showFriendOptionsPanel || showGroupOptionsPanel" 
      class="overlay"
      @click="closePanels"
    ></div>

    <!-- 修改备注对话框 -->
    <el-dialog 
      v-model="showRemarkModal" 
      title="修改备注" 
      width="450px"
      :close-on-click-modal="false"
    >
      <el-form label-width="80px">
        <el-form-item label="备注">
          <input 
            v-model="newRemark" 
            placeholder="请输入备注"
            @keyup.enter="saveRemark"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="saveRemark">保存</el-button>
          <el-button @click="showRemarkModal = false">取消</el-button>
        </el-form-item>
      </el-form>
    </el-dialog>

    <!-- 移动分组对话框 -->
    <el-dialog 
      v-model="showMoveGroupModal" 
      title="移动分组" 
      width="400px"
      :close-on-click-modal="false"
    >
      <el-form label-width="80px">
        <el-form-item label="选择分组">
          <select v-model="selectedMoveGroup">
            <option :value="null">默认分组</option>
            <option v-for="group in friendGroups" :key="group.id" :value="group.id">
              {{ group.groupName }}
            </option>
          </select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="moveFriendToGroup">确定</el-button>
          <el-button @click="showMoveGroupModal = false">取消</el-button>
        </el-form-item>
      </el-form>
    </el-dialog>

    <!-- 好友详细信息对话框 -->
    <el-dialog 
      v-model="showFriendDetailModal" 
      title="好友详情" 
      width="600px"
      :close-on-click-modal="false"
    >
      <div v-if="currentFriend" class="friend-detail">
        <div class="detail-header">
          <img 
            :src="currentFriend.friendAvatar || defaultAvatar" 
            :alt="currentFriend.remark || currentFriend.friendUsername"
            class="detail-avatar"
            @error="handleAvatarError"
          />
          <div class="detail-basic-info">
            <h3>{{ currentFriend.remark || currentFriend.friendUsername }}</h3>
            <div class="detail-status">
              <span 
                class="status-dot" 
                :class="currentFriend.onlineStatus === 1 || currentFriend.onlineStatus === 'online' ? 'online' : 'offline'"
              ></span>
              <span>{{ (currentFriend.onlineStatus === 1 || currentFriend.onlineStatus === 'online') ? '在线' : '离线' }}</span>
            </div>
            <div class="detail-group" v-if="currentFriend.groupName">
              <i class="fas fa-folder"></i>
              <span>{{ currentFriend.groupName }}</span>
            </div>
          </div>
        </div>
        
        <div class="detail-body">
          <!-- 备注 -->
          <div class="detail-item" v-if="currentFriend.remark">
            <label>
              <i class="fas fa-tag"></i>
              备注
            </label>
            <div class="item-content">
              <span class="remark-text">{{ currentFriend.remark }}</span>
              <el-button 
                type="text" 
                size="small" 
                @click="editRemark"
                class="edit-btn"
              >
                <i class="fas fa-edit"></i> 编辑
              </el-button>
            </div>
          </div>
          
          <!-- 用户名 -->
          <div class="detail-item">
            <label>
              <i class="fas fa-user"></i>
              用户名
            </label>
            <span>{{ currentFriend.friendUsername || '未知' }}</span>
          </div>
          
          <!-- 座右铭 -->
          <div class="detail-item" v-if="currentFriend.friendMotto">
            <label>
              <i class="fas fa-quote-left"></i>
              座右铭
            </label>
            <span class="motto-text">{{ currentFriend.friendMotto }}</span>
          </div>
          
          <!-- 好友ID -->
          <div class="detail-item">
            <label>
              <i class="fas fa-fingerprint"></i>
              好友ID
            </label>
            <span class="id-text">{{ currentFriend.friendUserId }}</span>
          </div>
          
          <!-- 好友状态 -->
          <div class="detail-item" v-if="currentFriend.status">
            <label>
              <i class="fas fa-info-circle"></i>
              好友状态
            </label>
            <span>{{ currentFriend.status }}</span>
          </div>
          
          <!-- 添加时间 -->
          <div class="detail-item">
            <label>
              <i class="fas fa-calendar-plus"></i>
              添加时间
            </label>
            <span>{{ formatDateTime(currentFriend.createTime) }}</span>
          </div>
          
          <!-- 空状态提示 -->
          <div v-if="!currentFriend.remark && !currentFriend.friendMotto" class="empty-info">
            <i class="fas fa-info-circle"></i>
            <p>该好友暂无备注和座右铭</p>
          </div>
        </div>
        
        <div class="detail-footer">
          <el-button type="primary" @click="startChat(currentFriend)">
            <i class="fas fa-comment"></i> 发消息
          </el-button>
          <el-button @click="showFriendDetailModal = false">
            <i class="fas fa-times"></i> 关闭
          </el-button>
        </div>
      </div>
    </el-dialog>

  </div>
</template>

<script>
import { ref, computed, onMounted, onUnmounted, onActivated } from 'vue';
import { useRouter } from 'vue-router';
import { useUserStore } from '../../stores/userStore.js';
import { useFriendStore } from '../../stores/friendStore.js';
import { ElMessage, ElMessageBox } from 'element-plus';
import { 
  searchUserApi, 
  sendFriendRequestApi, 
  getFriendListApi,
  getFriendGroupsApi,
  getReceivedRequestsApi,
  handleFriendRequestApi,
  createFriendGroupApi,
  deleteFriendGroupApi,
  updateFriendRemarkApi,
  moveFriendToGroupApi,
  deleteFriendApi
} from '../../api/friend.js';

export default {
  name: 'FriendList',
  setup() {
    const router = useRouter();
    const userStore = useUserStore();
    const friendStore = useFriendStore();

    // ========== 状态管理 ==========
    const friends = ref([]);
    const friendGroups = ref([]);
    const receivedRequests = ref([]);
    const loading = ref(false);
    const sendingRequest = ref(false);
    const creatingGroup = ref(false);

    // 搜索相关
    const searchKeyword = ref('');
    const searchAccount = ref('');
    const searchedUser = ref(null);

    // 添加好友相关
    const friendRemark = ref('');
    const friendGroupId = ref(null);
    const applyMessage = ref('');

    // 分组相关
    const newGroupName = ref('');
    const selectedGroupId = ref(null);

    // 备注和移动分组相关
    const newRemark = ref('');
    const selectedMoveGroup = ref(null);
    const showFriendDetailModal = ref(false);

    // 模态框控制
    const showAddFriendModal = ref(false);
    const showCreateGroupModal = ref(false);
    const showRequestsModal = ref(false);
    const showRemarkModal = ref(false);
    const showMoveGroupModal = ref(false);
    const showFriendOptionsPanel = ref(false);
    const showGroupOptionsPanel = ref(false);

    // 右键菜单相关
    const currentFriend = ref(null);
    const currentGroup = ref(null);
    const optionsPanelStyle = ref({});
    const groupOptionsPanelStyle = ref({});

    // 默认头像
    const defaultAvatar = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAiIGhlaWdodD0iNDAiIHZpZXdCb3g9IjAgMCA0MCA0MCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iNDAiIGhlaWdodD0iNDAiIGZpbGw9IiNFOEVDRjEiLz48cGF0aCBkPSJNMjAgMjBDITIyLjIwOTEgMjAgLTI0IDIyLjIwOTEgLTI0IDI0LjI0Qy0yNCAyNi4yNzA5LTIyLjIwOTEgMjggLTIwIDI4Qy0xNy43OTA5IDI4IC0xNiAyNi4yNzA5IC0xNiAyNC4yNEMtMTYgMjIuMjA5MSAtMTcuNzkwOSAyMCAtMjAgMjBaTTAgMzJDMC4wMjU2NzIzIDMyIDAuMDUxMzE2NSAzMS45OTg4IDAuMDc2OTMzIDMxLjk5NjFDOC43NjEyNyAzMS43MzM3IDE1Ljc2MzYgMjcuMTY4MSAxOS4wNjA5IDIwLjM5NDdDMjAuMzQ2OCAxNy44MTQzIDIwLjM0NjggMTQuODEzNyAxOS4wNjA5IDEyLjIzMzNDMTUuNzYzNiA1LjQ1OTg4IDguNzYxMjcgMC44OTQzNDIgMC4wNzY5MzMgMC42MzE5NjdDMC4wNTEzMTY1IDAuNjI5MjM2IDAuMDI1NjcyMyAwLjYyNzk2NiAwIDAuNjI1Qy0wLjAyNTY3MjMgMC42Mjc5NjYgLTAuMDUxMzE2NSAwLjYyOTIzNiAtMC4wNzY5MzMgMC42MzE5NjdDLTguNzYxMjcgMC44OTQzNDIgLTE1Ljc2MzYgNS40NTk4OCAtMTkuMDYwOSAxMi4yMzMzQy0yMC4zNDY4IDE0LjgxMzcgLTIwLjM0NjggMTcuODE0MyAtMTkuMDYwOSAyMC4zOTQ3Qy0xNS43NjM2IDI3LjE2ODEgLTguNzYxMjcgMzEuNzMzNyAtMC4wNzY5MzMgMzEuOTk2MUMtMC4wNTEzMTY1IDMxLjk5ODggLTAuMDI1NjcyMyAzMiAwIDMyWiIgZmlsbD0iI0IwQjhDMiIvPjwvc3ZnPg==';

    // ========== 计算属性 ==========
    const filteredFriends = computed(() => {
      let filtered = friends.value;
      
      // 按分组筛选
      if (selectedGroupId.value !== null) {
        filtered = filtered.filter(f => {
          // 兼容处理：groupId 可能是 null、undefined 或字符串
          const friendGroupId = f.groupId !== undefined && f.groupId !== null ? f.groupId : null;
          return friendGroupId === selectedGroupId.value;
        });
      }
      
      // 按关键词搜索
      if (searchKeyword.value.trim()) {
        const keyword = searchKeyword.value.trim().toLowerCase();
        filtered = filtered.filter(f => 
          (f.friendUsername && f.friendUsername.toLowerCase().includes(keyword)) ||
          (f.remark && f.remark.toLowerCase().includes(keyword))
        );
      }
      
      return filtered;
    });

    // 全部好友的总数（未过滤）
    const totalFriendsCount = computed(() => {
      return friends.value.length;
    });

    const shouldShowEmpty = computed(() => {
      return !loading.value && filteredFriends.value.length === 0;
    });

    // ========== 方法 ==========

    /**
     * 加载好友数据
     */
    const loadFriends = async () => {
      console.log('📦 loadFriends 函数被调用');
      
      try {
        loading.value = true;
        
        // 加载好友列表
        console.log('🔄 开始调用 friendStore.fetchFriends()');
        await friendStore.fetchFriends(selectedGroupId.value);
        friends.value = friendStore.friends;
        console.log('✅ 好友列表加载完成，共', friends.value.length, '个好友');
        
        // 🔍 调试：打印第一个好友的完整数据结构
        if (friends.value.length > 0) {
          console.log('🔍 第一个好友的完整数据:', friends.value[0]);
          console.log('🔍 所有字段名:', Object.keys(friends.value[0]));
        }
        
        // 加载分组列表
        console.log('🔄 开始加载分组列表');
        await friendStore.fetchGroups();
        friendGroups.value = friendStore.groups;
        console.log('✅ 分组列表加载完成，共', friendGroups.value.length, '个分组');
        
        // 加载好友申请
        console.log('🔄 开始加载好友申请');
        await friendStore.fetchReceivedRequests();
        receivedRequests.value = friendStore.receivedRequests;
        console.log('✅ 好友申请加载完成，共', receivedRequests.value.length, '个申请');
        
        console.log('🎉 所有数据加载完成！');
      } catch (error) {
        console.error('加载好友数据失败:', error);
        ElMessage.error('加载好友数据失败');
      } finally {
        loading.value = false;
        console.log('✅ Loading 状态已重置为', loading.value);
      }
    };

    /**
     * 搜索好友
     */
    const searchFriends = () => {
      console.log('搜索关键词:', searchKeyword.value);
    };

    /**
     * 选择分组
     */
    const selectGroup = (groupId) => {
      selectedGroupId.value = groupId;
      closePanels();
    };

    /**
     * 获取分组的好友数量
     */
    const getGroupFriendCount = (groupId) => {
      const count = friends.value.filter(f => {
        // 兼容处理：groupId 可能是 null、undefined 或字符串
        const friendGroupId = f.groupId !== undefined && f.groupId !== null ? f.groupId : null;
        return friendGroupId === groupId;
      }).length;
      
      console.log(`📊 分组 "${groupId}" 的好友数量:`, count);
      return count;
    };

    /**
     * 显示好友右键菜单
     */
    const showFriendOptions = (event, friend) => {
      event.preventDefault();
      currentFriend.value = friend;
      optionsPanelStyle.value = {
        top: event.clientY + 'px',
        left: event.clientX + 'px'
      };
      showFriendOptionsPanel.value = true;
      showGroupOptionsPanel.value = false;
    };

    /**
     * 显示分组右键菜单
     */
    const showGroupOptions = (event, group) => {
      event.preventDefault();
      currentGroup.value = group;
      groupOptionsPanelStyle.value = {
        top: event.clientY + 'px',
        left: event.clientX + 'px'
      };
      showGroupOptionsPanel.value = true;
      showFriendOptionsPanel.value = false;
    };

    /**
     * 关闭所有面板
     */
    const closePanels = () => {
      showFriendOptionsPanel.value = false;
      showGroupOptionsPanel.value = false;
      currentFriend.value = null;
      currentGroup.value = null;
    };

    /**
     * 开始聊天
     */
    const startChat = (friend) => {
      router.push(`/app/chat/private/${friend.friendUserId}`);
      closePanels();
    };

    /**
     * 显示好友详细信息
     */
    const showFriendDetail = () => {
      showFriendDetailModal.value = true;
      showFriendOptionsPanel.value = false;
    };

    /**
     * 编辑备注（从详情页面）
     */
    const editRemark = () => {
      newRemark.value = currentFriend.value.remark || '';
      showFriendDetailModal.value = false;
      showRemarkModal.value = true;
    };

    /**
     * 显示修改备注对话框
     */
    const showRemarkDialog = () => {
      newRemark.value = currentFriend.value.remark || '';
      showRemarkModal.value = true;
      showFriendOptionsPanel.value = false;
    };

    /**
     * 保存备注
     */
    const saveRemark = async () => {
      try {
        await updateFriendRemarkApi({
          friendUserId: currentFriend.value.friendUserId,
          remark: newRemark.value
        });
        
        // 更新本地数据
        currentFriend.value.remark = newRemark.value;
        
        // 同步到friends列表
        const friendInList = friends.value.find(f => f.friendUserId === currentFriend.value.friendUserId);
        if (friendInList) {
          friendInList.remark = newRemark.value;
        }
        
        ElMessage.success('备注已更新');
        showRemarkModal.value = false;
        
        // 如果从详情页打开的，重新打开详情页
        if (!showFriendDetailModal.value) {
          showFriendDetailModal.value = true;
        }
      } catch (error) {
        console.error('更新备注失败:', error);
        ElMessage.error('更新备注失败');
      }
    };

    /**
     * 格式化日期时间
     */
    const formatDateTime = (dateTime) => {
      if (!dateTime) return '未知';
      
      // 如果是ISO字符串，直接返回
      if (typeof dateTime === 'string' && dateTime.includes('T')) {
        return dateTime.replace('T', ' ').substring(0, 19);
      }
      
      return dateTime;
    };

    /**
     * 显示移动分组对话框
     */
    const showMoveGroupDialog = () => {
      selectedMoveGroup.value = currentFriend.value.groupId || null;
      showMoveGroupModal.value = true;
      showFriendOptionsPanel.value = false;
    };

    /**
     * 移动好友到分组
     */
    const moveFriendToGroup = async () => {
      try {
        await moveFriendToGroupApi({
          friendUserId: currentFriend.value.friendUserId,
          groupId: selectedMoveGroup.value
        });
        
        currentFriend.value.groupId = selectedMoveGroup.value;
        ElMessage.success('好友已移动到新分组');
        showMoveGroupModal.value = false;
      } catch (error) {
        console.error('移动好友失败:', error);
        ElMessage.error('移动好友失败');
      }
    };

    /**
     * 删除好友
     */
    const removeFriend = async () => {
      try {
        await ElMessageBox.confirm(
          '确定要删除该好友吗？删除后将无法恢复。',
          '删除好友',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }
        );
        
        await deleteFriendApi(currentFriend.value.friendUserId);
        
        friends.value = friends.value.filter(f => f.friendUserId !== currentFriend.value.friendUserId);
        ElMessage.success('好友已删除');
        closePanels();
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除好友失败:', error);
          ElMessage.error('删除好友失败');
        }
      }
    };

    /**
     * 搜索用户
     */
    const searchUser = async () => {
      if (!searchAccount.value.trim()) {
        ElMessage.warning('请输入邮箱或手机号');
        return;
      }
      
      try {
        sendingRequest.value = true;
        const result = await searchUserApi(searchAccount.value.trim());
        const userData = result.data.data;
        
        // 字段映射：确保数据结构一致
        searchedUser.value = userData ? {
          userId: userData.userId || userData.friendUserId,
          username: userData.username || userData.friendUsername,
          avatar: userData.avatar || userData.friendAvatar,
          motto: userData.motto || userData.friendMotto
        } : null;
        
        if (!searchedUser.value) {
          ElMessage.warning('未找到该用户');
        }
      } catch (error) {
        console.error('搜索用户失败:', error);
        ElMessage.error('搜索用户失败');
      } finally {
        sendingRequest.value = false;
      }
    };

    /**
     * 发送好友申请
     */
    const sendFriendRequest = async () => {
      if (!searchAccount.value.trim()) {
        ElMessage.warning('请输入邮箱或手机号');
        return;
      }
      
      try {
        sendingRequest.value = true;
        await sendFriendRequestApi({
          targetUserId: searchedUser.value.userId || searchedUser.value.friendUserId,
          message: applyMessage.value,
          remark: friendRemark.value
        });
        
        ElMessage.success('好友申请已发送');
        showAddFriendModal.value = false;
        clearSearch();
      } catch (error) {
        console.error('发送好友申请失败:', error);
        ElMessage.error('发送好友申请失败');
      } finally {
        sendingRequest.value = false;
      }
    };

    /**
     * 清空搜索
     */
    const clearSearch = () => {
      searchAccount.value = '';
      searchedUser.value = null;
      friendRemark.value = '';
      applyMessage.value = '';
      friendGroupId.value = null;
    };

    /**
     * 创建好友分组
     */
    const createFriendGroup = async () => {
      if (!newGroupName.value.trim()) {
        ElMessage.warning('请输入分组名称');
        return;
      }
      
      try {
        creatingGroup.value = true;
        await createFriendGroupApi(newGroupName.value.trim());
        
        ElMessage.success('分组创建成功');
        showCreateGroupModal.value = false;
        newGroupName.value = '';
        
        await friendStore.fetchGroups();
        friendGroups.value = friendStore.groups;
      } catch (error) {
        console.error('创建分组失败:', error);
        ElMessage.error('创建分组失败');
      } finally {
        creatingGroup.value = false;
      }
    };

    /**
     * 处理好友申请
     */
    const handleFriendRequest = async (requestId, approved) => {
      try {
        await handleFriendRequestApi({
          requestId,
          approved
        });
        
        ElMessage.success(approved ? '已同意好友申请' : '已拒绝好友申请');
        
        await friendStore.fetchReceivedRequests();
        receivedRequests.value = friendStore.receivedRequests;
        
        if (approved) {
          await friendStore.fetchFriends(selectedGroupId.value);
          friends.value = friendStore.friends;
        }
      } catch (error) {
        console.error('处理好友申请失败:', error);
        ElMessage.error('处理好友申请失败');
      }
    };

    /**
     * 重命名分组
     */
    const renameGroup = async () => {
      try {
        const { value } = await ElMessageBox.prompt('请输入新分组名称', '重命名分组', {
          confirmButtonText: '保存',
          cancelButtonText: '取消',
          inputValue: currentGroup.value.groupName
        });
        
        if (!value.trim()) {
          ElMessage.warning('分组名称不能为空');
          return;
        }
        
        ElMessage.info('重命名功能需要后端支持');
        showGroupOptionsPanel.value = false;
      } catch (error) {
        if (error !== 'cancel') {
          console.error('重命名分组失败:', error);
          ElMessage.error('重命名分组失败');
        }
      }
    };

    /**
     * 删除分组
     */
    const deleteGroup = async () => {
      try {
        await ElMessageBox.confirm(
          '删除分组后，该分组下的好友将移到默认分组，确定继续吗？',
          '删除分组',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }
        );
        
        await deleteFriendGroupApi(currentGroup.value.id);
        
        ElMessage.success('分组已删除');
        
        await friendStore.fetchGroups();
        friendGroups.value = friendStore.groups;
        
        if (selectedGroupId.value === currentGroup.value.id) {
          selectedGroupId.value = null;
        }
        
        showGroupOptionsPanel.value = false;
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除分组失败:', error);
          ElMessage.error('删除分组失败');
        }
      }
    };

    /**
     * 处理头像加载失败
     */
    const handleAvatarError = (event) => {
      event.target.src = defaultAvatar;
    };

    // ========== 生命周期钩子 ==========
    onMounted(async () => {
      console.log('🚀 [FriendList] onMounted - 开始加载好友数据');
      await loadFriends();
    });

    onActivated(async () => {
      console.log('⚡ [FriendList] onActivated - 重新加载好友数据');
      await loadFriends();
    });

    onUnmounted(() => {
      console.log('👋 [FriendList] onUnmounted - 组件已卸载');
      closePanels();
    });

    // ========== 返回 ==========
    return {
      friends,
      friendGroups,
      receivedRequests,
      loading,
      sendingRequest,
      creatingGroup,
      searchKeyword,
      searchAccount,
      searchedUser,
      friendRemark,
      friendGroupId,
      applyMessage,
      newGroupName,
      newRemark,
      selectedMoveGroup,
      selectedGroupId,
      showAddFriendModal,
      showCreateGroupModal,
      showRequestsModal,
      showRemarkModal,
      showMoveGroupModal,
      showFriendDetailModal,
      showFriendOptionsPanel,
      showGroupOptionsPanel,
      currentFriend,
      currentGroup,
      optionsPanelStyle,
      groupOptionsPanelStyle,
      defaultAvatar,
      totalFriendsCount,
      filteredFriends,
      shouldShowEmpty,
      loadFriends,
      searchFriends,
      selectGroup,
      getGroupFriendCount,
      showFriendOptions,
      showGroupOptions,
      closePanels,
      startChat,
      showFriendDetail,
      editRemark,
      showRemarkDialog,
      saveRemark,
      formatDateTime,
      showMoveGroupDialog,
      moveFriendToGroup,
      removeFriend,
      searchUser,
      sendFriendRequest,
      clearSearch,
      createFriendGroup,
      handleFriendRequest,
      renameGroup,
      deleteGroup,
      handleAvatarError
    };
  }
};
</script>

<style scoped>
/* ========== 主容器 ========== */
.friend-list {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
  border-radius: 12px;
  overflow: hidden;
}

/* ========== 头部区域 ========== */
.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  background: white;
  border-bottom: 2px solid #e8ecf1;
}

.list-header h2 {
  margin: 0;
  color: #1a1a1a;
  font-size: 24px;
  font-weight: 600;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.badge {
  background: #f85149;
  color: white;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 12px;
  margin-left: 6px;
  font-weight: 600;
}

/* ========== 搜索区域 ========== */
.search-section {
  padding: 16px 24px;
  background: white;
  border-bottom: 1px solid #e8ecf1;
}

.search-box {
  position: relative;
}

.search-box i {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  color: #656d76;
  font-size: 16px;
}

.search-box input {
  width: 100%;
  padding: 12px 16px 12px 42px;
  border: 2px solid #d0d7de;
  border-radius: 10px;
  font-size: 14px;
  outline: none;
  transition: all 0.2s ease;
  background: #f6f8fa;
}

.search-box input:focus {
  border-color: #0969da;
  background: white;
  box-shadow: 0 0 0 4px rgba(9, 105, 218, 0.1);
}

.search-box input::placeholder {
  color: #8b949e;
}

/* ========== 内容区域 ========== */
.friends-content {
  flex: 1;
  display: flex;
  overflow: hidden;
}

/* ========== 分组侧边栏 ========== */
.groups-section {
  width: 260px;
  background: white;
  border-right: 2px solid #e8ecf1;
  display: flex;
  flex-direction: column;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #e8ecf1;
}

.section-header h3 {
  margin: 0;
  font-size: 16px;
  color: #1a1a1a;
  font-weight: 600;
}

.add-group-btn {
  background: none;
  border: 2px solid #0969da;
  color: #0969da;
  cursor: pointer;
  padding: 6px 10px;
  font-size: 16px;
  font-weight: 700;
  border-radius: 6px;
  transition: all 0.2s ease;
}

.add-group-btn:hover {
  background: #0969da;
  color: white;
}

.group-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.group-item {
  display: flex;
  align-items: center;
  padding: 12px 14px;
  margin-bottom: 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 2px solid transparent;
}

.group-item:hover {
  background: #f6f8fa;
  border-color: #d0d7de;
}

.group-item.active {
  background: #ddf4ff;
  border-color: #0969da;
}

.group-item i {
  color: #656d76;
  margin-right: 10px;
  font-size: 16px;
}

.group-item.active i {
  color: #0969da;
}

.group-name {
  flex: 1;
  font-size: 14px;
  color: #24292f;
  font-weight: 500;
}

.friend-count {
  font-size: 12px;
  color: #656d76;
  background: #f6f8fa;
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: 600;
}

/* ========== 好友列表区域 ========== */
.friends-list {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: #f6f8fa;
}

.loading-state,
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 400px;
  color: #656d76;
}

.loading-spinner {
  width: 48px;
  height: 48px;
  border: 4px solid #e8ecf1;
  border-top: 4px solid #0969da;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.empty-state i {
  font-size: 64px;
  margin-bottom: 20px;
  color: #d0d7de;
}

.empty-state p {
  font-size: 18px;
  margin-bottom: 20px;
  font-weight: 500;
}

.add-first-btn {
  padding: 12px 24px;
  background: #0969da;
  color: white;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  font-size: 15px;
  font-weight: 600;
  transition: all 0.2s ease;
}

.add-first-btn:hover {
  background: #0550ae;
  transform: translateY(-2px);
}

.friend-items {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.friend-item {
  display: flex;
  align-items: center;
  padding: 16px;
  background: white;
  border: 2px solid #e8ecf1;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.friend-item:hover {
  border-color: #0969da;
  box-shadow: 0 4px 16px rgba(9, 105, 218, 0.12);
  transform: translateY(-2px);
}

.friend-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid #e8ecf1;
}

.friend-info {
  flex: 1;
  margin-left: 16px;
}

.friend-details {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}

.friend-name {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}

.friend-status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #656d76;
  font-weight: 500;
}

.status-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.status-dot.online {
  background: #3fb950;
  box-shadow: 0 0 0 2px white, 0 0 8px rgba(63, 185, 80, 0.4);
}

.status-dot.offline {
  background: #8b949e;
}

.friend-motto {
  font-size: 13px;
  color: #8b949e;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.friend-actions {
  display: none;
}

.friend-item:hover .friend-actions {
  display: block;
}

.action-btn {
  background: none;
  border: none;
  color: #656d76;
  cursor: pointer;
  padding: 8px 12px;
  font-size: 16px;
  border-radius: 6px;
  transition: all 0.2s ease;
}

.action-btn:hover {
  color: #0969da;
  background: #f6f8fa;
}

/* ========== 右键菜单 ========== */
.options-panel {
  position: fixed;
  background: white;
  border: 2px solid #e8ecf1;
  border-radius: 10px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  z-index: 1000;
  min-width: 160px;
  padding: 6px 0;
}

.options-item {
  padding: 12px 16px;
  cursor: pointer;
  font-size: 14px;
  color: #24292f;
  display: flex;
  align-items: center;
  gap: 10px;
  transition: all 0.15s ease;
  font-weight: 500;
}

.options-item:hover {
  background: #f6f8fa;
  color: #0969da;
}

.options-item.danger {
  color: #cf222e;
}

.options-item.danger:hover {
  background: #ffebe9;
}

.overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 999;
}

/* ========== 好友申请列表 ========== */
.requests-list {
  max-height: 400px;
  overflow-y: auto;
}

.no-requests {
  text-align: center;
  padding: 60px 20px;
  color: #656d76;
}

.no-requests i {
  font-size: 56px;
  margin-bottom: 16px;
  color: #d0d7de;
}

.request-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border: 2px solid #e8ecf1;
  border-radius: 10px;
  margin-bottom: 12px;
  background: white;
}

.request-info {
  display: flex;
  align-items: center;
  gap: 14px;
  flex: 1;
}

.request-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  object-fit: cover;
}

.request-details h4 {
  margin: 0 0 6px 0;
  font-size: 15px;
  color: #1a1a1a;
  font-weight: 600;
}

.request-details p {
  margin: 0 0 6px 0;
  font-size: 13px;
  color: #656d76;
}

.request-time {
  font-size: 12px;
  color: #8b949e;
}

.request-actions {
  display: flex;
  gap: 10px;
}

/* ========== 搜索结果 ========== */
.search-input-group {
  display: flex;
  gap: 10px;
}

.search-input-group input {
  flex: 1;
  padding: 10px 14px;
  border: 2px solid #d0d7de;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
}

.searched-user-info {
  margin-top: 20px;
}

.user-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: #f6f8fa;
  border-radius: 12px;
  border: 2px solid #d0d7de;
  margin-bottom: 16px;
}

.user-avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid #e8ecf1;
}

.user-details h4 {
  margin: 0 0 8px 0;
  font-size: 18px;
  color: #1a1a1a;
  font-weight: 600;
}

.motto {
  margin: 0;
  font-size: 14px;
  color: #656d76;
  font-style: italic;
}

/* ========== 滚动条美化 ========== */
.friends-list::-webkit-scrollbar,
.group-list::-webkit-scrollbar,
.requests-list::-webkit-scrollbar {
  width: 8px;
}

.friends-list::-webkit-scrollbar-track,
.group-list::-webkit-scrollbar-track,
.requests-list::-webkit-scrollbar-track {
  background: #f6f8fa;
  border-radius: 10px;
}

.friends-list::-webkit-scrollbar-thumb,
.group-list::-webkit-scrollbar-thumb,
.requests-list::-webkit-scrollbar-thumb {
  background: #d0d7de;
  border-radius: 10px;
}

.friends-list::-webkit-scrollbar-thumb:hover,
.group-list::-webkit-scrollbar-thumb:hover,
.requests-list::-webkit-scrollbar-thumb:hover {
  background: #8b949e;
}

/* ========== 好友详情对话框 ========== */
.friend-detail {
  padding: 0;
}

.detail-header {
  display: flex;
  gap: 20px;
  padding: 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  margin-bottom: 24px;
}

.detail-avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  object-fit: cover;
  border: 4px solid white;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.detail-basic-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  color: white;
}

.detail-basic-info h3 {
  margin: 0 0 12px 0;
  font-size: 24px;
  font-weight: 600;
}

.detail-status {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.detail-status .status-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #ccc;
}

.detail-status .status-dot.online {
  background: #10b981;
  box-shadow: 0 0 8px rgba(16, 185, 129, 0.6);
}

.detail-group {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  opacity: 0.9;
}

.detail-body {
  padding: 0 24px;
}

.detail-item {
  display: flex;
  align-items: flex-start;
  padding: 16px 0;
  border-bottom: 1px solid #e8ecf1;
  gap: 12px;
}

.detail-item:last-child {
  border-bottom: none;
}

.detail-item label {
  width: 120px;
  font-weight: 600;
  color: #656d76;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.detail-item label i {
  width: 16px;
  text-align: center;
  color: #0969da;
}

.detail-item span {
  flex: 1;
  color: #1a1a1a;
  font-size: 14px;
}

.detail-item .item-content {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.detail-item .remark-text {
  flex: 1;
  color: #1a1a1a;
  font-size: 14px;
}

.detail-item .edit-btn {
  flex-shrink: 0;
  padding: 4px 12px;
  color: #0969da;
}

.detail-item .edit-btn:hover {
  color: #0550ae;
}

.detail-item .motto-text {
  font-style: italic;
  color: #656d76;
  line-height: 1.6;
}

.detail-item .id-text {
  font-family: 'Courier New', monospace;
  background: #f6f8fa;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 13px;
}

.empty-info {
  text-align: center;
  padding: 32px 0;
  color: #656d76;
}

.empty-info i {
  font-size: 48px;
  margin-bottom: 12px;
  opacity: 0.3;
}

.empty-info p {
  margin: 0;
  font-size: 14px;
}

.detail-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 24px;
  margin-top: 16px;
  border-top: 1px solid #e8ecf1;
}
</style>
