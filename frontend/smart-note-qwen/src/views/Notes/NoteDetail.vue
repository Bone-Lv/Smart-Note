<template>
  <div class="note-detail">
    <!-- 顶部工具栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <button @click="goBack" class="back-btn">
          <i class="fas fa-arrow-left"></i>
        </button>
        <input 
          v-model="localTitle" 
          type="text" 
          class="title-input"
          :readonly="!editorState.hasEditLock"
          @blur="saveNote"
        >
      </div>
      
      <div class="toolbar-right">
        <button 
          v-if="!editorState.hasEditLock" 
          @click="handleRequestEdit" 
          class="edit-btn"
          :disabled="editorState.lockOwner && editorState.lockOwner !== 'me'"
        >
          <i class="fas fa-edit"></i>
          编辑
        </button>
        
        <button 
          v-if="editorState.hasEditLock" 
          @click="handleSaveAndRelease" 
          class="save-btn"
        >
          <i class="fas fa-save"></i>
          保存并释放
        </button>
        
        <button @click="showVisibilityModal = true" class="share-btn">
          <i class="fas fa-share-alt"></i>
          分享
        </button>
        
        <button @click="showVersionModal = true" class="history-btn">
          <i class="fas fa-history"></i>
          版本
        </button>
        
        <button @click="showAnnotationModal = true" class="annotation-btn">
          <i class="fas fa-comment"></i>
          批注 ({{ annotations.length }})
        </button>
      </div>
    </div>
    
    <!-- 笔记内容区域 -->
    <div class="content-area">
      <div class="editor-section" v-if="editorState.hasEditLock">
        <textarea 
          v-model="localContent" 
          class="markdown-editor"
          @input="onContentChange"
          placeholder="开始编写您的笔记..."
        ></textarea>
      </div>
      
      <div class="viewer-section" v-else>
        <!-- 侧边栏标题导航 -->
        <aside class="toc-sidebar" v-if="headings.length > 0">
          <div class="toc-header">
            <h4>目录导航</h4>
          </div>
          <nav class="toc-nav">
            <ul>
              <li 
                v-for="heading in headings" 
                :key="heading.id"
                :class="['toc-item', `toc-level-${heading.level}`]"
                @click="scrollToHeading(heading.id)"
              >
                {{ heading.text }}
              </li>
            </ul>
          </nav>
        </aside>
        
        <!-- 主内容区域 -->
        <div class="content-wrapper">
          <MarkdownRenderer 
            :content="localContent" 
            :annotations="annotations"
            @annotation-click="handleAnnotationClick"
            @headings-change="handleHeadingsChange"
          />
        </div>
      </div>
    </div>
    
    <!-- 版本历史模态框 -->
    <div v-if="showVersionModal" class="modal-overlay" @click="showVersionModal = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>版本历史</h3>
          <button @click="showVersionModal = false" class="close-btn">×</button>
        </div>
        
        <div class="modal-body">
          <div class="version-list">
            <div 
              v-for="version in versions" 
              :key="version.version"
              class="version-item"
              :class="{ current: version.version === noteData.version }"
            >
              <div class="version-info">
                <h4>版本 {{ version.version }}</h4>
                <p>{{ formatDate(version.createTime) }}</p>
                <p class="version-title">{{ version.title }}</p>
              </div>
              <div class="version-actions">
                <button 
                  v-if="version.version !== noteData.version"
                  @click="compareVersions(version.version)"
                  class="compare-btn"
                >
                  对比
                </button>
                <button 
                  v-if="version.version !== noteData.version"
                  @click="rollbackToVersion(version.version)"
                  class="rollback-btn"
                >
                  回退
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 分享设置模态框 -->
    <div v-if="showVisibilityModal" class="modal-overlay" @click="showVisibilityModal = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>分享设置</h3>
          <button @click="showVisibilityModal = false" class="close-btn">×</button>
        </div>
        
        <div class="modal-body">
          <div class="visibility-options">
            <label class="radio-option">
              <input 
                type="radio" 
                v-model="visibilitySetting" 
                :value="'0'"
              >
              <span>仅自己可见</span>
            </label>
            <label class="radio-option">
              <input 
                type="radio" 
                v-model="visibilitySetting" 
                :value="'1'"
              >
              <span>部分好友可见</span>
            </label>
            <label class="radio-option">
              <input 
                type="radio" 
                v-model="visibilitySetting" 
                :value="'2'"
              >
              <span>部分好友可编辑</span>
            </label>
            <label class="radio-option">
              <input 
                type="radio" 
                v-model="visibilitySetting" 
                :value="'3'"
              >
              <span>所有人可见</span>
            </label>
          </div>
          
          <div v-if="['1', '2'].includes(visibilitySetting)" class="friend-selection">
            <h4>选择好友</h4>
            <div class="friend-list">
              <label 
                v-for="friend in friends" 
                :key="friend.friendUserId"
                class="checkbox-option"
              >
                <input 
                  type="checkbox" 
                  :value="friend.friendUserId"
                  v-model="selectedFriends"
                >
                <span>{{ friend.remark || friend.friendUsername }}</span>
              </label>
            </div>
          </div>
          
          <div class="modal-actions">
            <button @click="saveVisibility" class="save-btn">保存</button>
            <button @click="showVisibilityModal = false" class="cancel-btn">取消</button>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 批注管理模态框 -->
    <div v-if="showAnnotationModal" class="modal-overlay" @click="showAnnotationModal = false">
      <div class="modal-content" @click.stop style="width: 80%; max-width: 1000px;">
        <div class="modal-header">
          <h3>批注管理</h3>
          <button @click="showAnnotationModal = false" class="close-btn">×</button>
        </div>
        
        <div class="modal-body">
          <div class="annotation-actions">
            <button @click="startCreatingAnnotation" class="create-annotation-btn">
              <i class="fas fa-plus"></i>
              创建批注
            </button>
          </div>
          
          <div class="annotation-list">
            <div 
              v-for="annotation in annotations" 
              :key="annotation.id"
              class="annotation-item"
            >
              <div class="annotation-header">
                <span class="annotation-author">{{ annotation.username }}</span>
                <span class="annotation-time">{{ formatDate(annotation.createTime) }}</span>
                <div class="annotation-actions">
                  <button @click="editAnnotation(annotation)" class="edit-btn">编辑</button>
                  <button @click="deleteAnnotation(annotation.id)" class="delete-btn">删除</button>
                </div>
              </div>
              <div class="annotation-content">
                <p><strong>批注内容:</strong> {{ annotation.content }}</p>
                <p><strong>目标内容:</strong> {{ annotation.targetContent }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 批注创建/编辑模态框 -->
    <div v-if="showCreateAnnotationModal" class="modal-overlay" @click="closeAnnotationModal">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>{{ editingAnnotation ? '编辑批注' : '创建批注' }}</h3>
          <button @click="closeAnnotationModal" class="close-btn">×</button>
        </div>
        
        <div class="modal-body">
          <div class="form-group">
            <label>目标内容</label>
            <textarea 
              v-model="newAnnotation.targetContent" 
              readonly
              rows="3"
            ></textarea>
          </div>
          <div class="form-group">
            <label>批注内容</label>
            <textarea 
              v-model="newAnnotation.content" 
              placeholder="请输入批注内容"
              rows="4"
            ></textarea>
          </div>
          <div class="modal-actions">
            <button @click="saveAnnotation" class="save-btn">保存</button>
            <button @click="closeAnnotationModal" class="cancel-btn">取消</button>
          </div>
        </div>
      </div>
    </div>
    
    <!-- Diff对比模态框 -->
    <DiffModal 
      :is-open="showDiffModal"
      :versions="versions"
      :note-id="noteId"
      @close="showDiffModal = false"
      @rollback="handleRollback"
    />
    
    <!-- AI分析按钮 -->
    <button 
      v-if="editorState.hasEditLock" 
      @click="analyzeNoteContent" 
      class="ai-analyze-btn"
      :disabled="analyzing"
    >
      <i class="fas fa-robot"></i>
      <span v-if="!analyzing">AI分析</span>
      <span v-else>分析中...</span>
    </button>
    
    <!-- 加载遮罩 -->
    <div v-if="loading" class="loading-overlay">
      <div class="loading-spinner"></div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, onUnmounted, watch } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage, ElLoading } from 'element-plus';
