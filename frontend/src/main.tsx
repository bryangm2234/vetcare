import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { App } from './App';
import { SesionProvider } from './auth/sesion';
import { AvisosProvider } from './componentes/avisos';
import './estilos.css';

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <BrowserRouter>
      <AvisosProvider>
        <SesionProvider>
          <App />
        </SesionProvider>
      </AvisosProvider>
    </BrowserRouter>
  </StrictMode>,
);
