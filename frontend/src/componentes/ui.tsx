import type { ReactNode } from 'react';
import { Icono } from './Icono';

export function Encabezado({ titulo, descripcion, children }: { titulo: string; descripcion?: ReactNode; children?: ReactNode }) {
  return (
    <header className="encabezado-pagina">
      <div>
        <h1>{titulo}</h1>
        {descripcion && <p>{descripcion}</p>}
      </div>
      {children && <div className="encabezado-acciones">{children}</div>}
    </header>
  );
}

const TONOS: Record<string, string> = {
  ACTIVO: 'bien', ACTIVA: 'bien', VIGENTE: 'bien', PAGADA: 'bien', COMPLETADA: 'bien', ATENDIDA: 'bien', REALIZADA: 'bien', DESPACHADA: 'bien', RECIBIDA: 'bien', CONFIRMADA: 'bien', ABIERTA: 'bien', PROGRAMADA: 'info', ENVIADA: 'info', EN_ESPERA: 'atencion', PENDIENTE: 'atencion', PROXIMA: 'atencion', PARCIAL: 'atencion', RECIBIDA_PARCIAL: 'atencion', CREDITO: 'atencion', INACTIVO: 'neutro', INACTIVA: 'neutro', FINALIZADA: 'neutro', CERRADA: 'neutro', CANCELADA: 'neutro', VENCIDA: 'critico', ANULADA: 'critico', FALLECIDA: 'neutro',
};

export function Estado({ valor }: { valor: string }) {
  return <span className={`insignia insignia-${TONOS[valor] ?? 'neutro'}`}>{valor.replace(/_/g, ' ').toLowerCase()}</span>;
}

export function Boton({ icono, children, tono = 'suave', ...resto }: { icono?: string; children: ReactNode; tono?: 'principal' | 'suave' | 'peligro' } & React.ButtonHTMLAttributes<HTMLButtonElement>) {
  return (
    <button type="button" className={`boton boton-${tono}`} {...resto}>
      {icono && <Icono nombre={icono} />}
      {children}
    </button>
  );
}

export function Indicador({ titulo, valor, detalle, error }: { titulo: string; valor: ReactNode; detalle?: string; error?: string | null }) {
  return (
    <div className="tarjeta indicador">
      <span className="indicador-titulo">{titulo}</span>
      {error ? <span className="indicador-error">No disponible</span> : <strong className="indicador-valor">{valor}</strong>}
      {detalle && !error && <span className="indicador-detalle">{detalle}</span>}
    </div>
  );
}

export function Seccion({ titulo, children, acciones }: { titulo: string; children: ReactNode; acciones?: ReactNode }) {
  return (
    <section className="seccion">
      <div className="seccion-encabezado">
        <h2>{titulo}</h2>
        {acciones}
      </div>
      {children}
    </section>
  );
}

export function CampoFiltro({ etiqueta, children }: { etiqueta: string; children: ReactNode }) {
  return <label className="filtro"><span>{etiqueta}</span>{children}</label>;
}

/** Barras horizontales de una sola serie: el título nombra lo medido y cada barra lleva su valor. */
export function Barras({ datos, formato = (v) => String(v), maximo }: { datos: { etiqueta: string; valor: number }[]; formato?: (v: number) => string; maximo?: number }) {
  const tope = maximo ?? Math.max(...datos.map((d) => d.valor), 1);
  if (datos.length === 0) {
    return <p className="tabla-vacia">No hay datos en este período.</p>;
  }
  return (
    <ul className="barras">
      {datos.map((d) => (
        <li key={d.etiqueta} title={`${d.etiqueta}: ${formato(d.valor)}`}>
          <span className="barra-etiqueta">{d.etiqueta}</span>
          <span className="barra-pista"><span className="barra-relleno" style={{ width: `${Math.min((d.valor / tope) * 100, 100)}%` }} /></span>
          <span className="barra-valor">{formato(d.valor)}</span>
        </li>
      ))}
    </ul>
  );
}
