import { api } from '../../api/cliente';
import { useDialogo } from '../../componentes/Formulario';
import { useDatos } from '../../componentes/ganchos';
import { Tabla } from '../../componentes/Tabla';
import { Boton, Encabezado } from '../../componentes/ui';

export function Proveedores() {
  const proveedores = useDatos(() => api<any[]>('farmacia', '/api/proveedores'));
  const { abrir, elemento } = useDialogo();
  return (
    <>
      <Encabezado titulo="Proveedores">
        <Boton tono="principal" icono="mas" onClick={() => abrir({
          titulo: 'Registrar proveedor', exito: 'Proveedor registrado', despues: proveedores.recargar,
          campos: [
            { nombre: 'nit', etiqueta: 'NIT', requerido: true },
            { nombre: 'nombre', etiqueta: 'Nombre', requerido: true },
            { nombre: 'telefono', etiqueta: 'Teléfono', requerido: true },
            { nombre: 'correo', etiqueta: 'Correo', tipo: 'email', requerido: true },
          ],
          enviar: (v) => api('farmacia', '/api/proveedores', 'POST', v),
        })}>Registrar proveedor</Boton>
      </Encabezado>
      <Tabla filas={proveedores.datos} cargando={proveedores.cargando} error={proveedores.error} clave={(p) => p.id} buscador="Buscar proveedor" vacio="Aún no hay proveedores."
        columnas={[{ titulo: 'NIT', valor: (p) => p.nit }, { titulo: 'Nombre', valor: (p) => p.nombre }, { titulo: 'Teléfono', valor: (p) => p.telefono }, { titulo: 'Correo', valor: (p) => p.correo }]} />
      {elemento}
    </>
  );
}
