import { useState } from 'react';
import { api } from '../../api/cliente';
import { NOMBRE_ROL, type Rol } from '../../auth/roles';
import { useUsuario } from '../../auth/sesion';
import { useDialogo } from '../../componentes/Formulario';
import { useDatos } from '../../componentes/ganchos';
import { Tabla } from '../../componentes/Tabla';
import { Boton, CampoFiltro, Encabezado, Estado } from '../../componentes/ui';
import { consulta } from '../../util';

const ROLES = (Object.keys(NOMBRE_ROL) as Rol[]).map((r) => ({ valor: r, texto: NOMBRE_ROL[r] }));

export function Usuarios() {
  const yo = useUsuario();
  const [rol, setRol] = useState('');
  const [estado, setEstado] = useState('');
  const [pagina, setPagina] = useState(0);
  const { datos, cargando, error, recargar } = useDatos(() => api('pacientes', '/api/usuarios' + consulta({ rol, estado, page: pagina, size: 10 })), [rol, estado, pagina]);
  const { abrir, elemento } = useDialogo();

  const nuevo = () => abrir({
    titulo: 'Crear usuario',
    exito: 'Usuario creado',
    textoEnviar: 'Crear',
    despues: recargar,
    campos: [
      { nombre: 'nombre', etiqueta: 'Nombre', requerido: true },
      { nombre: 'correo', etiqueta: 'Correo', tipo: 'email', requerido: true },
      { nombre: 'contrasena', etiqueta: 'Contraseña inicial', tipo: 'password', requerido: true, ayuda: 'Mínimo 8 caracteres' },
      { nombre: 'rol', etiqueta: 'Rol', tipo: 'select', requerido: true, opciones: ROLES },
      {
        nombre: 'propietarioId', etiqueta: 'Propietario vinculado', tipo: 'select', requerido: true, visible: (v) => v.rol === 'PROPIETARIO',
        opcionesDe: async () => (await api('pacientes', '/api/propietarios?size=100')).contenido.map((p: any) => ({ valor: p.id, texto: `${p.nombre} (${p.documento})` })),
      },
      {
        nombre: 'proveedorId', etiqueta: 'Proveedor vinculado', tipo: 'select', requerido: true, visible: (v) => v.rol === 'PROVEEDOR',
        opcionesDe: async () => (await api<any[]>('farmacia', '/api/proveedores')).map((p) => ({ valor: p.id, texto: p.nombre })),
      },
    ],
    enviar: ({ propietarioId, proveedorId, ...v }) => api('pacientes', '/api/usuarios', 'POST', { ...v, propietarioId: propietarioId || null, proveedorId: proveedorId || null }),
  });

  return (
    <>
      <Encabezado titulo="Usuarios" descripcion="Crea y controla las cuentas de los 10 roles.">
        <Boton tono="principal" icono="mas" onClick={nuevo}>Crear usuario</Boton>
      </Encabezado>
      <Tabla
        filas={datos?.contenido} cargando={cargando} error={error} clave={(u: any) => u.id}
        servidor={datos ? { pagina, totalPaginas: Math.max(datos.totalPaginas, 1), total: datos.totalElementos, cambiar: setPagina } : undefined}
        filtros={<>
          <CampoFiltro etiqueta="Rol">
            <select value={rol} onChange={(e) => { setRol(e.target.value); setPagina(0); }}>
              <option value="">Todos</option>
              {ROLES.map((r) => <option key={r.valor} value={r.valor}>{r.texto}</option>)}
            </select>
          </CampoFiltro>
          <CampoFiltro etiqueta="Estado">
            <select value={estado} onChange={(e) => { setEstado(e.target.value); setPagina(0); }}>
              <option value="">Todos</option>
              <option value="ACTIVO">Activo</option>
              <option value="INACTIVO">Inactivo</option>
            </select>
          </CampoFiltro>
        </>}
        columnas={[
          { titulo: 'Nombre', valor: (u: any) => u.nombre },
          { titulo: 'Correo', valor: (u: any) => u.correo },
          { titulo: 'Rol', valor: (u: any) => NOMBRE_ROL[u.rol as Rol] },
          { titulo: 'Estado', celda: (u: any) => <Estado valor={u.estado} /> },
        ]}
        acciones={(u: any) => u.estado === 'ACTIVO' && u.id !== yo.usuarioId ? (
          <Boton onClick={() => abrir({
            titulo: `Desactivar a ${u.nombre}`,
            descripcion: 'No podrá volver a iniciar sesión. Si tiene una sesión abierta, seguirá activa hasta que expire, en máximo 30 minutos.',
            textoEnviar: 'Desactivar', peligro: true, exito: 'Usuario desactivado', despues: recargar,
            enviar: () => api('pacientes', `/api/usuarios/${u.id}/desactivar`, 'PATCH'),
          })}>Desactivar</Boton>
        ) : null}
      />
      {elemento}
    </>
  );
}
