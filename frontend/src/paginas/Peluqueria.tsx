import { useState } from 'react';
import { api } from '../api/cliente';
import { useElegirMascota } from '../componentes/ElegirMascota';
import { useDialogo } from '../componentes/Formulario';
import { useDatos } from '../componentes/ganchos';
import { Tabla } from '../componentes/Tabla';
import { Boton, CampoFiltro, Encabezado, Estado } from '../componentes/ui';
import { hora, hoy } from '../util';

export function Peluqueria() {
  const [dia, setDia] = useState(hoy());
  const citas = useDatos(() => api<any[]>('agenda', `/api/peluqueria/citas?fecha=${dia}`), [dia]);
  const { abrir, elemento } = useDialogo();
  const { elegirMascota, elementoMascota } = useElegirMascota();

  const agendar = () => elegirMascota('Agendar servicio: elige la mascota', (m) => abrir({
    titulo: `Agendar servicio de ${m.nombre}`, exito: 'Servicio agendado', despues: citas.recargar, textoEnviar: 'Agendar',
    inicial: { fecha: dia },
    campos: [
      { nombre: 'fecha', etiqueta: 'Fecha', tipo: 'date', requerido: true },
      { nombre: 'hora', etiqueta: 'Hora', tipo: 'time', requerido: true, ayuda: 'Debe coincidir con el inicio de una franja del horario de peluquería.' },
      { nombre: 'servicio', etiqueta: 'Servicio', requerido: true },
    ],
    enviar: (v) => api('agenda', '/api/peluqueria/citas', 'POST', { ...v, mascotaId: m.id }),
  }));

  return (
    <>
      <Encabezado titulo="Peluquería" descripcion="Baño y estética, con cupos propios.">
        <Boton tono="principal" icono="mas" onClick={agendar}>Agendar servicio</Boton>
      </Encabezado>
      <Tabla filas={citas.datos} cargando={citas.cargando} error={citas.error} clave={(c) => c.id} vacio="No hay servicios en esta fecha."
        filtros={<CampoFiltro etiqueta="Fecha"><input type="date" value={dia} onChange={(e) => setDia(e.target.value)} /></CampoFiltro>}
        columnas={[
          { titulo: 'Hora', valor: (c) => hora(c.hora) },
          { titulo: 'Mascota', valor: (c) => c.mascotaNombre },
          { titulo: 'Servicio', valor: (c) => c.motivo },
          { titulo: 'Observaciones', valor: (c) => c.observaciones ?? '—' },
          { titulo: 'Estado', celda: (c) => <Estado valor={c.estado} /> },
        ]}
        acciones={(c) => c.estado === 'PROGRAMADA' ? <Boton tono="principal" onClick={() => abrir({
          titulo: `Servicio de ${c.mascotaNombre}`, descripcion: 'Queda pendiente de facturar.', exito: 'Servicio registrado', despues: citas.recargar, textoEnviar: 'Registrar',
          campos: [{ nombre: 'observaciones', etiqueta: 'Observaciones', tipo: 'textarea', requerido: true }],
          enviar: (v) => api('agenda', `/api/peluqueria/citas/${c.id}/servicio`, 'PATCH', v),
        })}>Registrar servicio</Boton> : null} />
      {elementoMascota}
      {elemento}
    </>
  );
}
