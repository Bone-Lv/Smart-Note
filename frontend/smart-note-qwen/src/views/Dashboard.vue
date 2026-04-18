<template>
  <div class="dashboard">
    <div class="welcome-section">
      <h1>欢迎回来，{{ userStore.userInfo?.username || '用户' }}！</h1>
      <p>{{ userStore.userInfo?.motto || '开始您的智能笔记之旅' }}</p>
    </div>
    
    <div class="stats-grid">
      <div class="stat-card" @click="goToNotes" title="点击查看笔记列表">
        <div class="stat-icon">
          <i class="fas fa-book"></i>
        </div>
        <div class="stat-info">
          <h3>{{ stats.totalNotes }}</h3>
          <p>总笔记数</p>
        </div>
      </div>
      
      <div class="stat-card" @click="goToNotes" title="点击查看笔记列表">
        <div class="stat-icon">
          <i class="fas fa-folder"></i>
        </div>
        <div class="stat-info">
          <h3>{{ stats.totalFolders }}</h3>
          <p>文件夹数</p>
        </div>
      </div>
      
      <div class="stat-card" @click="goToFriends" title="点击查看好友列表">
        <div class="stat-icon">
          <i class="fas fa-comment"></i>
        </div>
        <div class="stat-info">
          <h3>{{ stats.totalFriends }}</h3>
          <p>好友数</p>
        </div>
      </div>
      
      <div class="stat-card" @click="goToConversations" title="点击查看消息列表">
        <div class="stat-icon">
          <i class="fas fa-bell"></i>
        </div>
        <div class="stat-info">
          <h3>{{ stats.unreadMessages }}</h3>
          <p>未读消息</p>
        </div>
      </div>
    </div>
    
    <div class="recent-section">
      <div class="section-header">
        <h2>最近查看的笔记</h2>
        <router-link to="/notes" class="view-all">查看全部</router-link>
      </div>
      
      <div class="recent-notes">
        <div 
          v-for="note in recentNotes" 
          :key="note.id" 
          class="note-card"
          @click="goToNote(note.id)"
        >
          <h3>{{ note.title }}</h3>
          <p class="note-content">{{ truncateContent(note.content, 100) }}</p>
          <div class="note-meta">
            <span class="note-date">{{ formatDate(note.updateTime) }}</span>
            <span class="note-tags" v-if="note.tags">
              <i class="fas fa-tag"></i>
              {{ note.tags }}
            </span>
          </div>
        </div>
        
        <div v-if="recentNotes.length === 0" class="no-recent-notes">
          <i class="fas fa-book-open"></i>
          <p>暂无最近查看的笔记</p>
        </div>
      </div>
    </div>
    
    <div class="top-notes-section">
      <div class="section-header">
        <h2>最常查看的笔记</h2>
      </div>
      
      <div class="top-notes">
        <div 
          v-for="(note, index) in topNotes" 
          :key="note.id" 
          class="top-note-card"
          @click="goToNote(note.id)"
        >
          <div class="note-rank">#{{ index + 1 }}</div>
          <div class="note-info">
            <h3>{{ note.title }}</h3>
            <p class="note-date">{{ formatDate(note.updateTime) }}</p>
          </div>
        </div>
        
        <div v-if="topNotes.length === 0" class="no-top-notes">
          <i class="fas fa-chart-bar"></i>
          <p>暂无最常查看的笔记</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useUserStore } from '../stores/userStore.js';
import { getRecentViewedNotesApi, getTop3FrequentNotesApi, getNoteListApi } from '../api/note.js';
import { getFriendListApi, getConversationListApi } from '../api/social.js';

