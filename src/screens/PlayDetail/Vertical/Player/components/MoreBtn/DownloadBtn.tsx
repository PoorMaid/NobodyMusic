import { useState, useEffect } from 'react'
import playerState from '@/store/player/state'
import { action as downloadAction } from '@/store/download'
import { state as downloadState } from '@/store/download'
import { useTheme } from '@/store/theme/hook'
import { toast } from '@/utils/tools'
import Btn from './Btn'

export default () => {
  const theme = useTheme()
  const musicInfo = playerState.playMusicInfo.musicInfo
  const [downloaded, setDownloaded] = useState(false)

  const id = musicInfo ? ('progress' in musicInfo ? musicInfo.metadata.musicInfo.id : musicInfo.id) : ''

  useEffect(() => {
    if (!id) {
      setDownloaded(false)
      return
    }
    const check = () => {
      setDownloaded(downloadState.tasks.some(t => t.musicInfo.id == id && t.status == 'completed'))
    }
    check()
    const timer = setInterval(check, 1500)
    return () => { clearInterval(timer) }
  }, [id])

  const handleDownload = () => {
    if (!musicInfo) return
    const target = 'progress' in musicInfo ? musicInfo.metadata.musicInfo : musicInfo
    if (!('source' in target)) {
      toast('本地歌曲无需下载')
      return
    }
    if (downloaded) {
      toast('已下载')
      return
    }
    const ok = downloadAction.addTask(target as LX.Music.MusicInfoOnline, '320k')
    toast(ok ? '已加入下载' : '已在下载列表中')
  }

  return (
    <Btn
      icon='download-2'
      color={downloaded ? theme['c-primary'] : undefined}
      onPress={handleDownload}
    />
  )
}
