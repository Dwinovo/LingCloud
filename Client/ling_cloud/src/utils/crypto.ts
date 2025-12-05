/**
 * 加密相关的类型定义
 */
export interface EncryptedData {
  H: string          // 文件哈希 SHA-256
  IV: Uint8Array     // AES-GCM 初始化向量 (96 bits)
  TAG: Uint8Array    // AES-GCM 认证标签
  C: Uint8Array      // 密文
  L: number          // 文件长度
}

export interface UploadMessage {
  H: string          // 文件哈希
  IV: string         // IV的base64编码
  TAG: string        // TAG的base64编码
  C: Blob            // 密文Blob
  L: number          // 文件长度
}

/**
 * 计算文件的明文哈希 (H = HASH(M))
 */
export async function calculateFileHash(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = async (e) => {
      try {
        const arrayBuffer = e.target?.result as ArrayBuffer
        const hashBuffer = await crypto.subtle.digest('SHA-256', arrayBuffer)
        const hashArray = Array.from(new Uint8Array(hashBuffer))
        const hashHex = hashArray.map(b => b.toString(16).padStart(2, '0')).join('')
        resolve(hashHex)
      } catch (error) {
        reject(error)
      }
    }
    reader.onerror = reject
    reader.readAsArrayBuffer(file)
  })
}

/**
 * 从文件哈希派生数据加密密钥 (DEK = HKDF(H, salt, info))
 */
export async function deriveDEK(fileHash: string): Promise<CryptoKey> {
  // 将十六进制哈希转换为字节数组
  const hashBytes = new Uint8Array(fileHash.match(/.{1,2}/g)!.map(byte => parseInt(byte, 16)))

  // HKDF参数
  const salt = new TextEncoder().encode('lab-key-derivation')
  const info = new TextEncoder().encode('file-encryption-v1')

  // 导入HKDF密钥
  const baseKey = await crypto.subtle.importKey(
    'raw',
    hashBytes,
    'HKDF',
    false,
    ['deriveKey']
  )

  // 派生AES-GCM密钥
  const dek = await crypto.subtle.deriveKey(
    {
      name: 'HKDF',
      hash: 'SHA-256',
      salt: salt,
      info: info
    },
    baseKey,
    {
      name: 'AES-GCM',
      length: 256
    },
    false,
    ['encrypt', 'decrypt']
  )

  return dek
}

/**
 * 使用AES-GCM加密文件数据 (C, TAG = AES-GCM-Encrypt(M, DEK, IV))
 */
export async function aesGcmEncrypt(
  fileData: ArrayBuffer,
  dek: CryptoKey,
  fixedIV?: Uint8Array
): Promise<{ IV: Uint8Array; TAG: Uint8Array; C: Uint8Array }> {
  // 生成或使用提供的96位IV
  const IV = fixedIV ? new Uint8Array(fixedIV) : crypto.getRandomValues(new Uint8Array(12))

  // 使用AES-GCM加密
  const encryptedResult = await crypto.subtle.encrypt(
    {
      name: 'AES-GCM',
      iv: IV
    },
    dek,
    fileData
  )

  const encryptedArray = new Uint8Array(encryptedResult)

  // AES-GCM加密结果的结构：密文 + 认证标签
  // 假设标签长度为16字节（128位）
  const tagLength = 16
  const C = encryptedArray.slice(0, encryptedArray.length - tagLength)
  const TAG = encryptedArray.slice(encryptedArray.length - tagLength)

  return { IV, TAG, C }
}

/**
 * 完整的收敛加密流程
 */
export async function convergentEncrypt(file: File, precomputedHash?: string, fixedIvBase64?: string): Promise<EncryptedData> {
  // 1. 计算文件哈希 H = HASH(M)
  const H = precomputedHash ?? await calculateFileHash(file)

  // 2. 派生密钥 DEK = HKDF(H, salt, info)
  const DEK = await deriveDEK(H)

  // 3. 读取文件数据
  const fileBuffer = await file.arrayBuffer()

  // 4. AES-GCM加密 C, TAG = AES-GCM-Encrypt(M, DEK, IV)
  const fixedIv = fixedIvBase64 ? base64ToUint8Array(fixedIvBase64) : undefined
  const { IV, TAG, C } = await aesGcmEncrypt(fileBuffer, DEK, fixedIv)

  return {
    H,
    IV,
    TAG,
    C,
    L: file.size
  }
}

