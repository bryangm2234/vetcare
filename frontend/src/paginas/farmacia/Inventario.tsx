import { useState } from 'react';
import { api } from '../../api/cliente';
import { useDialogo } from '../../componentes/Formulario';
import { useDatos } from '../../componentes/ganchos';
import { Tabla } from '../../componentes/Tabla';
import { Boton, Encabezado, Seccion } from '../../componentes/ui';
import { consulta, fecha, fechaHora, pesos } from '../../util';

function Detalle({ medicamento, alCambiar }: { medicamento: any; alCambiar: () => void }) {
  const lotes = useDatos(() => api<any[]>('farmacia', `/api/medicamentos/${medicamento.id}/lotes`), [medicamento.id]);
  const kardex = useDatos(() => api('farmacia', `/api/medicamentos/${medicamento.id}/kardex?size=100`), [medicamento.id]);
  const { abrir, elemento } = useDialogo();
  const recargar = () => { lotes.recargar(); kardex.recargar(); alCambiar(); };
  return (
    <>
      <Seccion titulo={`Lotes de ${medicamento.nombre}`} acciones={<Boton tono="principal" icono="mas" onClick={() => abrir({
        titulo: 'Registrar lote', exito: 'Lote registrado', despues: recargar,
        campos: [
          { nombre: 'numero', etiqueta: 'Número de lote', requerido: true },
          { nombre: 'vencimiento', etiqueta: 'Vencimiento', tipo: 'date', requerido: true },
          { nombre: 'cantidad', etiqueta: 'Cantidad', tipo: 'number', minimo: 1, requerido: true },
        ],
        enviar: (v) => api('farmacia', `/api/medicamentos/${medicamento.id}/lotes`, 'POST', { ...v, cantidad: Number(v.cantidad) }),
      })}>Registrar lote</Boton>}>
        <Tabla filas={lotes.datos} cargando={lotes.cargando} error={lotes.error} clave={(l) => l.id} vacio="Sin lotes."
          columnas={[{ titulo: 'Lote', valor: (l) => l.numero }, { titulo: 'Vencimiento', valor: (l) => fecha(l.vencimiento) }, { titulo: 'Cantidad', valor: (l) => l.cantidad, alinear: 'derecha' }]}
          acciones={(l) => <Boton onClick={() => abrir({
            titulo: `Ajustar lote ${l.numero}`, descripcion: 'Positivo para sumar y negativo para restar. El stock nunca puede quedar negativo.', exito: 'Ajuste registrado', despues: recargar, textoEnviar: 'Ajustar',
            campos: [{ nombre: 'cantidad', etiqueta: 'Cantidad', tipo: 'number', requerido: true }, { nombre: 'motivo', etiqueta: 'Motivo', requerido: true }],
            enviar: (v) => api('farmacia', `/api/medicamentos/${medicamento.id}/ajustes`, 'POST', { loteId: l.id, cantidad: Number(v.cantidad), motivo: v.motivo }),
          })}>Ajustar</Boton>} />
      </Seccion>
      <Seccion titulo="Kardex">
        <Tabla filas={kardex.datos?.contenido} cargando={kardex.cargando} error={kardex.error} clave={(m: any) => m.id} vacio="Sin movimientos."
          columnas={[
            { titulo: 'Fecha', valor: (m: any) => fechaHora(m.fecha) },
            { titulo: 'Tipo', valor: (m: any) => m.tipo },
            { titulo: 'Lote', valor: (m: any) => m.loteNumero },
            { titulo: 'Cantidad', valor: (m: any) => m.cantidad, alinear: 'derecha' },
            { titulo: 'Saldo del lote', valor: (m: any) => m.saldoLote, alinear: 'derecha' },
            { titulo: 'Responsable', valor: (m: any) => m.responsable },
            { titulo: 'Referencia', valor: (m: any) => m.referencia },
          ]} />
      </Seccion>
      {elemento}
    </>
  );
}

export function Inventario() {
  const [texto, setTexto] = useState('');
  const [pagina, setPagina] = useState(0);
  const [elegido, setElegido] = useState<any>(null);
  const catalogo = useDatos(() => api('farmacia', '/api/medicamentos' + consulta({ texto: texto.trim(), page: pagina, size: 8 })), [texto, pagina]);
  const { abrir, elemento } = useDialogo();
  const d = catalogo.datos;
  return (
    <>
      <Encabezado titulo="Inventario" descripcion="Medicamentos e insumos. El stock disponible no cuenta lotes vencidos.">
        <Boton tono="principal" icono="mas" onClick={() => abrir({
          titulo: 'Registrar producto', exito: 'Producto registrado', despues: catalogo.recargar,
          campos: [
            { nombre: 'codigo', etiqueta: 'Código', requerido: true },
            { nombre: 'nombre', etiqueta: 'Nombre', requerido: true },
            { nombre: 'tipo', etiqueta: 'Tipo', tipo: 'select', requerido: true, opciones: [{ valor: 'MEDICAMENTO', texto: 'Medicamento' }, { valor: 'INSUMO', texto: 'Insumo' }] },
            { nombre: 'presentacion', etiqueta: 'Presentación', requerido: true },
            { nombre: 'precio', etiqueta: 'Precio unitario en pesos', tipo: 'number', minimo: 0, requerido: true },
            { nombre: 'stockMinimo', etiqueta: 'Stock mínimo', tipo: 'number', minimo: 0, requerido: true },
          ],
          enviar: (v) => api('farmacia', '/api/medicamentos', 'POST', { ...v, precio: Number(v.precio), stockMinimo: Number(v.stockMinimo) }),
        })}>Registrar producto</Boton>
      </Encabezado>
      <Seccion titulo="Catálogo">
        <Tabla filas={d?.contenido} cargando={catalogo.cargando} error={catalogo.error} clave={(m: any) => m.id} vacio="No se encontraron productos."
          servidor={d ? { pagina, totalPaginas: Math.max(d.totalPaginas, 1), total: d.totalElementos, cambiar: setPagina } : undefined}
          filtros={<label className="filtro"><span>Nombre o código</span><input type="search" value={texto} onChange={(e) => { setTexto(e.target.value); setPagina(0); }} /></label>}
          columnas={[
            { titulo: 'Código', valor: (m: any) => m.codigo },
            { titulo: 'Nombre', valor: (m: any) => m.nombre },
            { titulo: 'Presentación', valor: (m: any) => m.presentacion },
            { titulo: 'Precio', valor: (m: any) => pesos(m.precio), alinear: 'derecha' },
            { titulo: 'Disponible', valor: (m: any) => m.stockDisponible, alinear: 'derecha' },
            { titulo: 'Mínimo', valor: (m: any) => m.stockMinimo, alinear: 'derecha' },
          ]}
          acciones={(m: any) => <Boton onClick={() => setElegido(m)}>Lotes y kardex</Boton>} />
      </Seccion>
      {elegido && <Detalle medicamento={elegido} alCambiar={catalogo.recargar} />}
      {elemento}
    </>
  );
}
