import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.tsx'
import { SseProvider } from './sse/SseProvider.tsx'
import 'react-toastify/dist/ReactToastify.css';
import { ToastContainer } from 'react-toastify'
import { ColorModeProvider } from './context/ColorModeContext.tsx'

createRoot(document.getElementById('root')!).render(
    <StrictMode>
        <ColorModeProvider>
            <SseProvider>
                <App />
                <ToastContainer position="bottom-right" />
            </SseProvider>
        </ColorModeProvider>
    </StrictMode>,
)
