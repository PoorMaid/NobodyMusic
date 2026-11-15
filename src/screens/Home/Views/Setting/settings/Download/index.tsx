import { memo, useRef, useState } from 'react'
import { View } from 'react-native'
import Section from '../../components/Section'
import SubTitle from '../../components/SubTitle'
import Button from '@/components/common/Button'
import InputItem from '../../components/InputItem'
import Text from '@/components/common/Text'
import ChoosePath, { type ChoosePathType } from '@/components/common/ChoosePath'
import { useSettingValue } from '@/store/setting/hook'
import { updateSetting } from '@/core/common'
import { useI18n } from '@/lang'
import { externalStorageDirectoryPath } from '@/utils/fs'
import { createStyle } from '@/utils/tools'
const QUALITYS = ['128k', '320k', 'flac', 'flac24bit'] as const
const CONCURRENCY = [1, 2, 3, 4] as const
const QUALITY_LABEL = {
  '128k': '128K',
  '320k': '320K',
  flac: 'FLAC',
  flac24bit: 'FLAC 24bit',
}
export default memo(() => {
  const t = useI18n()
  const quality = useSettingValue('download.quality')
  const concurrency = useSettingValue('download.concurrency')
  const savePath = useSettingValue('download.savePath')
  const fileName = useSettingValue('download.fileName')
  const choosePathRef = useRef<ChoosePathType>(null)
  const [visible, setVisible] = useState(false)
  const defaultDir = externalStorageDirectoryPath + '/Download/NobodyMusic'
  const showChoosePath = () => {
    if (visible) {
      choosePathRef.current?.show({ title: t('download_save_path'), dirOnly: true })
    } else {
      setVisible(true)
      requestAnimationFrame(() => {
        choosePathRef.current?.show({ title: t('download_save_path'), dirOnly: true })
      })
    }
  }
  return (
    <Section title={t('setting_download')}>
      <SubTitle title={t('download_quality')}>
        <View style={styles.row}>
          {QUALITYS.map(q => (
            <Button key={q} style={styles.chip} onPress={() => { void updateSetting({ 'download.quality': q }) }}>
              <Text size={13} color={quality === q ? '#FFFFFF' : undefined} style={quality === q ? styles.chipActive : undefined}>{QUALITY_LABEL[q]}</Text>
            </Button>
          ))}
        </View>
      </SubTitle>
      <SubTitle title={t('download_concurrency')}>
        <View style={styles.row}>
          {CONCURRENCY.map(n => (
            <Button key={n} style={styles.chip} onPress={() => { void updateSetting({ 'download.concurrency': n }) }}>
              <Text size={13} color={concurrency === n ? '#FFFFFF' : undefined} style={concurrency === n ? styles.chipActive : undefined}>{String(n)}</Text>
            </Button>
          ))}
        </View>
      </SubTitle>
      <SubTitle title={t('download_file_name')}>
        <InputItem value={fileName} label={fileName} onChanged={value => { void updateSetting({ 'download.fileName': value as LX.AppSetting['download.fileName'] }) }} />
      </SubTitle>
      <SubTitle title={t('download_save_path')}>
        <View style={styles.pathRow}>
          <Text size={12} style={styles.pathText} numberOfLines={2}>{savePath || defaultDir}</Text>
          <Button onPress={showChoosePath}><Text size={13} color="#7C4DFF">{t('download_save_path')}</Text></Button>
        </View>
      </SubTitle>
      {visible ? <ChoosePath ref={choosePathRef} onConfirm={path => { void updateSetting({ 'download.savePath': path }) }} /> : null}
    </Section>
  )
})
const styles = createStyle({
  row: {
    flexDirection: 'row',
    flexWrap: 'wrap',
  },
  chip: {
    marginRight: 8,
    marginBottom: 8,
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 8,
    backgroundColor: 'rgba(124,77,255,0.12)',
  },
  chipActive: {
    fontWeight: 'bold',
    backgroundColor: '#7C4DFF',
    borderRadius: 8,
    paddingHorizontal: 6,
    paddingVertical: 2,
  },
  pathRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  pathText: {
    flex: 1,
    marginRight: 10,
  },
})
