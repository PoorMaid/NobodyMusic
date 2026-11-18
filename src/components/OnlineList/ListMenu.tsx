import { useRef, useImperativeHandle, forwardRef, useState } from 'react'
import { useI18n } from '@/lang'
import Menu, { type Menus, type MenuType, type Position } from '@/components/common/Menu'
import { hasDislike } from '@/core/dislikeList'
import { hasMusicUrlByMusic } from '@/utils/data'
import settingState from '@/store/setting/state'
import { requestStoragePermission, toast } from '@/utils/tools'
import { action as downloadAction } from '@/store/download'
import DownloadSettingModal, { type DownloadSettingModalRefType } from '@/components/DownloadSettingModal'

export interface SelectInfo {
  musicInfo: LX.Music.MusicInfoOnline
  selectedList: LX.Music.MusicInfoOnline[]
  index: number
  single: boolean
}
const initSelectInfo = {}

const hasUrlCache = async(musicInfo: LX.Music.MusicInfo) => {
  return hasMusicUrlByMusic(musicInfo)
}
export interface ListMenuProps {
  onPlay: (selectInfo: SelectInfo) => void
  onPlayLater: (selectInfo: SelectInfo) => void
  onAdd: (selectInfo: SelectInfo) => void
  onCopyName: (selectInfo: SelectInfo) => void
  onMusicSourceDetail: (selectInfo: SelectInfo) => void
  onRemoveCache: (selectInfo: SelectInfo) => void
  onDislikeMusic: (selectInfo: SelectInfo) => void
}
export interface ListMenuType {
  show: (selectInfo: SelectInfo, position: Position) => void
}

export type {
  Position,
}

export default forwardRef<ListMenuType, ListMenuProps>((props: ListMenuProps, ref) => {
  const t = useI18n()
  const [visible, setVisible] = useState(false)
  const menuRef = useRef<MenuType>(null)
  const selectInfoRef = useRef<SelectInfo>(initSelectInfo as SelectInfo)
  const [menus, setMenus] = useState<Menus>([])
  const downloadModalRef = useRef<DownloadSettingModalRefType>(null)
  const pendingDownloadRef = useRef<LX.Music.MusicInfoOnline[]>([])

  useImperativeHandle(ref, () => ({
    show(selectInfo, position) {
      selectInfoRef.current = selectInfo
      handleSetMenu(selectInfo.musicInfo)
      if (visible) menuRef.current?.show(position)
      else {
        setVisible(true)
        requestAnimationFrame(() => {
          menuRef.current?.show(position)
        })
      }
    },
  }))

  const handleSetMenu = (musicInfo: LX.Music.MusicInfo) => {
    let has_url_cache = false
    const menu = [
      { action: 'play', label: t('play') },
      { action: 'playLater', label: t('play_later') },
      { action: 'download', label: t('download') },
      { action: 'add', label: t('add_to') },
      { action: 'copyName', label: t('copy_name') },
      { action: 'musicSourceDetail', label: t('music_source_detail') },
      { action: 'removeCache', disabled: !has_url_cache, label: t('list_remove_cache') },
      { action: 'dislike', label: t('dislike'), disabled: hasDislike(musicInfo) },
    ]
    setMenus(menu)
    void hasUrlCache(musicInfo).then((_has_url_cache) => {
      let isUpdated = false
      if (has_url_cache != _has_url_cache) {
        has_url_cache = _has_url_cache
        menu[menu.findIndex(m => m.action == 'removeCache')].disabled = !has_url_cache
        isUpdated ||= true
      }

      if (isUpdated) setMenus([...menu])
    })
  }

  const handleMenuPress = ({ action }: typeof menus[number]) => {
    const selectInfo = selectInfoRef.current
    switch (action) {
      case 'play':
        props.onPlay(selectInfo)
        break
      case 'playLater':
        props.onPlayLater(selectInfo)
        break
      case 'download':
        void (async() => {
          const quality = settingState.setting['download.quality']
          const granted = await requestStoragePermission()
          if (granted === false) toast('未授予存储权限，可能无法保存文件')
          else if (granted === null) toast('存储权限被永久拒绝，请到系统设置中开启')
          pendingDownloadRef.current = selectInfo.single
            ? [selectInfo.musicInfo]
            : selectInfo.selectedList
          downloadModalRef.current?.show({
            quality,
            title: selectInfo.single ? undefined : t('download_unified_setting'),
          })
        })()
        break
      case 'add':
        props.onAdd(selectInfo)
        break
      case 'copyName':
        props.onCopyName(selectInfo)
        break
      case 'musicSourceDetail':
        props.onMusicSourceDetail(selectInfo)
        // setVIsibleMusicPosition(true)
        break
      case 'removeCache':
        props.onRemoveCache(selectInfo)
        break
      case 'dislike':
        props.onDislikeMusic(selectInfo)
        break
      default:
        break
    }
  }

  return (
    <>
      {visible
        ? <Menu ref={menuRef} menus={menus} onPress={handleMenuPress} />
        : null}
      <DownloadSettingModal
        ref={downloadModalRef}
        onConfirm={(quality, downloadPic, downloadLyric) => {
          const list = pendingDownloadRef.current
          pendingDownloadRef.current = []
          let added = 0
          for (const m of list) {
            if (downloadAction.addTask(m, quality, { downloadPic, downloadLyric })) added++
          }
          if (list.length > 1) toast(`已加入 ${added} 首到下载列表`)
        }}
      />
    </>
  )
})
