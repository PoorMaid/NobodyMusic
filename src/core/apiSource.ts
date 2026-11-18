// import { setUserApi as setUserApiAction } from '@renderer/utils/ipc'
import musicSdk from '@/utils/musicSdk'
// import apiSourceInfo from '@renderer/utils/musicSdk/api-source-info'
import { updateSetting } from './common'
import settingState from '@/store/setting/state'
import { destroyUserApi, setUserApi } from './userApi'
import apiSourceInfo from '@/utils/musicSdk/api-source-info'


export const setApiSource = (apiId: string) => {
  if (/^user_api/.test(apiId)) {
    global.lx.apiInitPromise[0] = new Promise(resolve => {
      global.lx.apiInitPromise[1] = false
      global.lx.apiInitPromise[2] = (result: boolean) => {
        global.lx.apiInitPromise[1] = true
        resolve(result)
      }
    })
  } else if (global.lx.apiInitPromise[1]) {
    global.lx.apiInitPromise[0] = new Promise(resolve => {
      global.lx.apiInitPromise[1] = false
      global.lx.apiInitPromise[2] = (result: boolean) => {
        global.lx.apiInitPromise[1] = true
        resolve(result)
      }
    })
    // @ts-expect-error
    global.lx.qualityList = musicSdk.supportQuality[apiId] ?? {}
    destroyUserApi()
    if (!global.lx.apiInitPromise[1]) global.lx.apiInitPromise[2](true)
  }
  if (/^user_api/.test(apiId)) {
    if (apiId != settingState.setting['common.apiSource']) {
      settingState.setting['common.apiSource'] = apiId
      updateSetting({ 'common.apiSource': apiId })
    }
    setUserApi(apiId).catch(err => {
      if (!global.lx.apiInitPromise[1]) global.lx.apiInitPromise[2](false)
      console.log(err)
      // Nobody Music: 加载失败也要落定勾选态，避免 UI 一直显示未勾选
      let api = apiSourceInfo.find(api => !api.disabled)
      if (api) {
        if (api.id != settingState.setting['common.apiSource']) setApiSource(api.id)
      } else if (settingState.setting['common.apiSource'] != apiId) {
        settingState.setting['common.apiSource'] = apiId
        updateSetting({ 'common.apiSource': apiId })
      }
    })
  } else {
    // @ts-expect-error
    global.lx.qualityList = musicSdk.supportQuality[apiId] ?? {}
    destroyUserApi()
    if (!global.lx.apiInitPromise[1]) global.lx.apiInitPromise[2](true)
    // apiSource.value = apiId
    // void setUserApiAction(apiId)
  }

  if (apiId != settingState.setting['common.apiSource']) {
    updateSetting({ 'common.apiSource': apiId })
  }
  // Nobody Music: apiSourceUpdated 必须无条件发出，否则换源后播放层仍用旧音源
  requestAnimationFrame(() => {
    global.state_event.apiSourceUpdated(apiId)
  })
}

