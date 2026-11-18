import { View } from 'react-native'
import Header from './Header'
import Main from './Main'

const Content = () => {
  return (
    <>
      <Header />
      <View style={{ flex: 1 }}>
        <Main />
      </View>
    </>
  )
}

export default Content
