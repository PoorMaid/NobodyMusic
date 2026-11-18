import { useRef, useImperativeHandle, forwardRef, useState } from 'react'
import { useI18n } from '@/lang'
import Menu, { type Menus, type MenuType, type Position } from '@/components/common/Menu'
import { hasDislike } from '@/core/dislikeList'
import { existsFile } from '@/utils/fs'
import { hasMusicUrlByMusic } from '@/utils/data'
import settingState from '@/store/setting/state'
import { requestStoragePermission, toast } from '@/utils/tools'
import { action as downloadAction } from '@/store/download'
import DownloadSettingModal, { type DownloadSettingModalRefType } from '@/components/DownloadSettingModal'

export interface SelectInfo {
  musicInfo: LX.Music.MusicInfo
  selectedList: LX.Music.MusicInfo[]
  index: number
  listId: string
  single: boolean
}
const initSelectInfo = {}

export interface ListMenuProps {
  onPlay: (selectInfo: SelectInfo) => void
  onPlayLater: (selectInfo: SelectInfo) => void
  onAdd: (selectInfo: SelectInfo) => void
  onMove: (selectInfo: SelectInfo) => void
  onEditMetadata: (selectInfo: SelectInfo) => void
  onCopyName: (selectInfo: SelectInfo) => void
  onChangePosition: (selectInfo: SelectInfo) => void
  onToggleSource: (selectInfo: SelectInfo) => void
  onMusicSourceDetail: (selectInfo: SelectInfo) => void
  onRemoveCache: (selectInfo: SelectInfo) => void
  onDislikeMusic: (selectInfo: SelectInfo) => void
  onRemove: (selectInfo: SelectInfo) => void
}
export interface ListMenuType {
  show: (selectInfo: SelectInfo, position: Position) => void
}

export type {
  Position,
}

const hasEditMetadata = async(musicInfo: LX.Music.MusicInfo) => {
  if (musicInfo.source != 'local') return false
  return existsFile(musicInfo.meta.filePath)
}
const hasUrlCache = async(musicInfo: LX.Music.MusicInfo) => {
  return hasMusicUrlByMusic(musicInfo)
}
export default forwardRef<ListMenuType, ListMenuProps>((props, ref) => {
  const t = useI18n()
  const [visible, setVisible] = useState(false)
  const menuRef = useRef<MenuType>(null)
  const selectInfoRef = useRef<SelectInfo>(initSelectInfo as SelectInfo)
  const [menus, setMenus] = useState<Menus>([])
  const downloadModalRef = useRef<DownloadSettingModalRefType>(null)
  const pendingDownloadRef = useRef<LX.Music.MusicInfo[]>([])

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
    let edit_metadata = false
    let has_url_cache = false
    const isLocal = musicInfo.source == 'local'
    const menu = [
      { action: 'play', label: t('play') },
      { action: 'playLater', label: t('play_later') },
      { action: 'download', label: t('download'), disabled: isLocal },
      { action: 'add', label: t('add_to') },
      { action: 'move', label: t('move_to') },
      { action: 'changePosition', label: t('change_position') },
      { action: 'toggleSource', label: t('toggle_source') },
      { action: 'copyName', label: t('copy_name') },
      { action: 'musicSourceDetail', disabled: isLocal, label: t('music_source_detail') },
      { action: 'removeCache', disabled: !has_url_cache, label: t('list_remove_cache') },
      // { action: 'musicSearch', label: t('music_search') },
      { action: 'dislike', disabled: hasDislike(musicInfo), label: t('dislike') },
      { action: 'remove', label: t('delete') },
    ]
    if (isLocal) menu.splice(5, 0, { action: 'editMetadata', disabled: !edit_metadata, label: t('edit_metadata') })
    setMenus(menu)
    void Promise.all([isLocal ? hasEditMetadata(musicInfo) : Promise.resolve(false), hasUrlCache(musicInfo)]).then(([_edit_metadata, _has_url_cache]) => {
      // console.log(_edit_metadata)
      let isUpdated = false
      if (edit_metadata != _edit_metadata) {
        edit_metadata = _edit_metadata
        menu[menu.findIndex(m => m.action == 'editMetadata')].disabled = !edit_metadata
        isUpdated ||= true
      }
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
          console.log('[NB-DL] download case start, quality=' + quality + ' music=' + selectInfo.musicInfo?.name + ' source=' + selectInfo.musicInfo?.source)
          const granted = await requestStoragePermission()
          console.log('[NB-DL] perm=' + String(granted))
          if (granted === false) toast('未授予存储权限，可能无法保存文件')
          else if (granted === null) toast('存储权限被永久拒绝，请到系统设置中开启')
          const selList = (selectInfo.selectedList ?? []) as LX.Music.MusicInfoOnline[]
          pendingDownloadRef.current = (selectInfo.single || selList.length === 0)
            ? [selectInfo.musicInfo]
            : selList
          console.log('[NB-DL] pending.len=' + pendingDownloadRef.current.length + ' refNull=' + (downloadModalRef.current == null))
          downloadModalRef.current?.show({
            quality,
            title: selectInfo.single ? undefined : t('download_unified_setting'),
          })
        })()
        break
      case 'add':
        props.onAdd(selectInfo)
        // isMoveRef.current = false
        // selectedListRef.current.length
        //   ? setVisibleMusicMultiAddModal(true)
        //   : setVisibleMusicAddModal(true)
        break
      case 'move':
        props.onMove(selectInfo)
        // isMoveRef.current = true
        // selectedListRef.current.length
        //   ? setVisibleMusicMultiAddModal(true)
        //   : setVisibleMusicAddModal(true)
        break
      case 'editMetadata':
        props.onEditMetadata(selectInfo)
        break
      case 'copyName':
        props.onCopyName(selectInfo)
        break
      case 'changePosition':
        props.onChangePosition(selectInfo)
        // setVIsibleMusicPosition(true)
        break
      case 'toggleSource':
        props.onToggleSource(selectInfo)
        // setVIsibleMusicPosition(true)
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
      case 'remove':
        props.onRemove(selectInfo)
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
          console.log('[NB-DL] ListMenu onConfirm fired, list.len=' + list.length, 'q=' + quality)
          pendingDownloadRef.current = []
          let added = 0
          for (const m of list) {
            if (m.source == 'local') continue
            try {
              if (downloadAction.addTask(m as LX.Music.MusicInfoOnline, quality, { downloadPic, downloadLyric })) added++
            } catch (err: any) {
              console.log('[NB-DL] addTask threw: ' + String(err?.message || err))
            }
          }
          console.log('[NB-DL] added=' + added + '/' + list.length)
          if (added === 0) toast('加入下载失败，请检查存储权限或重试')
          else if (list.length > 1) toast(`已加入 ${added} 首到下载列表`)
        }}
      />
    </>
  )
})
