import { memo, useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { View, FlatList, StyleSheet } from 'react-native'
import Text from '@/components/common/Text'
import Button from '@/components/common/Button'
import { useTheme } from '@/store/theme/hook'
import { useDownloadList } from '@/store/download/hook'
import { action as downloadAction } from '@/store/download'
import { useI18n } from '@/lang'
import { scaleSizeW, scaleSizeH } from '@/utils/pixelRatio'
import { LIST_IDS } from '@/config/constant'
import { getListMusics } from '@/core/list'
import { useListFetching } from '@/store/list/hook'
import Loading from '@/components/common/Loading'
import { playListById } from '@/core/player/player'
import ChoosePath from '@/components/common/ChoosePath'
import { handleImportMediaFile } from '@/screens/Home/Views/Mylist/MyList/listAction'

const { getDownloadDir } = downloadAction

const FILTERS = [
  { key: 'downloaded', label: 'download_tab_downloaded' },
  { key: 'run', label: 'download_list_run' },
  { key: 'all', label: 'download_list_all' },
  { key: 'waiting', label: 'download_list_waiting' },
  { key: 'error', label: 'download_list_error' },
  { key: 'completed', label: 'download_list_completed' },
]

const formatSize = size => {
  if (!size || size < 0) return '0 B'
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(1) + ' KB'
  if (size < 1024 * 1024 * 1024) return (size / 1024 / 1024).toFixed(1) + ' MB'
  return (size / 1024 / 1024 / 1024).toFixed(2) + ' GB'
}

const FilterBar = memo(({ active, onSwitch }) => {
  const theme = useTheme()
  const t = useI18n()
  return (
    <View style={styles.filterBar}>
      {FILTERS.map(f => {
        const isActive = active === f.key
        return (
          <Button
            key={f.key}
            style={[styles.filterBtn, { backgroundColor: isActive ? theme['c-primary-alpha-900'] : theme['c-primary-light-900-alpha-200'] }]}
            onPress={() => onSwitch(f.key)}
          >
            <Text size={12} color={isActive ? theme['c-primary'] : theme['c-font-label']}>{t(f.label)}</Text>
          </Button>
        )
      })}
    </View>
  )
})

const TaskItem = memo(({ task }) => {
  const theme = useTheme()
  const t = useI18n()
  const statusText = useMemo(() => {
    switch (task.status) {
      case 'run':
        return formatSize(task.downloaded) + '/' + formatSize(task.total) + '  ' + (task.speed || '')
      case 'waiting': return t('download_list_waiting')
      case 'error': return (task.statusText ? task.statusText : '') || t('download_list_error')
      case 'completed': return t('download_list_completed')
      default: return ''
    }
  }, [task.status, task.downloaded, task.total, task.speed, t])
  const handleRetry = useCallback(() => {
    downloadAction.retryTask(task.id)
  }, [task.id])
  const handleRemove = useCallback(() => {
    downloadAction.removeTask(task.id)
  }, [task.id])
  return (
    <View style={[styles.taskItem, { borderBottomColor: theme['c-primary-light-900-alpha-200'] }]}>
      <View style={styles.taskInfo}>
        <Text size={14} numberOfLines={1}>{task.musicInfo.name}</Text>
        <Text size={11} color={theme['c-font-label']} numberOfLines={1}>{task.musicInfo.singer}</Text>
        <View style={[styles.progressBar, { backgroundColor: theme['c-primary-light-900-alpha-200'] }]}>
          <View style={[styles.progressInner, { backgroundColor: theme['c-primary'], width: (task.progress || 0) + '%' }]} />
        </View>
        <Text size={11} color={theme['c-font-label']} numberOfLines={1}>{statusText}</Text>
      </View>
      <View style={styles.taskActions}>
        {task.status === 'error' ? (
          <Button style={styles.taskBtn} onPress={handleRetry}>
            <Text size={12} color={theme['c-primary']}>{t('download_retry')}</Text>
          </Button>
        ) : null}
        <Button style={styles.taskBtn} onPress={handleRemove}>
          <Text size={12} color={theme['c-font-label']}>{t('download_remove')}</Text>
        </Button>
      </View>
    </View>
  )
})

const LocalItem = memo(({ item, index, onPress }) => {
  const theme = useTheme()
  return (
    <Button style={[styles.localItem, { borderBottomColor: theme['c-primary-light-900-alpha-200'] }]} onPress={() => onPress(index)}>
      <Text size={14} numberOfLines={1}>{item.name}</Text>
      <Text size={11} color={theme['c-font-label']} numberOfLines={1}>{item.singer}</Text>
    </Button>
  )
})

export default memo(() => {
  const theme = useTheme()
  const t = useI18n()
  const { tasks } = useDownloadList()
  const [filter, setFilter] = useState('downloaded')
  const [localList, setLocalList] = useState([])
  const scanning = useListFetching(LIST_IDS.DOWNLOAD)
  const choosePathRef = useRef(null)

  const loadLocal = useCallback(() => {
    void getListMusics(LIST_IDS.DOWNLOAD).then(list => {
      setLocalList([...list])
    }).catch(() => {})
  }, [])

  useEffect(() => {
    loadLocal()
    const handleUpdate = () => { loadLocal() }
    const handleListMusic = listId => {
      if (listId === LIST_IDS.DOWNLOAD) handleUpdate()
    }
    global.app_event.on('downloadListUpdate', handleUpdate)
    global.app_event.on('list_music_add', handleListMusic)
    global.app_event.on('list_music_update', handleListMusic)
    global.app_event.on('list_music_remove', handleListMusic)
    return () => {
      global.app_event.off('downloadListUpdate', handleUpdate)
      global.app_event.off('list_music_add', handleListMusic)
      global.app_event.off('list_music_update', handleListMusic)
      global.app_event.off('list_music_remove', handleListMusic)
    }
  }, [loadLocal])

  const isLocalTab = filter === 'downloaded'

  const filteredTasks = useMemo(() => {
    if (filter === 'all') return tasks
    if (filter === 'run') return tasks.filter(task => task.status === 'run')
    if (filter === 'waiting') return tasks.filter(task => task.status === 'waiting')
    if (filter === 'error') return tasks.filter(task => task.status === 'error')
    if (filter === 'completed') return tasks.filter(task => task.status === 'completed')
    return tasks
  }, [tasks, filter])

  const handleScan = useCallback(() => {
    if (filter === 'downloaded') {
      void handleImportMediaFile({ id: LIST_IDS.DOWNLOAD, name: t('nav_local_music') }, getDownloadDir()).then(loadLocal)
      return
    }
    choosePathRef.current?.show({
      title: t('download_scan_tip'),
      dirOnly: true,
      isPersist: true,
    })
  }, [t, filter, loadLocal])

  const handleScanPath = useCallback(path => {
    void handleImportMediaFile({ id: LIST_IDS.DOWNLOAD, name: t('nav_local_music') }, path).then(loadLocal)
  }, [loadLocal, t])

  const handlePlayLocal = useCallback(index => {
    void playListById(LIST_IDS.DOWNLOAD, localList[index].id)
  }, [localList])

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Button style={[styles.scanBtn, { backgroundColor: theme['c-primary-alpha-900'], opacity: scanning ? 0.5 : 1 }]} onPress={handleScan} disabled={scanning}>
          <Text size={13} color={theme['c-primary']}>{scanning ? t('download_scanning') : (isLocalTab ? t('download_scan_saved') : t('download_scan_local'))}</Text>
        </Button>
        {scanning ? <Loading size={14} style={styles.headerLoading} /> : null}
      </View>
      <FilterBar active={filter} onSwitch={setFilter} />
      {isLocalTab
        ? <FlatList
            data={localList}
            renderItem={({ item, index }) => <LocalItem item={item} index={index} onPress={handlePlayLocal} />}
            keyExtractor={item => item.id}
            style={styles.mainList}
            contentContainerStyle={styles.listContent}
            ListEmptyComponent={
              <View style={styles.empty}>
                {scanning
                  ? <Loading size={22} label={t('download_scanning')} />
                  : <Text size={13} color={theme['c-font-label']}>{t('download_local_empty')}</Text>}
              </View>
            }
          />
        : <FlatList
            data={filteredTasks}
            renderItem={({ item }) => <TaskItem task={item} />}
            keyExtractor={item => item.id}
            style={styles.mainList}
            contentContainerStyle={styles.listContent}
            ListEmptyComponent={
              <View style={styles.empty}><Text size={13} color={theme['c-font-label']}>{t('download_empty')}</Text></View>
            }
          />
      }
      <ChoosePath ref={choosePathRef} onConfirm={handleScanPath} />
    </View>
  )
})

