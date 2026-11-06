import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { App } from './App';
import { SesionProvider } from './auth/sesion';
import { AvisosProvider } from './componentes/avisos';

afterEach(() => {
  cleanup();
  sessionStorage.clear();
  vi.unstubAllGlobals();
});

const abrir = (ruta: string) => render(
  <MemoryRouter initialEntries={[ruta]}>
    <AvisosProvider>
      <SesionProvider>
        <App />
      </SesionProvider>
    </AvisosProvider>
  </MemoryRouter>,
);

describe('Navegación sin sesión', () => {
  it('muestra la página de inicio con su información', () => {
    abrir('/');

    expect(screen.getByRole('heading', { level: 1 }).textContent).toContain('conectada de la cita al pago');
    expect(screen.getByRole('heading', { name: 'Un módulo para cada área de la clínica' })).toBeTruthy();
    expect(screen.queryByLabelText('Contraseña')).toBeNull();
  });

  it('va del inicio al acceso y regresa', () => {
    abrir('/');

    fireEvent.click(screen.getAllByRole('link', { name: 'Iniciar sesión' })[0]);
    expect(screen.getByLabelText('Contraseña')).toBeTruthy();

    fireEvent.click(screen.getByRole('button', { name: 'Regresar' }));
    expect(screen.getByRole('heading', { level: 1 }).textContent).toContain('conectada de la cita al pago');
    expect(screen.queryByLabelText('Contraseña')).toBeNull();
  });

  it('lleva una ruta protegida a la página de inicio', () => {
    abrir('/usuarios');

    expect(screen.getByRole('heading', { level: 1 }).textContent).toContain('conectada de la cita al pago');
  });
});

describe('Navegación con sesión', () => {
  const iniciarSesion = () => {
    const parte = (o: object) => btoa(JSON.stringify(o));
    sessionStorage.setItem('vetcare.token', `${parte({ alg: 'RS256' })}.${parte({ sub: 'u1', correo: 'caja@vetcare.test', rol: 'CAJERO', exp: Date.now() / 1000 + 600 })}.firma`);
    vi.stubGlobal('fetch', vi.fn(() => new Promise(() => {})));
  };

  it('abre el panel en vez del inicio y del acceso', () => {
    iniciarSesion();
    abrir('/login');

    expect(screen.getByRole('heading', { level: 1, name: 'Panel' })).toBeTruthy();
    expect(screen.queryByLabelText('Contraseña')).toBeNull();
  });

  it('vuelve al inicio al cerrar sesión', () => {
    iniciarSesion();
    abrir('/');

    fireEvent.click(screen.getByRole('button', { name: 'Cerrar sesión' }));

    expect(screen.getByRole('heading', { level: 1 }).textContent).toContain('conectada de la cita al pago');
    expect(sessionStorage.getItem('vetcare.token')).toBeNull();
  });
});
