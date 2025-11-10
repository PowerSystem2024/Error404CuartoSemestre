import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App.tsx'
import './index.css'
import { useUserStore } from './stores/userStore'
import { ThemeProvider } from './contexts/ThemeContext'

// console.log('Principal: Inicializando aplicación React');

// Inicializar el store del usuario desde localStorage
useUserStore.getState().initializeFromStorage();

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <ThemeProvider defaultTheme="dark" storageKey="ecommerce-theme">
      <App />
    </ThemeProvider>
  </React.StrictMode>,
)
