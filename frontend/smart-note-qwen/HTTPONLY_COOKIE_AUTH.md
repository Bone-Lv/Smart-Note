# HttpOnly Cookie 认证机制实现说明

## ✅ 已完成的配置

### 1. Axios 配置 (`src/utils/request.js`)

```javascript
const request = axios.create({
  baseURL: 'http://localhost:8080',
  withCredentials: true, // ✅ 允许携带 Cookie
  timeout: 30000
});
```

**关键配置：**
- ✅ `withCredentials: true` - 允许跨域请求携带 Cookie
- ✅ 已配置 401 错误处理，自动跳转登录页
- ✅ 统一的错误提示机制

### 2. UserStore 状态管理 (`src/stores/userStore.js`)

**认证状态管理：**
```javascript
// ✅ 不再手动存储 Token
const userInfo = ref({});
const isLoggedIn = ref(false);

// ✅ 登录时不处理 Token，后端自动设置 Cookie
const login = async (credentials) => {
  const response = await loginApi(credentials);
  setUser(response.data);
  return { success: true };
};

// ✅ 退出登录时调用后端 API 清除 Cookie
const logout = async () => {
  await logoutApi(); // 后端会清除 Cookie
  clearUser();
};
```

### 3. 路由守卫 (`src/router/index.js`)

**认证检查逻辑：**
```javascript
router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore();
  const requiresAuth = to.matched.some(record => record.meta.requiresAuth);
  
  if (requiresAuth && !userStore.isLoggedIn) {
    // 调用后端 API 验证 Cookie 是否有效
    await checkAuthApi(); // { hideLoading: true } 静默请求
    userStore.setUser(userData);
  }
});
```

**关键特性：**
- ✅ 依赖后端 `/user/check-auth` 接口验证 Cookie 有效性
- ✅ 认证请求使用 `{ hideLoading: true }` 避免影响用户体验
- ✅ 验证失败自动跳转登录页

### 4. WebSocket 连接说明

```javascript
// ✅ WebSocket 握手时浏览器会自动携带 Cookie
// 无需在 URL 中传递 token 参数
const ws = new WebSocket('ws://localhost:8080/ws');
```

---

## 🔐 安全优势

### 1. XSS 防护
- ✅ Token 存储在 HttpOnly Cookie 中
- ✅ JavaScript 无法读取 `document.cookie`
- ✅ 防止恶意脚本窃取 Token

### 2. CSRF 防护
- ✅ 后端已配置 CORS 允许携带凭证
- ✅ 前端正确设置 `withCredentials: true`
- ✅ 建议后端实现 CSRF Token 验证

### 3. Token 自动刷新
- ✅ 后端在 Token 即将过期时自动更新 Cookie
- ✅ 前端无需手动处理刷新逻辑
- ✅ 用户体验更流畅

---

## 📝 开发注意事项

### ❌ 禁止的操作

```javascript
// ❌ 不要手动存储 Token
localStorage.setItem('token', token)
sessionStorage.setItem('token', token)

// ❌ 不要手动设置 Authorization header
axios.defaults.headers.common['Authorization'] = `Bearer ${token}`

// ❌ 不要尝试读取 Cookie
document.cookie // HttpOnly Cookie 无法读取
```

### ✅ 正确的做法

```javascript
// ✅ 只需调用 API，Cookie 会自动处理
await loginApi({ account, password });

// ✅ 检查登录状态使用 userStore
if (userStore.isLoggedIn) {
  // 用户已登录
}

// ✅ 退出登录调用 API
await logoutApi(); // 后端清除 Cookie
```

---

## 🔧 Cookie 技术细节

| 属性 | 值 | 说明 |
|------|-----|------|
| Name | `token` | Cookie 名称 |
| HttpOnly | `true` | JavaScript 无法访问 |
| Secure | `false` | 开发环境，生产环境改为 `true` |
| Path | `/` | 所有路径可访问 |
| MaxAge | `7200` | 2 小时有效期 |
| SameSite | `None` | 允许跨域携带 |

---

## 🚀 快速测试

### 1. 登录流程
```javascript
// 1. 用户输入账号密码
const result = await userStore.login({
  account: 'test@example.com',
  password: '123456'
});

// 2. 后端验证成功后，通过 Set-Cookie 响应头设置 Token
// 响应头示例：
// Set-Cookie: token=eyJhbGciOiJIUzI1NiIs...; HttpOnly; Path=/; Max-Age=7200

// 3. 浏览器自动保存 Cookie
// 4. 前端 userStore.isLoggedIn = true
```

### 2. 后续请求
```javascript
// 所有请求自动携带 Cookie
await getUserInfoApi(); // 请求头自动包含 Cookie: token=xxx

// 开发者工具 Network 面板可以看到
// Request Headers: Cookie: token=eyJhbGci...
```

### 3. 退出登录
```javascript
// 1. 调用退出 API
await userStore.logout();

// 2. 后端清除 Cookie
// 响应头示例：
// Set-Cookie: token=; HttpOnly; Path=/; Max-Age=0

// 3. 浏览器删除 Cookie
// 4. 前端 userStore.isLoggedIn = false
```

---

## ⚠️ 常见问题

### Q1: 为什么登录后刷新页面会退出？
**A:** 确保路由守卫中调用了 `checkAuthApi()` 验证 Cookie 有效性。

### Q2: 为什么请求没有携带 Cookie？
**A:** 检查 Axios 配置中 `withCredentials: true` 是否设置。

### Q3: 如何查看 Cookie？
**A:** 
1. 打开浏览器开发者工具
2. 切换到 Application/存储 标签
3. 左侧选择 Cookies → http://localhost:5173
4. 查看 token Cookie（但看不到值，因为是 HttpOnly）

### Q4: 跨域请求无法携带 Cookie？
**A:** 
1. 后端 CORS 配置必须包含 `allowCredentials: true`
2. 后端 `allowedOrigins` 不能使用 `*`，必须指定具体域名
3. 前端必须设置 `withCredentials: true`

---

## 📚 相关文档

- [Vue Router 路由鉴权规范](./MEMORY.md#vue-router路由鉴权与状态管理规范)
- [全局 Loading 状态管理](./MEMORY.md#全局loading状态管理规范与最佳实践)
- [UI 交互与 API 错误处理](./MEMORY.md#ui交互与api错误处理规范)

---

**最后更新:** 2026-04-17
**维护者:** 前端开发团队
