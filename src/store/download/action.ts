import { getMusicUrl } from '@/core/music/online'
import { downloadFile, downloadToPublic, stopDownload, existsFile, mkdir, isPublicStoragePath, externalStorageDirectoryPath } from '@/utils/fs'
import settingState from '@/store/setting/state'
import state, { type DownloadTask, type InitState } from './state'

const MAX_CONCURRENT = 2

let seq = 0

const emitUpdate = () => {
  global.state_event.downloadTasksUpdated(state.tasks)
}

const getExtByQuality = (quality: LX.Quality): LX.Download.FileExt => {
  switch (quality) {
    case 'flac':
    case 'flac24bit':
      return 'flac'
    case 'ape':
      return 'ape'
    case 'wav':
      return 'wav'
    default:
      return 'mp3'
  }
}

const sanitize = (name: string) => name.replace(/[\\/:*?"<>|]/g, '_').trim()

const buildFileName = (musicInfo: LX.Music.MusicInfoOnline, ext: LX.Download.FileExt) => {
  const name = settingState.setting['download.fileName'] == '歌名' ? musicInfo.name : `${musicInfo.name} - ${musicInfo.singer}`
  return sanitize(name) + '.' + ext
}

export const getDownloadDir = () => {
  const dir = settingState.setting['download.savePath']
  return dir || `${externalStorageDirectoryPath}/Download/NobodyMusic`
}

const updateTask = (task: DownloadTask, patch: Partial<DownloadTask>) => {
  Object.assign(task, patch)
  emitUpdate()
}

const dlOptions = (task: DownloadTask) => ({
  progressInterval: 500,
  progressDivider: 1,
  begin: (res: { jobId: number, contentLength: number }) => {
    task.jobId = res.jobId
    updateTask(task, { total: res.contentLength || 0 })
  },
  progress: (res: { bytesWritten: number, contentLength: number }) => {
    const total = task.total || res.contentLength || 0
    const progress = total > 0 ? Math.min(100, Math.round(res.bytesWritten / total * 100)) : 0
    updateTask(task, {
      downloaded: res.bytesWritten,
      progress,
      speed: (res.bytesWritten / Math.max(1, (Date.now() - task.startAt) / 1000) / 1024).toFixed(1) + ' KB/s',
    })
  },
})

const startTask = async(task: DownloadTask) => {
  task.jobId = null
  updateTask(task, { status: 'run', statusText: '下载中', progress: 0, speed: '0 B/s' })
  try {
    let url = await getMusicUrl({ musicInfo: task.musicInfo, quality: task.quality, isRefresh: true })
    if (!url) throw new Error('empty url')
    const dir = getDownloadDir()
    const usePublic = isPublicStoragePath(dir)
    task.filePath = `${dir}/${task.fileName}`
    if (!usePublic) {
      await mkdir(dir)
      let filePath = `${dir}/${task.fileName}`
      if (await existsFile(filePath)) {
        filePath = `${dir}/${task.fileName.replace(/(\.[^.]+)$/, '')}(${seq}).$1`
        task.fileName = filePath.split('/').pop() || task.fileName
      }
      task.filePath = filePath
    }
    const dl = usePublic
      ? { promise: downloadToPublic(url, dir, task.fileName, dlOptions(task)) }
      : downloadFile(url, task.filePath, dlOptions(task))
    const res = await dl.promise
    if (res.statusCode && res.statusCode >= 400) throw new Error('http ' + res.statusCode)
    updateTask(task, { status: 'completed', statusText: '已完成', progress: 100, speed: '' })
  } catch (err: any) {
    updateTask(task, { status: 'error', statusText: String(err?.message || err), speed: '' })
  } finally {
    task.jobId = null
    void runQueue()
  }
}

const runQueue = async() => {
  const running = state.tasks.filter(t => t.status == 'run').length
  let slots = MAX_CONCURRENT - running
  if (slots <= 0) return
  const waiting = state.tasks.filter(t => t.status == 'waiting')
  for (const task of waiting) {
    if (slots-- <= 0) break
    void startTask(task)
  }
}

export const addTask = (musicInfo: LX.Music.MusicInfoOnline, quality: LX.Quality) => {
  const exist = state.tasks.find(t => t.musicInfo.id == musicInfo.id && t.quality == quality && t.status == 'completed')
  if (exist) return false
  const ext = getExtByQuality(quality)
  const task: DownloadTask = {
    id: 'dl_' + Date.now() + '_' + (seq++),
    musicInfo,
    quality,
    ext,
    fileName: buildFileName(musicInfo, ext),
    filePath: '',
    status: 'waiting',
    statusText: '等待中',
    downloaded: 0,
    total: 0,
    progress: 0,
    speed: '',
    jobId: null,
    startAt: Date.now(),
  }
  state.tasks.push(task)
  emitUpdate()
  void runQueue()
  return true
}

export const removeTask = async(id: string) => {
  const idx = state.tasks.findIndex(t => t.id == id)
  if (idx < 0) return
  const task = state.tasks[idx]
  if (task.jobId != null) await stopDownload(task.jobId)
  state.tasks.splice(idx, 1)
  emitUpdate()
  void runQueue()
}

export const retryTask = (id: string) => {
  const task = state.tasks.find(t => t.id == id)
  if (!task || task.status == 'run') return
  task.startAt = Date.now()
  updateTask(task, { status: 'waiting', statusText: '等待中', progress: 0, downloaded: 0, speed: '' })
  void runQueue()
}

export const setActiveList = (activeList: InitState['activeList']) => {
  state.activeList = activeList
  emitUpdate()
}

