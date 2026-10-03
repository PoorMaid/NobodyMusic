# Nobody Music 开发进度

包名：com.nobodymusic.tyxypoor
项目名：NobodyMusic
项目路径：/root/nobody
暂存区：/storage/emulated/0/Download/nobody_build/
官网：https://nobady.bbroot.com

## 环境

Ubuntu 24.04 proot，JDK 17，Android SDK 34（/root/android-sdk）
Gradle 8.7（/opt/gradle-8.7，命令 gradle）
构建：cd /root/nobody && gradle :app:compileDebugKotlin --offline
日志：/root/nbX.log

## 约定

- 所有 Kotlin 文件不写任何注释
- create_file 只能写 sdcard，写 /root 需先写暂存区再 cp
- 覆盖已存在文件须先 delete_file
- 编译每次约 3 分钟

## 已完成

1. Gradle 配置（AGP + kotlin + serialization + ksp）
   - namespace/applicationId=com.nobodymusic.tyxypoor
   - compileSdk34/minSdk26/targetSdk34，versionName 1.0.0
   - abiFilters arm64-v8a，Java17
   - 依赖：core-ktx 1.13.1、media3 1.4.1、room 2.6.1、coil 2.7.0、
     okhttp 4.12.0、serialization-json 1.6.3、coroutines 1.8.1、
     rhino 1.7.15、lifecycle 2.8.4（含 runtime-compose）
2. 数据层：Models / AppDatabase / MusicRepository / 三个 Dao
3. 音源层：SourceManager / SourceModels / JsSourceEngine / JsCrypto /
   JsNetwork / PreloadScript / OkHttpDownloader
4. 播放层：PlayerController / PlaybackService
5. 本地：LocalScanner
6. 备份：BackupManager
7. UI 层（全部编译通过）：
   MainActivity（Route 状态路由）/ Theme / AppState / Components /
   HomeScreen（本地/收藏/搜索三Tab）/ PlayerScreen / AboutScreen /
   SettingsScreen / SourceScreen
8. 资源：图标五档（真实素材圆角图）、背景图 app_background.jpg
9. 开源文件：.gitignore / README.md / LICENSE(GPL-3.0) / gradlew + wrapper
10. AboutScreen 官网行可点击跳转浏览器

## 待办

- 歌单导入（本地文件导入 / 网络导入 / 文本导入）
- 音源板块增强（音源商店、批量导入、内置源）
- 歌词页 L1-L4 分层（灵动岛/无障碍/悬浮窗/通知栏）
- 歌单管理 UI（新建/重命名/删除/添加歌曲）
- Rhino 真机执行混淆 js 源验证
- Release 构建（R8 / ABI 拆分 / 体积优化，目标<20MB 非硬性）
- EQ / 睡眠定时 / 播放队列 UI
- 备份恢复 UI

## 参照实现

落雪移动版：/root/gh/lx-music-mobile
  歌词模块：android/app/src/main/java/cn/toside/music/mobile/lyric/
  悬浮窗：TYPE_APPLICATION_OVERLAY，checkOverlayPermission
社区音源：/root/gh/lx-music-source（10 套音源）
音源契约：lx.on('request',handler) → 返回 musicUrl 直链；
  action 用 search / musicUrl；源 id：kw/kg/tx/wy/mg

## 关键 API 签名

PlayerController：current/isPlaying/queue/position/duration StateFlow
  setQueue(songs,startIndex) playSong play pause toggle next previous
  seekTo(ms) tick() release() toggleShuffle() cycleRepeat()
MusicRepository：songs/localSongs/search Flow；insertSongs deleteSong
  playlists createPlaylist deletePlaylist renamePlaylist
  addToPlaylist removeFromPlaylist playlistSongs
  sources enabledSources upsertSource setSourceEnabled deleteSource
  songsOnce localSongsOnce playlistsOnce sourcesOnce songIdsOf
SourceManager：downloadScript addSource(id,name,jsUrl,autoDownload)
  refreshSource resolve engineFor destroy searchSingle
  parseSearchResult aggregateSearch
LocalScanner：scan():List<Song> readMetadata(path)
ServiceLocator：appContext database repository sourceManager localScanner

