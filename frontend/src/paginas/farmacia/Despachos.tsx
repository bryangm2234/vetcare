import { api } from '../../api/cliente';
import { useDialogo } from '../../componentes/Formulario';
import { useDatos } from '../../componentes/ganchos';
import { Tabla } from '../../componentes/Tabla';
import { Boton, Encabezado, Estado } from '../../componentes/ui';
import { fecha } from '../../util';

export function Despachos() {
  const formulas = useDatos(() => api<any[]>('historia', '/api/formulas/despachables'));
  const { abrir, elemento } = useDialogo();
  return (
    <>
      <Encabezado titulo="Despachar fórmulas" descripcion="Solo aparecen fórmulas vigentes con cantidades pendientes. Sale primero el lote que vence antes." />
      <Tabla filas={formulas.datos} cargando={formulas.cargando} error={formulas.error} clave={(f) => f.id} buscador="Buscar medicamento" vacio="No hay fórmulas por despachar."
        columnas={[
          { titulo: 'Medicamentos pendientes', valor: (f) => f.items.filter((i: any) => i.pendiente > 0).map((i: any) => `${i.medicamento}: ${i.pendiente}`).join(', ') },
          { titulo: 'Emitida', valor: (f) => fecha(f.fecha) },
          { titulo: 'Vigente hasta', valor: (f) => fecha(f.vigenteHasta) },
          { titulo: 'Estado', celda: (f) => <Estado valor={f.estado} /> },
        ]}
        acciones={(f) => <Boton tono="principal" onClick={() => {
          const pendientes = f.items.filter((i: any) => i.pendiente > 0);
          abrir({
            titulo: 'Despachar fórmula', exito: 'Despacho registrado', despues: formulas.recargar, textoEnviar: 'Despachar',
            descripcion: 'Puedes despachar una parte; lo demás queda pendiente mientras la fórmula esté vigente.',
            inicial: Object.fromEntries(pendientes.map((i: any) => [i.medicamentoId, i.pendiente])),
            campos: pendientes.map((i: any) => ({ nombre: i.medicamentoId, etiqueta: `${i.medicamento} (${i.pendiente} pendientes)`, tipo: 'number' as const, minimo: 0 })),
            enviar: (v) => {
              const items = pendientes.map((i: any) => ({ medicamentoId: i.medicamentoId, cantidad: Number(v[i.medicamentoId]) })).filter((i: any) => i.cantidad > 0);
              if (items.length === 0) {
                return Promise.reject(new Error('Indica al menos una cantidad'));
              }
              return api('farmacia', '/api/despachos', 'POST', { formulaId: f.id, items });
            },
          });
        }}>Despachar</Boton>} />
      {elemento}
    </>
  );
}