import MarkdownRenderer from '../../components/MarkdownRenderer.vue';
import DiffModal from '../../components/DiffModal.vue';
import { useNoteEditor } from '../../hooks/useNoteEditor.js';
import { 
  getNoteDetailApi, 
  updateNoteApi, 
  updateNoteVisibilityApi,  // ✅ 修正：updateVisibilityApi → updateNoteVisibilityApi
  getNoteVersionsApi,        // ✅ 修正：getVersionHistoryApi → getNoteVersionsApi
  rollbackNoteVersionApi,    // ✅ 修正：rollbackToVersionApi → rollbackNoteVersionApi
  getNoteAnnotationsApi, 
  createAnnotationApi, 
  updateAnnotationApi, 
  deleteAnnotationApi,
  analyzeNoteWithAIApi       // ✅ 修正：analyzeNoteApi → analyzeNoteWithAIApi
} from '../../api/note.js';
import { getFriendListApi } from '../../api/social.js';
import { saveNoteDraft, getNoteDraft, deleteNoteDraft, saveNoteContent } from '../../utils/cache.js';
import { startPerf, endPerf, monitorApiCall } from '../../utils/performance.js';

export default {
  name: 'NoteDetail',
  components: {
    MarkdownRenderer,
    DiffModal
  },
  setup() {
    const router = useRouter();
    const route = useRoute();
    const noteId = parseInt(route.params.noteId);
    
    const { 
      editorState, 
      requestEditLock, 
      releaseEditLock, 
      syncContent, 
      startAutoSave, 
      stopAutoSave,
      startViewingNote,
      stopViewingNote
    } = useNoteEditor();
    
    // 笔记数据
    const noteData = ref({});
    const localTitle = ref('');
    const localContent = ref('');
    const annotations = ref([]);
    const headings = ref([]); // 标题导航
    const versions = ref([]);
    const friends = ref([]);
    
    // 状态变量
    const loading = ref(false);
    const analyzing = ref(false);
    const showVersionModal = ref(false);
    const showVisibilityModal = ref(false);
    const showAnnotationModal = ref(false);
    const showCreateAnnotationModal = ref(false);
    const showDiffModal = ref(false);
    const editingAnnotation = ref(null);
    
    // 分享设置
    const visibilitySetting = ref('0');
    const selectedFriends = ref([]);
    
    // 批注数据
    const newAnnotation = ref({
      id: null,
      content: '',
      targetContent: ''
    });
    
    // 防抖定时器
    let contentDebounceTimer = null;
    let loadingInstance = null;
    
    // 监听编辑锁状态变化
    watch(() => editorState.hasEditLock, (hasLock) => {
      if (hasLock) {
        console.log('✅ 获得编辑权限，启动自动保存');
        startAutoSave(noteId, localTitle.value, localContent.value, noteData.value.version, 5000);
        ElMessage.success('已获得编辑权限');
      } else {
        console.log('⚠️ 失去编辑权限，停止自动保存');
        stopAutoSave();
        if (editorState.lockOwner && editorState.lockOwner !== 'me') {
          ElMessage.warning(`用户 ${editorState.lockOwner} 正在编辑此笔记`);
        }
      }
    });
    
    // 监听内容变化（带防抖）
    const onContentChange = () => {
      if (!editorState.hasEditLock) return;
      
      // 清除之前的定时器
      if (contentDebounceTimer) {
        clearTimeout(contentDebounceTimer);
      }
      
      // 防抖500ms后同步
      contentDebounceTimer = setTimeout(async () => {
        // 先保存到本地缓存
        await saveNoteDraft(noteId, {
          title: localTitle.value,
          content: localContent.value
        });
        
        // 再同步到服务器
        syncContent(noteId, localTitle.value, localContent.value, noteData.value.version);
      }, 500);
    };
    
    // 加载笔记详情
    const loadNoteDetail = async () => {
      loading.value = true;
      loadingInstance = ElLoading.service({
        lock: true,
        text: '加载笔记中...',
        background: 'rgba(255, 255, 255, 0.7)'
      });
      
      // 开始性能监控
      startPerf('note-load');
      
      try {
        // 使用性能监控包装API调用
        const response = await monitorApiCall(`/note/${noteId}`, () => getNoteDetailApi(noteId));
        
        noteData.value = response.data.data;
        localTitle.value = noteData.value.title;
        localContent.value = noteData.value.content;
        visibilitySetting.value = noteData.value.visibility || '0';
        
        // 缓存笔记内容
        await saveNoteContent(noteId, noteData.value.content);
        
        // 并行加载相关数据
        await Promise.all([
          loadAnnotations(),
          loadVersions(),
          loadFriends()
        ]);
        
        // 开始查看笔记
        startViewingNote(noteId);
        
        console.log('✅ 笔记加载成功:', noteData.value.title);
        
        // 结束性能监控
        endPerf('note-load', { noteId, title: noteData.value.title });
      } catch (error) {
        console.error('❌ 加载笔记失败:', error);
        
        // 尝试从缓存恢复
        const cachedContent = await getNoteDraft(noteId);
        if (cachedContent) {
          console.log('📄 从缓存恢复笔记:', noteId);
          localContent.value = cachedContent.content;
          localTitle.value = cachedContent.title;
          ElMessage.warning('网络异常，已加载本地缓存版本');
        } else {
          ElMessage.error('加载笔记失败，请重试');
          router.push('/notes');
        }
        
        // 记录错误
        endPerf('note-load', { error: error.message });
      } finally {
        loading.value = false;
        if (loadingInstance) {
          loadingInstance.close();
        }
      }
    };
    
    // 加载批注
    const loadAnnotations = async () => {
      try {
        const response = await getNoteAnnotationsApi(noteId);
        annotations.value = response.data.data || [];
        console.log('📝 批注加载成功，共', annotations.value.length, '条');
      } catch (error) {
        console.error('❌ 加载批注失败:', error);
        ElMessage.warning('加载批注失败');
      }
    };
    
    // 加载版本历史
    const loadVersions = async () => {
      try {
        const response = await getNoteVersionsApi(noteId);
        versions.value = response.data.data || [];
        console.log('📋 版本历史加载成功，共', versions.value.length, '个版本');
      } catch (error) {
        console.error('❌ 加载版本历史失败:', error);
        ElMessage.warning('加载版本历史失败');
      }
    };
    
    // 加载好友列表
    const loadFriends = async () => {
      try {
        const response = await getFriendListApi();
        friends.value = response.data.data || [];
        console.log('👥 好友列表加载成功，共', friends.value.length, '人');
      } catch (error) {
        console.error('❌ 加载好友列表失败:', error);
        ElMessage.warning('加载好友列表失败');
      }
    };
    
    // 请求编辑锁
    const handleRequestEdit = async () => {
      try {
        const result = await requestEditLock(noteId);
        
        if (!result.success) {
          ElMessage.warning(result.message || '无法获取编辑权限');
        }
      } catch (error) {
        console.error('❌ 请求编辑锁失败:', error);
        ElMessage.error('请求编辑锁失败');
      }
    };
    
    // 保存笔记
    const saveNote = async () => {
      if (!editorState.hasEditLock) {
        ElMessage.warning('没有编辑权限');
        return;
      }
      
      try {
        await updateNoteApi(noteId, {
          title: localTitle.value,
          content: localContent.value,
          version: noteData.value.version
        });
        
        // 更新本地数据
        noteData.value.title = localTitle.value;
        noteData.value.content = localContent.value;
        noteData.value.version += 1;
        
        console.log('✅ 笔记保存成功');
        ElMessage.success('笔记已保存');
      } catch (error) {
        console.error('❌ 保存笔记失败:', error);
        ElMessage.error('保存笔记失败，请重试');
      }
    };
    
    // 保存并释放编辑锁
    const handleSaveAndRelease = async () => {
      try {
        // 最后一次保存
        await saveNote();
        
        // 释放编辑锁
        await releaseEditLock(noteId);
        
        // 停止自动保存
        stopAutoSave();
        
        ElMessage.success('已保存并释放编辑权限');
      } catch (error) {
        console.error('❌ 保存或释放锁失败:', error);
        ElMessage.error('操作失败，请重试');
      }
    };
    
    // 保存分享设置
    const saveVisibility = async () => {
      try {
        await updateNoteVisibilityApi(noteId, {  // ✅ 修正函数名
          visibility: visibilitySetting.value,
          friendUserIds: selectedFriends.value
        });
        
        ElMessage.success('分享设置已保存');
        showVisibilityModal.value = false;
      } catch (error) {
        console.error('❌ 保存分享设置失败:', error);
        ElMessage.error('保存分享设置失败');
      }
    };
    
    // 开始创建批注
    const startCreatingAnnotation = () => {
      newAnnotation.value = {
        content: '',
        targetContent: ''
      };
      editingAnnotation.value = null;
      showCreateAnnotationModal.value = true;
    };
    
    // 编辑批注
    const editAnnotation = (annotation) => {
      newAnnotation.value = {
        content: annotation.content,
        targetContent: annotation.targetContent
      };
      editingAnnotation.value = annotation;
      showCreateAnnotationModal.value = true;
    };
    
    // 保存批注
    const saveAnnotation = async () => {
      if (!newAnnotation.value.content.trim()) {
        ElMessage.warning('请输入批注内容');
        return;
      }
      
      try {
        if (editingAnnotation.value) {
          // 更新现有批注
          await updateAnnotationApi(editingAnnotation.value.id, {
            content: newAnnotation.value.content,
            targetContent: newAnnotation.value.targetContent
          });
          ElMessage.success('批注更新成功');
        } else {
          // 创建新批注
          await createAnnotationApi(noteId, {
            content: newAnnotation.value.content,
            targetContent: newAnnotation.value.targetContent,
            startPosition: 0,
            endPosition: newAnnotation.value.targetContent.length
          });
          ElMessage.success('批注创建成功');
        }
        
        closeAnnotationModal();
        await loadAnnotations();
      } catch (error) {
        console.error('❌ 保存批注失败:', error);
        ElMessage.error('保存批注失败');
      }
    };
    
    // 删除批注
    const deleteAnnotation = async (annotationId) => {
      try {
        await deleteAnnotationApi(annotationId);
        ElMessage.success('批注已删除');
        await loadAnnotations();
      } catch (error) {
        console.error('❌ 删除批注失败:', error);
        ElMessage.error('删除批注失败');
      }
    };
    
    // 关闭批注模态框
    const closeAnnotationModal = () => {
      showCreateAnnotationModal.value = false;
      editingAnnotation.value = null;
      newAnnotation.value = {
        content: '',
        targetContent: ''
      };
    };
    
    // 回退到指定版本
    const rollbackToVersion = async (version) => {
      try {
        const response = await rollbackNoteVersionApi(noteId, version);  // ✅ 修正函数名
        const newVersion = response.data.data;
        
        ElMessage.success(`已回退到版本 ${version}，新版本号：${newVersion}`);
        showVersionModal.value = false;
        await loadNoteDetail();
      } catch (error) {
        console.error('❌ 回退版本失败:', error);
        ElMessage.error('回退版本失败');
      }
    };
    
    // 比较版本
    const compareVersions = (version) => {
      showVersionModal.value = false;
      showDiffModal.value = true;
    };
    
    // 处理回退事件
    const handleRollback = (noteId, version) => {
      rollbackToVersion(version);
    };
    
    // AI分析笔记
    const analyzeNoteContent = async () => {
      if (!localContent.value.trim()) {
        ElMessage.warning('笔记内容为空，无法分析');
        return;
      }
      
      analyzing.value = true;
      
      try {
        const response = await analyzeNoteWithAIApi(noteId, true);  // ✅ 修正函数名，第二个参数是 forceRefresh
        
        const analysis = response.data.data;
        console.log('🤖 AI分析结果:', analysis);
        
        ElMessage.success('AI分析完成');
        
        // 这里可以显示分析结果，比如打开一个模态框展示摘要、要点等
        if (analysis.summary) {
          ElMessage.info({
            message: `摘要: ${analysis.summary}`,
            duration: 5000,
            showClose: true
          });
        }
      } catch (error) {
        console.error('❌ AI分析失败:', error);
        ElMessage.error('AI分析失败，请重试');
      } finally {
        analyzing.value = false;
      }
    };
    
    // 处理批注点击
    const handleAnnotationClick = (annotation) => {
      console.log('点击批注:', annotation);
      // 可以在这里实现滚动到目标位置等功能
    };
    
    // 处理标题变化
    const handleHeadingsChange = (newHeadings) => {
      headings.value = newHeadings;
    };
    
    // 滚动到指定标题
    const scrollToHeading = (headingId) => {
      const element = document.getElementById(headingId);
      if (element) {
        element.scrollIntoView({
          behavior: 'smooth',
          block: 'start'
        });
      }
    };
    
    // 格式化日期
    const formatDate = (dateString) => {
      if (!dateString) return '';
      return new Date(dateString).toLocaleString('zh-CN');
    };
    
    // 返回上一页
    const goBack = () => {
      router.push('/notes');
    };
    
    onMounted(async () => {
      await loadNoteDetail();
    });
    
    onUnmounted(() => {
      console.log('🧹 清理资源...');
      
      // 清除防抖定时器
      if (contentDebounceTimer) {
        clearTimeout(contentDebounceTimer);
        contentDebounceTimer = null;
      }
      
      // 如果有编辑锁，释放它
      if (editorState.hasEditLock) {
        releaseEditLock(noteId);
      }
      
      // 停止查看笔记
      stopViewingNote(noteId);
      
      // 停止自动保存
      stopAutoSave();
      
      // 清除本地草稿（已同步到服务器）
      if (!editorState.hasEditLock) {
        deleteNoteDraft(noteId);
      }
      
      console.log('✅ 资源清理完成');
    });
    
    return {
      noteId,
      noteData,
      localTitle,
      localContent,
      headings,
      annotations,
      versions,
      friends,
      loading,
      analyzing,
      showVersionModal,
      showVisibilityModal,
      showAnnotationModal,
      showCreateAnnotationModal,
      showDiffModal,
      editingAnnotation,
      visibilitySetting,
      selectedFriends,
      newAnnotation,
      editorState,
      handleRequestEdit,
      releaseEditLock,
      saveNote,
      handleSaveAndRelease,
      saveVisibility,
      startCreatingAnnotation,
      editAnnotation,
      saveAnnotation,
      deleteAnnotation,
      closeAnnotationModal,
      rollbackToVersion,
      compareVersions,
      handleRollback,
      analyzeNoteContent,
      handleAnnotationClick,
      handleHeadingsChange,
      scrollToHeading,
      formatDate,
      goBack,
      onContentChange
    };
  }
};
</script>

