import { useState } from 'react';
import { api } from '../../api/cliente';
import { useElegirMascota } from '../../componentes/ElegirMascota';
import { useDialogo, type Campo } from '../../componentes/Formulario';
import { useDatos } from '../../componentes/ganchos';
import { Tabla } from '../../componentes/Tabla';
import { Boton, CampoFiltro, Encabezado, Estado, Seccion } from '../../componentes/ui';
import { fechaHora, pesos } from '../../util';

const MEDIOS = [{ valor: 'EFECTIVO', texto: 'Efectivo' }, { valor: 'TARJETA', texto: 'Tarjeta' }, { valor: 'TRANSFERENCIA', texto: 'Transferencia' }];
const camposPago: Campo[] = [
  { nombre: 'medio', etiqueta: 'Medio', tipo: 'select', requerido: true, opciones: MEDIOS },
  { nombre: 'valor', etiqueta: 'Valor', tipo: 'number', minimo: 1, requerido: true },
];

function Detalle({ factura, alCambiar }: { factura: any; alCambiar: () => void }) {
  const { abrir, elemento } = useDialogo();
  const editable = factura.estado === 'PENDIENTE' || factura.estado === 'CREDITO';
  return (
    <Seccion titulo={`Factura ${factura.numero}`} acciones={<div className="encabezado-acciones">
      {factura.estado === 'PENDIENTE' && <Boton tono="principal" onClick={() => abrir({
        titulo: `Cobrar ${factura.numero}`, descripcion: `Saldo: ${pesos(factura.saldo)}. Puedes combinar medios de pago.`, exito: 'Pago registrado', despues: alCambiar, textoEnviar: 'Registrar pago',
        inicial: { pagos: [{ medio: 'EFECTIVO', valor: factura.saldo }] },
        campos: [{ nombre: 'pagos', etiqueta: 'Pagos', tipo: 'filas', subcampos: camposPago }],
        enviar: (v) => api('facturacion', `/api/facturas/${factura.id}/pagos`, 'POST', { pagos: v.pagos.map((p: any) => ({ medio: p.medio, valor: Number(p.valor) })) }),
      })}>Cobrar</Boton>}
      {factura.estado === 'CREDITO' && <Boton tono="principal" onClick={() => abrir({
        titulo: `Abono a ${factura.numero}`, descripcion: `Saldo: ${pesos(factura.saldo)}.`, exito: 'Abono registrado', despues: alCambiar, textoEnviar: 'Registrar abono',
        inicial: { medio: 'EFECTIVO' }, campos: camposPago,
        enviar: (v) => api('facturacion', `/api/facturas/${factura.id}/abonos`, 'POST', { medio: v.medio, valor: Number(v.valor) }),
      })}>Abonar</Boton>}
      {editable && factura.pagos.length === 0 && <Boton onClick={() => abrir({
        titulo: 'Aplicar descuento', descripcion: `Subtotal: ${pesos(factura.subtotal)}. El impuesto se calcula sobre el valor con descuento.`, exito: 'Descuento aplicado', despues: alCambiar, textoEnviar: 'Aplicar',
        campos: [{ nombre: 'descuentoPorcentaje', etiqueta: 'Descuento en porcentaje', tipo: 'number', minimo: 0, requerido: true }],
        enviar: (v) => api('facturacion', `/api/facturas/${factura.id}/ajustes`, 'PATCH', { descuentoPorcentaje: Number(v.descuentoPorcentaje) }),
      })}>Descuento</Boton>}
      {factura.estado !== 'ANULADA' && <Boton onClick={() => abrir({
        titulo: 'Enviar por correo', descripcion: `Se enviará el PDF a ${factura.propietarioCorreo}.`, exito: 'Factura enviada', despues: alCambiar, textoEnviar: 'Enviar',
        enviar: () => api('facturacion', `/api/facturas/${factura.id}/envio-correo`, 'POST'),
      })}>Enviar por correo</Boton>}
      {factura.estado !== 'ANULADA' && <Boton onClick={() => abrir({
        titulo: `Anular ${factura.numero}`, descripcion: 'Sus ítems volverán a quedar pendientes de facturar y sus pagos se revertirán.', textoEnviar: 'Anular', peligro: true, exito: 'Factura anulada', despues: alCambiar,
        campos: [{ nombre: 'motivo', etiqueta: 'Motivo', requerido: true }],
        enviar: (v) => api('facturacion', `/api/facturas/${factura.id}/anulacion`, 'PATCH', v),
      })}>Anular</Boton>}
    </div>}>
      <div className="tarjeta tarjeta-contenido" style={{ marginBottom: 12 }}>
        <dl className="datos">
          <div><dt>Mascota</dt><dd>{factura.mascotaNombre}</dd></div>
          <div><dt>Subtotal</dt><dd>{pesos(factura.subtotal)}</dd></div>
          <div><dt>Descuento</dt><dd>{pesos(factura.descuento)}</dd></div>
          <div><dt>Impuesto ({factura.tasaImpuesto} %)</dt><dd>{pesos(factura.impuesto)}</dd></div>
          <div><dt>Total</dt><dd>{pesos(factura.total)}</dd></div>
          <div><dt>Saldo</dt><dd>{pesos(factura.saldo)}</dd></div>
        </dl>
      </div>
      <Tabla filas={factura.items} clave={(i: any) => `${i.tipo}-${i.origenId}`}
        columnas={[
          { titulo: 'Concepto', valor: (i: any) => i.descripcion },
          { titulo: 'Cantidad', valor: (i: any) => i.cantidad, alinear: 'derecha' },
          { titulo: 'Valor unitario', valor: (i: any) => pesos(i.precioUnitario), alinear: 'derecha' },
          { titulo: 'Subtotal', valor: (i: any) => pesos(i.subtotal), alinear: 'derecha' },
        ]} />
      {elemento}
    </Seccion>
  );
}

