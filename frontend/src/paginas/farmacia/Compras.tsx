import { api } from '../../api/cliente';
import { useDialogo } from '../../componentes/Formulario';
import { useDatos } from '../../componentes/ganchos';
import { Tabla } from '../../componentes/Tabla';
import { Boton, Encabezado, Estado } from '../../componentes/ui';
import { fechaHora } from '../../util';

export function Compras() {
  const ordenes = useDatos(() => api<any[]>('farmacia', '/api/ordenes-compra'));
  const proveedores = useDatos(() => api<any[]>('farmacia', '/api/proveedores'));
  const catalogo = useDatos(() => api('farmacia', '/api/medicamentos?size=100'));
  const { abrir, elemento } = useDialogo();
  const nombreProveedor = (id: string) => proveedores.datos?.find((p) => p.id === id)?.nombre ?? '—';
  const nombreProducto = (id: string) => catalogo.datos?.contenido.find((m: any) => m.id === id)?.nombre ?? id;

  const nueva = () => abrir({
    titulo: 'Generar orden de compra', exito: 'Orden generada', despues: ordenes.recargar, textoEnviar: 'Generar',
    campos: [
      { nombre: 'proveedorId', etiqueta: 'Proveedor', tipo: 'select', requerido: true, opciones: (proveedores.datos ?? []).map((p) => ({ valor: p.id, texto: p.nombre })) },
      {
        nombre: 'items', etiqueta: 'Productos', tipo: 'filas', subcampos: [
          { nombre: 'medicamentoId', etiqueta: 'Producto', tipo: 'select', requerido: true, opciones: (catalogo.datos?.contenido ?? []).map((m: any) => ({ valor: m.id, texto: m.nombre })) },
          { nombre: 'cantidad', etiqueta: 'Cantidad', tipo: 'number', minimo: 1, requerido: true },
        ],
      },
    ],
    enviar: (v) => api('farmacia', '/api/ordenes-compra', 'POST', { proveedorId: v.proveedorId, items: v.items.map((i: any) => ({ ...i, cantidad: Number(i.cantidad) })) }),
  });

  const recibir = (o: any) => {
    const pendientes = o.items.filter((i: any) => i.recibida < i.cantidad);
    abrir({
      titulo: 'Registrar recepción', descripcion: 'Cada producto recibido crea un lote y aumenta el stock.', exito: 'Mercancía recibida', despues: ordenes.recargar, textoEnviar: 'Registrar',
      campos: [{
        nombre: 'items', etiqueta: 'Mercancía recibida', tipo: 'filas', subcampos: [
          { nombre: 'medicamentoId', etiqueta: 'Producto', tipo: 'select', requerido: true, opciones: pendientes.map((i: any) => ({ valor: i.medicamentoId, texto: `${nombreProducto(i.medicamentoId)} (faltan ${i.cantidad - i.recibida})` })) },
          { nombre: 'lote', etiqueta: 'Lote', requerido: true },
          { nombre: 'vencimiento', etiqueta: 'Vencimiento', tipo: 'date', requerido: true },
          { nombre: 'cantidad', etiqueta: 'Cantidad', tipo: 'number', minimo: 1, requerido: true },
        ],
      }],
      enviar: (v) => api('farmacia', `/api/ordenes-compra/${o.id}/recepciones`, 'POST', { items: v.items.map((i: any) => ({ ...i, cantidad: Number(i.cantidad) })) }),
    });
  };

  return (
    <>
      <Encabezado titulo="Órdenes de compra" descripcion="El proveedor debe confirmar la orden antes de registrar la recepción.">
        <Boton tono="principal" icono="mas" disabled={!proveedores.datos?.length} title={proveedores.datos?.length ? undefined : 'El administrador debe registrar un proveedor'} onClick={nueva}>Generar orden</Boton>
      </Encabezado>
      <Tabla filas={ordenes.datos} cargando={ordenes.cargando} error={ordenes.error} clave={(o) => o.id} vacio="Aún no hay órdenes."
        columnas={[
          { titulo: 'Fecha', valor: (o) => fechaHora(o.fecha) },
          { titulo: 'Proveedor', valor: (o) => nombreProveedor(o.proveedorId) },
          { titulo: 'Productos', valor: (o) => o.items.map((i: any) => `${nombreProducto(i.medicamentoId)}: ${i.recibida} de ${i.cantidad}`).join(', ') },
          { titulo: 'Estado', celda: (o) => <Estado valor={o.estado} /> },
        ]}
        acciones={(o) => (o.estado === 'CONFIRMADA' || o.estado === 'RECIBIDA_PARCIAL') ? <Boton tono="principal" onClick={() => recibir(o)}>Recibir</Boton> : null} />
      {elemento}
    </>
  );
}
