import { BUILTIN_SOURCES, BUILTIN_UPDATE_URLS, URL_OVERRIDE, isBuiltinSourceId, type BuiltinSourceDef } from './index'
import { getUserApiList, getUserApiScript, saveBuiltinUserApi } from '@/utils/data'
import { setUserApiList } from '@/core/userApi'
import { httpFetch } from '@/utils/request'
import { log } from '@/utils/log'

const MAX_SCRIPT_SIZE = 9_000_000

const normalize = (raw: any): BuiltinSourceDef | null => {
  if (!raw || typeof raw !== 'object') return null
  const key = typeof raw.key === 'string' ? raw.key.trim() : ''
  const name = typeof raw.name === 'string' ? raw.name.trim() : ''
  const url = typeof raw.url === 'string' ? raw.url.trim() : ''
  if (!key && !name) return null
  if (!/^https?:\/\//.test(url)) return null
  const k = key || name.toLowerCase().replace(/[^a-z0-9]/g, '')
  return { key: k, id: `user_api_builtin_${k}`, name: name || k, url }
}

const applyOverride = (defs: BuiltinSourceDef[]): BuiltinSourceDef[] => {
  if (!defs.length) return defs
  const localMap = new Map(BUILTIN_SOURCES.map(s => [s.id, s]))
  const out = defs.map(d => {
    const local = localMap.get(d.id)
    return local && local.url !== d.url && /zrcdy\.dpdns\.org/.test(local.url) ? { ...d, url: local.url } : d
  })
  return out
}

const loadSourceDefs = async(): Promise<BuiltinSourceDef[]> => {
  for (const url of BUILTIN_UPDATE_URLS) {
    try {
      const { promise } = httpFetch(url, { timeout: 15000 })
      const resp = await promise
      const text = typeof resp.body === 'string' ? resp.body : JSON.stringify(resp.body)
      const data = JSON.parse(text)
      const arr = Array.isArray(data) ? data : (Array.isArray(data?.sources) ? data.sources : [])
      const defs: BuiltinSourceDef[] = []
      const seen = new Set<string>()
      for (const item of arr) {
        const d = normalize(item)
        if (!d || seen.has(d.id)) continue
        seen.add(d.id)
        defs.push(d)
      }
      if (defs.length) {
        log.info(`builtin sources: remote list from ${url} (${defs.length})`)
        return applyOverride(defs)
      }
    } catch (err) {
      log.warn(`builtin sources: load list failed ${url}`, err)
    }
  }
  log.info(`builtin sources: fallback to builtin links (${BUILTIN_SOURCES.length})`)
  return BUILTIN_SOURCES
}

const fetchScript = async(url: string): Promise<string | null> => {
  const { promise } = httpFetch(url, { timeout: 20000 })
  const resp = await promise
  let script: any = resp.body
  if (script == null) return null
  if (typeof script !== 'string') script = String(script)
  if (script.length === 0 || script.length > MAX_SCRIPT_SIZE) return null
  if (!/^\/\*/.test(script)) return null
  return script as string
}

const importOne = async(def: BuiltinSourceDef, oldScript: string | null): Promise<boolean> => {
  try {
    const script = await fetchScript(def.url)
    if (!script) return false
    const pinned = !!URL_OVERRIDE[def.key]
    if (oldScript && oldScript === script && !pinned) return true
    await saveBuiltinUserApi({
      id: def.id,
      name: def.name,
      description: '',
      author: '',
      homepage: '',
      version: 'latest',
      allowShowUpdateAlert: false,
    }, script)
    return true
  } catch (err) {
    log.warn(`builtin sources: import failed ${def.key}`, err)
    return false
  }
}

export const ensureBuiltinSources = async(): Promise<void> => {
  const existing = await getUserApiList()
  const defs = await loadSourceDefs()
  const oldMap = new Map<string, string>()
  for (const api of existing) {
    if (!isBuiltinSourceId(api.id)) continue
    const script = await getUserApiScript(api.id)
    if (script) oldMap.set(api.id, script)
  }
  const results = await Promise.all(defs.map(def => importOne(def, oldMap.get(def.id) || null)))
  const okCount = results.filter(Boolean).length
  await setUserApiList(await getUserApiList())
  log.info(`builtin sources refreshed: ${okCount}/${defs.length}`)
}
