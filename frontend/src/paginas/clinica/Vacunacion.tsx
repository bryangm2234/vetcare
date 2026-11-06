import { api } from '../../api/cliente';
import { useElegirMascota } from '../../componentes/ElegirMascota';
import { useDialogo } from '../../componentes/Formulario';
import { useDatos } from '../../componentes/ganchos';
import { Tabla } from '../../componentes/Tabla';
import { Boton, Encabezado } from '../../componentes/ui';
import { hoy } from '../../util';

export function Vacunacion() {
  const esquemas = useDatos(() => api<any[]>('historia', '/api/esquemas-vacunacion'));
  const { abrir, elemento } = useDialogo();
  const { elegirMascota, elementoMascota } = useElegirMascota();

  const aplicar = () => elegirMascota('Aplicar vacuna: elige la mascota', (m) => {
    const propias = (esquemas.datos ?? []).filter((e) => e.especie === m.especie);
    abrir({
      titulo: `Aplicar vacuna a ${m.nombre}`,
      descripcion: propias.length === 0 ? `No hay esquemas para la especie ${m.especie}. Crea uno primero.` : 'La próxima dosis se calcula con el esquema. Se descuenta una unidad del lote.',
      exito: 'Vacuna registrada', textoEnviar: 'Registrar',
      inicial: { fecha: hoy() },
      campos: [
        { nombre: 'vacuna', etiqueta: 'Vacuna', tipo: 'select', requerido: true, opciones: propias.map((e) => ({ valor: e.vacuna, texto: `${e.vacuna} (${e.dosis} dosis cada ${e.intervaloDias} días)` })) },
        { nombre: 'lote', etiqueta: 'Número de lote', requerido: true },
        { nombre: 'fecha', etiqueta: 'Fecha de aplicación', tipo: 'date', requerido: true },
      ],
      enviar: (v) => api('historia', '/api/vacunas', 'POST', { ...v, mascotaId: m.id }),
    });
  });

  return (
    <>
      <Encabezado titulo="Vacunación" descripcion="Define los esquemas por especie y registra las dosis aplicadas.">
        <Boton onClick={() => abrir({
          titulo: 'Crear esquema', exito: 'Esquema creado', despues: esquemas.recargar,
          campos: [
            { nombre: 'especie', etiqueta: 'Especie', requerido: true, ayuda: 'Debe coincidir con la especie registrada en la mascota.' },
            { nombre: 'vacuna', etiqueta: 'Vacuna', requerido: true },
            { nombre: 'dosis', etiqueta: 'Número de dosis', tipo: 'number', minimo: 1, requerido: true },
            { nombre: 'intervaloDias', etiqueta: 'Días entre dosis', tipo: 'number', minimo: 1, requerido: true },
          ],
          enviar: (v) => api('historia', '/api/esquemas-vacunacion', 'POST', { ...v, dosis: Number(v.dosis), intervaloDias: Number(v.intervaloDias) }),
        })}>Crear esquema</Boton>
        <Boton tono="principal" icono="mas" onClick={aplicar}>Aplicar vacuna</Boton>
      </Encabezado>
      <Tabla filas={esquemas.datos} cargando={esquemas.cargando} error={esquemas.error} clave={(e) => e.id} buscador="Buscar especie o vacuna" vacio="Aún no hay esquemas."
        columnas={[
          { titulo: 'Especie', valor: (e) => e.especie },
          { titulo: 'Vacuna', valor: (e) => e.vacuna },
          { titulo: 'Dosis', valor: (e) => e.dosis, alinear: 'derecha' },
          { titulo: 'Días entre dosis', valor: (e) => e.intervaloDias, alinear: 'derecha' },
        ]} />
      {elementoMascota}
      {elemento}
    </>
  );
}
