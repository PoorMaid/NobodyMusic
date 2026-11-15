/**
 * Nobody Music 内置在线音源订阅清单。
 * 启动时自动从这些 URL 拉取脚本并注册为「用户自定义音源」（固定 id，覆盖式更新）。
 */
export interface BuiltinSourceDef {
  key: string
  id: string
  name: string
  url: string
}

const RAW = 'https://raw.githubusercontent.com/pdone/lx-music-source/main'

export const BUILTIN_SOURCES: BuiltinSourceDef[] = [
  { key: 'juhe', id: 'user_api_builtin_juhe', name: '聚合API', url: `${RAW}/juhe/latest.js` },
  { key: 'grass', id: 'user_api_builtin_grass', name: 'Grass', url: `${RAW}/grass/latest.js` },
  { key: 'ikun', id: 'user_api_builtin_ikun', name: 'Ikun', url: `${RAW}/ikun/latest.js` },
  { key: 'lx', id: 'user_api_builtin_lx', name: 'LX', url: `${RAW}/lx/latest.js` },
  { key: 'flower', id: 'user_api_builtin_flower', name: 'Flower', url: `${RAW}/flower/latest.js` },
  { key: 'huibq', id: 'user_api_builtin_huibq', name: 'Huibq', url: `${RAW}/huibq/latest.js` },
  { key: 'sixyin', id: 'user_api_builtin_sixyin', name: 'SixYin', url: `${RAW}/sixyin/latest.js` },
]

export const BUILTIN_SOURCE_IDS = BUILTIN_SOURCES.map(s => s.id)
export const isBuiltinSourceId = (id: string) => BUILTIN_SOURCE_IDS.includes(id)
