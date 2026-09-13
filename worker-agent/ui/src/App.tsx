import { Routes, Route, Navigate } from 'react-router-dom'
import Layout from './components/Layout'
import Home from './pages/Home'
import Setup from './pages/Setup'
import Node from './pages/Node'
import Tasks from './pages/Tasks'
import Connection from './pages/Connection'
import Settings from './pages/Settings'

function App() {
  return (
    <Routes>
      <Route path="/" element={<Layout />}>
        <Route index element={<Navigate to="/home" replace />} />
        <Route path="setup" element={<Setup />} />
        <Route path="home" element={<Home />} />
        <Route path="node" element={<Node />} />
        <Route path="tasks" element={<Tasks />} />
        <Route path="connection" element={<Connection />} />
        <Route path="settings" element={<Settings />} />
      </Route>
    </Routes>
  )
}

export default App
