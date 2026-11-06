import { useState } from 'react';
import { api } from '../../api/cliente';
import { useUsuario } from '../../auth/sesion';
import { useElegirMascota } from '../../componentes/ElegirMascota';
import { useDialogo } from '../../componentes/Formulario';
import { useDatos } from '../../componentes/ganchos';
import { Tabla } from '../../componentes/Tabla';
import { Boton, CampoFiltro, Encabezado, Estado } from '../../componentes/ui';
import { hora, hoy } from '../../util';
import { campoFranja, opcionesVeterinarios } from './citaCampos';

export function Citas() {
  const { rol } = useUsuario();
  const [dia, setDia] = useState(hoy());
  const citas = useDatos(() => api<any[]>('agenda', `/api/citas?fecha=${dia}`), [dia]);
  const horarios = useDatos(() => (rol === 'AUXILIAR_VETERINARIO' ? Promise.resolve([]) : api<any[]>('agenda', '/api/horarios')));
  const { abrir, elemento } = useDialogo();
  const { elegirMascota, elementoMascota } = useElegirMascota();
  const recepcion = rol === 'RECEPCIONISTA';
  const nombreVet = (id: string) => horarios.datos?.find((h) => h.id === id)?.nombre ?? '—';

  const agendar = () => elegirMascota('Agendar cita: elige la mascota', (m) => abrir({
    titulo: `Agendar cita de ${m.nombre}`, exito: 'Cita agendada', textoEnviar: 'Agendar', despues: citas.recargar,
    inicial: { fecha: dia },
    campos: [
      { nombre: 'veterinarioId', etiqueta: 'Veterinario', tipo: 'select', requerido: true, opcionesDe: opcionesVeterinarios },
      { nombre: 'fecha', etiqueta: 'Fecha', tipo: 'date', requerido: true },
      campoFranja,
      { nombre: 'motivo', etiqueta: 'Motivo', requerido: true },
    ],
    enviar: (v) => api('agenda', '/api/citas', 'POST', { ...v, mascotaId: m.id }),
  }));

  const reprogramar = (c: any) => abrir({
    titulo: `Reprogramar cita de ${c.mascotaNombre}`, exito: 'Cita reprogramada', textoEnviar: 'Reprogramar', despues: citas.recargar,
    inicial: { fecha: c.fecha, veterinarioId: c.recursoId },
    campos: [{ nombre: 'fecha', etiqueta: 'Fecha', tipo: 'date', requerido: true }, campoFranja],
    enviar: (v) => api('agenda', `/api/citas/${c.id}/reprogramacion`, 'PUT', { fecha: v.fecha, hora: v.hora }),
  });

  const signos = (c: any) => abrir({
    titulo: `Signos vitales de ${c.mascotaNombre}`, exito: 'Signos vitales registrados',
    campos: [
      { nombre: 'pesoKg', etiqueta: 'Peso en kg', tipo: 'number', requerido: true },
      { nombre: 'temperatura', etiqueta: 'Temperatura en °C', tipo: 'number', requerido: true },
      { nombre: 'frecuenciaCardiaca', etiqueta: 'Frecuencia cardíaca', tipo: 'number', requerido: true },
      { nombre: 'frecuenciaRespiratoria', etiqueta: 'Frecuencia respiratoria', tipo: 'number', requerido: true },
    ],
    enviar: (v) => api('historia', `/api/citas/${c.id}/signos-vitales`, 'PUT', {
      pesoKg: Number(v.pesoKg), temperatura: Number(v.temperatura), frecuenciaCardiaca: Number(v.frecuenciaCardiaca), frecuenciaRespiratoria: Number(v.frecuenciaRespiratoria),
    }),
  });

  return (
    <>
      <Encabezado titulo="Citas" descripcion="Agenda, reprograma y registra la llegada de los pacientes.">
        {recepcion && <Boton tono="principal" icono="mas" onClick={agendar}>Agendar cita</Boton>}
      </Encabezado>
      <Tabla filas={citas.datos} cargando={citas.cargando} error={citas.error} clave={(c) => c.id} buscador="Buscar mascota o motivo" vacio="No hay citas en esta fecha."
        filtros={<CampoFiltro etiqueta="Fecha"><input type="date" value={dia} onChange={(e) => setDia(e.target.value)} /></CampoFiltro>}
        columnas={[
          { titulo: 'Hora', valor: (c) => hora(c.hora) },
          { titulo: 'Mascota', valor: (c) => c.mascotaNombre },
          ...(rol === 'AUXILIAR_VETERINARIO' ? [] : [{ titulo: 'Veterinario', valor: (c: any) => nombreVet(c.recursoId) }]),
          { titulo: 'Motivo', valor: (c) => c.motivo },
          { titulo: 'Estado', celda: (c) => <Estado valor={c.estado} /> },
        ]}
        acciones={(c) => <>
          {recepcion && c.estado === 'PROGRAMADA' && c.fecha === hoy() && (
            <Boton tono="principal" onClick={() => abrir({
              titulo: `Registrar llegada de ${c.mascotaNombre}`, textoEnviar: 'Registrar llegada', exito: 'Paciente en espera', despues: citas.recargar,
              enviar: () => api('agenda', `/api/citas/${c.id}/llegada`, 'PATCH'),
            })}>Llegó</Boton>
          )}
          {recepcion && c.estado === 'PROGRAMADA' && <Boton onClick={() => reprogramar(c)}>Reprogramar</Boton>}
          {recepcion && (c.estado === 'PROGRAMADA' || c.estado === 'EN_ESPERA') && (
            <Boton onClick={() => abrir({
              titulo: `Cancelar cita de ${c.mascotaNombre}`, descripcion: 'Se liberan el cupo y la franja.', textoEnviar: 'Cancelar cita', peligro: true, exito: 'Cita cancelada', despues: citas.recargar,
              campos: [{ nombre: 'motivo', etiqueta: 'Motivo', requerido: true }],
              enviar: (v) => api('agenda', `/api/citas/${c.id}/cancelacion`, 'PATCH', v),
            })}>Cancelar</Boton>
          )}
          {rol === 'AUXILIAR_VETERINARIO' && c.estado === 'EN_ESPERA' && <Boton tono="principal" onClick={() => signos(c)}>Signos vitales</Boton>}
        </>} />
      {elementoMascota}
      {elemento}
    </>
  );
}
