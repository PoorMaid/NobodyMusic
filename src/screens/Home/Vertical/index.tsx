import { useCallback, useRef } from 'react'
import { Animated, Easing } from 'react-native'
import Content from './Content'
import BottomNav, { BOTTOM_NAV_HEIGHT, BOTTOM_NAV_COLLAPSED_HEIGHT } from './BottomNav'
import PlayerBar from '@/components/player/PlayerBar'

export default () => {
  const navHeight = useRef(new Animated.Value(BOTTOM_NAV_HEIGHT)).current

  const handleVisibleChange = useCallback((visible: boolean) => {
    Animated.timing(navHeight, {
      toValue: visible ? BOTTOM_NAV_HEIGHT : BOTTOM_NAV_COLLAPSED_HEIGHT,
      duration: 240,
      easing: visible ? Easing.out(Easing.cubic) : Easing.in(Easing.cubic),
      useNativeDriver: false,
    }).start()
  }, [navHeight])

  return (
    <>
      <Content />
      <Animated.View style={{ height: navHeight, overflow: 'hidden' }}>
        <BottomNav onVisibleChange={handleVisibleChange} />
      </Animated.View>
      <PlayerBar isHome />
    </>
  )
}
