import { useState } from 'react';
import { api } from '../api/cliente';
import { Boton, Estado } from './ui';

export interface MascotaElegida {
  id: string;
  nombre: string;
  especie: string;
  raza: string;
  estado: string;
  propietarioId: string;
  propietario: string;
}

/** Busca un propietario por nombre o documento y elige una de sus mascotas. */
export function ElegirMascota({ titulo, elegir, cerrar }: { titulo: string; elegir: (m: MascotaElegida) => void; cerrar: () => void }) {
  const [texto, setTexto] = useState('');
  const [resultados, setResultados] = useState<MascotaElegida[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [buscando, setBuscando] = useState(false);

  const buscar = async (e: React.FormEvent) => {
    e.preventDefault();
    setBuscando(true);
    setError(null);
    try {
      const parametro = encodeURIComponent(texto.trim());
      const [porNombre, porDocumento] = await Promise.all([
        api('pacientes', `/api/propietarios?nombre=${parametro}&size=20`),
        api('pacientes', `/api/propietarios?documento=${parametro}&size=20`),
      ]);
      const propietarios = new Map<string, any>([...porNombre.contenido, ...porDocumento.contenido].map((p: any) => [p.id, p]));
      const listas = await Promise.all([...propietarios.values()].map(async (p: any) => {
        const mascotas: any[] = await api('pacientes', `/api/propietarios/${p.id}/mascotas`);
        return mascotas.map((m) => ({ ...m, propietario: p.nombre }));
      }));
      setResultados(listas.flat());
    } catch (causa) {
      setError(causa instanceof Error ? causa.message : 'No se pudo buscar');
    } finally {
      setBuscando(false);
    }
  };

  return (
    <div className="dialogo-fondo" onMouseDown={(e) => { if (e.target === e.currentTarget) { cerrar(); } }}>
      <div className="dialogo" role="dialog" aria-modal="true" aria-label={titulo}>
        <h2>{titulo}</h2>
        <form className="busqueda-mascota" onSubmit={buscar}>
          <input autoFocus type="search" required placeholder="Nombre o documento del propietario" aria-label="Nombre o documento del propietario" value={texto} onChange={(e) => setTexto(e.target.value)} />
          <button type="submit" className="boton boton-principal" disabled={buscando}>{buscando ? 'Buscando…' : 'Buscar'}</button>
        </form>
        {error && <p className="mensaje-error" role="alert">{error}</p>}
        {resultados && resultados.length === 0 && <p className="tabla-vacia">No se encontraron mascotas.</p>}
        {resultados && resultados.length > 0 && (
          <ul className="lista-mascotas">
            {resultados.map((m) => (
              <li key={m.id}>
                <button type="button" onClick={() => elegir(m)}>
                  <span><strong>{m.nombre}</strong> · {m.especie}, {m.raza}<small>{m.propietario}</small></span>
                  <Estado valor={m.estado} />
                </button>
              </li>
            ))}
          </ul>
        )}
        <div className="dialogo-acciones">
          <Boton onClick={cerrar}>Cancelar</Boton>
        </div>
      </div>
    </div>
  );
}

export function useElegirMascota() {
  const [pedido, setPedido] = useState<{ titulo: string; alElegir: (m: MascotaElegida) => void } | null>(null);
  return {
    elegirMascota: (titulo: string, alElegir: (m: MascotaElegida) => void) => setPedido({ titulo, alElegir }),
    elementoMascota: pedido ? <ElegirMascota titulo={pedido.titulo} cerrar={() => setPedido(null)} elegir={(m) => { setPedido(null); pedido.alElegir(m); }} /> : null,
  };
}
