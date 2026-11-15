import { useState, useEffect, useRef } from 'react'
import playerState from '@/store/player/state'
import { collectMusic, uncollectMusic } from '@/core/player/player'
import { getListAsync } from '@/core/player/playInfo'
import { LIST_IDS } from '@/config/constant'
import { useTheme } from '@/store/theme/hook'
import Btn from './Btn'
export default () => {
  const theme = useTheme()
  const musicInfo = playerState.playMusicInfo.musicInfo
  const [loved, setLoved] = useState(false)
  const idRef = useRef('')
  const id = musicInfo ? ('progress' in musicInfo ? musicInfo.metadata.musicInfo.id : musicInfo.id) : ''
  idRef.current = id
  const check = async() => {
    if (!id) {
      setLoved(false)
      return
    }
    const list = await getListAsync(LIST_IDS.LOVE)
    if (idRef.current != id) return
    setLoved(list.some(m => m.id == id))
  }
  useEffect(() => {
    void check()
  }, [id])
  const handlePress = () => {
    if (!musicInfo) return
    if (loved) uncollectMusic()
    else collectMusic()
    setTimeout(() => { void check() }, 300)
  }
  return (
    <Btn
      icon='love'
      color={loved ? theme['c-primary'] : undefined}
      onPress={handlePress}
    />
  )
}
