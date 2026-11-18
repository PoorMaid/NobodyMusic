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
      console.log('[NB-DL] outer show, visible=' + visible + ' refNull=' + (modalRef.current == null))
      if (visible) modalRef.current?.show(info)
      else {
        setVisible(true)
        requestAnimationFrame(() => {
          console.log('[NB-DL] outer rAF refNull=' + (modalRef.current == null))
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
