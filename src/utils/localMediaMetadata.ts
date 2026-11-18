import { temporaryDirectoryPath, readDir, unlink, extname } from '@/utils/fs'
import { readPic as _readPic } from 'react-native-local-media-metadata'
export {
  type MusicMetadata,
  type MusicMetadataFull,
  readMetadata,
  writeMetadata,
  writePic,
  readLyric,
  writeLyric,
} from 'react-native-local-media-metadata'

let cleared = false
const picCachePath = temporaryDirectoryPath + '/local-media-metadata'

export const scanAudioFiles = async(dirPath: string, depth = 0): Promise<Awaited<ReturnType<typeof readDir>>> => {
  const MAX_DEPTH = 8
  let files: Awaited<ReturnType<typeof readDir>> = []
  try {
    files = await readDir(dirPath)
  } catch {
    return []
  }
  const result: Awaited<ReturnType<typeof readDir>> = []
  const subDirs: string[] = []
  for (const file of files) {
    if (file.isDirectory) {
      if (depth < MAX_DEPTH && file.path) subDirs.push(file.path)
      continue
    }
    if (file.mimeType?.startsWith('audio/')) {
      result.push(file)
      continue
    }
    if (extname(file?.name ?? '') === 'ogg') result.push(file)
  }
  for (const sub of subDirs) {
    result.push(...await scanAudioFiles(sub, depth + 1))
  }
  return result
}

const clearPicCache = async() => {
  await unlink(picCachePath)
  cleared = true
}

export const readPic = async(dirPath: string): Promise<string> => {
  if (!cleared) await clearPicCache()
  return _readPic(dirPath, picCachePath)
}

// export interface MusicMetadata {
//   type: 'mp3' | 'flac' | 'ogg' | 'wav'
//   bitrate: string
//   interval: number
//   size: number
//   ext: 'mp3' | 'flac' | 'ogg' | 'wav'
//   albumName: string
//   singer: string
//   name: string
// }
// export const readMetadata = async(filePath: string): Promise<MusicMetadata | null> => {
//   return LocalMediaModule.readMetadata(filePath)
// }

// export const readPic = async(filePath: string): Promise<string> => {
//   return LocalMediaModule.readPic(filePath)
// }

// export const readLyric = async(filePath: string): Promise<string> => {
//   return LocalMediaModule.readLyric(filePath)
// }


