import { useState } from 'react';
import { api, type Servicio } from '../../api/cliente';
import { useDatos } from '../../componentes/ganchos';
import { Tabla } from '../../componentes/Tabla';
import { CampoFiltro, Encabezado } from '../../componentes/ui';
import { consulta, fechaHora } from '../../util';

const SERVICIOS: { valor: Servicio; texto: string }[] = [
  { valor: 'pacientes', texto: 'Pacientes y usuarios' },
  { valor: 'agenda', texto: 'Agenda' },
  { valor: 'historia', texto: 'Historia clínica' },
  { valor: 'farmacia', texto: 'Farmacia' },
  { valor: 'facturacion', texto: 'Facturación' },
];

export function Auditoria() {
  const [servicio, setServicio] = useState<Servicio>('pacientes');
  const [usuario, setUsuario] = useState('');
  const [accion, setAccion] = useState('');
  const [pagina, setPagina] = useState(0);
  const { datos, cargando, error } = useDatos(() => api(servicio, '/api/auditoria' + consulta({ usuario, accion, page: pagina, size: 15 })), [servicio, usuario, accion, pagina]);
  return (
    <>
      <Encabezado titulo="Auditoría" descripcion="Cada microservicio registra sus accesos y operaciones críticas." />
      <Tabla filas={datos?.contenido} cargando={cargando} error={error} clave={(a: any) => a.id} vacio="No hay registros con estos filtros."
        servidor={datos ? { pagina, totalPaginas: Math.max(datos.totalPaginas, 1), total: datos.totalElementos, cambiar: setPagina } : undefined}
        filtros={<>
          <CampoFiltro etiqueta="Servicio">
            <select value={servicio} onChange={(e) => { setServicio(e.target.value as Servicio); setPagina(0); }}>
              {SERVICIOS.map((s) => <option key={s.valor} value={s.valor}>{s.texto}</option>)}
            </select>
          </CampoFiltro>
          <CampoFiltro etiqueta="Usuario"><input value={usuario} onChange={(e) => { setUsuario(e.target.value); setPagina(0); }} placeholder="correo exacto" /></CampoFiltro>
          <CampoFiltro etiqueta="Acción"><input value={accion} onChange={(e) => { setAccion(e.target.value.toUpperCase()); setPagina(0); }} placeholder="INICIO_SESION" /></CampoFiltro>
        </>}
        columnas={[
          { titulo: 'Fecha', valor: (a: any) => fechaHora(a.fecha) },
          { titulo: 'Usuario', valor: (a: any) => a.usuario },
          { titulo: 'Acción', valor: (a: any) => a.accion },
          { titulo: 'Recurso', valor: (a: any) => a.recurso },
          { titulo: 'Resultado', valor: (a: any) => a.resultado },
        ]} />
    </>
  );
}
