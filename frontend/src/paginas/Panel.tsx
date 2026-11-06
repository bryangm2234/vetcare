import { Link } from 'react-router-dom';
import { api } from '../api/cliente';
import { useUsuario } from '../auth/sesion';
import { useDatos, type Datos } from '../componentes/ganchos';
import { Tabla } from '../componentes/Tabla';
import { Barras, Encabezado, Estado, Indicador, Seccion } from '../componentes/ui';
import { fecha, hora, hoy, pesos } from '../util';

const cuenta = (d: Datos<any[]>, filtro?: (x: any) => boolean) => (d.datos ? (filtro ? d.datos.filter(filtro).length : d.datos.length) : d.cargando ? '…' : 0);

function PanelRecepcion() {
  const citas = useDatos(() => api<any[]>('agenda', `/api/citas?fecha=${hoy()}`));
  const alertas = useDatos(() => api<any[]>('historia', '/api/vacunas/alertas'));
  return (
    <>
      <div className="indicadores">
        <Indicador titulo="Citas de hoy" valor={cuenta(citas)} error={citas.error} />
        <Indicador titulo="En espera" valor={cuenta(citas, (c) => c.estado === 'EN_ESPERA')} error={citas.error} />
        <Indicador titulo="Por llegar" valor={cuenta(citas, (c) => c.estado === 'PROGRAMADA')} error={citas.error} />
        <Indicador titulo="Vacunas por vencer o vencidas" valor={cuenta(alertas)} error={alertas.error} />
      </div>
      <Seccion titulo="Citas de hoy" acciones={<Link to="/citas">Ver todas</Link>}>
        <Tabla filas={citas.datos} cargando={citas.cargando} error={citas.error} clave={(c) => c.id} vacio="No hay citas para hoy."
          columnas={[{ titulo: 'Hora', valor: (c) => hora(c.hora) }, { titulo: 'Mascota', valor: (c) => c.mascotaNombre }, { titulo: 'Motivo', valor: (c) => c.motivo }, { titulo: 'Estado', celda: (c) => <Estado valor={c.estado} /> }]} />
      </Seccion>
    </>
  );
}

function PanelVeterinario() {
  const agenda = useDatos(() => api<any[]>('agenda', `/api/citas/mi-agenda?fecha=${hoy()}`));
  const abiertas = useDatos(() => api<any[]>('historia', '/api/consultas/abiertas'));
  const hospital = useDatos(() => api<any[]>('historia', '/api/hospitalizaciones/activas'));
  return (
    <>
      <div className="indicadores">
        <Indicador titulo="Citas de hoy" valor={cuenta(agenda)} error={agenda.error} />
        <Indicador titulo="Pacientes en espera" valor={cuenta(agenda, (c) => c.estado === 'EN_ESPERA')} error={agenda.error} />
        <Indicador titulo="Consultas abiertas" valor={cuenta(abiertas)} error={abiertas.error} />
        <Indicador titulo="Hospitalizados" valor={cuenta(hospital)} error={hospital.error} />
      </div>
      <Seccion titulo="Mi agenda de hoy" acciones={<Link to="/mi-agenda">Abrir agenda</Link>}>
        <Tabla filas={agenda.datos} cargando={agenda.cargando} error={agenda.error} clave={(c) => c.id} vacio="No tienes citas hoy."
          columnas={[{ titulo: 'Hora', valor: (c) => hora(c.hora) }, { titulo: 'Mascota', valor: (c) => c.mascotaNombre }, { titulo: 'Motivo', valor: (c) => c.motivo }, { titulo: 'Estado', celda: (c) => <Estado valor={c.estado} /> }]} />
      </Seccion>
    </>
  );
}

function PanelAuxiliar() {
  const citas = useDatos(() => api<any[]>('agenda', `/api/citas?fecha=${hoy()}`));
  const hospital = useDatos(() => api<any[]>('historia', '/api/hospitalizaciones/activas'));
  return (
    <div className="indicadores">
      <Indicador titulo="Pacientes en espera de signos vitales" valor={cuenta(citas, (c) => c.estado === 'EN_ESPERA')} error={citas.error} />
      <Indicador titulo="Hospitalizados" valor={cuenta(hospital)} error={hospital.error} />
    </div>
  );
}

