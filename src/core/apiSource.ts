// import { setUserApi as setUserApiAction } from '@renderer/utils/ipc'
import musicSdk from '@/utils/musicSdk'
// import apiSourceInfo from '@renderer/utils/musicSdk/api-source-info'
import { updateSetting } from './common'
import settingState from '@/store/setting/state'
import { destroyUserApi, setUserApi } from './userApi'
import apiSourceInfo from '@/utils/musicSdk/api-source-info'


export const setApiSource = (apiId: string) => {
  const isUserApi = /^user_api/.test(apiId)
  console.log('[NBSRC] click ' + apiId + ' cur=' + String(settingState.setting['common.apiSource']) + ' initFlag=' + String(global.lx.apiInitPromise[1]))

  if (isUserApi) {
    global.lx.apiInitPromise[0] = new Promise(resolve => {
      global.lx.apiInitPromise[1] = false
      global.lx.apiInitPromise[2] = (result: boolean) => {
        global.lx.apiInitPromise[1] = true
        resolve(result)
      }
    })
    global.lx.qualityList = {}
    // Nobody Music: 脚本可能永不回调 init（网络/脚本异常），加超时兑底，避免卡在初始化中
    setTimeout(() => {
      if (!global.lx.apiInitPromise[1]) {
        console.log('[NBSRC] init timeout, force release ' + apiId)
        global.lx.apiInitPromise[2](true)
      }
    }, 12_000)
  } else {
    global.lx.apiInitPromise[0] = Promise.resolve(true)
    global.lx.apiInitPromise[1] = true
    global.lx.apiInitPromise[2] = () => {}
    // @ts-expect-error
    global.lx.qualityList = musicSdk.supportQuality[apiId] ?? {}
    destroyUserApi()
  }

  if (settingState.setting['common.apiSource'] !== apiId) {
    updateSetting({ 'common.apiSource': apiId })
  }

  requestAnimationFrame(() => {
    global.state_event.apiSourceUpdated(apiId)
  })

  if (isUserApi) {
    setUserApi(apiId).catch(err => {
      console.log('[NBSRC] load fail ' + apiId + ': ' + (err as Error).message)
      // Nobody Music: 失败也放开闸门，避免永久 source init failed
      if (!global.lx.apiInitPromise[1]) global.lx.apiInitPromise[2](true)
    })
  }
}
