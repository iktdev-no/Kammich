import { defineConfig } from 'vite'
import react, { reactCompilerPreset } from '@vitejs/plugin-react'
import babel from '@rolldown/plugin-babel'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    react(),
    babel({ presets: [reactCompilerPreset()] })
  ],

  server: {
    host: true, // gjør at du kan nå dev-serveren fra LAN
    port: 5173,

    proxy: {
      '/api': {
        target: 'http://192.168.2.22:8080',
        changeOrigin: true,
        secure: false,
      },
      '/sse': {
        target: 'http://192.168.2.22:8080',
        changeOrigin: true,
        secure: false,
      }
    }
  }
})
