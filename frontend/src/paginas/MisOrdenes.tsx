import { api } from '../api/cliente';
import { useDialogo } from '../componentes/Formulario';
import { useDatos } from '../componentes/ganchos';
import { Tabla } from '../componentes/Tabla';
import { Boton, Encabezado, Estado } from '../componentes/ui';
import { fechaHora } from '../util';

export function MisOrdenes() {
  const ordenes = useDatos(() => api<any[]>('farmacia', '/api/proveedor/ordenes-compra'));
  const { abrir, elemento } = useDialogo();
  return (
    <>
      <Encabezado titulo="Órdenes de compra" descripcion="Confirma las órdenes que te asigna la clínica." />
      <Tabla filas={ordenes.datos} cargando={ordenes.cargando} error={ordenes.error} clave={(o) => o.id} vacio="No tienes órdenes asignadas."
        columnas={[
          { titulo: 'Fecha', valor: (o) => fechaHora(o.fecha) },
          { titulo: 'Productos', valor: (o) => `${o.items.length} ${o.items.length === 1 ? 'producto' : 'productos'}, ${o.items.reduce((t: number, i: any) => t + i.cantidad, 0)} unidades` },
          { titulo: 'Estado', celda: (o) => <Estado valor={o.estado} /> },
        ]}
        acciones={(o) => o.estado === 'ENVIADA' ? <Boton tono="principal" onClick={() => abrir({
          titulo: 'Confirmar orden', textoEnviar: 'Confirmar', exito: 'Orden confirmada', despues: ordenes.recargar,
          enviar: () => api('farmacia', `/api/proveedor/ordenes-compra/${o.id}/confirmacion`, 'PATCH'),
        })}>Confirmar</Boton> : null} />
      {elemento}
    </>
  );
}
