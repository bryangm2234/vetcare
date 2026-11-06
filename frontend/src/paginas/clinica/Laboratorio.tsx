import { useState } from 'react';
import { api } from '../../api/cliente';
import { useDialogo } from '../../componentes/Formulario';
import { useDatos } from '../../componentes/ganchos';
import { Tabla } from '../../componentes/Tabla';
import { Boton, CampoFiltro, Encabezado, Estado } from '../../componentes/ui';
import { fechaHora } from '../../util';

export function Laboratorio() {
  const [estado, setEstado] = useState('PENDIENTE');
  const [pagina, setPagina] = useState(0);
  const ordenes = useDatos(() => api('historia', `/api/ordenes-laboratorio?estado=${estado}&page=${pagina}&size=10`), [estado, pagina]);
  const { abrir, elemento } = useDialogo();
  const d = ordenes.datos;
  return (
    <>
      <Encabezado titulo="Órdenes de laboratorio" descripcion="Las más antiguas aparecen primero." />
      <Tabla filas={d?.contenido} cargando={ordenes.cargando} error={ordenes.error} clave={(o: any) => o.id} vacio="No hay órdenes en este estado."
        servidor={d ? { pagina, totalPaginas: Math.max(d.totalPaginas, 1), total: d.totalElementos, cambiar: setPagina } : undefined}
        filtros={<CampoFiltro etiqueta="Estado">
          <select value={estado} onChange={(e) => { setEstado(e.target.value); setPagina(0); }}>
            <option value="PENDIENTE">Pendientes</option>
            <option value="COMPLETADA">Completadas</option>
          </select>
        </CampoFiltro>}
        columnas={[
          { titulo: 'Ordenada', valor: (o: any) => fechaHora(o.fechaOrden) },
          { titulo: 'Examen', valor: (o: any) => o.examen },
          { titulo: 'Indicaciones', valor: (o: any) => o.indicaciones ?? '—' },
          { titulo: 'Resultado', valor: (o: any) => o.resultado ?? '—' },
          { titulo: 'Estado', celda: (o: any) => <Estado valor={o.estado} /> },
        ]}
        acciones={(o: any) => o.estado === 'PENDIENTE' ? <Boton tono="principal" onClick={() => abrir({
          titulo: `Resultado de ${o.examen}`, exito: 'Resultado registrado', despues: ordenes.recargar,
          campos: [{ nombre: 'resultado', etiqueta: 'Resultado', tipo: 'textarea', requerido: true }],
          enviar: (v) => api('historia', `/api/ordenes-laboratorio/${o.id}/resultado`, 'PUT', v),
        })}>Registrar resultado</Boton> : null} />
      {elemento}
    </>
  );
}
