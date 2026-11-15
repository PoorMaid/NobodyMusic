const path = require('path')
const { getDefaultConfig, mergeConfig } = require('@react-native/metro-config')
const config = {
  resolver: {
    extraNodeModules: {
      buffer: require.resolve('@craftzdog/react-native-buffer'),
    },
  },
  watcher: {
    watchman: false,
    enableWatch: false,
    ignorePatterns: [
      new RegExp('node_modules/.*build/'),
      new RegExp('node_modules/.*\.cxx/'),
    ],
  },
}
module.exports = mergeConfig(getDefaultConfig(__dirname), config)