function PanelLaboratorio() {
  const ordenes = useDatos(() => api('historia', '/api/ordenes-laboratorio?size=100'));
  return (
    <>
      <div className="indicadores"><Indicador titulo="Órdenes pendientes" valor={ordenes.datos?.totalElementos ?? '…'} error={ordenes.error} /></div>
      <p><Link to="/laboratorio">Ir a las órdenes de laboratorio</Link></p>
    </>
  );
}

function PanelFarmacia() {
  const stock = useDatos(() => api<any[]>('farmacia', '/api/alertas/stock-minimo'));
  const lotes = useDatos(() => api<any[]>('farmacia', '/api/lotes/proximos-vencer'));
  const formulas = useDatos(() => api<any[]>('historia', '/api/formulas/despachables'));
  return (
    <>
      <div className="indicadores">
        <Indicador titulo="Fórmulas por despachar" valor={cuenta(formulas)} error={formulas.error} />
        <Indicador titulo="Productos en stock mínimo" valor={cuenta(stock)} error={stock.error} />
        <Indicador titulo="Lotes próximos a vencer" valor={cuenta(lotes)} error={lotes.error} />
      </div>
      <Seccion titulo="Stock mínimo" acciones={<Link to="/alertas-inventario">Ver alertas</Link>}>
        <Tabla filas={stock.datos} cargando={stock.cargando} error={stock.error} clave={(a) => a.medicamentoId} vacio="Ningún producto está en su stock mínimo."
          columnas={[{ titulo: 'Producto', valor: (a) => a.nombre }, { titulo: 'Disponible', valor: (a) => a.stockDisponible, alinear: 'derecha' }, { titulo: 'Mínimo', valor: (a) => a.stockMinimo, alinear: 'derecha' }]} />
      </Seccion>
    </>
  );
}

function PanelPeluqueria() {
  const citas = useDatos(() => api<any[]>('agenda', `/api/peluqueria/citas?fecha=${hoy()}`));
  return (
    <div className="indicadores">
      <Indicador titulo="Servicios de hoy" valor={cuenta(citas)} error={citas.error} />
      <Indicador titulo="Por realizar" valor={cuenta(citas, (c) => c.estado === 'PROGRAMADA')} error={citas.error} />
      <Indicador titulo="Realizados" valor={cuenta(citas, (c) => c.estado === 'REALIZADA')} error={citas.error} />
    </div>
  );
}

function PanelCajero() {
  const pendientes = useDatos(() => api('facturacion', '/api/facturas?estado=PENDIENTE&size=1'));
  const credito = useDatos(() => api('facturacion', '/api/facturas?estado=CREDITO&size=1'));
  const caja = useDatos(() => api('facturacion', '/api/cajas/actual'));
  return (
    <>
      <div className="indicadores">
        <Indicador titulo="Caja" valor={caja.datos ? 'Abierta' : caja.cargando ? '…' : 'Cerrada'} detalle={caja.datos ? `Base ${pesos(caja.datos.base)}` : undefined} />
        <Indicador titulo="Facturas por cobrar" valor={pendientes.datos?.totalElementos ?? '…'} error={pendientes.error} />
        <Indicador titulo="Facturas a crédito" valor={credito.datos?.totalElementos ?? '…'} error={credito.error} />
      </div>
      <p><Link to="/facturas">Ir a las facturas</Link></p>
    </>
  );
}

function PanelPropietario() {
  const mascotas = useDatos(() => api<any[]>('pacientes', '/api/portal/mascotas'));
  const citas = useDatos(() => api<any[]>('agenda', '/api/portal/citas'));
  const vacunas = useDatos(() => api<any[]>('historia', '/api/portal/vacunas/pendientes'));
  return (
    <>
      <div className="indicadores">
        <Indicador titulo="Mis mascotas" valor={cuenta(mascotas)} error={mascotas.error} />
        <Indicador titulo="Próximas citas" valor={cuenta(citas)} error={citas.error} />
        <Indicador titulo="Vacunas pendientes" valor={cuenta(vacunas)} error={vacunas.error} />
      </div>
      <div className="columnas">
        <Seccion titulo="Próximas citas">
          <Tabla filas={citas.datos} cargando={citas.cargando} error={citas.error && 'Las citas no están disponibles en este momento.'} clave={(c) => c.id} vacio="No tienes citas próximas."
            columnas={[{ titulo: 'Fecha', valor: (c) => `${fecha(c.fecha)} ${hora(c.hora)}` }, { titulo: 'Mascota', valor: (c) => c.mascotaNombre }]} />
        </Seccion>
        <Seccion titulo="Vacunas pendientes">
          <Tabla filas={vacunas.datos} cargando={vacunas.cargando} error={vacunas.error && 'Las vacunas no están disponibles en este momento.'} clave={(v) => `${v.mascotaId}-${v.vacuna}`} vacio="No tienes vacunas pendientes."
            columnas={[{ titulo: 'Mascota', valor: (v) => v.mascotaNombre }, { titulo: 'Vacuna', valor: (v) => v.vacuna }, { titulo: 'Fecha', valor: (v) => fecha(v.proximaDosis) }, { titulo: 'Estado', celda: (v) => <Estado valor={v.estado} /> }]} />
        </Seccion>
      </div>
    </>
  );
}