export default {
  name: 'Dashboard',
  setup() {
    const router = useRouter();
    const userStore = useUserStore();
    
    const stats = ref({
      totalNotes: 0,
      totalFolders: 0,
      totalFriends: 0,
      unreadMessages: 0
    });
    
    const recentNotes = ref([]);
    const topNotes = ref([]);
    
    const fetchStats = async () => {
      try {
        // 获取笔记总数
        const notesResponse = await getNoteListApi({ page: 1, pageSize: 1 });
        stats.value.totalNotes = notesResponse.data.data?.total || 0;
      } catch (error) {
        console.error('获取笔记统计失败:', error);
        stats.value.totalNotes = 0;
      }
      
      try {
        // 获取好友总数
        const friendsResponse = await getFriendListApi();
        stats.value.totalFriends = (friendsResponse.data.data || []).length;
      } catch (error) {
        console.error('获取好友统计失败:', error);
        stats.value.totalFriends = 0;
      }
      
      try {
        // 获取未读消息数量
        const conversationsResponse = await getConversationListApi();
        const conversations = conversationsResponse.data.data || [];
        // 累加所有会话的未读数量
        stats.value.unreadMessages = conversations.reduce((sum, conv) => {
          return sum + (conv.unreadCount || 0);
        }, 0);
      } catch (error) {
        console.error('获取未读消息统计失败:', error);
        stats.value.unreadMessages = 0;
      }
    };
    
    const fetchRecentNotes = async () => {
      try {
        const response = await getRecentViewedNotesApi(5);
        recentNotes.value = response.data.data || [];
      } catch (error) {
        console.error('Fetch recent notes error:', error);
      }
    };
    
    const fetchTopNotes = async () => {
      try {
        const response = await getTop3FrequentNotesApi();
        topNotes.value = response.data.data || [];
      } catch (error) {
        console.error('Fetch top notes error:', error);
      }
    };
    
    const truncateContent = (content, maxLength) => {
      if (!content) return '';
      return content.length > maxLength ? content.substring(0, maxLength) + '...' : content;
    };
    
    const formatDate = (dateString) => {
      if (!dateString) return '';
      return new Date(dateString).toLocaleDateString('zh-CN');
    };
    
    const goToNote = (noteId) => {
      router.push(`/app/note/${noteId}`);
    };
    
    const goToNotes = () => {
      router.push('/app/notes');
    };
    
    const goToFriends = () => {
      router.push('/app/friends');
    };
    
    const goToConversations = () => {
      router.push('/app/conversations');
    };
    
    onMounted(async () => {
      await Promise.all([
        fetchStats(),
        fetchRecentNotes(),
        fetchTopNotes()
      ]);
    });
    
    return {
      userStore,
      stats,
      recentNotes,
      topNotes,
      truncateContent,
      formatDate,
      goToNote,
      goToNotes,
      goToFriends,
      goToConversations
    };
  }
};
</script>

<style scoped>
.dashboard {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.welcome-section {
  margin-bottom: 32px;
}

.welcome-section h1 {
  color: #333;
  margin-bottom: 8px;
  font-size: 28px;
}

.welcome-section p {
  color: #666;
  font-size: 16px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 20px;
  margin-bottom: 32px;
}

.stat-card {
  background: white;
  border-radius: 8px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  transition: all 0.3s ease;
  cursor: pointer;
  user-select: none;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 20px rgba(0,0,0,0.15);
  background: linear-gradient(135deg, #f5f7fa 0%, #ffffff 100%);
}

.stat-card:active {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.stat-icon {
  width: 60px;
  height: 60px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: white;
}

.stat-info h3 {
  margin: 0;
  font-size: 28px;
  color: #333;
}

.stat-info p {
  margin: 0;
  color: #666;
  font-size: 14px;
}

.recent-section, .top-notes-section {
  margin-bottom: 32px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-header h2 {
  margin: 0;
  color: #333;
  font-size: 20px;
}

.view-all {
  color: #409eff;
  text-decoration: none;
  font-size: 14px;
}

.view-all:hover {
  text-decoration: underline;
}

.recent-notes, .top-notes {
  display: grid;
  gap: 16px;
}

.note-card {
  background: white;
  border-radius: 8px;
  padding: 16px;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  transition: transform 0.3s, box-shadow 0.3s;
}

.note-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0,0,0,0.15);
}

.note-card h3 {
  margin: 0 0 8px 0;
  color: #333;
  font-size: 16px;
}

.note-content {
  margin: 0 0 12px 0;
  color: #666;
  font-size: 14px;
  line-height: 1.5;
}

.note-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #999;
}

.note-tags {
  display: flex;
  align-items: center;
  gap: 4px;
}

.top-note-card {
  background: white;
  border-radius: 8px;
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 16px;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  transition: transform 0.3s, box-shadow 0.3s;
}

.top-note-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0,0,0,0.15);
}

.note-rank {
  font-size: 24px;
  font-weight: bold;
  color: #ff6b35;
}

.note-info h3 {
  margin: 0 0 4px 0;
  color: #333;
  font-size: 16px;
}

.note-info .note-date {
  margin: 0;
  color: #999;
  font-size: 12px;
}

.no-recent-notes, .no-top-notes {
  text-align: center;
  padding: 40px 20px;
  color: #999;
}

.no-recent-notes i, .no-top-notes i {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.no-recent-notes p, .no-top-notes p {
  margin: 0;
  font-size: 16px;
}
</style>