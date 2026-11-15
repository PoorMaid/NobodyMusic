import { BUILTIN_SOURCES, type BuiltinSourceDef } from './index'
import { httpFetch } from '@/utils/request'
import { saveBuiltinUserApi, getUserApiList } from '@/utils/data'
import { setUserApiList } from '@/core/userApi'
import { storageDataPrefix } from '@/config/constant'
import { saveData } from '@/plugins/storage'
import { log } from '@/utils/log'

const MAX_SCRIPT_SIZE = 9_000_000

const matchScriptInfo = (script: string) => {
  const header = /^\/\*[\S\s]+?\*\//.exec(script)?.[0] ?? ''
  const rxp = /^\s?\*\s?@(\w+)\s(.+)$/gm
  const info: Record<string, string> = {}
  let m: RegExpExecArray | null
  while ((m = rxp.exec(header)) != null) {
    if (['name', 'description', 'author', 'homepage'].includes(m[1])) info[m[1]] = m[2].trim()
  }
  return info
}

const fetchScript = async(url: string): Promise<string> => {
  const res = await httpFetch(url, { method: 'get' }).promise
  const body = res.body
  if (typeof body !== 'string') throw new Error('invalid response')
  if (body.length > MAX_SCRIPT_SIZE) throw new Error('script too large')
  if (!/^\/\*/.test(body)) throw new Error('invalid script header')
  return body
}

const importOne = async(def: BuiltinSourceDef): Promise<boolean> => {
  try {
    const script = await fetchScript(def.url)
    const info = matchScriptInfo(script)
    await saveBuiltinUserApi({
      id: def.id,
      name: info.name || def.name,
      description: info.description || `在线音源：${def.name}`,
      author: info.author || '',
      homepage: info.homepage || '',
      version: 'latest',
      allowShowUpdateAlert: false,
    }, script)
    return true
  } catch (err) {
    log.warn(`builtin source ${def.key} import failed: ${(err as Error).message}`)
    return false
  }
}

/**
 * 启动时自动在线导入内置音源。
 * 已存在同名内置项时也会重新拉取覆盖（保留用户其他自定义音源）。
 */
export const ensureBuiltinSources = async(force = false): Promise<void> => {
  const existing = await getUserApiList()
  if (!force) {
    const missing = BUILTIN_SOURCES.some(def => !existing.some(api => api.id === def.id))
    if (!missing) return
  }
  const results = await Promise.all(BUILTIN_SOURCES.map(def => importOne(def)))
  const list = await getUserApiList()
  setUserApiList(list)
  const imported = BUILTIN_SOURCES.filter((_, i) => results[i])
  if (imported.length > 0) {
    const settingState = (await import('@/store/setting/state')).default
    const current = settingState.setting['common.apiSource']
    if (!current || !list.some(api => api.id === current)) {
      const { updateSetting } = await import('@/core/common')
      await updateSetting({ 'common.apiSource': imported[0].id })
      settingState.setting['common.apiSource'] = imported[0].id
    }
  }
  void existing
  log.info(`builtin sources imported: ${results.filter(Boolean).length}/${results.length}`)
}
