import { useState } from 'react';
import { api } from '../../api/cliente';
import { useDatos } from '../../componentes/ganchos';
import { Tabla } from '../../componentes/Tabla';
import { CampoFiltro, Encabezado, Seccion } from '../../componentes/ui';
import { fecha } from '../../util';

export function AlertasInventario() {
  const [dias, setDias] = useState(60);
  const stock = useDatos(() => api<any[]>('farmacia', '/api/alertas/stock-minimo'));
  const lotes = useDatos(() => api<any[]>('farmacia', `/api/lotes/proximos-vencer?dias=${dias}`), [dias]);
  return (
    <>
      <Encabezado titulo="Alertas de inventario" />
      <Seccion titulo="Productos en stock mínimo">
        <Tabla filas={stock.datos} cargando={stock.cargando} error={stock.error} clave={(a) => a.medicamentoId} vacio="Ningún producto está en su stock mínimo."
          columnas={[{ titulo: 'Código', valor: (a) => a.codigo }, { titulo: 'Producto', valor: (a) => a.nombre }, { titulo: 'Disponible', valor: (a) => a.stockDisponible, alinear: 'derecha' }, { titulo: 'Mínimo', valor: (a) => a.stockMinimo, alinear: 'derecha' }]} />
      </Seccion>
      <Seccion titulo="Lotes próximos a vencer">
        <Tabla filas={lotes.datos} cargando={lotes.cargando} error={lotes.error} clave={(l) => l.loteId} vacio="No hay lotes por vencer en este período."
          filtros={<CampoFiltro etiqueta="Ventana en días"><input type="number" min={0} value={dias} onChange={(e) => setDias(Number(e.target.value))} /></CampoFiltro>}
          columnas={[{ titulo: 'Lote', valor: (l) => l.numero }, { titulo: 'Producto', valor: (l) => l.medicamento }, { titulo: 'Cantidad', valor: (l) => l.cantidad, alinear: 'derecha' }, { titulo: 'Vencimiento', valor: (l) => fecha(l.vencimiento) }]} />
      </Seccion>
    </>
  );
}
