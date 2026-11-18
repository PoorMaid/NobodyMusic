import { View } from 'react-native'
import Header from './Header'
import Main from './Main'
import { BOTTOM_NAV_HEIGHT } from './BottomNav'

const Content = () => {
  return (
    <>
      <Header />
      <View style={{ flex: 1 }}>
        <Main />
        <View style={{ height: BOTTOM_NAV_HEIGHT }} pointerEvents='none' />
      </View>
    </>
  )
}

export default Content
