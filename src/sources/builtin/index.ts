const RAW = 'https://raw.githubusercontent.com/pdone/lx-music-source/main'

export interface BuiltinSourceDef {
  key: string
  id: string
  name: string
  url: string
}

const NAME_MAP: Record<string, string> = {
  juhe: '聚合',
  lx: 'LX',
  ikun: 'i坤',
  grass: 'Grass',
  flower: 'Flower',
  huibq: 'Huibq',
  sixyin: 'SixYin',
  qdy: 'qdy',
  yuningxi: 'Yun',
  qingliang: '清凉',
  xinghai: '星海',
  suyin: '随音',
  xinglan: '星岚',
  jiexiang: '解耦',
  hyw: 'H氪',
  huanyin: '幻音',
  helloworld: 'Hello',
  fish: 'Fish',
  feichangdao: '非常道',
  changqing: '长青',
}

const KEYS = [
  'juhe', 'lx', 'ikun', 'grass', 'flower', 'huibq', 'sixyin', 'qdy',
  'yuningxi', 'qingliang', 'xinghai', 'suyin', 'xinglan', 'jiexiang',
  'hyw', 'huanyin', 'helloworld', 'fish', 'feichangdao', 'changqing',
]

export const URL_OVERRIDE: Record<string, string> = {
  xinghai: 'https://zrcdy.dpdns.org/lx/xinghai-music-sourcev2.3.19.js',
}

const def = (key: string): BuiltinSourceDef => ({
  key,
  id: `user_api_builtin_${key}`,
  name: NAME_MAP[key] || key,
  url: URL_OVERRIDE[key] || `${RAW}/${key}/latest.js`,
})

export const BUILTIN_SOURCES: BuiltinSourceDef[] = KEYS.map(def)

export const BUILTIN_SOURCE_IDS = BUILTIN_SOURCES.map(s => s.id)

export const BUILTIN_UPDATE_FILE = 'builtin_sources_urls.json'

export const BUILTIN_UPDATE_URLS = [
  `https://raw.githubusercontent.com/PoorMaid/NobodyMusic/master/${BUILTIN_UPDATE_FILE}`,
  `https://cdn.jsdelivr.net/gh/PoorMaid/NobodyMusic@master/${BUILTIN_UPDATE_FILE}`,
]

export const isBuiltinSourceId = (id: string): boolean => id.startsWith('user_api_builtin_')
