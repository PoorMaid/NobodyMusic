import { BUILTIN_SOURCES } from './index'
import { getUserApiList } from '@/utils/data'
import settingState from '@/store/setting/state'
import { setApiSource } from '@/core/apiSource'
import { log } from '@/utils/log'

const PROBE_TIMEOUT = 25_000

const waitApiStatus = (): Promise<boolean> => new Promise(resolve => {
  global.lx.apiInitPromise[1] = false
  global.lx.apiInitPromise[0] = Promise.resolve(false)
  global.lx.apiInitPromise[2] = (result: boolean) => {
    global.lx.apiInitPromise[1] = true
    resolve(!!result)
  }
  setTimeout(() => {
    if (global.lx.apiInitPromise[1]) return
    global.lx.apiInitPromise[1] = true
    resolve(false)
  }, PROBE_TIMEOUT)
})

const isBuiltinId = (id: string) => BUILTIN_SOURCES.some(def => def.id === id)

export const autoPickApiSource = async(): Promise<void> => {
  const list = await getUserApiList()
  const order = [
    ...BUILTIN_SOURCES.map(def => def.id),
    ...list.map(api => api.id).filter(id => !isBuiltinId(id)),
  ].filter(id => list.some(api => api.id === id))
  if (!order.length) return

  const manual = settingState.setting['common.autoPickApiSource'] === false
  const current = settingState.setting['common.apiSource']
  const probeList = manual && order.includes(current) ? [current] : order

  for (const id of probeList) {
    log.info('auto pick user api: try ' + id)
    const waiter = waitApiStatus()
    setApiSource(id)
    const ok = await waiter
    if (ok) {
      settingState.setting['common.apiSource'] = id
      log.info('auto pick user api: ready ' + id)
      return
    }
    log.warn('auto pick user api: failed ' + id)
  }
  log.warn('auto pick user api: none available')
}
