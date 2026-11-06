import { api } from '../../api/cliente';
import { useUsuario } from '../../auth/sesion';
import { useElegirMascota } from '../../componentes/ElegirMascota';
import { useDialogo } from '../../componentes/Formulario';
import { useDatos } from '../../componentes/ganchos';
import { Tabla } from '../../componentes/Tabla';
import { Boton, Encabezado } from '../../componentes/ui';
import { fechaHora } from '../../util';

export function Hospitalizacion() {
  const { rol } = useUsuario();
  const activas = useDatos(() => api<any[]>('historia', '/api/hospitalizaciones/activas'));
  const { abrir, elemento } = useDialogo();
  const { elegirMascota, elementoMascota } = useElegirMascota();
  const veterinario = rol === 'VETERINARIO';

  const evolucion = async (h: any) => {
    const despachados = await api<any[]>('historia', `/api/hospitalizaciones/${h.id}/medicamentos-despachados`);
    abrir({
      titulo: 'Registrar evolución', exito: 'Evolución registrada', despues: activas.recargar,
      campos: [
        { nombre: 'observaciones', etiqueta: 'Observaciones', tipo: 'textarea', requerido: true },
        { nombre: 'temperatura', etiqueta: 'Temperatura en °C', tipo: 'number' },
        ...despachados.map((m) => ({ nombre: `med-${m.medicamentoId}`, etiqueta: `Se administró ${m.medicamento}`, tipo: 'checkbox' as const })),
      ],
      enviar: (v) => api('historia', `/api/hospitalizaciones/${h.id}/evoluciones`, 'POST', {
        observaciones: v.observaciones,
        temperatura: v.temperatura ? Number(v.temperatura) : null,
        medicamentosAdministrados: despachados.filter((m) => v[`med-${m.medicamentoId}`]).map((m) => m.medicamentoId),
      }),
    });
  };

  return (
    <>
      <Encabezado titulo="Hospitalización" descripcion="Pacientes internados y su seguimiento.">
        {veterinario && <Boton tono="principal" icono="mas" onClick={() => elegirMascota('Hospitalizar: elige la mascota', (m) => abrir({
          titulo: `Hospitalizar a ${m.nombre}`, exito: 'Hospitalización registrada', despues: activas.recargar, textoEnviar: 'Hospitalizar',
          campos: [{ nombre: 'motivo', etiqueta: 'Motivo', tipo: 'textarea', requerido: true }, { nombre: 'cubiculo', etiqueta: 'Cubículo', requerido: true }],
          enviar: (v) => api('historia', '/api/hospitalizaciones', 'POST', { ...v, mascotaId: m.id }),
        }))}>Hospitalizar</Boton>}
      </Encabezado>
      <Tabla filas={activas.datos} cargando={activas.cargando} error={activas.error} clave={(h) => h.id} vacio="No hay pacientes hospitalizados."
        columnas={[
          { titulo: 'Cubículo', valor: (h) => h.cubiculo },
          { titulo: 'Ingreso', valor: (h) => fechaHora(h.fechaIngreso) },
          { titulo: 'Motivo', valor: (h) => h.motivo },
          { titulo: 'Última evolución', valor: (h) => h.evoluciones.at(-1)?.observaciones ?? 'Sin evoluciones' },
        ]}
        acciones={(h) => <>
          {rol === 'AUXILIAR_VETERINARIO' && <Boton tono="principal" onClick={() => evolucion(h)}>Evolución</Boton>}
          {veterinario && <Boton onClick={() => abrir({
            titulo: 'Dar de alta', descripcion: 'Libera el cubículo y pasa la estancia a facturación.', textoEnviar: 'Dar de alta', exito: 'Alta registrada', despues: activas.recargar,
            campos: [{ nombre: 'indicaciones', etiqueta: 'Indicaciones', tipo: 'textarea', requerido: true }],
            enviar: (v) => api('historia', `/api/hospitalizaciones/${h.id}/alta`, 'PATCH', v),
          })}>Dar de alta</Boton>}
        </>} />
      {elementoMascota}
      {elemento}
    </>
  );
}
