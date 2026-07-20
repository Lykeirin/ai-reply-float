# AI 回复建议 · 安卓浮窗 App（方案 C）

一个**常驻悬浮窗**的安卓 App：你切到微信聊天时，它浮在最上层。
长按对方消息 → 复制 → 点浮窗「粘贴」→「生成候选」→ 点任意一条，**自动复制到剪贴板**，
回微信粘贴发送即可。

- **零自动发送**：只给建议，永远由你亲手发送，零封号风险。
- **零 root / 零 curl / 零 Termux**：请求由 App 自己的网络栈直连云端，不碰系统二进制。
  这正是它比「Trime 输入法方案」稳的地方——华为 `/sdcard` 是 noexec，Trime 方案卡死在 curl 上，
  而本 App 根本不需要 curl。
- **云端已上线**：`https://novelgrok.nsfs.cn/suggest`，Token 已写死在 App 默认值里，一般不用改。

---

## 一、怎么拿到 APK（两条路，任选）

### 路线 A：GitHub Actions 自动构建（推荐，本机零环境）

你完全不需要装 Android Studio，让 GitHub 的免费机器帮你编译：

1. 注册 / 登录 GitHub（github.com）。
2. 新建一个**空仓库**（名字随便，比如 `ai-reply-float`）。
3. 把本目录 `ai-reply-float/` 下的**所有文件**上传到这个仓库
   （可以用 GitHub 网页的「Add file → Upload files」，直接把文件夹拖进去）。
4. 进仓库的 **Actions** 标签页 → 看到 `Build Debug APK` → 点 **Run workflow** → 确认运行。
5. 等 2~4 分钟变绿，点进该次运行 → 最下方 **Artifacts** → 下载 `ai-reply-float-debug`（是个 zip）。
6. 解压得到 `app-debug.apk`，传到手机（微信文件传输助手 / 数据线 / 网盘都行）。

> 没有 GitHub 账号或不想折腾？走路线 B。

### 路线 B：本机 Android Studio 构建

1. 在 Mac/Windows 装 [Android Studio](https://developer.android.com/studio)。
2. 打开 Android Studio → **Open** → 选本目录 `ai-reply-float`（首次会提示生成 Gradle Wrapper，点确定）。
3. 等待「Sync Project with Gradle Files」完成。
4. 菜单 **Build → Build Bundle(s) / APK(s) → Build APK(s)**。
5. 编译完右下角弹窗，点 **locate** 找到 `app-debug.apk`，传手机。

---

## 二、安装到华为手机（HarmonyOS 4.x）

1. **退出「纯净模式」**：设置 → 系统和更新 → 纯净模式 → 退出（不退出不让装外部 APK）。
2. **允许「未知来源」**：设置 → 安全 → 更多安全设置 → 安装未知应用 → 给「文件管理 / 浏览器」打开开关。
3. 找到传过来的 `app-debug.apk`，点它 → 安装。
4. 打开 App，第一屏点 **「启动浮窗」**。
5. 华为会弹出「是否允许在其他应用上层显示」→ **允许**。
   （若没弹，手动去：设置 → 应用 → 应用管理 → AI 回复建议 → 权限 → 开启「悬浮窗 / 显示在其他应用上」。）
6. 回到桌面，左上角出现绿色小条「AI 回复建议」即成功。

---

## 三、日常使用

1. 在微信里长按对方的消息 → **复制**。
2. 点屏幕左上角的浮窗 → 点**「粘贴」**（对方消息进框）→ 点**「生成候选」**。
3. 稍候出现 4 条候选（本色 / 幽默 / 高冷 / 角色）。
4. 点你中意的那条 → **已复制到剪贴板** → 切回微信粘贴发送。

浮窗可拖动（拖顶部绿条）、可折叠（点 `▾` / `▴`）、可关（点 `✕`，再开就重进 App 点启动）。

---

## 四、配置（一般不用改）

App 里点「设置」可改：

- **服务器地址**：默认 `https://novelgrok.nsfs.cn`
- **Token**：默认 `8104dbd8ac33919dcab4ea0f6070aeae6cb53af3`（云端 Bearer 鉴权）
- **候选条数**：默认 4（1~6）

这两个默认值和云端一一对应，正常情况无需动。

---

## 五、即时免构建备选（不想装 App 时）

云端还有个网页版：`https://novelgrok.nsfs.cn/app`
华为浏览器打开 → 菜单「添加到主屏幕」→ 之后当网页 App 用。
HarmonyOS 4.x 支持把应用开成**小窗 / 悬浮窗**，把网页开在小窗里，也能边聊边点。
（网页版需要手动复制粘贴，体验略逊于原生浮窗，但零安装。）

---

## 六、排错

| 现象 | 处理 |
|------|------|
| 启动后看不到浮窗 | 检查「悬浮窗」权限是否授予（见二·5）；或重启 App 再点启动 |
| 点生成一直「生成中…」 | 飞行模式/网络问题；确认能开 `https://novelgrok.nsfs.cn/health` |
| 提示「服务端错误 401」 | Token 被改错，去设置把 Token 贴回默认值 |
| 候选只有 1~2 条 | 正常（模型偶尔少给）；多生成几次即可 |
| 软键盘挡住输入框 | 浮窗主要走「粘贴」流程，直接粘贴即可，无需手打 |

---

## 七、工程结构（给想改代码的人）

```
ai-reply-float/
├─ app/src/main/java/com/nsfs/aireply/
│   ├─ MainActivity.kt       # 入口：权限引导 + 启动浮窗
│   ├─ SettingsActivity.kt   # 服务器 / Token / 条数
│   ├─ FloatingService.kt   # 核心：浮窗 UI + 拖拽 + 点击复制
│   ├─ ApiClient.kt         # 直连云端 /suggest（HttpURLConnection，无 curl）
│   └─ Prefs.kt             # SharedPreferences 持久化
├─ app/src/main/res/        # 布局与主题
└─ .github/workflows/build.yml  # 自动构建出 APK
```

接口契约：`GET /suggest?fmt=lines&token=<T>&n=<条数>&context=<URL编码的对方消息>`
返回每行 `风格|文本`，App 解析后逐条展示。
