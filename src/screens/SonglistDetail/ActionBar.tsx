import { memo, useRef } from 'react'
import { View } from 'react-native'
import Button from '@/components/common/Button'

import { createStyle, toast } from '@/utils/tools'
import { pop } from '@/navigation'
import { useTheme } from '@/store/theme/hook'
import commonState from '@/store/common/state'
import Text from '@/components/common/Text'
import { handleCollect, handlePlay } from './listAction'
import songlistState from '@/store/songlist/state'
import { useI18n } from '@/lang'
import { useListInfo } from './state'
import { getListDetailAll } from '@/core/songlist'
import { addTask } from '@/store/download/action'
import settingState from '@/store/setting/state'
import DownloadSettingModal, { type DownloadSettingModalRefType } from '@/components/DownloadSettingModal'
// import { NAV_SHEAR_NATIVE_IDS } from '@/config/constant'

export default memo(() => {
  const theme = useTheme()
  const t = useI18n()
  const info = useListInfo()
  const downloadModalRef = useRef<DownloadSettingModalRefType>(null)
  const pendingRef = useRef<LX.Music.MusicInfoOnline[]>([])

  const back = () => {
    void pop(commonState.componentIds.songlistDetail!)
  }

  const handlePlayAll = () => {
    if (!songlistState.listDetailInfo.info.name) return
    void handlePlay(info.id, info.source, songlistState.listDetailInfo.list)
  }

  const handleCollection = () => {
    if (!songlistState.listDetailInfo.info.name) return
    void handleCollect(info.id, info.source, songlistState.listDetailInfo.info.name || info.name)
  }

    const handleDownloadAll = () => {
    if (!songlistState.listDetailInfo.info.name) return
    void (async() => {
      const list = await getListDetailAll(info.source, info.id)
      if (!list.length) {
        toast('没有可下载的歌曲')
        return
      }
      pendingRef.current = list
      downloadModalRef.current?.show({
        quality: settingState.setting['download.quality'],
        title: t('download_unified_setting'),
      })
    })()
  }
  return (
    <View style={styles.container}>
      <Button onPress={handleCollection} style={styles.controlBtn}>
        <Text style={{ ...styles.controlBtnText, color: theme['c-button-font'] }}>{t('collect_songlist')}</Text>
      </Button>
      <Button onPress={handlePlayAll} style={styles.controlBtn}>
        <Text style={{ ...styles.controlBtnText, color: theme['c-button-font'] }}>{t('play_all')}</Text>
      </Button>
      <Button onPress={handleDownloadAll} style={styles.controlBtn}>
        <Text style={{ ...styles.controlBtnText, color: theme['c-button-font'] }}>下载全部</Text>
      </Button>
      <Button onPress={back} style={styles.controlBtn}>
        <Text style={{ ...styles.controlBtnText, color: theme['c-button-font'] }}>{t('back')}</Text>
      </Button>
      <DownloadSettingModal
        ref={downloadModalRef}
        onConfirm={(quality, downloadPic, downloadLyric) => {
          const list = pendingRef.current
          pendingRef.current = []
          let added = 0
          for (const m of list) {
            if (addTask(m, quality, { downloadPic, downloadLyric })) added++
          }
          if (added === 0) toast('加入下载失败')
          else toast(`已加入 ${added} 首到下载列表`)
        }}
      />
    </View>
  )
})

const styles = createStyle({
  container: {
    flexDirection: 'row',
    width: '100%',
    flexGrow: 0,
    flexShrink: 0,
  },
  controlBtn: {
    flexGrow: 1,
    flexShrink: 1,
    width: '25%',
    paddingTop: 12,
    paddingBottom: 12,
    paddingLeft: 10,
    paddingRight: 10,
  },
  controlBtnText: {
    fontSize: 13,
    textAlign: 'center',
  },
})

