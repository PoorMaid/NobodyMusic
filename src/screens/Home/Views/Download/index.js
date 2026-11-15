import { useMemo } from 'react'
import { View, FlatList, StyleSheet } from 'react-native'
import Text from '@/components/common/Text'
import Button from '@/components/common/Button'
import Image from '@/components/common/Image'
import { useTheme } from '@/store/theme/hook'
import { useDownloadList } from '@/store/download/hook'
import { action } from '@/store/download'
import { scaleSizeW, scaleSizeH, setSpText } from '@/utils/pixelRatio'
import { useI18n } from '@/lang'

const FILTERS = [
  { id: 'all', label: 'download_list_all' },
  { id: 'run', label: 'download_list_run' },
  { id: 'waiting', label: 'download_list_waiting' },
  { id: 'error', label: 'download_list_error' },
  { id: 'completed', label: 'download_list_completed' },
]

const formatSize = (bytes) => {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let i = 0
  let n = bytes
  while (n >= 1024 && i < units.length - 1) {
    n /= 1024
    i++
  }
  return n.toFixed(i === 0 ? 0 : 1) + ' ' + units[i]
}

const FilterBar = ({ active, onChange }) => {
  const theme = useTheme()
  const t = useI18n()
  return (
    <View style={styles.filterBar}>
      {FILTERS.map(f => {
        const selected = active === f.id
        return (
          <Button
            key={f.id}
            style={[styles.filterBtn, { backgroundColor: selected ? theme['c-primary-alpha-900'] : theme['c-primary-light-900-alpha-200'] }]}
            onPress={() => onChange(f.id)}
          >
            <Text size={13} color={selected ? theme['c-primary'] : theme['c-font-label']}>{t(f.label)}</Text>
          </Button>
        )
      })}
    </View>
  )
}

const TaskItem = ({ task }) => {
  const theme = useTheme()
  const t = useI18n()
  const pic = task.musicInfo.meta && task.musicInfo.meta.picUrl
  return (
    <View style={[styles.item, { borderBottomColor: theme['c-border-background'] }]}>
      <Image url={pic} style={styles.pic} />
      <View style={styles.itemInfo}>
        <Text size={14} numberOfLines={1}>{task.musicInfo.name}</Text>
        <Text size={12} color={theme['c-font-label']} numberOfLines={1}>{task.musicInfo.singer}</Text>
        <View style={styles.progressRow}>
          <View style={[styles.progressBg, { backgroundColor: theme['c-primary-light-900-alpha-200'] }]}>
            <View style={[styles.progressFg, { width: (task.progress || 0) + '%', backgroundColor: theme['c-primary'] }]} />
          </View>
          <Text size={11} color={theme['c-font-label']} style={styles.progressText}>{task.progress || 0}%</Text>
        </View>
        <Text size={11} color={theme['c-font-label']} numberOfLines={1}>
          {task.statusText}{task.status === 'run' ? '  ' + formatSize(task.downloaded) + '/' + formatSize(task.total) + '  ' + task.speed : ''}
        </Text>
      </View>
      <View style={styles.actions}>
        {task.status === 'error'
          ? <Button style={styles.actionBtn} onPress={() => action.retryTask(task.id)}><Text size={12} color={theme['c-primary']}>{t('download_retry')}</Text></Button>
          : null}
        <Button style={styles.actionBtn} onPress={() => action.removeTask(task.id)}><Text size={12} color={theme['c-font-label']}>{t('download_remove')}</Text></Button>
      </View>
    </View>
  )
}

export default () => {
  const theme = useTheme()
  const t = useI18n()
  const { tasks, activeList } = useDownloadList()
  const list = useMemo(() => {
    return activeList === 'all' ? tasks : tasks.filter(x => x.status === activeList)
  }, [tasks, activeList])
  return (
    <View style={[styles.container, { backgroundColor: theme['c-content-background'] }]}>
      <FilterBar active={activeList} onChange={action.setActiveList} />
      {list.length
        ? <FlatList
          data={list}
          keyExtractor={item => item.id}
          renderItem={({ item }) => <TaskItem task={item} />}
          contentContainerStyle={styles.listContent}
        />
        : <View style={styles.empty}>
          <Text size={14} color={theme['c-font-label']}>{t('download_empty')}</Text>
        </View>}
    </View>
  )
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  filterBar: { flexDirection: 'row', flexWrap: 'wrap', gap: scaleSizeW(8), paddingHorizontal: scaleSizeW(12), paddingVertical: scaleSizeH(10) },
  filterBtn: { paddingHorizontal: scaleSizeW(12), paddingVertical: scaleSizeH(6), borderRadius: scaleSizeW(14) },
  listContent: { paddingBottom: scaleSizeH(120) },
  item: { flexDirection: 'row', paddingHorizontal: scaleSizeW(12), paddingVertical: scaleSizeH(10), borderBottomWidth: StyleSheet.hairlineWidth },
  pic: { width: scaleSizeW(46), height: scaleSizeW(46), borderRadius: scaleSizeW(6) },
  itemInfo: { flex: 1, marginLeft: scaleSizeW(10), justifyContent: 'space-between', gap: scaleSizeH(2) },
  progressRow: { flexDirection: 'row', alignItems: 'center', gap: scaleSizeW(6) },
  progressBg: { flex: 1, height: scaleSizeH(4), borderRadius: 2, overflow: 'hidden' },
  progressFg: { height: '100%' },
  progressText: { minWidth: scaleSizeW(34), textAlign: 'right', fontSize: setSpText(11) },
  actions: { justifyContent: 'center', alignItems: 'flex-end', gap: scaleSizeH(6) },
  actionBtn: { paddingHorizontal: scaleSizeW(8), paddingVertical: scaleSizeH(4) },
  empty: { flex: 1, alignItems: 'center', justifyContent: 'center', gap: scaleSizeH(8) },
})
