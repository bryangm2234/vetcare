import { describe, expect, it } from 'vitest';
import { leerToken } from './sesion';
import { menuDe } from './roles';

const base64 = (objeto: object) => btoa(String.fromCharCode(...new TextEncoder().encode(JSON.stringify(objeto)))).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');

describe('leerToken', () => {
  it('lee los datos del usuario, incluidos caracteres con tilde', () => {
    const token = `${base64({ alg: 'RS256' })}.${base64({ sub: 'u1', correo: 'maría@vetcare.test', rol: 'PROPIETARIO', propietarioId: 'p1', exp: 2000 })}.firma`;

    expect(leerToken(token)).toEqual({ token, usuarioId: 'u1', correo: 'maría@vetcare.test', rol: 'PROPIETARIO', propietarioId: 'p1', proveedorId: undefined, expira: 2000000 });
  });

  it('devuelve null con un token inválido', () => {
    expect(leerToken('no-es-un-token')).toBeNull();
  });
});

describe('menuDe', () => {
  it('muestra a cada rol solo sus opciones', () => {
    const rutas = (rol: Parameters<typeof menuDe>[0]) => menuDe(rol).map((o) => o.ruta);

    expect(rutas('CAJERO')).toEqual(['/', '/facturas', '/caja']);
    expect(rutas('PROPIETARIO')).not.toContain('/usuarios');
    expect(rutas('ADMINISTRADOR')).toContain('/auditoria');
    expect(rutas('LABORATORIO')).toEqual(['/', '/laboratorio']);
  });
});
