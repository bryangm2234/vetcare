import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { api, configurarExpiracion, configurarToken } from '../api/cliente';
import type { Rol } from './roles';

export interface Sesion {
  token: string;
  usuarioId: string;
  correo: string;
  rol: Rol;
  propietarioId?: string;
  proveedorId?: string;
  expira: number;
}

const CLAVE = 'vetcare.token';

/** Lee los datos del JWT sin validarlo: la firma la verifica cada microservicio. */
export function leerToken(token: string): Sesion | null {
  try {
    const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    const bytes = Uint8Array.from(atob(base64), (c) => c.charCodeAt(0));
    const datos = JSON.parse(new TextDecoder().decode(bytes));
    return {
      token,
      usuarioId: datos.sub,
      correo: datos.correo,
      rol: datos.rol,
      propietarioId: datos.propietarioId,
      proveedorId: datos.proveedorId,
      expira: datos.exp * 1000,
    };
  } catch {
    return null;
  }
}

function sesionGuardada(): Sesion | null {
  const token = sessionStorage.getItem(CLAVE);
  const sesion = token ? leerToken(token) : null;
  if (sesion && sesion.expira > Date.now()) {
    configurarToken(sesion.token);
    return sesion;
  }
  sessionStorage.removeItem(CLAVE);
  return null;
}

interface ContextoSesion {
  sesion: Sesion | null;
  aviso: string | null;
  iniciar: (correo: string, contrasena: string) => Promise<void>;
  cerrar: (aviso?: string) => void;
}

const Contexto = createContext<ContextoSesion | null>(null);

export function SesionProvider({ children }: { children: ReactNode }) {
  const [sesion, setSesion] = useState<Sesion | null>(sesionGuardada);
  const [aviso, setAviso] = useState<string | null>(null);

  const cerrar = useCallback((mensaje?: string) => {
    sessionStorage.removeItem(CLAVE);
    configurarToken(null);
    setSesion(null);
    setAviso(mensaje ?? null);
  }, []);

  const iniciar = useCallback(async (correo: string, contrasena: string) => {
    const respuesta = await api<{ token: string }>('pacientes', '/api/auth/login', 'POST', { correo, contrasena });
    const nueva = leerToken(respuesta.token);
    if (!nueva) {
      throw new Error('No se pudo leer la sesión');
    }
    sessionStorage.setItem(CLAVE, respuesta.token);
    configurarToken(respuesta.token);
    setAviso(null);
    setSesion(nueva);
  }, []);

  useEffect(() => {
    configurarExpiracion(() => cerrar('Tu sesión expiró. Vuelve a iniciar sesión.'));
  }, [cerrar]);

  useEffect(() => {
    if (!sesion) {
      return;
    }
    const temporizador = setTimeout(() => cerrar('Tu sesión expiró. Vuelve a iniciar sesión.'), sesion.expira - Date.now());
    return () => clearTimeout(temporizador);
  }, [sesion, cerrar]);

  const valor = useMemo(() => ({ sesion, aviso, iniciar, cerrar }), [sesion, aviso, iniciar, cerrar]);
  return <Contexto.Provider value={valor}>{children}</Contexto.Provider>;
}

export function useSesion(): ContextoSesion {
  const contexto = useContext(Contexto);
  if (!contexto) {
    throw new Error('useSesion debe usarse dentro de SesionProvider');
  }
  return contexto;
}

/** Sesión de una página protegida, donde siempre existe. */
export function useUsuario(): Sesion {
  return useSesion().sesion!;
}
