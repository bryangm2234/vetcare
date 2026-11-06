import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../../api/cliente';
import { useAvisos } from '../../componentes/avisos';
import { useDatos } from '../../componentes/ganchos';
import { Tabla } from '../../componentes/Tabla';
import { Boton, CampoFiltro, Encabezado, Estado } from '../../componentes/ui';
import { hora, hoy } from '../../util';

export function MiAgenda() {
  const [dia, setDia] = useState(hoy());
  const agenda = useDatos(() => api<any[]>('agenda', `/api/citas/mi-agenda?fecha=${dia}`), [dia]);
  const navegar = useNavigate();
  const avisos = useAvisos();

  const abrirConsulta = async (cita: any) => {
    try {
      const consulta = await api('historia', '/api/consultas', 'POST', { citaId: cita.id });
      navegar(`/consultas/${consulta.id}`);
    } catch (causa) {
      avisos.error(causa);
    }
  };

  return (
    <>
      <Encabezado titulo="Mi agenda" descripcion="Abre la consulta cuando el paciente esté en espera." />
      <Tabla filas={agenda.datos} cargando={agenda.cargando} error={agenda.error} clave={(c) => c.id} vacio="No tienes citas en esta fecha."
        filtros={<CampoFiltro etiqueta="Fecha"><input type="date" value={dia} onChange={(e) => setDia(e.target.value)} /></CampoFiltro>}
        columnas={[
          { titulo: 'Hora', valor: (c) => hora(c.hora) },
          { titulo: 'Mascota', valor: (c) => c.mascotaNombre },
          { titulo: 'Motivo', valor: (c) => c.motivo },
          { titulo: 'Estado', celda: (c) => <Estado valor={c.estado} /> },
        ]}
        acciones={(c) => <>
          {c.estado === 'EN_ESPERA' && <Boton tono="principal" onClick={() => abrirConsulta(c)}>Abrir consulta</Boton>}
          <Boton onClick={() => navegar(`/historia/${c.mascotaId}`)}>Historia</Boton>
        </>} />
    </>
  );
}
