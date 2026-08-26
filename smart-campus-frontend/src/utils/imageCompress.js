/**
 * 前端图片压缩：上传前把相机原图缩放/重编码，降低体积与流量。
 * 原则：任何一步失败都原样返回，绝不阻塞上传流程。
 */
const MAX_EDGE = 1920 // 最长边上限，超出按比例缩小
const SKIP_SIZE = 512 * 1024 // 512KB 以内且类型/尺寸达标的不处理
const KEEP_TYPES = ['image/jpeg', 'image/png', 'image/webp']

const decodeImage = async (file) => {
  if (typeof createImageBitmap === 'function') {
    try {
      return await createImageBitmap(file)
    } catch {
      /* 老浏览器/特殊格式落到 <img> 解码 */
    }
  }
  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(file)
    const img = new Image()
    img.onload = () => {
      URL.revokeObjectURL(url)
      resolve(img)
    }
    img.onerror = () => {
      URL.revokeObjectURL(url)
      reject(new Error('image decode failed'))
    }
    img.src = url
  })
}

/**
 * 压缩图片文件：
 * - 最长边超过 1920 按比例缩小；JPEG/WEBP 重编码（质量 0.85），PNG 保持 PNG（保住透明）
 * - 非后端支持类型（如相机特殊格式）重编码为 JPEG
 * - 压缩后反而更大且原类型合规时保留原文件
 * @param {File} file
 * @returns {Promise<File>} 处理后的文件（无法处理时返回原文件）
 */
export const compressImageFile = async (file) => {
  if (!file?.type?.startsWith('image/')) {
    return file
  }

  try {
    const bitmap = await decodeImage(file)
    const width = bitmap.width || bitmap.naturalWidth
    const height = bitmap.height || bitmap.naturalHeight
    if (!width || !height) {
      bitmap.close?.()
      return file
    }

    const isKeepType = KEEP_TYPES.includes(file.type)
    const scale = Math.min(1, MAX_EDGE / Math.max(width, height))
    if (scale >= 1 && isKeepType && file.size <= SKIP_SIZE) {
      bitmap.close?.()
      return file
    }

    const canvas = document.createElement('canvas')
    canvas.width = Math.round(width * scale)
    canvas.height = Math.round(height * scale)
    const ctx = canvas.getContext('2d')
    ctx.drawImage(bitmap, 0, 0, canvas.width, canvas.height)
    bitmap.close?.()

    const outputType = file.type === 'image/png' ? 'image/png' : 'image/jpeg'
    const blob = await new Promise((resolve) => canvas.toBlob(resolve, outputType, 0.85))
    if (!blob || (blob.size >= file.size && isKeepType)) {
      return file
    }

    const ext = outputType === 'image/png' ? 'png' : 'jpg'
    const baseName = (file.name || 'image').replace(/\.[^.]+$/, '')
    return new File([blob], `${baseName}.${ext}`, { type: outputType })
  } catch {
    return file
  }
}
