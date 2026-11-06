import { Link, useParams } from 'react-router-dom';
import { api } from '../../api/cliente';
import { useDialogo } from '../../componentes/Formulario';
import { useDatos } from '../../componentes/ganchos';
import { Tabla } from '../../componentes/Tabla';
import { Boton, Encabezado, Estado, Seccion } from '../../componentes/ui';
import { fecha, hoy } from '../../util';

export function ConsultaDetalle() {
  const { id } = useParams();
  const consulta = useDatos(() => api('historia', `/api/consultas/${id}`), [id]);
  const c = consulta.datos;
  const historia = useDatos(() => (c ? api('historia', `/api/historias/${c.mascotaId}`) : Promise.resolve(undefined)), [c?.mascotaId, c?.estado]);
  const { abrir, elemento } = useDialogo();
  const abierta = c?.estado === 'ABIERTA';
  const ordenes = (historia.datos?.laboratorio ?? []).filter((o: any) => o.consultaId === id);
  const formulas = (historia.datos?.formulas ?? []).filter((f: any) => f.consultaId === id);
  const recargarTodo = () => { consulta.recargar(); historia.recargar(); };

  if (consulta.error) {
    return <><Encabezado titulo="Consulta" /><p className="mensaje-error" role="alert">{consulta.error}</p></>;
  }
  if (!c) {
    return <Encabezado titulo="Consulta" descripcion="Cargando…" />;
  }

  const registro = () => abrir({
    titulo: 'Registro clínico', exito: 'Registro guardado', despues: recargarTodo,
    inicial: { anamnesis: c.anamnesis ?? '', examenFisico: c.examenFisico ?? '', diagnostico: c.diagnostico ?? '', planTratamiento: c.planTratamiento ?? '' },
    campos: [
      { nombre: 'anamnesis', etiqueta: 'Anamnesis', tipo: 'textarea', requerido: true },
      { nombre: 'examenFisico', etiqueta: 'Examen físico', tipo: 'textarea', requerido: true },
      { nombre: 'diagnostico', etiqueta: 'Diagnóstico', tipo: 'textarea', requerido: true },
      { nombre: 'planTratamiento', etiqueta: 'Plan de tratamiento', tipo: 'textarea', requerido: true },
    ],
    enviar: (v) => api('historia', `/api/consultas/${id}/registro-clinico`, 'PUT', v),
  });

  const orden = () => abrir({
    titulo: 'Orden de laboratorio', exito: 'Orden creada', despues: historia.recargar, textoEnviar: 'Crear orden',
    campos: [{ nombre: 'examen', etiqueta: 'Examen', requerido: true }, { nombre: 'indicaciones', etiqueta: 'Indicaciones' }],
    enviar: (v) => api('historia', `/api/consultas/${id}/ordenes-laboratorio`, 'POST', v),
  });

  const formula = async () => {
    const catalogo = (await api('farmacia', '/api/medicamentos?size=100')).contenido;
    abrir({
      titulo: 'Emitir fórmula', exito: 'Fórmula emitida', despues: historia.recargar, textoEnviar: 'Emitir',
      inicial: { vigenteHasta: hoy(15) },
      campos: [
        { nombre: 'vigenteHasta', etiqueta: 'Vigente hasta', tipo: 'date', requerido: true },
        {
          nombre: 'items', etiqueta: 'Medicamentos', tipo: 'filas', subcampos: [
            { nombre: 'medicamentoId', etiqueta: 'Medicamento', tipo: 'select', requerido: true, opciones: catalogo.map((m: any) => ({ valor: m.id, texto: m.nombre })) },
            { nombre: 'dosis', etiqueta: 'Dosis', requerido: true },
            { nombre: 'frecuencia', etiqueta: 'Frecuencia', requerido: true },
            { nombre: 'duracion', etiqueta: 'Duración', requerido: true },
            { nombre: 'cantidad', etiqueta: 'Cantidad', tipo: 'number', minimo: 1, requerido: true },
          ],
        },
      ],
      enviar: (v) => api('historia', '/api/formulas', 'POST', {
        consultaId: id, vigenteHasta: v.vigenteHasta, items: v.items.map((i: any) => ({ ...i, cantidad: Number(i.cantidad) })),
      }),
    });
  };

  const alergias: string[] = historia.datos?.alergias ?? [];
  return (
    <>
      <Encabezado titulo="Consulta" descripcion={<Link to={`/historia/${c.mascotaId}`}>Ver historia clínica</Link>}>
        {abierta && <>
          <Boton tono="principal" onClick={registro}>Registro clínico</Boton>
          <Boton onClick={orden}>Orden de laboratorio</Boton>
          <Boton onClick={() => formula()}>Emitir fórmula</Boton>
          <Boton disabled={!c.diagnostico} title={c.diagnostico ? undefined : 'Registra primero el diagnóstico'} onClick={() => abrir({
            titulo: 'Cerrar consulta', descripcion: 'Quedará inmodificable y pasará a facturación.', textoEnviar: 'Cerrar consulta', peligro: true, exito: 'Consulta cerrada', despues: recargarTodo,
            enviar: () => api('historia', `/api/consultas/${id}/cierre`, 'PATCH'),
          })}>Cerrar consulta</Boton>
        </>}
      </Encabezado>
      {alergias.length > 0 && <p className="mensaje-error" role="alert" style={{ marginBottom: 14 }}><strong>Alergias:</strong> {alergias.join(', ')}</p>}
      {(historia.datos?.avisos ?? []).map((a: string) => <p key={a} className="mensaje-info" style={{ marginBottom: 14 }}>{a}</p>)}
      <div className="tarjeta tarjeta-contenido" style={{ marginBottom: 20 }}>
        <dl className="datos">
          <div><dt>Estado</dt><dd><Estado valor={c.estado} /></dd></div>
          <div><dt>Peso</dt><dd>{c.signosVitales ? `${c.signosVitales.pesoKg} kg` : 'Sin registrar'}</dd></div>
          <div><dt>Temperatura</dt><dd>{c.signosVitales ? `${c.signosVitales.temperatura} °C` : 'Sin registrar'}</dd></div>
          <div><dt>Frecuencia cardíaca</dt><dd>{c.signosVitales?.frecuenciaCardiaca ?? 'Sin registrar'}</dd></div>
          <div><dt>Frecuencia respiratoria</dt><dd>{c.signosVitales?.frecuenciaRespiratoria ?? 'Sin registrar'}</dd></div>
        </dl>
        <hr style={{ border: 0, borderTop: '1px solid var(--borde)', margin: '14px 0' }} />
        <dl className="datos">
          <div><dt>Anamnesis</dt><dd>{c.anamnesis ?? 'Sin registrar'}</dd></div>
          <div><dt>Examen físico</dt><dd>{c.examenFisico ?? 'Sin registrar'}</dd></div>
          <div><dt>Diagnóstico</dt><dd>{c.diagnostico ?? 'Sin registrar'}</dd></div>
          <div><dt>Plan de tratamiento</dt><dd>{c.planTratamiento ?? 'Sin registrar'}</dd></div>
        </dl>
      </div>
      <div className="columnas">
        <Seccion titulo="Órdenes de laboratorio">
          <Tabla filas={ordenes} clave={(o: any) => o.id} vacio="Sin órdenes."
            columnas={[{ titulo: 'Examen', valor: (o: any) => o.examen }, { titulo: 'Resultado', valor: (o: any) => o.resultado ?? 'Pendiente' }, { titulo: 'Estado', celda: (o: any) => <Estado valor={o.estado} /> }]} />
        </Seccion>
        <Seccion titulo="Fórmulas">
          <Tabla filas={formulas} clave={(f: any) => f.id} vacio="Sin fórmulas."
            columnas={[{ titulo: 'Medicamentos', valor: (f: any) => f.items.map((i: any) => `${i.medicamento} x ${i.cantidad}`).join(', ') }, { titulo: 'Vigente hasta', valor: (f: any) => fecha(f.vigenteHasta) }, { titulo: 'Estado', celda: (f: any) => <Estado valor={f.estado} /> }]} />
        </Seccion>
      </div>
      {elemento}
    </>
  );
}
