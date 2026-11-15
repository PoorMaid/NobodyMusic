import { useRef } from 'react'
import { Animated, Easing, TouchableOpacity } from 'react-native'
import { Icon } from '@/components/common/Icon'
import { createStyle } from '@/utils/tools'
import { scaleSizeH } from '@/utils/pixelRatio'
import { HEADER_HEIGHT as _HEADER_HEIGHT } from '@/config/constant'
import { useTheme } from '@/store/theme/hook'

export const HEADER_HEIGHT = scaleSizeH(_HEADER_HEIGHT)

export default ({ icon, color, onPress }: {
  icon: string
  color?: string
  onPress: () => void
}) => {
  const theme = useTheme()
  const scale = useRef(new Animated.Value(1)).current

  const pressIn = () => {
    Animated.timing(scale, { toValue: 0.9, duration: 90, easing: Easing.out(Easing.quad), useNativeDriver: true }).start()
  }
  const pressOut = () => {
    Animated.timing(scale, { toValue: 1, duration: 170, easing: Easing.out(Easing.quad), useNativeDriver: true }).start()
  }

  return (
    <Animated.View style={{ transform: [{ scale }], paddingHorizontal: 3 }}>
      <TouchableOpacity
        activeOpacity={0.8}
        onPress={onPress}
        onPressIn={pressIn}
        onPressOut={pressOut}
        style={[styles.button, { backgroundColor: theme['c-primary-light-700-alpha-500'] }]}
      >
        <Icon name={icon} color={color} size={18} />
      </TouchableOpacity>
    </Animated.View>
  )
}

const styles = createStyle({
  button: {
    justifyContent: 'center',
    alignItems: 'center',
    width: 38,
    height: 38,
    borderRadius: 19,
  },
})
