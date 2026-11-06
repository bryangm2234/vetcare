import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { ApiError } from '../api/cliente';
import { AvisosProvider } from './avisos';
import { Dialogo } from './Formulario';

afterEach(cleanup);

const abrir = (props: Partial<Parameters<typeof Dialogo>[0]>) => {
  const cerrar = vi.fn();
  render(
    <AvisosProvider>
      <Dialogo titulo="Prueba" campos={[{ nombre: 'correo', etiqueta: 'Correo' }]} enviar={vi.fn()} cerrar={cerrar} {...props} />
    </AvisosProvider>,
  );
  return cerrar;
};

describe('Dialogo', () => {
  it('envía los valores y se cierra', async () => {
    const enviar = vi.fn().mockResolvedValue({ id: '1' });
    const despues = vi.fn();
    const cerrar = abrir({ enviar, despues });

    fireEvent.change(screen.getByLabelText('Correo'), { target: { value: 'a@b.co' } });
    fireEvent.click(screen.getByRole('button', { name: 'Guardar' }));

    await waitFor(() => expect(cerrar).toHaveBeenCalled());
    expect(enviar).toHaveBeenCalledWith({ correo: 'a@b.co' });
    expect(despues).toHaveBeenCalledWith({ id: '1' });
  });

  it('muestra el error de la API y conserva lo escrito', async () => {
    const cerrar = abrir({ enviar: vi.fn().mockRejectedValue(new ApiError(400, 'Datos no válidos', { correo: 'Correo inválido' })) });

    fireEvent.change(screen.getByLabelText('Correo'), { target: { value: 'malo' } });
    fireEvent.click(screen.getByRole('button', { name: 'Guardar' }));

    expect(await screen.findByText('Datos no válidos')).toBeTruthy();
    expect(screen.getByText('Correo inválido')).toBeTruthy();
    expect((screen.getByLabelText('Correo') as HTMLInputElement).value).toBe('malo');
    expect(cerrar).not.toHaveBeenCalled();
  });

  it('oculta los campos condicionales', () => {
    abrir({ campos: [{ nombre: 'rol', etiqueta: 'Rol' }, { nombre: 'extra', etiqueta: 'Extra', visible: (v) => v.rol === 'X' }] });

    expect(screen.queryByLabelText('Extra')).toBeNull();
    fireEvent.change(screen.getByLabelText('Rol'), { target: { value: 'X' } });
    expect(screen.getByLabelText('Extra')).toBeTruthy();
  });
});
