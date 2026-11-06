import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { SesionProvider } from '../../auth/sesion';
import { AvisosProvider } from '../../componentes/avisos';
import { hoy } from '../../util';
import { Citas } from './Citas';

afterEach(() => {
  cleanup();
  sessionStorage.clear();
  vi.unstubAllGlobals();
});

const parte = (o: object) => btoa(JSON.stringify(o));

describe('Citas', () => {
  it('HU-025: al reprogramar ofrece las franjas libres del veterinario de la cita', async () => {
    sessionStorage.setItem('vetcare.token', `${parte({ alg: 'RS256' })}.${parte({ sub: 'u1', correo: 'r@vetcare.test', rol: 'RECEPCIONISTA', exp: Date.now() / 1000 + 600 })}.firma`);
    const fetchFalso = vi.fn(async (url: string) => {
      let datos: unknown = [];
      if (url.includes('/api/citas?')) {
        datos = [{ id: 'c1', recursoId: 'v1', mascotaNombre: 'Max', fecha: hoy(), hora: '09:00:00', motivo: 'Control', estado: 'PROGRAMADA' }];
      }
      if (url.includes('/api/disponibilidad')) {
        datos = [{ recursoId: 'v1', franjasLibres: ['10:30:00'] }];
      }
      return new Response(JSON.stringify(datos), { status: 200 });
    });
    vi.stubGlobal('fetch', fetchFalso);
    render(<MemoryRouter><AvisosProvider><SesionProvider><Citas /></SesionProvider></AvisosProvider></MemoryRouter>);

    fireEvent.click(await screen.findByRole('button', { name: 'Reprogramar' }));

    expect(await screen.findByRole('option', { name: '10:30' })).toBeTruthy();
    expect(fetchFalso.mock.calls.some(([url]) => url.includes('veterinarioId=v1'))).toBe(true);
  });
});
