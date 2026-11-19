import { BUILTIN_SOURCES } from './index'
import { getUserApiList } from '@/utils/data'
import settingState from '@/store/setting/state'
import { setApiSource } from '@/core/apiSource'
import { setUserApiStatus } from '@/core/userApi'
import { updateSetting } from '@/core/common'
import { log } from '@/utils/log'

const PROBE_TIMEOUT = 8_000
const POLL_INTERVAL = 200

const waitApiStatus = async(promise: Promise<boolean>): Promise<boolean> => {
  const result = await Promise.race([
    promise,
    new Promise<boolean>(resolve => setTimeout(() => { resolve(false) }, PROBE_TIMEOUT)),
  ])
  if (result) return true
  // 超时或失败：轮询兑底，但必须以仍处于 initing 为前题
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
  if (!global.lx.apiInitPromise[1] && settingState.setting['common.apiSource']) {
    // Nobody Music: 后台探测阶段闸门还开着，先等启动时的音源初始化结果
    await waitApiStatus(global.lx.apiInitPromise[0])
  }
  const current = settingState.setting['common.apiSource']
  // 已有可用音源且已初始化成功：直接沿用，不再探测、不再改源
  if (current && order.includes(current) && global.lx.apiInitPromise[1]) {
    log.info('auto pick user api: keep current ' + current)
    // Nobody Music: 沿用当前音源时也要确认闸门已放开，否则播放会一直 source init failed
    void global.lx.apiInitPromise[0].then(ok => {
      if (ok) setUserApiStatus(true, '')
      else setUserApiStatus(false, 'failed')
    })
    return
  }
  const probeList = manual && order.includes(current) ? [current] : order

  for (const id of probeList) {
    log.info('auto pick user api: try ' + id)
    setApiSource(id)
    const ok = await waitApiStatus(global.lx.apiInitPromise[0])
    if (ok) {
      settingState.setting['common.apiSource'] = id
      updateSetting({ 'common.apiSource': id })
      log.info('auto pick user api: ready ' + id)
      return
    }
    log.warn('auto pick user api: failed ' + id)
    // 探测失败必须落定状态，否则设置页永久显示“初始化中”
    setUserApiStatus(false, 'failed')
  }

  // 全部失败：仍要放开初始化闸门，否则后续一直报 source init failed
  log.warn('auto pick user api: none available')
  global.lx.apiInitPromise[1] = true
  global.lx.apiInitPromise[0] = Promise.resolve(true)
  setUserApiStatus(false, 'failed')
}
