import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it } from 'vitest';
import { Tabla } from './Tabla';

afterEach(cleanup);

const filas = Array.from({ length: 12 }, (_, i) => ({ id: String(i), nombre: i === 0 ? 'María Gómez' : `Persona ${i}` }));
const columnas = [{ titulo: 'Nombre', valor: (f: { nombre: string }) => f.nombre }];

describe('Tabla', () => {
  it('pagina los registros', () => {
    render(<Tabla filas={filas} columnas={columnas} clave={(f) => f.id} porPagina={10} />);

    expect(screen.queryByText('Persona 11')).toBeNull();
    fireEvent.click(screen.getByRole('button', { name: 'Siguiente' }));
    expect(screen.getByText('Persona 11')).toBeTruthy();
  });

  it('busca sin distinguir tildes ni mayúsculas', () => {
    render(<Tabla filas={filas} columnas={columnas} clave={(f) => f.id} buscador="Buscar" />);

    fireEvent.change(screen.getByRole('searchbox'), { target: { value: 'gomez' } });
    expect(screen.getByText('María Gómez')).toBeTruthy();
    expect(screen.queryByText('Persona 1')).toBeNull();
  });

  it('muestra el mensaje de error y el de tabla vacía', () => {
    render(<Tabla filas={[]} columnas={columnas} clave={() => 'x'} vacio="Sin datos" />);
    expect(screen.getByText('Sin datos')).toBeTruthy();
    cleanup();

    render(<Tabla filas={[]} columnas={columnas} clave={() => 'x'} error="Falló" />);
    expect(screen.getByRole('alert').textContent).toBe('Falló');
  });
});