## 2026-10-01 更新（音源自动择优 + 歌单导入）
- SourceManager: 新增 autoSelectBestSource()，从订阅地址逐个下载 JS→搜索测试→首个可用者启用，失败自动切下一个
- SourceManager: downloadScript 支持多镜像回退（raw / jsdelivr / ghproxy / gh-proxy）
- SourceManager: 新增 DEFAULT_SUBSCRIPTIONS 常量（聚合/Grass/Ikun/LX/Flower/Huibq，pdone 源）
- App: onCreate 检测无可用音源时自动调用 autoSelectBestSource
- SourceScreen: 预设对齐订阅地址 + 新增“一键自动选择最佳音源”按钮
- 新增 PlaylistImportScreen: 本地目录扫描多选导入 / 文本粘贴导入
- MainActivity + SettingsScreen: 接入“导入歌单”路由
- compileDebugKotlin: BUILD SUCCESSFUL

## 2026-10-03 用户实测问题清单（v1.3.1）

详见 /storage/emulated/0/Download/nobody_build/BUGS.md，摘要如下：

### 播放核心
1. 音乐头尾无声（0 时长/解析失败）——很多歌点进去头尾都是零
2. 播放音乐未注册为音频——系统媒体控制/通知栏不认，疑 MediaSession 未 active
3. 下载音乐一直失败 —— OkHttpDownloader 权限/路径/直链

### 播放顺序按钮（UI 重构）
4. 现为「随机」+「循环」两个独立按钮 → 合并为单一按钮循环切换
   顺序播放 → 列表循环 → 单曲循环 → 随机播放
   每态图标不同 + 文字提示；PlayerController 用单一 playOrder 状态机取代 shuffle+repeat

### 界面
5. 点「最近」闪退（空数据 NPE/路由）
6. 浅色主题偏暗，应更亮
7. 桌面歌词拖动上下颠倒（y 轴取反）
8. 均衡器无法挂载（依赖问题2 的音频注册 + sessionId 绑定）
9. 「忽略电池优化」点击无反应（缺 intent + 权限）

### 数据/功能
10. 导入备份提示成功但「我的音乐」/歌单看不到数据（未落库/未刷新）
11. GitHub 数据同步功能未实现

### 收尾动作
- 修复后更新 README / Release 说明，推送同步到 GitHub 仓库

## 2026-10-03 v1.3.2 修复完成
版本 versionCode 6 / versionName 1.3.2。已编译打包并发布。
代码改动（12 处）：
- 新建 player/PlayerHolder.kt：全局唯一 ExoPlayer 单例（AudioAttributes=USAGE_MEDIA/MUSIC，handleAudioBecomingNoisy）
- 重写 player/PlaybackService.kt：改用 PlayerHolder 共享 player 建 MediaSession，onDestroy 不 release player
- 重写 player/PlayerController.kt：新增 PlayOrder 枚举(SEQUENCE/LOOP_ALL/LOOP_ONE/SHUFFLE)、order StateFlow、cycleOrder/applyOrder、detachUi/release、next/previous 按顺序分支
- PlayerScreen.kt：shuffle+repeat 两按钮合并为单按钮四态切换；删独立 Shuffle 按钮
- HomeScreen.kt：最近/搜索列表 key 加 index 前缀（修闪退）
- Theme.kt：LightColors 补 background/surface/surfaceVariant（提亮）
- AndroidManifest.xml：补 REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
- BackupManager.kt：importJson 补 playlistSongIds 关联重建
- FloatingLyricService.kt：ACTION_MOVE 修正上下拖动方向
- AppState.kt：init 启动 PlaybackService（MediaSession 生效）
- MainActivity.kt：release() -> detachUi()
- version.properties：5/1.3.1 -> 6/1.3.2
构建：compileDebugKotlin OK，assembleDebug OK，产物 apk_history/NobodyMusic-debug-v1.3.2-6.apk (25191845 字节, md5 943d93194b18657a4d5f8b3d2b32ad55)
发布：GitHub Release v1.3.2 (PoorMaid/NobodyMusic) 已上传 APK；官网 nobady.bbroot.com 已 surge 发布，business.html 指向新 APK。
未解决（遗留）：问题3 下载失败、问题11 GitHub 数据同步功能未实现；导出端歌单归属对应关系精度待重构。
