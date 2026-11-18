import { View, TouchableOpacity } from 'react-native'
import { useTheme } from '@/store/theme/hook'
import { useNavActiveId, useStatusbarHeight } from '@/store/common/hook'
import { useSettingValue } from '@/store/setting/hook'
import { useI18n } from '@/lang'
import { createStyle, confirmDialog, exitApp } from '@/utils/tools'
import { Icon } from '@/components/common/Icon'
import Text from '@/components/common/Text'
import StatusBar from '@/components/common/StatusBar'
import { scaleSizeH, scaleSizeW } from '@/utils/pixelRatio'
import { HEADER_HEIGHT } from '@/config/constant'
import { type InitState as CommonState } from '@/store/common/state'
import SearchTypeSelector from '@/screens/Home/Views/Search/SearchTypeSelector'
import { setNavActiveId } from '@/core/common'
import { BorderRadius } from '@/theme'

const headerComponents: Partial<Record<CommonState['navActiveId'], React.ReactNode>> = {
  nav_search: <SearchTypeSelector />,
}

const goHome = () => {
  exitApp()
}

const Header = () => {
  const theme = useTheme()
  const id = useNavActiveId()
  const t = useI18n()
  const statusBarHeight = useStatusbarHeight()
  const showBackBtn = useSettingValue('common.showBackBtn')
  const showExitBtn = useSettingValue('common.showExitBtn')
  const handleExit = () => {
    void confirmDialog({
      message: t('exit_app_tip'),
      confirmButtonText: t('list_remove_tip_button'),
    }).then(isExit => {
      if (!isExit) return
      exitApp()
    })
  }

  return (
    <>
      <StatusBar />
      <View style={{
        ...styles.container,
        height: scaleSizeH(HEADER_HEIGHT) + statusBarHeight,
        paddingTop: statusBarHeight,
      }}>
        <View style={styles.left}>
          <Text style={styles.title} size={20}>{t(id)}</Text>
        </View>
        {headerComponents[id] ?? null}
        <View style={styles.right}>
          {showBackBtn ? (
            <TouchableOpacity
              style={[styles.btn, { backgroundColor: theme['c-primary-light-400-alpha-700'] }]}
              activeOpacity={0.8}
              onPress={goHome}
            >
              <Icon color={theme['c-primary-font-active']} name="home" size={17} />
            </TouchableOpacity>
          ) : null}
          {showExitBtn ? (
            <TouchableOpacity
              style={[styles.btn, { backgroundColor: theme['c-primary-light-400-alpha-700'] }]}
              activeOpacity={0.8}
              onPress={handleExit}
            >
              <Icon color={theme['c-primary-font-active']} name="exit2" size={17} />
            </TouchableOpacity>
          ) : null}
          <TouchableOpacity
            style={[styles.btn, { backgroundColor: theme['c-primary-light-400-alpha-700'] }]}
            activeOpacity={0.8}
            onPress={() => { setNavActiveId('nav_setting') }}
          >
            <Icon color={theme['c-primary-font-active']} name="setting" size={17} />
          </TouchableOpacity>
        </View>
      </View>
    </>
  )
}

const styles = createStyle({
  container: {
    paddingRight: 12,
    paddingLeft: 16,
    flexDirection: 'row',
    alignItems: 'center',
    zIndex: 10,
  },
  left: {
    flex: 1,
    flexShrink: 1,
    justifyContent: 'center',
    height: '100%',
  },
  right: {
    flexDirection: 'row',
    alignItems: 'center',
    flexShrink: 0,
  },
  title: {
    fontWeight: '700',
    letterSpacing: 0.6,
  },
  btn: {
    width: scaleSizeW(36),
    height: scaleSizeW(36),
    borderRadius: BorderRadius.round,
    alignItems: 'center',
    justifyContent: 'center',
    marginLeft: 8,
  },
})

export default Header