<style scoped>
.note-detail {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}

.toolbar {
  background: white;
  padding: 12px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #eee;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  z-index: 10;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.back-btn {
  background: none;
  border: none;
  color: #409eff;
  cursor: pointer;
  font-size: 18px;
  padding: 6px;
  border-radius: 4px;
}

.back-btn:hover {
  background: #f5f7fa;
}

.title-input {
  font-size: 18px;
  font-weight: 500;
  border: none;
  outline: none;
  padding: 6px 12px;
  border-radius: 4px;
  flex: 1;
  background: #f8f9fa;
}

.title-input:read-only {
  background: transparent;
}

.toolbar-right {
  display: flex;
  gap: 8px;
}

.toolbar button {
  padding: 8px 12px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: white;
  color: #606266;
  cursor: pointer;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: all 0.3s;
}

.toolbar button:hover:not(:disabled) {
  background: #ecf5ff;
  color: #409eff;
  border-color: #b3d8ff;
}

.toolbar button:disabled {
  background: #f5f7fa;
  color: #c0c4cc;
  cursor: not-allowed;
}

.edit-btn {
  background: #409eff;
  color: white;
  border-color: #409eff;
}

.edit-btn:hover:not(:disabled) {
  background: #66b1ff;
  border-color: #66b1ff;
}

