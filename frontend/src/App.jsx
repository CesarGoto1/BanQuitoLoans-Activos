import { useState } from 'react'
import BancaWebLayout from './online-banking/components/BancaWebLayout'
import MyRequestsPage from './online-banking/pages/MyRequestsPage'
import MyOperationsPage from './online-banking/pages/MyOperationsPage'

export default function App() {
  const [pagina, setPagina] = useState('solicitudes')
  return (
    <BancaWebLayout pagina={pagina} setPage={setPagina}>
      {pagina === 'solicitudes' ? <MyRequestsPage /> : <MyOperationsPage />}
    </BancaWebLayout>
  )
}






