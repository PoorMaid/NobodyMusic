import { BUILTIN_SOURCES } from './index'
import { getUserApiList } from '@/utils/data'
import settingState from '@/store/setting/state'
import { setApiSource } from '@/core/apiSource'
import { updateSetting } from '@/core/common'
import { log } from '@/utils/log'

const PROBE_TIMEOUT = 8_000
const POLL_INTERVAL = 200

const waitApiStatus = async(): Promise<boolean> => {
  const startAt = Date.now()
  while (Date.now() - startAt < PROBE_TIMEOUT) {
    if (global.lx.apiInitPromise[1]) return true
    await new Promise(resolve => setTimeout(resolve, POLL_INTERVAL))
  }
  return false
}

const isBuiltinId = (id: string) => BUILTIN_SOURCES.some(def => def.id === id)

export const autoPickApiSource = async(): Promise<void> => {
  const list = await getUserApiList()
  const order = [
    ...BUILTIN_SOURCES.map(def => def.id),
    ...list.map(api => api.id).filter(id => !isBuiltinId(id)),
  ].filter(id => list.some(api => api.id === id))
  if (!order.length) {
    log.warn('auto pick user api: empty list')
    return
  }

  const manual = settingState.setting['common.autoPickApiSource'] === false
  const current = settingState.setting['common.apiSource']
  // 已有可用音源且已初始化成功：直接沿用，不再探测、不再改源
  if (current && order.includes(current) && global.lx.apiInitPromise[1]) {
    log.info('auto pick user api: keep current ' + current)
    return
  }
  const probeList = manual && order.includes(current) ? [current] : order

  for (const id of probeList) {
    log.info('auto pick user api: try ' + id)
    const waiter = waitApiStatus()
    setApiSource(id)
    const ok = await waiter
    if (ok) {
      settingState.setting['common.apiSource'] = id
      updateSetting({ 'common.apiSource': id })
      log.info('auto pick user api: ready ' + id)
      return
    }
    log.warn('auto pick user api: failed ' + id)
  }

  // 全部失败：仍要放开初始化闸门，否则后续一直报 source init failed
  log.warn('auto pick user api: none available')
  global.lx.apiInitPromise[1] = true
  global.lx.apiInitPromise[0] = Promise.resolve(true)
}