.save-btn {
  background: #67c23a;
  color: white;
  border-color: #67c23a;
}

.save-btn:hover:not(:disabled) {
  background: #85ce61;
  border-color: #85ce61;
}

.content-area {
  flex: 1;
  overflow: hidden;
  display: flex;
}

.editor-section {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.markdown-editor {
  flex: 1;
  border: none;
  outline: none;
  padding: 20px;
  font-size: 16px;
  line-height: 1.6;
  resize: none;
  background: white;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
}

.viewer-section {
  flex: 1;
  display: flex;
  overflow: hidden;
}

/* 侧边栏标题导航 */
.toc-sidebar {
  width: 240px;
  background: #fafbfc;
  border-right: 1px solid #e1e4e8;
  overflow-y: auto;
  flex-shrink: 0;
}

.toc-header {
  padding: 16px;
  border-bottom: 1px solid #e1e4e8;
  background: white;
}

.toc-header h4 {
  margin: 0;
  color: #24292e;
  font-size: 14px;
  font-weight: 600;
}

.toc-nav {
  padding: 8px 0;
}

.toc-nav ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.toc-item {
  padding: 8px 16px;
  cursor: pointer;
  color: #586069;
  font-size: 13px;
  line-height: 1.5;
  border-left: 2px solid transparent;
  transition: all 0.2s;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.toc-item:hover {
  background: #f6f8fa;
  color: #0366d6;
  border-left-color: #0366d6;
}

.toc-level-1 {
  padding-left: 16px;
  font-weight: 600;
}

.toc-level-2 {
  padding-left: 24px;
}

.toc-level-3 {
  padding-left: 32px;
  font-size: 12px;
}

.toc-level-4,
.toc-level-5,
.toc-level-6 {
  padding-left: 40px;
  font-size: 12px;
  color: #959da5;
}

/* 内容包装器 */
.content-wrapper {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: white;
}

.version-list {

}

.version-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border: 1px solid #eee;
  border-radius: 6px;
  margin-bottom: 8px;
}

.version-item.current {
  border-color: #409eff;
  background: #ecf5ff;
}

.version-info h4 {
  margin: 0 0 4px 0;
  color: #333;
}

.version-info p {
  margin: 0 0 2px 0;
  color: #666;
  font-size: 14px;
}

.version-title {
  font-size: 12px;
  color: #999;
  margin: 0;
}

.version-actions {
  display: flex;
  gap: 8px;
}

.compare-btn, .rollback-btn {
  padding: 6px 12px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: white;
  color: #606266;
  cursor: pointer;
  font-size: 12px;
}

.compare-btn:hover, .rollback-btn:hover {
  background: #f5f7fa;
}

.rollback-btn {
  background: #f56c6c;
  color: white;
  border-color: #f56c6c;
}

.rollback-btn:hover {
  background: #f78989;
  border-color: #f78989;
}

.visibility-options {
  margin-bottom: 20px;
}

.radio-option {
  display: block;
  margin-bottom: 12px;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 4px;
  transition: background 0.3s;
}

.radio-option:hover {
  background: #f5f7fa;
}

.radio-option input {
  margin-right: 8px;
}

.friend-selection {
  margin-bottom: 20px;
}

.friend-selection h4 {
  margin-bottom: 12px;
  color: #333;
}

.friend-list {
  max-height: 200px;
  overflow-y: auto;
  border: 1px solid #eee;
  border-radius: 4px;
  padding: 8px;
}

.checkbox-option {
  display: block;
  padding: 6px 12px;
  cursor: pointer;
  border-radius: 4px;
  transition: background 0.3s;
}

.checkbox-option:hover {
  background: #f5f7fa;
}

.checkbox-option input {
  margin-right: 8px;
}

.annotation-actions {
  margin-bottom: 16px;
}

.create-annotation-btn {
  padding: 8px 16px;
  background: #409eff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
}

.annotation-list {
  max-height: 400px;
  overflow-y: auto;
}

.annotation-item {
  background: #f8f9fa;
  border: 1px solid #eee;
  border-radius: 6px;
  padding: 12px;
  margin-bottom: 8px;
}

.annotation-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.annotation-author {
  font-weight: 500;
  color: #333;
}

.annotation-time {
  font-size: 12px;
  color: #999;
}

.annotation-actions {
  display: flex;
  gap: 6px;
}

.annotation-actions button {
  padding: 4px 8px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: white;
  color: #606266;
  cursor: pointer;
  font-size: 12px;
}

.annotation-actions button:hover {
  background: #f5f7fa;
}

.annotation-content {
  padding: 8px 0;
  border-top: 1px solid #eee;
}

.annotation-content p {
  margin: 6px 0;
  font-size: 14px;
  line-height: 1.5;
}

.annotation-content strong {
  color: #333;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 6px;
  color: #333;
  font-weight: 500;
}

.form-group textarea {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  resize: vertical;
}

.modal-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 20px;
}

