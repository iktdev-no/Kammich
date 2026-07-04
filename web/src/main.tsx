import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { ThemeProvider } from '@mui/material'
import { theme } from './theme/theme'
import { SseProvider } from './context/SseContext'
import './index.css'
import App from './App.tsx'

createRoot(document.getElementById('root')!).render(
    <StrictMode>
        <ThemeProvider theme={theme}>
            <SseProvider>
                <App />
            </SseProvider>
        </ThemeProvider>
    </StrictMode>,
)
