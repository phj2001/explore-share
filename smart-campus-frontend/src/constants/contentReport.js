export const REPORT_TARGET_SHARE = 1
export const REPORT_TARGET_REPLY = 2

export const REPORT_REASON_SPAM = 1
export const REPORT_REASON_INAPPROPRIATE = 2
export const REPORT_REASON_FALSE_INFO = 3
export const REPORT_REASON_ABUSE = 4
export const REPORT_REASON_OTHER = 5

export const REPORT_STATUS_PENDING = 1
export const REPORT_STATUS_PROCESSED = 2
export const REPORT_STATUS_REJECTED = 3

export const REPORT_ACTION_NONE = 0
export const REPORT_ACTION_DELETE_TARGET = 1

export const REPORT_REASON_OPTIONS = [
  { value: REPORT_REASON_SPAM, label: '垃圾广告' },
  { value: REPORT_REASON_INAPPROPRIATE, label: '不当内容' },
  { value: REPORT_REASON_FALSE_INFO, label: '虚假信息' },
  { value: REPORT_REASON_ABUSE, label: '人身攻击' },
  { value: REPORT_REASON_OTHER, label: '其他' }
]

export const REPORT_STATUS_OPTIONS = [
  { value: REPORT_STATUS_PENDING, label: '待处理' },
  { value: REPORT_STATUS_PROCESSED, label: '已处理' },
  { value: REPORT_STATUS_REJECTED, label: '已驳回' }
]

export const REPORT_TARGET_OPTIONS = [
  { value: REPORT_TARGET_SHARE, label: '分享' },
  { value: REPORT_TARGET_REPLY, label: '回复' }
]

export const REPORT_ACTION_OPTIONS = [
  { value: REPORT_ACTION_NONE, label: '保留内容并标记已处理' },
  { value: REPORT_ACTION_DELETE_TARGET, label: '删除被举报内容' }
]

export const getReportReasonLabel = (value) => {
  return REPORT_REASON_OPTIONS.find((item) => item.value === value)?.label || '未知理由'
}

export const getReportStatusLabel = (value) => {
  return REPORT_STATUS_OPTIONS.find((item) => item.value === value)?.label || '未知状态'
}

export const getReportTargetLabel = (value) => {
  return REPORT_TARGET_OPTIONS.find((item) => item.value === value)?.label || '未知对象'
}

export const getReportActionLabel = (value) => {
  return REPORT_ACTION_OPTIONS.find((item) => item.value === value)?.label || '保留内容'
}
