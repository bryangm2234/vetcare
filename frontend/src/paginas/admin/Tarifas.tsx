import { api } from '../../api/cliente';
import { useDialogo } from '../../componentes/Formulario';
import { useDatos } from '../../componentes/ganchos';
import { Tabla } from '../../componentes/Tabla';
import { Boton, Encabezado } from '../../componentes/ui';
import { fechaHora, pesos } from '../../util';

const SERVICIOS = [
  { valor: 'CONSULTA', texto: 'Consulta' },
  { valor: 'LABORATORIO', texto: 'Laboratorio' },
  { valor: 'HOSPITALIZACION', texto: 'Hospitalización, por día' },
  { valor: 'PELUQUERIA', texto: 'Peluquería' },
];

export function Tarifas() {
  const tarifas = useDatos(() => api<any[]>('facturacion', '/api/tarifas'));
  const { abrir, elemento } = useDialogo();
  const usados = new Set((tarifas.datos ?? []).map((t) => t.servicio));
  return (
    <>
      <Encabezado titulo="Tarifas" descripcion="Los cambios solo afectan las facturas nuevas. Los medicamentos se cobran al precio de farmacia.">
        <Boton tono="principal" icono="mas" disabled={usados.size >= SERVICIOS.length} onClick={() => abrir({
          titulo: 'Crear tarifa', exito: 'Tarifa creada', despues: tarifas.recargar,
          campos: [
            { nombre: 'servicio', etiqueta: 'Servicio', tipo: 'select', requerido: true, opciones: SERVICIOS.filter((s) => !usados.has(s.valor)) },
            { nombre: 'nombre', etiqueta: 'Nombre', requerido: true },
            { nombre: 'precio', etiqueta: 'Precio en pesos', tipo: 'number', minimo: 0, requerido: true },
          ],
          enviar: (v) => api('facturacion', '/api/tarifas', 'POST', { ...v, precio: Number(v.precio) }),
        })}>Crear tarifa</Boton>
      </Encabezado>
      <Tabla filas={tarifas.datos} cargando={tarifas.cargando} error={tarifas.error} clave={(t) => t.id} vacio="Aún no hay tarifas."
        columnas={[
          { titulo: 'Servicio', valor: (t) => SERVICIOS.find((s) => s.valor === t.servicio)?.texto ?? t.servicio },
          { titulo: 'Nombre', valor: (t) => t.nombre },
          { titulo: 'Precio', valor: (t) => pesos(t.precio), alinear: 'derecha' },
          { titulo: 'Actualizada', valor: (t) => fechaHora(t.actualizadaEn) },
        ]}
        acciones={(t) => <Boton onClick={() => abrir({
          titulo: `Editar ${t.nombre}`, exito: 'Tarifa actualizada', despues: tarifas.recargar,
          inicial: { nombre: t.nombre, precio: t.precio },
          campos: [
            { nombre: 'nombre', etiqueta: 'Nombre', requerido: true },
            { nombre: 'precio', etiqueta: 'Precio en pesos', tipo: 'number', minimo: 0, requerido: true },
          ],
          enviar: (v) => api('facturacion', `/api/tarifas/${t.id}`, 'PUT', { servicio: t.servicio, nombre: v.nombre, precio: Number(v.precio) }),
        })}>Editar</Boton>} />
      {elemento}
    </>
  );
}
