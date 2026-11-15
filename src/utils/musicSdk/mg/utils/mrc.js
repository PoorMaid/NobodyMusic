
// const key = 'karakal@123Qcomyidongtiantianhaoting'
// 无 BigInt 环境（如 Android JSC）下不得不降级：直接返回原数据，避免整个 bundle 解析失败
const HAS_BIGINT = typeof BigInt == 'function'
const B = v => BigInt(v)
const DELTA_STR = '2654435769'
const KEY_STRS = [
  '27303562373562475',
  '18014862372307051',
  '22799692160172081',
  '34058940340699235',
  '30962724186095721',
  '27303523720101991',
  '27303523720101998',
  '31244139033526382',
  '28992395054481524',
]
const DELTA = HAS_BIGINT ? B(DELTA_STR) : 0
const keyArr = HAS_BIGINT ? KEY_STRS.map(B) : []
const MIN_LENGTH = 32

const teaDecrypt = (data, key) => {
  const length = data.length
  const lengthBitint = B(length)
  if (length >= 1) {
    let j2 = data[0]
    let j3 = toLong((B('6') + (B('52') / lengthBitint)) * DELTA)
    while (true) {
      let j4 = j3
      if (j4 == B('0')) break
      let j5 = toLong(B('3') & toLong(j4 >> B('2')))
      let j6 = lengthBitint
      while (true) {
        j6--
        if (j6 > B('0')) {
          let j7 = data[j6 - B('1')]
          let i = j6
          j2 = toLong(data[i] - (toLong(toLong(j2 ^ j4) + toLong(j7 ^ key[toLong(toLong(B('3') & j6) ^ j5)])) ^ toLong(toLong(toLong(j7 >> B('5')) ^ toLong(j2 << B('2'))) + toLong(toLong(j2 >> B('3')) ^ toLong(j7 << B('4'))))))
          data[i] = j2
        } else break
      }
      let j8 = data[lengthBitint - B('1')]
      j2 = toLong(data[B('0')] - toLong(toLong(toLong(key[toLong(toLong(j6 & B('3')) ^ j5)] ^ j8) + toLong(j2 ^ j4)) ^ toLong(toLong(toLong(j8 >> B('5')) ^ toLong(j2 << B('2'))) + toLong(toLong(j2 >> B('3')) ^ toLong(j8 << B('4'))))))
      data[B('0')] = j2
      j3 = toLong(j4 - DELTA)
    }
  }
  return data
}

const longArrToString = (data) => {
  const arrayList = []
  for (const j of data) arrayList.push(longToBytes(j).toString('utf16le'))
  return arrayList.join('')
}

// https://stackoverflow.com/a/29132118
const longToBytes = (l) => {
  const result = Buffer.alloc(8)
  for (let i = 0; i < 8; i++) {
    result[i] = Number(l & B('255'))
    l >>= B('8')
  }
  return result
}

const toBigintArray = (data) => {
  const length = Math.floor(data.length / 16)
  const jArr = Array(length)
  for (let i = 0; i < length; i++) {
    jArr[i] = toLong(data.substring(i * 16, (i * 16) + 16))
  }
  return jArr
}

// https://github.com/lyswhut/lx-music-desktop/issues/445#issuecomment-1139338682
const MAX = HAS_BIGINT ? B('9223372036854775807') : 0
const MIN = HAS_BIGINT ? B('-9223372036854775808') : 0
const toLong = str => {
  const num = typeof str == 'string' ? B('0x' + str) : str
  if (num > MAX) return toLong(num - (B('1') << B('64')))
  else if (num < MIN) return toLong(num + (B('1') << B('64')))
  return num
}

export const decrypt = (data) => {
  if (!HAS_BIGINT) return data
  return (data == null || data.length < MIN_LENGTH)
    ? data
    : longArrToString(teaDecrypt(toBigintArray(data), keyArr))
}
