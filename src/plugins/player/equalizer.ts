import TrackPlayer, {
  eqSetEnabled,
  eqSetGains,
  eqGetGains,
  eqIsEnabled,
  eqGetBandLabels,
} from 'react-native-track-player'

export interface EqualizerBridge {
  setEnabled: (enabled: boolean) => Promise<void>
  setGains: (gains: number[]) => Promise<void>
  getGains: () => Promise<number[]>
  isEnabled: () => Promise<boolean>
  getBandLabels: () => Promise<string[]>
  apply: (enabled: boolean, gains: number[]) => Promise<void>
}

const bridge: EqualizerBridge = {
  async setEnabled(enabled) {
    await eqSetEnabled(enabled)
  },
  async setGains(gains) {
    await eqSetGains(gains)
  },
  async getGains() {
    return eqGetGains()
  },
  async isEnabled() {
    return eqIsEnabled()
  },
  async getBandLabels() {
    return eqGetBandLabels()
  },
  async apply(enabled, gains) {
    try {
      await eqSetGains(gains)
      await eqSetEnabled(enabled)
    } catch (err) {
      console.log('equalizer apply error', err)
    }
  },
}

let inited = false
// eslint-disable-next-line @typescript-eslint/no-explicit-any
export const getEqualizerBridge = (): EqualizerBridge => {
  if (!inited) {
    inited = true
    // ensure native module exists
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    if (!(TrackPlayer as any)) console.log('TrackPlayer missing')
  }
  return bridge
}
