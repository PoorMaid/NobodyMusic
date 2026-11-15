import { memo, useCallback, useEffect, useMemo, useState } from 'react'
import { View, TouchableOpacity } from 'react-native'
import { updateSetting } from '@/core/common'
import { useI18n } from '@/lang'
import { useSettingValue } from '@/store/setting/hook'
import { createStyle } from '@/utils/tools'
import Text from '@/components/common/Text'
import SubTitle from '../../../components/SubTitle'
import Slider from '../../../components/Slider'
import CheckBoxItem from '../../../components/CheckBoxItem'
import { getEqualizerBridge } from '@/plugins/player/equalizer'

const BANDS = ['31', '62', '125', '250', '500', '1k', '2k', '4k', '8k', '16k']
const MIN_DB = -12
const MAX_DB = 12

const PRESETS: Array<{ id: string; label: string; gains: number[] }> = [
  { id: 'flat', label: 'setting_play_equalizer_flat', gains: [0, 0, 0, 0, 0, 0, 0, 0, 0, 0] },
  { id: 'bass', label: 'setting_play_equalizer_bass', gains: [8, 7, 5, 3, 1, 0, 0, 0, 0, 0] },
  { id: 'treble', label: 'setting_play_equalizer_treble', gains: [0, 0, 0, 0, 0, 1, 3, 5, 7, 8] },
  { id: 'vocal', label: 'setting_play_equalizer_vocal', gains: [-2, -1, 0, 2, 4, 4, 3, 1, 0, -1] },
  { id: 'rock', label: 'setting_play_equalizer_rock', gains: [5, 4, 2, -1, -2, 0, 2, 4, 5, 5] },
  { id: 'pop', label: 'setting_play_equalizer_pop', gains: [-1, 1, 3, 4, 3, 1, -1, -1, 1, 2] },
  { id: 'jazz', label: 'setting_play_equalizer_jazz', gains: [3, 2, 1, 2, -1, -1, 0, 1, 2, 3] },
  { id: 'classical', label: 'setting_play_equalizer_classical', gains: [4, 3, 2, 1, -1, -1, 0, 2, 3, 4] },
]

const useActivePreset = (gains: number[]) => {
  return useMemo(() => {
    for (const p of PRESETS) {
      if (p.gains.every((v, i) => Math.abs(v - (gains[i] ?? 0)) < 0.01)) return p.id
    }
    return ''
  }, [gains])
}

const BandRow = ({ label, value, onChange, onComplete, disabled }: {
  label: string
  value: number
  onChange: (v: number) => void
  onComplete: (v: number) => void
  disabled: boolean
}) => {
  return (
    <View style={styles.bandRow}>
      <Text size={12} style={styles.bandName}>{label}</Text>
      <Slider
        value={value}
        minimumValue={MIN_DB}
        maximumValue={MAX_DB}
        step={1}
        disabled={disabled}
        onValueChange={onChange}
        onSlidingComplete={onComplete}
      />
      <Text size={12} style={styles.gainVal}>{`${value > 0 ? '+' : ''}${Math.round(value * 10) / 10} dB`}</Text>
    </View>
  )
}

export default memo(() => {
  const t = useI18n() as (key: string) => string
  const enabled = useSettingValue('player.isEnableEqualizer')
  const settingGains = useSettingValue('player.equalizerGains')
  const [gains, setGains] = useState<number[]>(settingGains ?? new Array(10).fill(0))
  const activePreset = useActivePreset(gains)

  useEffect(() => {
    setGains(settingGains ?? new Array(10).fill(0))
  }, [settingGains])

  useEffect(() => {
    void getEqualizerBridge().apply(enabled, gains)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const onToggle = useCallback((on: boolean) => {
    updateSetting({ 'player.isEnableEqualizer': on })
    void getEqualizerBridge().apply(on, gains)
  }, [gains])

  const onBandChange = useCallback((index: number, value: number) => {
    setGains(prev => {
      const next = [...prev]
      next[index] = value
      return next
    })
  }, [])

  const onBandComplete = useCallback((index: number, value: number) => {
    setGains(prev => {
      const next = [...prev]
      next[index] = value
      const rounded = next.map(v => Math.round(v * 10) / 10)
      updateSetting({ 'player.equalizerGains': rounded })
      void getEqualizerBridge().apply(enabled, rounded)
      return rounded
    })
  }, [enabled])

  const onPreset = useCallback((presetGains: number[]) => {
    setGains(presetGains)
    updateSetting({ 'player.equalizerGains': presetGains })
    void getEqualizerBridge().apply(enabled, presetGains)
  }, [enabled])

  const onReset = useCallback(() => {
    onPreset(new Array(10).fill(0))
  }, [onPreset])

  return (
    <SubTitle title={t('setting_play_equalizer')}>
      <View style={styles.content}>
        <CheckBoxItem
          check={enabled}
          onChange={onToggle}
          label={t('setting_play_equalizer_enable')}
          helpDesc={t('setting_play_equalizer_tip')}
        />
        {
          enabled
            ? (
              <>
                <View style={styles.presetRow}>
                  {
                    PRESETS.map(p => (
                      <TouchableOpacity
                        key={p.id}
                        activeOpacity={0.6}
                        onPress={() => onPreset(p.gains)}
                        style={[styles.presetBtn, activePreset === p.id && styles.presetBtnActive]}>
                        <Text size={12}>{t(p.label)}</Text>
                      </TouchableOpacity>
                    ))
                  }
                  <TouchableOpacity activeOpacity={0.6} onPress={onReset} style={styles.presetBtn}>
                    <Text size={12}>{t('setting_play_equalizer_reset')}</Text>
                  </TouchableOpacity>
                </View>
                <View style={styles.bands}>
                  {
                    BANDS.map((band, i) => (
                      <BandRow
                        key={band}
                        label={band}
                        value={gains[i] ?? 0}
                        disabled={false}
                        onChange={(v) => onBandChange(i, v)}
                        onComplete={(v) => onBandComplete(i, v)}
                      />
                    ))
                  }
                </View>
                <Text size={11} style={styles.tip}>{t('setting_play_equalizer_tip_offload')}</Text>
              </>
            )
            : null
        }
      </View>
    </SubTitle>
  )
})

const styles = createStyle({
  content: {
    marginTop: 5,
  },
  presetRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
    marginTop: 12,
  },
  presetBtn: {
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 6,
    backgroundColor: 'rgba(128,128,128,0.18)',
  },
  presetBtnActive: {
    backgroundColor: 'rgba(128,128,128,0.4)',
  },
  bands: {
    marginTop: 12,
  },
  bandRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  bandName: {
    width: 42,
    opacity: 0.8,
  },
  gainVal: {
    width: 62,
    textAlign: 'right',
    opacity: 0.7,
  },
  tip: {
    marginTop: 14,
    opacity: 0.5,
  },
})
