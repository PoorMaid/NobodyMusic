import { initSetting, showPactModal } from '@/core/common'
import registerPlaybackService from '@/plugins/player/service'
import initTheme from './theme'
import initI18n from './i18n'
import initUserApi from './userApi'
import initPlayer from './player'
import dataInit from './dataInit'
import initSync from './sync'
import initCommonState from './common'
import { initDeeplink } from './deeplink'
import { setApiSource } from '@/core/apiSource'
import commonActions from '@/store/common/action'
import settingState from '@/store/setting/state'
import { state as userApiState } from '@/store/userApi/state'
import { checkUpdate } from '@/core/version'
import { bootLog } from '@/utils/bootLog'
import { cheatTip } from '@/utils/tools'

let isFirstPush = true
const handlePushedHomeScreen = async() => {
  await cheatTip()
  if (settingState.setting['common.isAgreePact']) {
    if (isFirstPush) {
      isFirstPush = false
      // void checkUpdate()
      void initDeeplink()
    }
  } else {
    if (isFirstPush) isFirstPush = false
    showPactModal()
  }
}

let isInited = false
export default async() => {
  if (isInited) return handlePushedHomeScreen
  bootLog('Initing...')
  commonActions.setFontSize(global.lx.fontSize)
  bootLog('Font size changed.')
  const setting = await initSetting()
  bootLog('Setting inited.')
  // console.log(setting)

  await initTheme(setting)
  bootLog('Theme inited.')
  await initI18n(setting)
  bootLog('I18n inited.')

  await initUserApi(setting)
  bootLog('User Api inited.')
  // Nobody Music: 启动即标记“加载中”，避免首屏误显示“初始化失败/未初始化”
  userApiState.status.status = false
  userApiState.status.message = 'initing'

  // Nobody Music: 启动时先落定一个生效音源，避免 UI 全部未勾选
  // 已保存音源优先，其次用户/内置音源列表首项；探测挑选交给后台 autoPick
  const savedApiSource = setting['common.apiSource']
  const fallbackApiSource = userApiState.list[0]?.id
  const targetApiSource = savedApiSource || fallbackApiSource
  // Nobody Music: 无条件调用 setApiSource，保证 apiInitPromise 闸门每一轮启动都被真正建立
  // 否则第二次启动沿用已保存音源时跳过调用，闸门停留在初值 false，导致永久“初始化中 / 播放不了”
  if (targetApiSource) {
    setApiSource(targetApiSource)
    bootLog('Api source applied: ' + targetApiSource)
  }
  bootLog('Api inited.')

  registerPlaybackService()
  bootLog('Playback Service Registered.')
  await initPlayer(setting)
  bootLog('Player inited.')
  await dataInit(setting)
  bootLog('Data inited.')
  await initCommonState(setting)
  bootLog('Common State inited.')

  void initSync(setting)
  bootLog('Sync inited.')

  // syncSetting()

  isInited ||= true

  return handlePushedHomeScreen
}
