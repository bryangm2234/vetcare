import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react';
import { Icono } from './Icono';

interface Aviso {
  id: number;
  tipo: 'exito' | 'error';
  texto: string;
}

interface ContextoAvisos {
  exito: (texto: string) => void;
  error: (causa: unknown) => void;
}

const Contexto = createContext<ContextoAvisos | null>(null);
let siguiente = 1;

export function AvisosProvider({ children }: { children: ReactNode }) {
  const [avisos, setAvisos] = useState<Aviso[]>([]);

  const quitar = useCallback((id: number) => setAvisos((lista) => lista.filter((a) => a.id !== id)), []);
  const agregar = useCallback((tipo: Aviso['tipo'], texto: string) => {
    const id = siguiente++;
    setAvisos((lista) => [...lista, { id, tipo, texto }]);
    setTimeout(() => quitar(id), tipo === 'exito' ? 4000 : 8000);
  }, [quitar]);

  const valor = useMemo<ContextoAvisos>(() => ({
    exito: (texto) => agregar('exito', texto),
    error: (causa) => agregar('error', causa instanceof Error ? causa.message : 'Ocurrió un error inesperado'),
  }), [agregar]);

  return (
    <Contexto.Provider value={valor}>
      {children}
      <div className="avisos" role="status" aria-live="polite">
        {avisos.map((aviso) => (
          <div key={aviso.id} className={`aviso aviso-${aviso.tipo}`}>
            <Icono nombre={aviso.tipo === 'exito' ? 'check' : 'alerta'} />
            <span>{aviso.texto}</span>
            <button type="button" className="aviso-cerrar" aria-label="Cerrar aviso" onClick={() => quitar(aviso.id)}>×</button>
          </div>
        ))}
      </div>
    </Contexto.Provider>
  );
}

export function useAvisos(): ContextoAvisos {
  const contexto = useContext(Contexto);
  if (!contexto) {
    throw new Error('useAvisos debe usarse dentro de AvisosProvider');
  }
  return contexto;
}
