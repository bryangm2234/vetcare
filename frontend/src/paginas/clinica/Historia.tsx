import { useRef } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { api, descargar, subirArchivo } from '../../api/cliente';
import { useAvisos } from '../../componentes/avisos';
import { useElegirMascota } from '../../componentes/ElegirMascota';
import { useDialogo } from '../../componentes/Formulario';
import { useDatos } from '../../componentes/ganchos';
import { Tabla } from '../../componentes/Tabla';
import { Boton, Encabezado, Estado, Seccion } from '../../componentes/ui';
import { fecha, fechaHora } from '../../util';

export function HistoriaBuscar() {
  const navegar = useNavigate();
  const { elegirMascota, elementoMascota } = useElegirMascota();
  return (
    <>
      <Encabezado titulo="Historia clínica" descripcion="Busca a la mascota por su propietario para consultar sus antecedentes.">
        <Boton tono="principal" icono="buscar" onClick={() => elegirMascota('Buscar mascota', (m) => navegar(`/historia/${m.id}`))}>Buscar mascota</Boton>
      </Encabezado>
      {elementoMascota}
    </>
  );
}

export function Historia() {
  const { mascotaId } = useParams();
  const historia = useDatos(() => api('historia', `/api/historias/${mascotaId}`), [mascotaId]);
  const mascota = useDatos(() => api('pacientes', `/api/mascotas/${mascotaId}`), [mascotaId]);
  const { abrir, elemento } = useDialogo();
  const avisos = useAvisos();
  const selector = useRef<HTMLInputElement>(null);
  const h = historia.datos;

  const adjuntar = async (archivo?: File) => {
    if (!archivo) {
      return;
    }
    try {
      await subirArchivo('historia', `/api/historias/${mascotaId}/adjuntos`, archivo);
      avisos.exito('Archivo adjuntado');
      historia.recargar();
    } catch (causa) {
      avisos.error(causa);
    }
    if (selector.current) {
      selector.current.value = '';
    }
  };

  if (historia.error) {
    return <><Encabezado titulo="Historia clínica" /><p className="mensaje-error" role="alert">{historia.error}</p></>;
  }
  return (
    <>
      <Encabezado titulo={mascota.datos ? `Historia de ${mascota.datos.nombre}` : 'Historia clínica'} descripcion={mascota.datos ? `${mascota.datos.especie}, ${mascota.datos.raza} · ${mascota.datos.pesoKg} kg` : undefined}>
        <Boton icono="mas" onClick={() => selector.current?.click()}>Adjuntar archivo</Boton>
        <input ref={selector} type="file" hidden accept="application/pdf,image/*" aria-label="Archivo para adjuntar" onChange={(e) => adjuntar(e.target.files?.[0])} />
      </Encabezado>
      {(h?.avisos ?? []).map((a: string) => <p key={a} className="mensaje-info" style={{ marginBottom: 14 }}>{a}</p>)}
      {(h?.alergias?.length ?? 0) > 0 && <p className="mensaje-error" style={{ marginBottom: 14 }}><strong>Alergias:</strong> {h.alergias.join(', ')}</p>}
      {(h?.condicionesCronicas?.length ?? 0) > 0 && <p className="mensaje-info" style={{ marginBottom: 14 }}><strong>Condiciones crónicas:</strong> {h.condicionesCronicas.join(', ')}</p>}
      <Seccion titulo="Consultas">
        <Tabla filas={h?.consultas} cargando={historia.cargando} clave={(c: any) => c.id} vacio="Sin consultas."
          columnas={[{ titulo: 'Fecha', valor: (c: any) => fechaHora(c.fechaApertura) }, { titulo: 'Diagnóstico', valor: (c: any) => c.diagnostico ?? 'Sin registrar' }, { titulo: 'Plan', valor: (c: any) => c.planTratamiento ?? '—' }, { titulo: 'Estado', celda: (c: any) => <Estado valor={c.estado} /> }]} />
      </Seccion>
      <div className="columnas">
        <Seccion titulo="Vacunas">
          <Tabla filas={h?.vacunas} clave={(v: any) => v.id} vacio="Sin vacunas."
            columnas={[{ titulo: 'Vacuna', valor: (v: any) => `${v.vacuna}, dosis ${v.dosis}` }, { titulo: 'Fecha', valor: (v: any) => fecha(v.fecha) }, { titulo: 'Próxima', valor: (v: any) => fecha(v.proximaDosis) }]} />
        </Seccion>
        <Seccion titulo="Laboratorio">
          <Tabla filas={h?.laboratorio} clave={(o: any) => o.id} vacio="Sin exámenes."
            columnas={[{ titulo: 'Examen', valor: (o: any) => o.examen }, { titulo: 'Resultado', valor: (o: any) => o.resultado ?? 'Pendiente' }, { titulo: 'Estado', celda: (o: any) => <Estado valor={o.estado} /> }]} />
        </Seccion>
      </div>
      <Seccion titulo="Fórmulas">
        <Tabla filas={h?.formulas} clave={(f: any) => f.id} vacio="Sin fórmulas."
          columnas={[
            { titulo: 'Medicamentos', valor: (f: any) => f.items.map((i: any) => `${i.medicamento}: ${i.dosis}, ${i.frecuencia} (${i.pendiente} de ${i.cantidad} pendientes)`).join('; ') },
            { titulo: 'Vigente hasta', valor: (f: any) => fecha(f.vigenteHasta) },
            { titulo: 'Estado', celda: (f: any) => <Estado valor={f.estado} /> },
          ]}
          acciones={(f: any) => (f.estado === 'VIGENTE' || f.estado === 'PARCIAL') ? (
            <Boton onClick={() => abrir({
              titulo: 'Anular fórmula', descripcion: 'Las cantidades pendientes dejarán de poder despacharse.', textoEnviar: 'Anular', peligro: true, exito: 'Fórmula anulada', despues: historia.recargar,
              campos: [{ nombre: 'motivo', etiqueta: 'Motivo', requerido: true }],
              enviar: (v) => api('historia', `/api/formulas/${f.id}/anulacion`, 'PATCH', v),
            })}>Anular</Boton>
          ) : null} />
      </Seccion>
      <div className="columnas">
        <Seccion titulo="Hospitalizaciones">
          <Tabla filas={h?.hospitalizaciones} clave={(x: any) => x.id} vacio="Sin hospitalizaciones."
            columnas={[{ titulo: 'Ingreso', valor: (x: any) => fechaHora(x.fechaIngreso) }, { titulo: 'Motivo', valor: (x: any) => x.motivo }, { titulo: 'Estado', celda: (x: any) => <Estado valor={x.estado} /> }]} />
        </Seccion>
        <Seccion titulo="Adjuntos">
          <Tabla filas={h?.adjuntos} clave={(a: any) => a.id} vacio="Sin adjuntos."
            columnas={[{ titulo: 'Archivo', valor: (a: any) => a.nombre }, { titulo: 'Fecha', valor: (a: any) => fechaHora(a.fecha) }]}
            acciones={(a: any) => <Boton icono="descargar" onClick={() => descargar('historia', `/api/adjuntos/${a.id}`, a.nombre).catch(avisos.error)}>Descargar</Boton>} />
        </Seccion>
      </div>
      {elemento}
    </>
  );
}