const styles = StyleSheet.create({
  container: { flex: 1 },
  header: { flexDirection: 'row', alignItems: 'center', paddingHorizontal: scaleSizeW(12), paddingVertical: scaleSizeH(8) },
  scanBtn: { paddingHorizontal: scaleSizeW(14), paddingVertical: scaleSizeH(7), borderRadius: scaleSizeW(14) },
  headerLoading: { marginLeft: scaleSizeW(8) },
  filterBar: { flexDirection: 'row', flexWrap: 'wrap', paddingHorizontal: scaleSizeW(12), paddingBottom: scaleSizeH(6) },
  filterBtn: { paddingHorizontal: scaleSizeW(12), paddingVertical: scaleSizeH(5), borderRadius: scaleSizeW(12), marginRight: scaleSizeW(8), marginBottom: scaleSizeH(6) },
  mainList: { flex: 1 },
  listContent: { paddingBottom: scaleSizeH(16) },
  taskItem: { flexDirection: 'row', alignItems: 'center', paddingHorizontal: scaleSizeW(12), paddingVertical: scaleSizeH(8), borderBottomWidth: StyleSheet.hairlineWidth },
  taskInfo: { flex: 1, marginRight: scaleSizeW(8) },
  progressBar: { height: scaleSizeH(3), borderRadius: scaleSizeH(2), marginVertical: scaleSizeH(4), overflow: 'hidden' },
  progressInner: { height: '100%' },
  taskActions: { flexDirection: 'row', alignItems: 'center' },
  taskBtn: { paddingHorizontal: scaleSizeW(8), paddingVertical: scaleSizeH(4) },
  localItem: { paddingHorizontal: scaleSizeW(12), paddingVertical: scaleSizeH(9), borderBottomWidth: StyleSheet.hairlineWidth },
  empty: { flex: 1, alignItems: 'center', justifyContent: 'center', paddingVertical: scaleSizeH(24) },
})