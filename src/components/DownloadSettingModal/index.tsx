import { useRef, useState, forwardRef, useImperativeHandle, useEffect } from 'react'
import Modal, { type DownloadSettingModalType as ModalType, type DownloadSettingModalProps as ModalProps, type SelectInfo } from './DownloadSettingModal'
export interface DownloadSettingModalRefType {
  show: (info: SelectInfo) => void
}
export default forwardRef<DownloadSettingModalRefType, ModalProps>(({ onConfirm }, ref) => {
  const modalRef = useRef<ModalType>(null)
  const pendingRef = useRef<SelectInfo | null>(null)
  const [visible, setVisible] = useState(false)
  useImperativeHandle(ref, () => ({
    show(info) {
      if (modalRef.current != null) {
        modalRef.current.show(info)
        return
      }
      pendingRef.current = info
      setVisible(true)
    },
  }))
  useEffect(() => {
    if (!visible) return
    const info = pendingRef.current
    if (info == null) return
    pendingRef.current = null
    modalRef.current?.show(info)
  }, [visible])
  return (
    visible
      ? <Modal ref={modalRef} onConfirm={onConfirm} />
      : null
  )
})