export function Facturas() {
  const [estado, setEstado] = useState('');
  const [pagina, setPagina] = useState(0);
  const [elegidaId, setElegidaId] = useState<string | null>(null);
  const facturas = useDatos(() => api('facturacion', `/api/facturas?estado=${estado}&page=${pagina}&size=8`), [estado, pagina]);
  const elegida = useDatos(() => (elegidaId ? api('facturacion', `/api/facturas/${elegidaId}`) : Promise.resolve(null)), [elegidaId]);
  const { abrir, elemento } = useDialogo();
  const { elegirMascota, elementoMascota } = useElegirMascota();
  const d = facturas.datos;

  const generar = () => elegirMascota('Generar factura: elige la mascota', (m) => abrir({
    titulo: `Factura de ${m.nombre}`, descripcion: 'Incluye todos sus ítems pendientes: consulta, laboratorio, medicamentos, hospitalización y peluquería.', exito: 'Factura generada', textoEnviar: 'Generar',
    despues: (f) => { facturas.recargar(); setElegidaId(f.id); elegida.recargar(); },
    campos: [{ nombre: 'credito', etiqueta: 'Factura a crédito', tipo: 'checkbox' }],
    enviar: (v) => api('facturacion', '/api/facturas', 'POST', { mascotaId: m.id, credito: v.credito }),
  }));

  return (
    <>
      <Encabezado titulo="Facturas" descripcion="Para cobrar debes tener la caja abierta.">
        <Boton tono="principal" icono="mas" onClick={generar}>Generar factura</Boton>
      </Encabezado>
      <Seccion titulo="Listado">
        <Tabla filas={d?.contenido} cargando={facturas.cargando} error={facturas.error} clave={(f: any) => f.id} vacio="No hay facturas en este estado."
          servidor={d ? { pagina, totalPaginas: Math.max(d.totalPaginas, 1), total: d.totalElementos, cambiar: setPagina } : undefined}
          filtros={<CampoFiltro etiqueta="Estado">
            <select value={estado} onChange={(e) => { setEstado(e.target.value); setPagina(0); }}>
              <option value="">Todas</option>
              {['PENDIENTE', 'CREDITO', 'PAGADA', 'ANULADA'].map((s) => <option key={s} value={s}>{s}</option>)}
            </select>
          </CampoFiltro>}
          columnas={[
            { titulo: 'Número', valor: (f: any) => f.numero },
            { titulo: 'Fecha', valor: (f: any) => fechaHora(f.fecha) },
            { titulo: 'Mascota', valor: (f: any) => f.mascotaNombre },
            { titulo: 'Total', valor: (f: any) => pesos(f.total), alinear: 'derecha' },
            { titulo: 'Saldo', valor: (f: any) => pesos(f.saldo), alinear: 'derecha' },
            { titulo: 'Estado', celda: (f: any) => <Estado valor={f.estado} /> },
          ]}
          acciones={(f: any) => <Boton onClick={() => setElegidaId(f.id)}>Ver</Boton>} />
      </Seccion>
      {elegida.datos && <Detalle factura={elegida.datos} alCambiar={() => { elegida.recargar(); facturas.recargar(); }} />}
      {elementoMascota}
      {elemento}
    </>
  );
}
