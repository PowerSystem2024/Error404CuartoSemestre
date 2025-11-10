import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // Configuración simple para desarrollo local
    https: false,
    allowedHosts: [
      'localhost',
      '127.0.0.1',
      'localhost:5173',
      'localhost:8443',
      'localhost:8081'
    ],
    // Proxy para desarrollo local (comenta si usas API externa)
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        secure: false,
      }
    }
  },
})
