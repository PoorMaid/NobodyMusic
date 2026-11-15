import { memo, useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { View, Animated, PanResponder, Easing } from 'react-native'
import { createStyle } from '@/utils/tools'
import { useTheme } from '@/store/theme/hook'
import { scaleSizeW, scaleSizeH } from '@/utils/pixelRatio'
import { useDrag } from '@/utils/hooks'

const WAVE_COUNT = 42
const CONTENT_PADDING = 10
const WAVE_GAP = 2
const PROGRESS_TICK = 120

const randomHeight = () => 0.35 + Math.random() * 0.65

const WaveBars = memo(({ progress, buffered, dragProgress, draging }: {
  progress: number
  buffered: number
  dragProgress: number
  draging: boolean
}) => {
  const theme = useTheme()
  const heights = useRef<number[]>(Array.from({ length: WAVE_COUNT }, randomHeight)).current
  const phases = useRef<number[]>(Array.from({ length: WAVE_COUNT }, () => Math.random() * Math.PI * 2)).current
  const anims = useRef<Animated.Value[]>(Array.from({ length: WAVE_COUNT }, () => new Animated.Value(0.55))).current
  const [tick, setTick] = useState(0)
  const effProgress = draging ? dragProgress : progress

  useEffect(() => {
    const timer = setInterval(() => setTick(t => t + 1), PROGRESS_TICK)
    return () => clearInterval(timer)
  }, [])

  useEffect(() => {
    const t = tick * 0.9
    for (let i = 0; i < WAVE_COUNT; i++) {
      const wobble = 0.62 + 0.38 * Math.sin(t + phases[i])
      const factor = heights[i] * wobble
      const a = anims[i]
      a.stopAnimation()
      Animated.timing(a, {
        toValue: factor,
        duration: PROGRESS_TICK,
        easing: Easing.linear,
        useNativeDriver: true,
      }).start()
    }
  }, [tick, anims, heights, phases])

  const activeColor = theme['c-primary-light-100']
  const inactiveColor = theme['c-primary-light-300-alpha-800']
  const bufferColor = theme['c-primary-light-400-alpha-700']
  const activated = Math.round(effProgress * WAVE_COUNT)
  const bufferedCount = Math.max(Math.round(buffered * WAVE_COUNT), activated)

  return (
    <View style={styles.waves}>
      {
        anims.map((a, i) => {
          const color = i < activated ? activeColor : (i < bufferedCount ? bufferColor : inactiveColor)
          return (
            <Animated.View
              key={i}
              style={{
                ...styles.wave,
                backgroundColor: color,
                opacity: i < bufferedCount ? 1 : 0.7,
                transform: [{ scaleY: a }],
              }}
            />
          )
        })
      }
    </View>
  )
})

const PressArea = ({ onDragState, setDragProgress, onSetProgress }: {
  onDragState: (drag: boolean) => void
  setDragProgress: (progress: number) => void
  onSetProgress: (progress: number) => void
}) => {
  const { onLayout, onDragStart, onDragEnd, onDrag } = useDrag(onSetProgress, onDragState, setDragProgress)
  const panResponder = useRef(
    PanResponder.create({
      onStartShouldSetPanResponderCapture: () => true,
      onMoveShouldSetPanResponderCapture: () => true,
      onPanResponderMove: (evt, gestureState) => {
        onDrag(gestureState.dx)
      },
      onPanResponderGrant: (evt, gestureState) => {
        onDragStart(gestureState.dx, evt.nativeEvent.locationX)
      },
      onPanResponderRelease: () => {
        onDragEnd()
      },
    }),
  ).current

  return <View onLayout={onLayout} style={styles.pressArea} {...panResponder.panHandlers} />
}

const WaveProgressBar = ({ progress, duration, buffered }: {
  progress: number
  duration: number
  buffered: number
}) => {
  const [draging, setDraging] = useState(false)
  const [dragProgress, setDragProgress] = useState(0)
  const durationRef = useRef(duration)

  useEffect(() => {
    durationRef.current = duration
  }, [duration])

  const onSetProgress = useCallback((p: number) => {
    global.app_event.setProgress(p * durationRef.current)
  }, [])

  const safeProgress = useMemo(() => {
    if (!isFinite(progress) || progress < 0) return 0
    return progress > 1 ? 1 : progress
  }, [progress])

  return (
    <View style={styles.progress}>
      <WaveBars
        progress={safeProgress}
        buffered={buffered || 0}
        dragProgress={dragProgress}
        draging={draging}
      />
      <PressArea onDragState={setDraging} setDragProgress={setDragProgress} onSetProgress={onSetProgress} />
    </View>
  )
}

const styles = createStyle({
  progress: {
    width: '100%',
    paddingTop: CONTENT_PADDING,
    paddingBottom: CONTENT_PADDING,
    zIndex: 1,
  },
  waves: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    width: '100%',
    height: scaleSizeH(26),
  },
  wave: {
    flex: 1,
    height: scaleSizeH(26),
    marginRight: WAVE_GAP,
    borderRadius: scaleSizeW(2),
  },
  pressArea: {
    position: 'absolute',
    left: 0,
    top: 0,
    width: '100%',
    height: CONTENT_PADDING * 2 + scaleSizeH(26),
    zIndex: 6,
  },
})

export default WaveProgressBar
