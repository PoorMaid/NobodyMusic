import { useRef, useState, forwardRef, useImperativeHandle } from 'react'
import Modal, { type DownloadSettingModalType as ModalType, type DownloadSettingModalProps as ModalProps, type SelectInfo } from './DownloadSettingModal'

export interface DownloadSettingModalRefType {
  show: (info: SelectInfo) => void
}

export default forwardRef<DownloadSettingModalRefType, ModalProps>(({ onConfirm }, ref) => {
  const modalRef = useRef<ModalType>(null)
  const [visible, setVisible] = useState(false)
  useImperativeHandle(ref, () => ({
    show(info) {
      if (visible) modalRef.current?.show(info)
      else {
        setVisible(true)
        requestAnimationFrame(() => {
          modalRef.current?.show(info)
        })
      }
    },
  }))
  return (
    visible
      ? <Modal ref={modalRef} onConfirm={onConfirm} />
      : null
  )
})