/**
 * 构建上传消息 (H|IV|TAG|C)
 */
export function buildUploadMessage(encryptedData: EncryptedData): UploadMessage {
  // 创建新的ArrayBuffer来避免ArrayBufferLike类型问题
  const ivBuffer = new ArrayBuffer(encryptedData.IV.length)
  const ivView = new Uint8Array(ivBuffer)
  ivView.set(encryptedData.IV)

  const tagBuffer = new ArrayBuffer(encryptedData.TAG.length)
  const tagView = new Uint8Array(tagBuffer)
  tagView.set(encryptedData.TAG)

  const cBuffer = new ArrayBuffer(encryptedData.C.length)
  const cView = new Uint8Array(cBuffer)
  cView.set(encryptedData.C)

  return {
    H: encryptedData.H,
    IV: arrayBufferToBase64(ivBuffer),
    TAG: arrayBufferToBase64(tagBuffer),
    C: new Blob([cBuffer], { type: 'application/octet-stream' }),
    L: encryptedData.L
  }
}

/**
 * AES-GCM解密
 */
export async function aesGcmDecrypt(
  C: Uint8Array,
  TAG: Uint8Array,
  IV: Uint8Array,
  dek: CryptoKey
): Promise<ArrayBuffer> {
  // 合并密文和认证标签
  const encryptedData = new Uint8Array(C.length + TAG.length)
  encryptedData.set(C)
  encryptedData.set(TAG, C.length)

  // 创建新的ArrayBuffer来确保类型安全
  const ivBuffer = new Uint8Array(IV)
  const encryptedBuffer = new Uint8Array(encryptedData)

  // 使用AES-GCM解密
  const decryptedResult = await crypto.subtle.decrypt(
    {
      name: 'AES-GCM',
      iv: ivBuffer
    },
    dek,
    encryptedBuffer
  )

  return decryptedResult
}

/**
 * 收敛解密 - 供下载使用
 */
export async function convergentDecrypt(
  H: string,
  IV: string,
  TAG: string,
  encryptedBlob: Blob
): Promise<Blob> {
  try {
    // 派生密钥
    const DEK = await deriveDEK(H)

    // 转换数据格式
    const IV_bytes = base64ToUint8Array(IV)
    const TAG_bytes = base64ToUint8Array(TAG)
    const C_bytes = new Uint8Array(await encryptedBlob.arrayBuffer())

    // 解密
    const decryptedBuffer = await aesGcmDecrypt(C_bytes, TAG_bytes, IV_bytes, DEK)

    return new Blob([decryptedBuffer], { type: 'application/octet-stream' })
  } catch (error) {
    throw new Error('解密失败：' + error)
  }
}

/**
 * 工具函数：ArrayBuffer转Base64
 */
function arrayBufferToBase64(buffer: ArrayBuffer): string {
  const bytes = new Uint8Array(buffer)
  let binary = ''
  for (let i = 0; i < bytes.length; i++) {
    const byte = bytes[i]
    if (byte !== undefined) {
      binary += String.fromCharCode(byte)
    }
  }
  return btoa(binary)
}

/**
 * 工具函数：Base64转ArrayBuffer
 */
function base64ToUint8Array(base64: string): Uint8Array {
  const binary = atob(base64)
  const bytes = new Uint8Array(binary.length)
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i)
  }
  return bytes
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

/**
 * 计算PoW片段的MAC
 */
export async function calculatePoWChallenge(
  file: File,
  offset: number,
  size: number
): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = async (e) => {
      try {
        const arrayBuffer = e.target?.result as ArrayBuffer
        const chunk = new Uint8Array(arrayBuffer, offset, size)
        const hashBuffer = await crypto.subtle.digest('SHA-256', chunk)
        const hashArray = Array.from(new Uint8Array(hashBuffer))
        const hashHex = hashArray.map(b => b.toString(16).padStart(2, '0')).join('')
        resolve(hashHex)
      } catch (error) {
        reject(error)
      }
    }
    reader.onerror = reject
    // 读取指定范围的文件片段
    const blob = file.slice(offset, offset + size)
    reader.readAsArrayBuffer(blob)
  })
}
