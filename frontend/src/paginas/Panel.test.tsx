import { cleanup, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { SesionProvider } from '../auth/sesion';
import { AvisosProvider } from '../componentes/avisos';
import { Panel } from './Panel';

afterEach(() => {
  cleanup();
  sessionStorage.clear();
  vi.unstubAllGlobals();
});

const parte = (o: object) => btoa(JSON.stringify(o));

function abrirComo(rol: string, respuestas: (url: string) => unknown) {
  sessionStorage.setItem('vetcare.token', `${parte({ alg: 'RS256' })}.${parte({ sub: 'u1', correo: 'u@vetcare.test', rol, exp: Date.now() / 1000 + 600 })}.firma`);
  vi.stubGlobal('fetch', vi.fn(async (url: string) => {
    const datos = respuestas(url);
    if (datos === undefined) {
      throw new TypeError('sin conexión');
    }
    return new Response(JSON.stringify(datos), { status: 200 });
  }));
  render(
    <MemoryRouter>
      <AvisosProvider>
        <SesionProvider>
          <Panel />
        </SesionProvider>
      </AvisosProvider>
    </MemoryRouter>,
  );
}

describe('Panel', () => {
  it('HU-071: si la agenda no responde, muestra lo demás con un aviso', async () => {
    abrirComo('PROPIETARIO', (url) => {
      if (url.includes(':8082')) {
        return undefined;
      }
      return url.includes('mascotas')
        ? [{ id: 'm1', nombre: 'Max' }]
        : [{ mascotaId: 'm1', mascotaNombre: 'Max', vacuna: 'Rabia', proximaDosis: '2027-01-10', estado: 'PENDIENTE' }];
    });

    expect(await screen.findByText('Las citas no están disponibles en este momento.')).toBeTruthy();
    expect(await screen.findByText('Rabia')).toBeTruthy();
    expect(screen.getByText('Mis mascotas').parentElement?.textContent).toContain('1');
    expect(screen.getByText('Próximas citas', { selector: '.indicador-titulo' }).parentElement?.textContent).toContain('No disponible');
  });

  it('HU-072: si farmacia no responde, muestra los demás indicadores', async () => {
    abrirComo('ADMINISTRADOR', (url) => {
      if (url.includes(':8084')) {
        return undefined;
      }
      if (url.includes('/ocupacion')) {
        return [{ veterinarioId: 'v1', nombre: 'Dra. Pérez', porcentaje: 75 }];
      }
      if (url.includes('/ingresos')) {
        return { total: 100000, porServicio: { CONSULTA: 100000 }, porMedioPago: {} };
      }
      return { atendidas: 3, canceladas: 1 };
    });

    await waitFor(() => expect(screen.getByText('Ventas de farmacia').parentElement?.textContent).toContain('No disponible'));
    expect(await screen.findByText('Dra. Pérez')).toBeTruthy();
    expect(screen.getByText('Citas atendidas').parentElement?.textContent).toContain('3');
    expect(screen.getByText('CONSULTA')).toBeTruthy();
  });

  it('HU-071: muestra el panel completo del propietario', async () => {
    abrirComo('PROPIETARIO', (url) => {
      if (url.includes('/api/portal/citas')) {
        return [{ id: 'c1', fecha: '2027-01-10', hora: '09:00:00', mascotaNombre: 'Max' }];
      }
      return url.includes('mascotas') ? [{ id: 'm1', nombre: 'Max' }] : [];
    });

    expect(await screen.findByText('2027', { exact: false })).toBeTruthy();
    expect(screen.getByText('No tienes vacunas pendientes.')).toBeTruthy();
  });
});
