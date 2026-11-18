import { memo, useCallback, useEffect, useRef, useState } from 'react'
import { View, TouchableOpacity, Animated, Easing } from 'react-native'
import { useTheme } from '@/store/theme/hook'
import { useNavActiveId } from '@/store/common/hook'
import { useI18n } from '@/lang'
import { createStyle } from '@/utils/tools'
import { Icon } from '@/components/common/Icon'
import Text from '@/components/common/Text'
import { BorderRadius } from '@/theme'
import { setNavActiveId } from '@/core/common'
import { NAV_MENUS, type NAV_ID_Type } from '@/config/constant'
import { scaleSizeH, scaleSizeW } from '@/utils/pixelRatio'

const BAR_HEIGHT = scaleSizeH(54)
const ICON_SIZE = 20

const HANDLE_SIZE = scaleSizeW(34)
const OUTER_PADDING_TOP = scaleSizeH(4)
const OUTER_PADDING_BOTTOM = scaleSizeH(4)

export const BOTTOM_NAV_HEIGHT = BAR_HEIGHT + OUTER_PADDING_TOP + OUTER_PADDING_BOTTOM
export const BOTTOM_NAV_COLLAPSED_HEIGHT = HANDLE_SIZE + OUTER_PADDING_TOP + OUTER_PADDING_BOTTOM

const NavItem = ({ id, icon, active }: { id: NAV_ID_Type, icon: string, active: boolean }) => {
  const theme = useTheme()
  const t = useI18n()
  const scale = useRef(new Animated.Value(1)).current
  const rise = useRef(new Animated.Value(active ? 1 : 0)).current

  useEffect(() => {
    Animated.timing(rise, {
      toValue: active ? 1 : 0,
      duration: 220,
      easing: Easing.out(Easing.cubic),
      useNativeDriver: true,
    }).start()
  }, [active, rise])

  const onPressIn = () => {
    Animated.timing(scale, {
      toValue: 0.9,
      duration: 90,
      easing: Easing.out(Easing.quad),
      useNativeDriver: true,
    }).start()
  }
  const onPressOut = () => {
    Animated.timing(scale, {
      toValue: 1,
      duration: 170,
      easing: Easing.out(Easing.back(1.6)),
      useNativeDriver: true,
    }).start()
  }

  return (
    <Animated.View style={{ flex: 1, transform: [{ scale }] }}>
      <TouchableOpacity
        activeOpacity={0.85}
        onPressIn={onPressIn}
        onPressOut={onPressOut}
        onPress={() => { setNavActiveId(id) }}
        style={styles.item}
      >
        <Animated.View style={{
          ...styles.pill,
          backgroundColor: active ? theme['c-primary-light-400-alpha-700'] : 'transparent',
          opacity: rise.interpolate({ inputRange: [0, 1], outputRange: [0.45, 1] }),
          transform: [{ translateY: rise.interpolate({ inputRange: [0, 1], outputRange: [2, 0] }) }],
        }}>
          <Icon
            name={icon}
            size={ICON_SIZE}
            color={active ? theme['c-primary-font-active'] : theme['c-font-label']}
          />
        </Animated.View>
        <Text
          size={10}
          numberOfLines={1}
          color={active ? theme['c-primary-font-active'] : theme['c-font-label']}
          style={{ marginTop: 2, opacity: active ? 1 : 0.8 }}
        >{t(id)}</Text>
      </TouchableOpacity>
    </Animated.View>
  )
}

const BottomNav = () => {
  const theme = useTheme()
  const activeId = useNavActiveId()
  const [open, setOpen] = useState(true)
  const slide = useRef(new Animated.Value(1)).current
  const toggle = useCallback(() => {
    setOpen(prev => {
      const next = !prev
      Animated.timing(slide, {
        toValue: next ? 1 : 0,
        duration: 240,
        easing: next ? Easing.out(Easing.cubic) : Easing.in(Easing.cubic),
        useNativeDriver: true,
      }).start()
      return next
    })
  }, [slide])
  const handleSlide = slide.interpolate({
    inputRange: [0, 1],
    outputRange: [HANDLE_SIZE * 0.7, 0],
  })
  return (
    <View style={styles.outer} pointerEvents='box-none'>
      <Animated.View style={[styles.row, { transform: [{ translateY: handleSlide }] }]} pointerEvents='box-none'>
        <Animated.View style={{
          flex: 1,
          opacity: slide,
          transform: [{ translateY: slide.interpolate({ inputRange: [0, 1], outputRange: [BAR_HEIGHT + 12, 0] }) }],
        }} pointerEvents={open ? 'auto' : 'none'}>
          <View style={[styles.bar, { backgroundColor: theme['c-content-background'] }]}>
            <View style={styles.list}>
              {NAV_MENUS.filter(menu => menu.id != 'nav_setting').map(menu => (
                <NavItem key={menu.id} id={menu.id} icon={menu.icon} active={activeId == menu.id && open} />
              ))}
            </View>
          </View>
        </Animated.View>
        <TouchableOpacity style={[styles.handle, { backgroundColor: theme['c-content-background'] }]} onPress={toggle} activeOpacity={0.8}>
          <Icon
            name={open ? 'chevron-right-2' : 'chevron-left-2'}
            size={16}
            color={theme['c-primary-font-active']}
            style={{ transform: [{ rotate: open ? '90deg' : '-90deg' }] }}
          />
        </TouchableOpacity>
      </Animated.View>
    </View>
  )
}

const styles = createStyle({
  outer: {
    position: 'absolute',
    left: 0,
    right: 0,
    bottom: 0,
    paddingHorizontal: 10,
    paddingTop: OUTER_PADDING_TOP,
    paddingBottom: OUTER_PADDING_BOTTOM,
  },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  bar: {
    height: BAR_HEIGHT,
    borderRadius: BorderRadius.xlarge,
    flexDirection: 'row',
    alignItems: 'center',
    elevation: 8,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.16,
    shadowRadius: 14,
    overflow: 'hidden',
  },
  list: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 4,
  },
  item: {
    flex: 1,
    height: BAR_HEIGHT,
    alignItems: 'center',
    justifyContent: 'center',
  },
  pill: {
    width: scaleSizeW(40),
    height: scaleSizeW(30),
    borderRadius: BorderRadius.round,
    alignItems: 'center',
    justifyContent: 'center',
  },
  handle: {
    width: HANDLE_SIZE,
    height: HANDLE_SIZE,
    marginLeft: scaleSizeW(10),
    borderRadius: BorderRadius.round,
    alignItems: 'center',
    justifyContent: 'center',
    elevation: 12,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.28,
    shadowRadius: 12,
  },
})

export default memo(BottomNav)
