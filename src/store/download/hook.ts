import { useState, useEffect } from 'react'
import state, { type InitState } from './state'

export const useDownloadList = () => {
  const [tasks, setTasks] = useState(state.tasks)
  const [activeList, setActiveList] = useState(state.activeList)
  useEffect(() => {
    const handler = (newTasks: InitState['tasks']) => {
      setTasks([...newTasks])
      setActiveList(state.activeList)
    }
    global.state_event.on('downloadTasksUpdated', handler)
    setTasks([...state.tasks])
    setActiveList(state.activeList)
    return () => {
      global.state_event.off('downloadTasksUpdated', handler)
    }
  }, [])
  return { tasks, activeList }
}