function PanelProveedor() {
  const ordenes = useDatos(() => api<any[]>('farmacia', '/api/proveedor/ordenes-compra'));
  return (
    <>
      <div className="indicadores">
        <Indicador titulo="Órdenes por confirmar" valor={cuenta(ordenes, (o) => o.estado === 'ENVIADA')} error={ordenes.error} />
        <Indicador titulo="Órdenes totales" valor={cuenta(ordenes)} error={ordenes.error} />
      </div>
      <p><Link to="/mis-ordenes">Ir a mis órdenes</Link></p>
    </>
  );
}

function PanelAdministrador() {
  const dia = hoy();
  const inicio = hoy(-30);
  const periodo = `?desde=${inicio}&hasta=${dia}`;
  const citas = useDatos(() => api('agenda', '/api/reportes/citas' + periodo));
  const ocupacion = useDatos(() => api<any[]>('agenda', '/api/reportes/ocupacion' + periodo));
  const ventas = useDatos(() => api('farmacia', '/api/reportes/ventas-farmacia' + periodo));
  const ingresos = useDatos(() => api('facturacion', '/api/reportes/ingresos' + periodo));
  return (
    <>
      <p style={{ color: 'var(--tenue)', marginBottom: 14 }}>Últimos 30 días</p>
      <div className="indicadores">
        <Indicador titulo="Citas atendidas" valor={citas.datos?.atendidas ?? '…'} detalle={citas.datos ? `${citas.datos.canceladas} canceladas` : undefined} error={citas.error} />
        <Indicador titulo="Ventas de farmacia" valor={ventas.datos ? pesos(ventas.datos.valor) : '…'} detalle={ventas.datos ? `${ventas.datos.unidades} unidades` : undefined} error={ventas.error} />
        <Indicador titulo="Ingresos" valor={ingresos.datos ? pesos(ingresos.datos.total) : '…'} error={ingresos.error} />
      </div>
      <div className="columnas">
        <Seccion titulo="Ocupación de cupos por veterinario">
          <div className="tarjeta">
            {ocupacion.error ? <p className="mensaje-error">La ocupación no está disponible.</p> : (
              <Barras maximo={100} formato={(v) => `${v} %`} datos={(ocupacion.datos ?? []).map((o) => ({ etiqueta: o.nombre ?? o.veterinarioId, valor: o.porcentaje }))} />
            )}
          </div>
        </Seccion>
        <Seccion titulo="Ingresos por servicio">
          <div className="tarjeta">
            {ingresos.error ? <p className="mensaje-error">Los ingresos no están disponibles.</p> : (
              <Barras formato={pesos} datos={Object.entries(ingresos.datos?.porServicio ?? {}).map(([etiqueta, valor]) => ({ etiqueta, valor: valor as number }))} />
            )}
          </div>
        </Seccion>
      </div>
    </>
  );
}

const PANELES: Record<string, () => React.JSX.Element> = {
  RECEPCIONISTA: PanelRecepcion,
  VETERINARIO: PanelVeterinario,
  AUXILIAR_VETERINARIO: PanelAuxiliar,
  LABORATORIO: PanelLaboratorio,
  FARMACIA: PanelFarmacia,
  PELUQUERIA: PanelPeluqueria,
  CAJERO: PanelCajero,
  PROPIETARIO: PanelPropietario,
  PROVEEDOR: PanelProveedor,
  ADMINISTRADOR: PanelAdministrador,
};

export function Panel() {
  const { rol } = useUsuario();
  const Contenido = PANELES[rol];
  return (
    <>
      <Encabezado titulo="Panel" descripcion={new Date().toLocaleDateString('es-CO', { dateStyle: 'full' })} />
      <Contenido />
    </>
  );
}
