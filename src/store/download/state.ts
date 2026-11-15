export type DownloadStatus = LX.Download.DownloadTaskStatus

export interface DownloadTask {
  id: string
  musicInfo: LX.Music.MusicInfoOnline
  quality: LX.Quality
  ext: LX.Download.FileExt
  fileName: string
  filePath: string
  status: DownloadStatus
  statusText: string
  downloaded: number
  total: number
  progress: number
  speed: string
  jobId: number | null
  startAt: number
}

export interface InitState {
  tasks: DownloadTask[]
  activeList: DownloadStatus | 'all'
}

const state: InitState = {
  tasks: [],
  activeList: 'all',
}

export default state
