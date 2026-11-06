import { useNavigate } from 'react-router-dom';
import { api } from '../../api/cliente';
import { useDatos } from '../../componentes/ganchos';
import { Tabla } from '../../componentes/Tabla';
import { Boton, Encabezado, Estado } from '../../componentes/ui';
import { fechaHora } from '../../util';

export function Consultas() {
  const abiertas = useDatos(() => api<any[]>('historia', '/api/consultas/abiertas'));
  const navegar = useNavigate();
  return (
    <>
      <Encabezado titulo="Consultas abiertas" descripcion="Las consultas se abren desde tu agenda, cuando el paciente está en espera." />
      <Tabla filas={abiertas.datos} cargando={abiertas.cargando} error={abiertas.error} clave={(c) => c.id} vacio="No tienes consultas abiertas."
        columnas={[
          { titulo: 'Abierta', valor: (c) => fechaHora(c.fechaApertura) },
          { titulo: 'Diagnóstico', valor: (c) => c.diagnostico ?? 'Sin registrar' },
          { titulo: 'Estado', celda: (c) => <Estado valor={c.estado} /> },
        ]}
        acciones={(c) => <Boton tono="principal" onClick={() => navegar(`/consultas/${c.id}`)}>Continuar</Boton>} />
    </>
  );
}
