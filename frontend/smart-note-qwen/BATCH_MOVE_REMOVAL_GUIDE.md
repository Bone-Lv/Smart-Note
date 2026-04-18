# 批量移动功能移除说明

## 📋 已完成的工作

### 1. ✅ 删除组件文件
- `src/components/BatchMoveNotes.vue` - 已删除

### 2. ✅ 添加移动笔记API
- `src/api/note.js` - 已添加 `moveNoteApi(noteId, folderId)`

### 3. ⚠️ NoteManager.vue 需要手动清理

由于自动化工具的限制，以下代码需要**手动删除或注释**：

#### 位置1: Template部分 (约第38-45行)
```vue
<!-- 删除这段 -->
<button 
  v-if="selectedNotes.length > 0"
  @click="showBatchMoveDialog = true" 
  class="batch-action-btn"
>
  <i class="fas fa-folder-open"></i>
  批量移动 ({{ selectedNotes.length }})
</button>
```

#### 位置2: Template部分 (约第60-70行)
```vue
<!-- 修改note-card，移除多选相关 -->
<div 
  v-for="note in notes" 
  :key="note.id" 
  class="note-card"
  <!-- 删除这行: :class="{ selected: selectedNotes.includes(note) }" -->
  @click="goToNote(note.id)"  <!-- 改回简单的点击跳转 -->
>
  <!-- 删除整个note-checkbox div -->
  <div class="note-checkbox" @click.stop>
    <input 
      type="checkbox" 
      :checked="selectedNotes.includes(note)"
      @change="toggleNoteSelection(note)"
    >
  </div>
```

#### 位置3: Template末尾 (约第262-266行)
```vue
<!-- 删除这段 -->
<BatchMoveNotes
  v-model="showBatchMoveDialog"
  :selected-notes="selectedNotes"
  @moved="handleBatchMoved"
/>
```

#### 位置4: Script导入 (约第278行)
```javascript
// 删除这行
import BatchMoveNotes from '../../components/BatchMoveNotes.vue';
```

#### 位置5: Components注册 (约第293行)
```javascript
components: {
  FolderTree
  // 删除这行: BatchMoveNotes
},
```

#### 位置6: State定义 (约第303行)
```javascript
// 删除这行
// const selectedNotes = ref([]);
```

#### 位置7: 函数定义 (约第635-655行)
```javascript
// 删除这些函数
const handleNoteClick = (event, note) => { /* ... */ };
const toggleNoteSelection = (note) => { /* ... */ };
const handleBatchMoved = async (targetFolderId) => { /* ... */ };
```

#### 位置8: Return语句 (约第688-691行)
```javascript
return {
  // ... 其他属性
  handleFolderSupported  // ← 确保这里没有注释
};
```

#### 位置9: Style样式 (约第871-890行)
```css
/* 删除这些样式 */
.batch-action-btn { /* ... */ }
.note-card.selected { /* ... */ }
.note-checkbox { /* ... */ }
```

---

## 🔧 快速修复步骤

### 方法1: 使用VSCode搜索替换

1. **打开 NoteManager.vue**
2. **搜索并删除**:
   - 搜索 `selectedNotes` → 删除所有相关代码
   - 搜索 `BatchMoveNotes` → 删除所有相关代码
   - 搜索 `batch-action-btn` → 删除按钮和样式
   - 搜索 `note-checkbox` → 删除多选框
   - 搜索 `handleNoteClick` → 删除函数
   - 搜索 `toggleNoteSelection` → 删除函数
   - 搜索 `handleBatchMoved` → 删除函数

3. **修改note-card点击事件**:
   ```vue
   <!-- 从 -->
   @click="handleNoteClick($event, note)"
   <!-- 改为 -->
   @click="goToNote(note.id)"
   ```

### 方法2: 恢复到之前的版本

如果你之前有备份，可以直接恢复NoteManager.vue到添加批量移动功能之前的版本。

---

## ✅ 验证清单

清理完成后，确保：
- [ ] 没有编译错误
- [ ] 笔记卡片可以正常点击跳转
- [ ] 文件夹选择功能正常
- [ ] 单个笔记移动功能可用（通过编辑笔记时选择文件夹）
- [ ] 页面样式正常，没有多余的空隙

---

## 💡 替代方案

既然不支持批量移动，用户可以通过以下方式移动笔记：

1. **编辑笔记时修改文件夹**:
   - 点击笔记进入编辑页面
   - 在表单中选择目标文件夹
   - 保存即可

2. **未来可以考虑**:
   - 在笔记列表中添加"移动到文件夹"的右键菜单选项
   - 支持拖拽笔记到文件夹树中

---

**注意**: 由于自动化清理遇到困难，建议手动执行上述清理步骤，或使用Git恢复到之前的干净版本。
