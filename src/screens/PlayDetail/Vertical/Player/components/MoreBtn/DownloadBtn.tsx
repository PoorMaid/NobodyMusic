import { useState, useEffect } from 'react'
import playerState from '@/store/player/state'
import { action as downloadAction } from '@/store/download'
import { state as downloadState } from '@/store/download'
import { useSettingValue } from '@/store/setting/hook'
import { useTheme } from '@/store/theme/hook'
import { toast, requestStoragePermission } from '@/utils/tools'
import Btn from './Btn'

export default () => {
  const theme = useTheme()
  const quality = useSettingValue('download.quality')
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

  const handleDownload = async() => {
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
    const granted = await requestStoragePermission()
    if (granted === false) {
      toast('未授予存储权限，可能无法保存文件')
    } else if (granted === null) {
      toast('存储权限被永久拒绝，请到系统设置中开启')
    }
    const ok = downloadAction.addTask(target as LX.Music.MusicInfoOnline, quality)
    toast(ok ? '已加入下载' : '已在下载列表中')
  }

  return (
    <Btn
      icon='download-2'
      color={downloaded ? theme['c-primary'] : undefined}
      onPress={() => { void handleDownload() }}
    />
  )
}
