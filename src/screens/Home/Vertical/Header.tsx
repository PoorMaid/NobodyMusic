import { View, TouchableOpacity } from 'react-native'
import { useTheme } from '@/store/theme/hook'
import { useNavActiveId, useStatusbarHeight } from '@/store/common/hook'
import { useI18n } from '@/lang'
import { createStyle } from '@/utils/tools'
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

const Header = () => {
  const theme = useTheme()
  const id = useNavActiveId()
  const t = useI18n()
  const statusBarHeight = useStatusbarHeight()

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
        <TouchableOpacity
          style={[styles.btn, { backgroundColor: theme['c-primary-light-400-alpha-700'] }]}
          activeOpacity={0.8}
          onPress={() => { setNavActiveId('nav_setting') }}
        >
          <Icon color={theme['c-primary-font-active']} name="setting" size={17} />
        </TouchableOpacity>
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
    justifyContent: 'center',
    height: '100%',
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