.modal-actions button {
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.save-btn {
  background: #409eff;
  color: white;
}

.save-btn:hover:not(:disabled) {
  background: #66b1ff;
}

.cancel-btn {
  background: #f5f7fa;
  color: #666;
  border: 1px solid #dcdfe6;
}

.cancel-btn:hover {
  background: #ecf5ff;
  color: #409eff;
}

.ai-analyze-btn {
  position: fixed;
  bottom: 30px;
  right: 30px;
  background: #ff6b35;
  color: white;
  border: none;
  border-radius: 50px;
  padding: 12px 20px;
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(255, 107, 53, 0.4);
  z-index: 999;
  transition: all 0.3s;
}

.ai-analyze-btn:hover:not(:disabled) {
  background: #ff8552;
  transform: translateY(-2px);
}

.ai-analyze-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
  transform: none;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 8px;
  width: 90%;
  max-width: 600px;
  max-height: 80vh;
  overflow-y: auto;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  border-bottom: 1px solid #eee;
}

.modal-header h3 {
  margin: 0;
  color: #333;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #999;
  padding: 0;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.close-btn:hover {
  color: #333;
}

.modal-body {
  padding: 24px;
}

.loading-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(255, 255, 255, 0.8);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 2000;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #e0e0e0;
  border-top: 4px solid #409eff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
</style>