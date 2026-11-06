import { useCallback, useEffect, useRef, useState, type DependencyList } from 'react';
import { ApiError } from '../api/cliente';

export interface Datos<T> {
  datos: T | undefined;
  cargando: boolean;
  error: string | null;
  recargar: () => void;
}

/** Carga datos y descarta las respuestas de peticiones antiguas. */
export function useDatos<T>(cargar: () => Promise<T>, dependencias: DependencyList = []): Datos<T> {
  const [datos, setDatos] = useState<T>();
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [version, setVersion] = useState(0);
  const ultima = useRef(0);

  useEffect(() => {
    const peticion = ++ultima.current;
    setCargando(true);
    cargar()
      .then((resultado) => {
        if (peticion === ultima.current) {
          setDatos(resultado);
          setError(null);
        }
      })
      .catch((causa: unknown) => {
        if (peticion === ultima.current) {
          setError(causa instanceof ApiError || causa instanceof Error ? causa.message : 'Ocurrió un error inesperado');
        }
      })
      .finally(() => {
        if (peticion === ultima.current) {
          setCargando(false);
        }
      });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...dependencias, version]);

  const recargar = useCallback(() => setVersion((v) => v + 1), []);
  return { datos, cargando, error, recargar };
}
