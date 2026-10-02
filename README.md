# Nobody Music

**开源 · 免费 · 无广告的音乐播放器。** 本地优先，支持自定义音源插件，不依赖任何自营服务器。

> ## ⚠️ 当前为测试版（v1.3.1-debug / versionCode 5）
>
> 本项目目前处于**早期测试阶段**，功能与界面仍在快速迭代中，**稳定性无法保证**。
>
> 已知可能出现的问题包括但不限于：
> - 偶发闪退、界面卡顿、返回栈异常
> - 播放中断、歌词加载失败、切歌错位
> - 数据库升级导致的历史数据丢失（当前版本会清库重建）
> - 音源脚本兼容性问题、搜索无结果
> - 通知栏 / 悬浮窗歌词显示异常
>
> **请勿将其作为日常主力播放器使用，重要的收藏与歌单请提前备份。**
> 欢迎通过下方邮箱反馈 Bug，你的每一条反馈都会帮助它变得更好。

---

## 下载

| 渠道 | 地址 |
|---|---|
| 官方网站 | https://nobady.bbroot.com/business.html |
| GitHub Releases | https://github.com/PoorMaid/NobodyMusic/releases |

> 当前仅提供 **Debug 测试包**（约 24 MB），支持 Android 8.0 及以上。

---

## 功能一览

### 🎵 播放
- 本地音乐扫描与播放
- 在线音源搜索与播放（多源聚合）
- 后台播放与通知栏控制
- 播放队列管理、顺序 / 随机 / 单曲循环
- 倍速播放、睡眠定时
- 均衡器（Equalizer）音效调节

### 🔌 音源（插件化）
- 自定义音源：用户自行导入 JavaScript 插件
- 多音源并行搜索、结果聚合去重
- 音源订阅地址批量导入
- 内置 Rhino 脚本引擎，无需重新编译即可扩展

### 📚 音乐库
- 歌单创建 / 编辑 / 排序
- 收藏歌曲与收藏歌单
- 播放历史记录、搜索历史
- 本地音乐与在线音乐统一管理

### 📝 歌词
- 应用内歌词滚动显示
- 通知栏歌词
- 悬浮窗歌词（实验性）
- 无障碍分层歌词

### 🎨 界面与体验
- Material You 动态取色
- 深色模式 / 纯黑主题
- Jetpack Compose 全声明式 UI
- 播放页封面模糊背景

### 💾 数据
- 本地数据备份与恢复（不含敏感信息）
- 数据导出 / 导入

---

## 技术栈

- **语言**：Kotlin
- **UI**：Jetpack Compose + Material 3
- **播放**：Media3 / ExoPlayer + MediaSession
- **数据库**：Room
- **网络**：OkHttp + kotlinx.serialization
- **脚本引擎**：Mozilla Rhino（运行音源 JS 插件）

---

## 构建

```bash
./gradlew assembleDebug
```

环境要求：**JDK 17**、**Android SDK 34**。

---

## 项目结构

```
app/src/main/java/com/nobodymusic/tyxypoor/
├── backup/      数据备份与恢复
├── data/        数据模型、数据库、仓库
├── di/          依赖定位
├── localmusic/  本地音乐扫描
├── player/      播放器控制与播放服务
├── source/      音源管理与脚本引擎
└── ui/          界面（主题、组件、屏幕）

website/         官方网站源码（nobady.bbroot.com）
```

---

## 开源协议

**GPL-3.0** — 你可以自由使用、修改和分发，但衍生作品必须同样开源。

---

## 免责声明

本项目**不提供任何音乐内容**，所有音源均由用户自行导入。
请支持正版音乐，遵守当地法律法规。

---

## 联系方式

- 作者：Nobody 团队
- 反馈邮箱：tyxypoor@Outlook.com
- 官方网站：https://nobady.bbroot.com
- GitHub：https://github.com/PoorMaid