import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { ThemeProvider } from '@mui/material'
import { theme } from './theme/theme'
import './index.css'
import App from './App.tsx'
import { SseProvider } from './sse/SseProvider.tsx'
import 'react-toastify/dist/ReactToastify.css';
import { ToastContainer } from 'react-toastify'

createRoot(document.getElementById('root')!).render(
    <StrictMode>
        <ThemeProvider theme={theme}>
            <SseProvider>
                <App />
                <ToastContainer position="bottom-right" />
            </SseProvider>
        </ThemeProvider>
    </StrictMode>,
)
