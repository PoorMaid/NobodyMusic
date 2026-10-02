# Nobody Music

开源、免费、无广告的音乐播放器。本地优先，支持自定义音源插件，不依赖任何自营服务器。

## 特性

- 本地音乐扫描与播放
- 自定义音源（JavaScript 插件）多源搜索与播放
- 歌单、收藏、播放历史
- 歌词显示（应用内 / 通知栏 / 悬浮窗 / 无障碍分层）
- 均衡器、睡眠定时、播放队列
- 数据备份与恢复（不含敏感信息）
- 动态取色、深色模式、纯黑主题

## 技术栈

- Kotlin + Jetpack Compose
- Media3 / ExoPlayer + MediaSession
- Room
- OkHttp + kotlinx.serialization
- Rhino（音源脚本引擎）

## 构建

```bash
./gradlew assembleDebug
```

环境要求：JDK 17、Android SDK 34。

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
```

## 开源协议

GPL-3.0

## 免责声明

本项目不提供任何音乐内容，所有音源均由用户自行导入。请支持正版音乐。

## 联系方式

- 作者：Nobody 团队
- 反馈邮箱：tyxypoor@Outlook.com
- 官方网站：https://nobady.bbroot.com
