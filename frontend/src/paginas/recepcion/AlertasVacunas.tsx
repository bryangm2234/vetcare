import { useState } from 'react';
import { api } from '../../api/cliente';
import { useDatos } from '../../componentes/ganchos';
import { Tabla } from '../../componentes/Tabla';
import { CampoFiltro, Encabezado, Estado } from '../../componentes/ui';
import { fecha } from '../../util';

export function AlertasVacunas() {
  const [dias, setDias] = useState(30);
  const alertas = useDatos(() => api<any[]>('historia', `/api/vacunas/alertas?dias=${dias}`), [dias]);
  return (
    <>
      <Encabezado titulo="Alertas de vacunas" descripcion="Contacta a los propietarios antes de que venza la próxima dosis." />
      <Tabla filas={alertas.datos} cargando={alertas.cargando} error={alertas.error} clave={(a) => `${a.mascotaId}-${a.vacuna}`} buscador="Buscar mascota o vacuna" vacio="No hay vacunas por vencer en este período."
        filtros={<CampoFiltro etiqueta="Ventana en días"><input type="number" min={0} value={dias} onChange={(e) => setDias(Number(e.target.value))} /></CampoFiltro>}
        columnas={[
          { titulo: 'Mascota', valor: (a) => a.mascotaNombre },
          { titulo: 'Vacuna', valor: (a) => a.vacuna },
          { titulo: 'Última dosis', valor: (a) => a.dosisAplicada, alinear: 'derecha' },
          { titulo: 'Próxima dosis', valor: (a) => fecha(a.proximaDosis) },
          { titulo: 'Estado', celda: (a) => <Estado valor={a.estado} /> },
        ]} />
    </>
  );
}
