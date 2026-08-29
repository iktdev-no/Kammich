import { defineConfig } from "vite";
import react, { reactCompilerPreset } from "@vitejs/plugin-react";
import babel from "@rolldown/plugin-babel";
import { VitePWA } from "vite-plugin-pwa";

export default defineConfig({
  plugins: [
    react(),

    babel({
      presets: [reactCompilerPreset()]
    }),

    VitePWA({
      registerType: "autoUpdate",

      manifest: {
        name: "Kammich",
        short_name: "Kammich",
        description: "Kammich",

        theme_color: "#000000",
        background_color: "#000000",

        display: "standalone",

        icons: [
          {
            src: "/icon-192.png",
            sizes: "192x192",
            type: "image/png"
          },
          {
            src: "/icon-512.png",
            sizes: "512x512",
            type: "image/png"
          }
        ]
      }
    })
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
