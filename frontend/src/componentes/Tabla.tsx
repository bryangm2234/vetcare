import { useEffect, useMemo, useState, type ReactNode } from 'react';
import { Icono } from './Icono';

export interface Columna<T> {
  titulo: string;
  valor?: (fila: T) => unknown;
  celda?: (fila: T) => ReactNode;
  alinear?: 'derecha';
}

export interface PaginacionServidor {
  pagina: number;
  totalPaginas: number;
  total: number;
  cambiar: (pagina: number) => void;
}

interface Props<T> {
  columnas: Columna<T>[];
  filas: T[] | undefined;
  clave: (fila: T) => string;
  cargando?: boolean;
  error?: string | null;
  vacio?: string;
  buscador?: string;
  filtros?: ReactNode;
  acciones?: (fila: T) => ReactNode;
  porPagina?: number;
  servidor?: PaginacionServidor;
}

const normalizar = (texto: unknown) => String(texto ?? '').normalize('NFD').replace(/\p{M}/gu, '').toLowerCase();

export function Tabla<T>({ columnas, filas, clave, cargando, error, vacio = 'No hay registros.', buscador, filtros, acciones, porPagina = 10, servidor }: Props<T>) {
  const [busqueda, setBusqueda] = useState('');
  const [pagina, setPagina] = useState(0);

  const filtradas = useMemo(() => {
    const lista = filas ?? [];
    const termino = normalizar(busqueda).trim();
    if (!termino) {
      return lista;
    }
    return lista.filter((fila) => columnas.some((c) => c.valor && normalizar(c.valor(fila)).includes(termino)));
  }, [filas, busqueda, columnas]);

  const totalPaginas = servidor ? servidor.totalPaginas : Math.max(Math.ceil(filtradas.length / porPagina), 1);
  const paginaActual = servidor ? servidor.pagina : Math.min(pagina, totalPaginas - 1);
  const visibles = servidor ? filtradas : filtradas.slice(paginaActual * porPagina, (paginaActual + 1) * porPagina);
  const total = servidor ? servidor.total : filtradas.length;

  useEffect(() => setPagina(0), [busqueda]);

  return (
    <div className="tarjeta tabla-contenedor">
      {(buscador || filtros) && (
        <div className="tabla-barra">
          {buscador && (
            <label className="buscador">
              <Icono nombre="buscar" />
              <input type="search" placeholder={buscador} aria-label={buscador} value={busqueda} onChange={(e) => setBusqueda(e.target.value)} />
            </label>
          )}
          {filtros}
        </div>
      )}
      {error && <p className="mensaje-error" role="alert">{error}</p>}
      <div className="tabla-desplazable">
        <table className="tabla">
          <thead>
            <tr>
              {columnas.map((c) => <th key={c.titulo} className={c.alinear === 'derecha' ? 'derecha' : undefined}>{c.titulo}</th>)}
              {acciones && <th className="derecha">Acciones</th>}
            </tr>
          </thead>
          <tbody>
            {visibles.map((fila) => (
              <tr key={clave(fila)}>
                {columnas.map((c) => (
                  <td key={c.titulo} data-titulo={c.titulo} className={c.alinear === 'derecha' ? 'derecha' : undefined}>
                    {c.celda ? c.celda(fila) : String(c.valor?.(fila) ?? '—')}
                  </td>
                ))}
                {acciones && <td data-titulo="Acciones" className="derecha"><div className="acciones-fila">{acciones(fila)}</div></td>}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {visibles.length === 0 && !error && <p className="tabla-vacia">{cargando ? 'Cargando…' : vacio}</p>}
      {total > 0 && (
        <div className="paginacion">
          <span>{total} {total === 1 ? 'registro' : 'registros'}</span>
          {totalPaginas > 1 && (
            <div className="paginacion-botones">
              <button type="button" className="boton boton-suave" disabled={paginaActual === 0} onClick={() => (servidor ? servidor.cambiar(paginaActual - 1) : setPagina(paginaActual - 1))}>Anterior</button>
              <span>Página {paginaActual + 1} de {totalPaginas}</span>
              <button type="button" className="boton boton-suave" disabled={paginaActual >= totalPaginas - 1} onClick={() => (servidor ? servidor.cambiar(paginaActual + 1) : setPagina(paginaActual + 1))}>Siguiente</button>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
