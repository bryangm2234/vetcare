import { useState } from 'react';
import { api, descargar } from '../api/cliente';
import { useAvisos } from '../componentes/avisos';
import { useDialogo } from '../componentes/Formulario';
import { useDatos } from '../componentes/ganchos';
import { Tabla } from '../componentes/Tabla';
import { Boton, Encabezado, Estado, Seccion } from '../componentes/ui';
import { fecha, fechaHora, hora, hoy, pesos } from '../util';
import { campoFranja, opcionesVeterinarios } from './recepcion/citaCampos';

function DetalleMascota({ mascota }: { mascota: any }) {
  const carne = useDatos(() => api<any[]>('historia', `/api/portal/mascotas/${mascota.id}/carne`), [mascota.id]);
  const clinico = useDatos(() => api('historia', `/api/portal/mascotas/${mascota.id}/resultados-formulas`), [mascota.id]);
  return (
    <>
      <Seccion titulo={`Carné de vacunación de ${mascota.nombre}`}>
        <Tabla filas={carne.datos} cargando={carne.cargando} error={carne.error} clave={(v) => v.id} vacio="Aún no tiene vacunas registradas."
          columnas={[{ titulo: 'Vacuna', valor: (v) => v.vacuna }, { titulo: 'Dosis', valor: (v) => v.dosis, alinear: 'derecha' }, { titulo: 'Fecha', valor: (v) => fecha(v.fecha) }, { titulo: 'Próxima dosis', valor: (v) => fecha(v.proximaDosis) }]} />
      </Seccion>
      <div className="columnas">
        <Seccion titulo="Resultados de laboratorio">
          <Tabla filas={clinico.datos?.resultados} cargando={clinico.cargando} error={clinico.error} clave={(o: any) => o.id} vacio="Sin resultados."
            columnas={[{ titulo: 'Examen', valor: (o: any) => o.examen }, { titulo: 'Resultado', valor: (o: any) => o.resultado }, { titulo: 'Fecha', valor: (o: any) => fechaHora(o.fechaResultado) }]} />
        </Seccion>
        <Seccion titulo="Fórmulas">
          <Tabla filas={clinico.datos?.formulas} cargando={clinico.cargando} clave={(f: any) => f.id} vacio="Sin fórmulas."
            columnas={[{ titulo: 'Medicamentos', valor: (f: any) => f.items.map((i: any) => `${i.medicamento}: ${i.dosis}, ${i.frecuencia}, ${i.duracion}`).join('; ') }, { titulo: 'Vigente hasta', valor: (f: any) => fecha(f.vigenteHasta) }, { titulo: 'Estado', celda: (f: any) => <Estado valor={f.estado} /> }]} />
        </Seccion>
      </div>
    </>
  );
}

export function MisMascotas() {
  const mascotas = useDatos(() => api<any[]>('pacientes', '/api/portal/mascotas'));
  const [elegida, setElegida] = useState<any>(null);
  return (
    <>
      <Encabezado titulo="Mis mascotas" />
      <Seccion titulo="Mascotas">
        <Tabla filas={mascotas.datos} cargando={mascotas.cargando} error={mascotas.error} clave={(m) => m.id} vacio="No tienes mascotas registradas."
          columnas={[{ titulo: 'Nombre', valor: (m) => m.nombre }, { titulo: 'Especie y raza', valor: (m) => `${m.especie}, ${m.raza}` }, { titulo: 'Estado', celda: (m) => <Estado valor={m.estado} /> }]}
          acciones={(m) => <Boton onClick={() => setElegida(m)}>Ver carné y resultados</Boton>} />
      </Seccion>
      {elegida && <DetalleMascota mascota={elegida} />}
    </>
  );
}

export function MisCitas() {
  const citas = useDatos(() => api<any[]>('agenda', '/api/portal/citas'));
  const mascotas = useDatos(() => api<any[]>('pacientes', '/api/portal/mascotas'));
  const { abrir, elemento } = useDialogo();
  return (
    <>
      <Encabezado titulo="Mis citas">
        <Boton tono="principal" icono="mas" disabled={!mascotas.datos?.length} onClick={() => abrir({
          titulo: 'Solicitar cita', exito: 'Cita solicitada', despues: citas.recargar, textoEnviar: 'Solicitar',
          inicial: { fecha: hoy(1) },
          campos: [
            { nombre: 'mascotaId', etiqueta: 'Mascota', tipo: 'select', requerido: true, opciones: (mascotas.datos ?? []).filter((m) => m.estado === 'ACTIVA').map((m) => ({ valor: m.id, texto: m.nombre })) },
            { nombre: 'veterinarioId', etiqueta: 'Veterinario', tipo: 'select', requerido: true, opcionesDe: opcionesVeterinarios },
            { nombre: 'fecha', etiqueta: 'Fecha', tipo: 'date', requerido: true },
            campoFranja,
            { nombre: 'motivo', etiqueta: 'Motivo', requerido: true },
          ],
          enviar: (v) => api('agenda', '/api/portal/citas', 'POST', v),
        })}>Solicitar cita</Boton>
      </Encabezado>
      <Tabla filas={citas.datos} cargando={citas.cargando} error={citas.error} clave={(c) => c.id} vacio="No tienes citas próximas."
        columnas={[{ titulo: 'Fecha', valor: (c) => fecha(c.fecha) }, { titulo: 'Hora', valor: (c) => hora(c.hora) }, { titulo: 'Mascota', valor: (c) => c.mascotaNombre }, { titulo: 'Motivo', valor: (c) => c.motivo }, { titulo: 'Estado', celda: (c) => <Estado valor={c.estado} /> }]} />
      {elemento}
    </>
  );
}

export function MisFacturas() {
  const facturas = useDatos(() => api<any[]>('facturacion', '/api/portal/facturas'));
  const avisos = useAvisos();
  return (
    <>
      <Encabezado titulo="Mis facturas" />
      <Tabla filas={facturas.datos} cargando={facturas.cargando} error={facturas.error} clave={(f) => f.id} vacio="Aún no tienes facturas."
        columnas={[
          { titulo: 'Número', valor: (f) => f.numero },
          { titulo: 'Fecha', valor: (f) => fechaHora(f.fecha) },
          { titulo: 'Mascota', valor: (f) => f.mascotaNombre },
          { titulo: 'Total', valor: (f) => pesos(f.total), alinear: 'derecha' },
          { titulo: 'Saldo', valor: (f) => pesos(f.saldo), alinear: 'derecha' },
          { titulo: 'Estado', celda: (f) => <Estado valor={f.estado} /> },
        ]}
        acciones={(f) => <Boton icono="descargar" onClick={() => descargar('facturacion', `/api/portal/facturas/${f.id}/pdf`, `${f.numero}.pdf`).catch(avisos.error)}>PDF</Boton>} />
    </>
  );
}
