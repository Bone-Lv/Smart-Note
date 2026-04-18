<template>
  <div class="profile-page">
    <div class="profile-container">
      <h2>个人资料</h2>
      
      <div class="profile-content">
        <!-- 头像区域 -->
        <div class="avatar-section">
          <div class="avatar-wrapper">
            <img 
              :src="previewAvatar || userStore.userInfo?.avatar || '/default-avatar.svg'" 
              alt="头像" 
              class="avatar"
            />
            <div class="avatar-overlay" @click="triggerFileInput">
              <i class="fas fa-camera"></i>
              <span>更换头像</span>
            </div>
          </div>
          <input 
            ref="fileInput" 
            type="file" 
            accept="image/*" 
            @change="handleAvatarChange" 
            style="display: none"
          />
          <p class="avatar-tip">支持 JPG、PNG 格式，大小不超过 2MB</p>
        </div>

        <!-- 信息表单 -->
        <el-form 
          :model="profileForm" 
          label-width="100px" 
          class="profile-form"
        >
          <el-form-item label="用户名">
            <el-input 
              v-model="profileForm.username" 
              placeholder="请输入用户名"
              maxlength="50"
            />
          </el-form-item>

          <el-form-item label="邮箱">
            <el-input 
              :value="userStore.userInfo?.email" 
              disabled
            />
            <span class="form-tip">邮箱不可修改</span>
          </el-form-item>

          <el-form-item label="手机号">
            <el-input 
              v-model="profileForm.phone" 
              placeholder="请输入手机号"
              maxlength="11"
            />
          </el-form-item>

          <el-form-item label="座右铭">
            <el-input 
              v-model="profileForm.motto" 
              type="textarea"
              :rows="3"
              placeholder="写一句话介绍自己吧~"
              maxlength="200"
              show-word-limit
            />
          </el-form-item>

          <el-form-item>
            <el-button 
              type="primary" 
              @click="saveProfile"
              :loading="saving"
            >
              保存修改
            </el-button>
            <el-button @click="resetForm">重置</el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue';
import { useUserStore } from '../../stores/userStore.js';
import { updateUserInfoApi, uploadAvatarApi } from '../../api/auth.js';
import { ElMessage } from 'element-plus';

export default {
  name: 'UserProfile',
  setup() {
    const userStore = useUserStore();
    const fileInput = ref(null);
    const saving = ref(false);
    const uploadingAvatar = ref(false);
    const previewAvatar = ref(null);
    const selectedAvatarFile = ref(null);

    const profileForm = ref({
      username: '',
      phone: '',
      motto: ''
    });

    // 初始化表单数据
    const initForm = () => {
      if (userStore.userInfo) {
        profileForm.value = {
          username: userStore.userInfo.username || '',
          phone: userStore.userInfo.phone || '',
          motto: userStore.userInfo.motto || ''
        };
      }
    };

    // 触发文件选择
    const triggerFileInput = () => {
      fileInput.value?.click();
    };

    // 处理头像选择
    const handleAvatarChange = (event) => {
      const file = event.target.files[0];
      if (!file) return;

      // 验证文件类型
      if (!file.type.startsWith('image/')) {
        ElMessage.error('请选择图片文件');
        return;
      }

      // 验证文件大小（2MB）
      if (file.size > 2 * 1024 * 1024) {
        ElMessage.error('图片大小不能超过 2MB');
        return;
      }

      selectedAvatarFile.value = file;
      
      // 预览头像
      const reader = new FileReader();
      reader.onload = (e) => {
        previewAvatar.value = e.target.result;
      };
      reader.readAsDataURL(file);
    };

    // 上传头像
    const uploadAvatar = async () => {
      if (!selectedAvatarFile.value) return;

      try {
        uploadingAvatar.value = true;
        const response = await uploadAvatarApi(selectedAvatarFile.value);
        
        // 更新用户Store
        await userStore.fetchUserInfo();
        
        ElMessage.success('头像上传成功');
        previewAvatar.value = null;
        selectedAvatarFile.value = null;
      } catch (error) {
        console.error('上传头像失败:', error);
        // 错误提示已由 Axios 拦截器统一处理
      } finally {
        uploadingAvatar.value = false;
      }
    };

    // 保存个人资料
    const saveProfile = async () => {
      // 验证用户名
      if (!profileForm.value.username.trim()) {
        ElMessage.warning('请输入用户名');
        return;
      }

      try {
        saving.value = true;

        // 如果有新头像，先上传头像
        if (selectedAvatarFile.value) {
          await uploadAvatar();
        }

        // 更新用户信息
        await updateUserInfoApi({
          username: profileForm.value.username,
          phone: profileForm.value.phone,
          motto: profileForm.value.motto
        });

        // 刷新用户Store
        await userStore.fetchUserInfo();

        ElMessage.success('资料保存成功');
      } catch (error) {
        console.error('保存资料失败:', error);
        // 错误提示已由 Axios 拦截器统一处理
      } finally {
        saving.value = false;
      }
    };

    // 重置表单
    const resetForm = () => {
      initForm();
      previewAvatar.value = null;
      selectedAvatarFile.value = null;
      if (fileInput.value) {
        fileInput.value.value = '';
      }
    };

    onMounted(() => {
      initForm();
    });

    return {
      userStore,
      profileForm,
      fileInput,
      saving,
      uploadingAvatar,
      previewAvatar,
      triggerFileInput,
      handleAvatarChange,
      saveProfile,
      resetForm
    };
  }
};
</script>

<style scoped>
.profile-page {
  padding: 2rem;
  max-width: 800px;
  margin: 0 auto;
}

.profile-container h2 {
  margin-bottom: 2rem;
  color: #303133;
  font-size: 1.5rem;
}

.profile-content {
  background: white;
  padding: 2rem;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.avatar-section {
  text-align: center;
  margin-bottom: 2rem;
  padding-bottom: 2rem;
  border-bottom: 1px solid #e4e7ed;
}

.avatar-wrapper {
  position: relative;
  display: inline-block;
  margin-bottom: 1rem;
}

.avatar {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid #e4e7ed;
}

.avatar-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: white;
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.3s;
}

.avatar-wrapper:hover .avatar-overlay {
  opacity: 1;
}

.avatar-overlay i {
  font-size: 1.5rem;
  margin-bottom: 0.5rem;
}

.avatar-overlay span {
  font-size: 0.875rem;
}

.avatar-tip {
  color: #909399;
  font-size: 0.875rem;
  margin: 0;
}

.profile-form {
  max-width: 500px;
  margin: 0 auto;
}

.form-tip {
  color: #909399;
  font-size: 0.875rem;
  margin-left: 0.5rem;
}
</style>
