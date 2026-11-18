import { useRef, useState, forwardRef, useImperativeHandle } from 'react'
import { View, ScrollView, StyleSheet } from 'react-native'
import Dialog, { type DialogType } from '@/components/common/Dialog'
import Text from '@/components/common/Text'
import Button from '@/components/common/Button'
import CheckBox from '@/components/common/CheckBox'
import { useI18n } from '@/lang'
import { useTheme } from '@/store/theme/hook'
import { useSettingValue } from '@/store/setting/hook'
import { scaleSizeH, scaleSizeW } from '@/utils/pixelRatio'
const QUALITYS: { id: LX.Quality, label: string }[] = [
  { id: '128k', label: '128k' },
  { id: '320k', label: '320k' },
  { id: 'flac', label: 'FLAC' },
  { id: 'flac24bit', label: 'FLAC24BIT' },
]
export interface SelectInfo {
  title?: string
  quality?: LX.Quality
  downloadPic?: boolean
  downloadLyric?: boolean
}
export interface DownloadSettingModalProps {
  onConfirm?: (quality: LX.Quality, downloadPic: boolean, downloadLyric: boolean) => void
}
export interface DownloadSettingModalType {
  show: (info: SelectInfo) => void
}
const DownloadSettingModal = forwardRef<DownloadSettingModalType, DownloadSettingModalProps>(({ onConfirm }, ref) => {
  const t = useI18n()
  const theme = useTheme()
  const defaultQuality = useSettingValue('download.quality')
  const dialogRef = useRef<DialogType>(null)
  const [title, setTitle] = useState('')
  const [quality, setQuality] = useState<LX.Quality>(defaultQuality)
  const [downloadPic, setDownloadPic] = useState(true)
  const [downloadLyric, setDownloadLyric] = useState(true)
  useImperativeHandle(ref, () => ({
    show(info: SelectInfo) {
      setTitle(info.title || t('download_quality'))
      setQuality(info.quality || defaultQuality)
      setDownloadPic(info.downloadPic !== false)
      setDownloadLyric(info.downloadLyric !== false)
      requestAnimationFrame(() => {
        dialogRef.current?.setVisible(true)
      })
    },
  }), [defaultQuality, t])
  const handleConfirm = () => {
    dialogRef.current?.setVisible(false)
    onConfirm?.(quality, downloadPic, downloadLyric)
  }
  return (
    <Dialog ref={dialogRef} title={title} closeBtn={false} bgHide={true}>
      <ScrollView style={styles.content} keyboardShouldPersistTaps={'always'}>
        <Text style={styles.label} size={13} color={theme['c-font-label']}>{t('download_quality')}</Text>
        <View style={styles.qualityRow}>
          {QUALITYS.map(q => {
            const selected = quality === q.id
            return (
              <Button
                key={q.id}
                style={[styles.qualityBtn, { backgroundColor: selected ? theme['c-primary-alpha-900'] : theme['c-primary-light-900-alpha-200'] }]}
                onPress={() => { setQuality(q.id) }}
              >
                <Text size={12} color={selected ? theme['c-primary'] : theme['c-font-label']}>{q.label}</Text>
              </Button>
            )
          })}
        </View>
        <CheckBox
          check={downloadPic}
          label={t('download_pic')}
          onChange={setDownloadPic}
          marginBottom={scaleSizeH(8)}
        />
        <CheckBox
          check={downloadLyric}
          label={t('download_lyric')}
          onChange={setDownloadLyric}
          marginBottom={scaleSizeH(8)}
        />
        <Button style={[styles.confirmBtn, { backgroundColor: theme['c-primary'] }]} onPress={handleConfirm}>
          <Text size={14} color={theme['c-primary-font']}>{t('download_confirm')}</Text>
        </Button>
      </ScrollView>
    </Dialog>
  )
})
DownloadSettingModal.displayName = 'DownloadSettingModal'
export default DownloadSettingModal
const styles = StyleSheet.create({
  content: { paddingHorizontal: scaleSizeW(16), paddingBottom: scaleSizeH(16) },
  label: { marginTop: scaleSizeH(8), marginBottom: scaleSizeH(8) },
  qualityRow: { flexDirection: 'row', flexWrap: 'wrap', marginBottom: scaleSizeH(12) },
  qualityBtn: { paddingHorizontal: scaleSizeW(14), paddingVertical: scaleSizeH(7), borderRadius: scaleSizeW(14), marginRight: scaleSizeW(8), marginBottom: scaleSizeH(8) },
  confirmBtn: { marginTop: scaleSizeH(8), paddingVertical: scaleSizeH(10), borderRadius: scaleSizeW(20), alignItems: 'center' },
})