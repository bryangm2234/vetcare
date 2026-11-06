import { api } from '../../api/cliente';
import { useDialogo, type Campo, type Valores } from '../../componentes/Formulario';
import { useDatos } from '../../componentes/ganchos';
import { Tabla } from '../../componentes/Tabla';
import { Boton, Encabezado } from '../../componentes/ui';
import { hora } from '../../util';

const DIAS = [['MONDAY', 'Lun'], ['TUESDAY', 'Mar'], ['WEDNESDAY', 'Mié'], ['THURSDAY', 'Jue'], ['FRIDAY', 'Vie'], ['SATURDAY', 'Sáb'], ['SUNDAY', 'Dom']];

const camposComunes: Campo[] = [
  ...DIAS.map(([valor, texto]) => ({ nombre: valor, etiqueta: texto, tipo: 'checkbox' as const })),
  { nombre: 'horaInicio', etiqueta: 'Hora de inicio', tipo: 'time', requerido: true },
  { nombre: 'horaFin', etiqueta: 'Hora de fin', tipo: 'time', requerido: true },
  { nombre: 'duracionMinutos', etiqueta: 'Duración de la franja, en minutos', tipo: 'number', minimo: 5, requerido: true },
  { nombre: 'cuposDiarios', etiqueta: 'Cupos diarios', tipo: 'number', minimo: 1, requerido: true },
];

const cuerpo = (v: Valores, nombre: string) => ({
  nombre,
  dias: DIAS.map(([d]) => d).filter((d) => v[d]),
  horaInicio: v.horaInicio,
  horaFin: v.horaFin,
  duracionMinutos: Number(v.duracionMinutos),
  cuposDiarios: Number(v.cuposDiarios),
});

const iniciales = (h?: any): Valores => ({
  horaInicio: h ? hora(h.horaInicio) : '08:00',
  horaFin: h ? hora(h.horaFin) : '17:00',
  duracionMinutos: h?.duracionMinutos ?? 30,
  cuposDiarios: h?.cuposDiarios ?? 12,
  ...Object.fromEntries(DIAS.map(([d]) => [d, h ? h.dias.includes(d) : !['SATURDAY', 'SUNDAY'].includes(d)])),
});

export function Horarios() {
  const horarios = useDatos(() => api<any[]>('agenda', '/api/horarios'));
  const { abrir, elemento } = useDialogo();

  const configurar = async (horario?: any) => {
    const veterinarios = horario ? [] : (await api('pacientes', '/api/usuarios?rol=VETERINARIO&estado=ACTIVO&size=100')).contenido;
    abrir({
      titulo: horario ? `Horario de ${horario.nombre}` : 'Configurar horario',
      exito: 'Horario guardado',
      despues: horarios.recargar,
      inicial: iniciales(horario),
      campos: [
        ...(horario ? [] : [{ nombre: 'veterinario', etiqueta: 'Veterinario', tipo: 'select' as const, requerido: true, opciones: veterinarios.map((u: any) => ({ valor: u.id, texto: u.nombre })) }]),
        ...camposComunes,
      ],
      enviar: (v) => {
        const id = horario?.id ?? v.veterinario;
        const nombre = horario?.nombre ?? veterinarios.find((u: any) => u.id === id)?.nombre;
        return api('agenda', `/api/horarios/${id}`, 'PUT', cuerpo(v, nombre));
      },
    });
  };

  const peluqueria = () => abrir({
    titulo: 'Horario de peluquería',
    exito: 'Horario de peluquería guardado',
    inicial: iniciales(),
    campos: camposComunes,
    enviar: (v) => api('agenda', '/api/peluqueria/horario', 'PUT', cuerpo(v, 'Peluquería')),
  });

  return (
    <>
      <Encabezado titulo="Horarios y cupos" descripcion="Define cuándo atiende cada veterinario y cuántas citas recibe por día.">
        <Boton onClick={peluqueria}>Horario de peluquería</Boton>
        <Boton tono="principal" icono="mas" onClick={() => configurar().catch(() => undefined)}>Configurar horario</Boton>
      </Encabezado>
      <Tabla filas={horarios.datos} cargando={horarios.cargando} error={horarios.error} clave={(h) => h.id} vacio="Aún no hay horarios configurados."
        columnas={[
          { titulo: 'Veterinario', valor: (h) => h.nombre },
          { titulo: 'Días', valor: (h) => DIAS.filter(([d]) => h.dias.includes(d)).map(([, t]) => t).join(', ') },
          { titulo: 'Horario', valor: (h) => `${hora(h.horaInicio)} a ${hora(h.horaFin)}` },
          { titulo: 'Franja', valor: (h) => `${h.duracionMinutos} min` },
          { titulo: 'Cupos diarios', valor: (h) => h.cuposDiarios, alinear: 'derecha' },
        ]}
        acciones={(h) => <Boton onClick={() => configurar(h)}>Editar</Boton>} />
      {elemento}
    </>
  );
}
