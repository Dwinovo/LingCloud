import CryptoJS from 'crypto-js'

/**
 * 计算文件的明文哈希
 */
export async function calculateFileHash(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = async (e) => {
      try {
        const wordArray = CryptoJS.lib.WordArray.create(e.target?.result as ArrayBuffer)
        const hash = CryptoJS.SHA256(wordArray).toString()
        resolve(hash)
      } catch (error) {
        reject(error)
      }
    }
    reader.onerror = reject
    reader.readAsArrayBuffer(file)
  })
}

/**
 * 收敛加密 - 使用明文哈希作为密钥进行加密
 */
export async function convergentEncrypt(file: File, key: string): Promise<Blob> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = (e) => {
      try {
        const wordArray = CryptoJS.lib.WordArray.create(e.target?.result as ArrayBuffer)
        const encrypted = CryptoJS.AES.encrypt(wordArray, key).toString()
        const encryptedBlob = new Blob([encrypted], { type: 'application/octet-stream' })
        resolve(encryptedBlob)
      } catch (error) {
        reject(error)
      }
    }
    reader.onerror = reject
    reader.readAsArrayBuffer(file)
  })
}

/**
 * 收敛解密
 */
export async function convergentDecrypt(encryptedData: string | Blob, key: string): Promise<Blob> {
  try {
    let encryptedText: string

    if (encryptedData instanceof Blob) {
      encryptedText = await blobToString(encryptedData)
    } else {
      encryptedText = encryptedData
    }

    const decrypted = CryptoJS.AES.decrypt(encryptedText, key)
    const decryptedWordArray = decrypted.toString(CryptoJS.enc.Utf8)

    // 如果解密结果是空字符串，说明可能是二进制数据
    if (decryptedWordArray === '') {
      // 尝试作为Latin1处理
      const decryptedBytes = decrypted.toString(CryptoJS.enc.Latin1)
      return new Blob([decryptedBytes], { type: 'application/octet-stream' })
    }

    return new Blob([decryptedWordArray], { type: 'application/octet-stream' })
  } catch (error) {
    throw new Error('解密失败：' + error)
  }
}

/**
 * Blob转字符串
 */
function blobToString(blob: Blob): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result as string)
    reader.onerror = reject
    reader.readAsText(blob)
  })
}

/**
 * 下载文件到本地
 */
export function downloadFile(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}