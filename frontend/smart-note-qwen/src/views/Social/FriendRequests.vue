<template>
  <div class="friend-requests">
    <h2>好友申请</h2>
    
    <div v-if="requests.length === 0" class="empty-state">
      <el-empty description="暂无好友申请" />
    </div>
    
    <div v-else class="requests-list">
      <div v-for="request in requests" :key="request.id" class="request-item">
        <div class="applicant-info">
          <img :src="request.applicantAvatar || '/default-avatar.png'" alt="头像" class="avatar" />
          <div class="info">
            <p class="name">{{ request.applicantName }}</p>
            <p class="message">{{ request.message || '无验证消息' }}</p>
            <p class="time">{{ request.createTime }}</p>
          </div>
        </div>
        
        <div class="actions">
          <el-button type="primary" size="small" @click="handleAccept(request.id)">
            同意
          </el-button>
          <el-button size="small" @click="handleReject(request.id)">
            拒绝
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useFriendStore } from '@/stores/friendStore.js';
import { ElMessage } from 'element-plus';

const friendStore = useFriendStore();
const requests = ref([]);

onMounted(async () => {
  await friendStore.fetchReceivedRequests();
  requests.value = friendStore.receivedRequests;
});

const handleAccept = async (requestId) => {
  try {
    await friendStore.acceptRequest(requestId);
    requests.value = friendStore.receivedRequests;
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('处理失败');
    }
  }
};

const handleReject = async (requestId) => {
  try {
    await friendStore.rejectRequest(requestId);
    requests.value = friendStore.receivedRequests;
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('处理失败');
    }
  }
};
</script>

<style scoped>
.friend-requests {
  padding: 20px;
}

.empty-state {
  margin-top: 40px;
}

.requests-list {
  margin-top: 20px;
}

.request-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  margin-bottom: 12px;
  transition: all 0.3s;
}

.request-item:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.applicant-info {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  object-fit: cover;
}

.info {
  flex: 1;
}

.name {
  font-weight: 600;
  margin: 0 0 4px 0;
}

.message {
  color: #606266;
  font-size: 14px;
  margin: 0 0 4px 0;
}

.time {
  color: #909399;
  font-size: 12px;
  margin: 0;
}

.actions {
  display: flex;
  gap: 8px;
}
</style>
