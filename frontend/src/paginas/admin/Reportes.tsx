import { useState } from 'react';
import { api } from '../../api/cliente';
import { useDatos } from '../../componentes/ganchos';
import { Tabla } from '../../componentes/Tabla';
import { Barras, CampoFiltro, Encabezado, Indicador, Seccion } from '../../componentes/ui';
import { consulta, hoy, pesos } from '../../util';

export function Reportes() {
  const [desde, setDesde] = useState(hoy(-30));
  const [hasta, setHasta] = useState(hoy());
  const [servicio, setServicio] = useState('');
  const [medioPago, setMedioPago] = useState('');
  const invalido = desde > hasta;
  const ingresos = useDatos(() => (invalido ? Promise.resolve(undefined) : api('facturacion', '/api/reportes/ingresos' + consulta({ desde, hasta, servicio, medioPago }))), [desde, hasta, servicio, medioPago]);
  const ocupacion = useDatos(() => (invalido ? Promise.resolve([]) : api<any[]>('agenda', '/api/reportes/ocupacion' + consulta({ desde, hasta }))), [desde, hasta]);
  const ventas = useDatos(() => (invalido ? Promise.resolve(undefined) : api('farmacia', '/api/reportes/ventas-farmacia' + consulta({ desde, hasta }))), [desde, hasta]);
  const citas = useDatos(() => (invalido ? Promise.resolve(undefined) : api('agenda', '/api/reportes/citas' + consulta({ desde, hasta }))), [desde, hasta]);
  const mascotas = useDatos(() => api<any[]>('pacientes', '/api/reportes/mascotas'));

  return (
    <>
      <Encabezado titulo="Reportes" descripcion="Indicadores del período seleccionado." />
      <div className="tarjeta tabla-barra" style={{ marginBottom: 18 }}>
        <CampoFiltro etiqueta="Desde"><input type="date" value={desde} onChange={(e) => setDesde(e.target.value)} /></CampoFiltro>
        <CampoFiltro etiqueta="Hasta"><input type="date" value={hasta} onChange={(e) => setHasta(e.target.value)} /></CampoFiltro>
        <CampoFiltro etiqueta="Servicio">
          <select value={servicio} onChange={(e) => setServicio(e.target.value)}>
            <option value="">Todos</option>
            {['CONSULTA', 'LABORATORIO', 'MEDICAMENTOS', 'HOSPITALIZACION', 'PELUQUERIA'].map((s) => <option key={s} value={s}>{s}</option>)}
          </select>
        </CampoFiltro>
        <CampoFiltro etiqueta="Medio de pago">
          <select value={medioPago} onChange={(e) => setMedioPago(e.target.value)}>
            <option value="">Todos</option>
            {['EFECTIVO', 'TARJETA', 'TRANSFERENCIA'].map((m) => <option key={m} value={m}>{m}</option>)}
          </select>
        </CampoFiltro>
      </div>
      {invalido && <p className="mensaje-error" role="alert" style={{ marginBottom: 14 }}>La fecha inicial es posterior a la final.</p>}
      <div className="indicadores">
        <Indicador titulo="Ingresos" valor={ingresos.datos ? pesos(ingresos.datos.total) : '…'} error={ingresos.error} />
        <Indicador titulo="Citas atendidas" valor={citas.datos?.atendidas ?? '…'} detalle={citas.datos ? `${citas.datos.canceladas} canceladas` : undefined} error={citas.error} />
        <Indicador titulo="Ventas de farmacia" valor={ventas.datos ? pesos(ventas.datos.valor) : '…'} detalle={ventas.datos ? `${ventas.datos.despachos} despachos` : undefined} error={ventas.error} />
      </div>
      <div className="columnas">
        <Seccion titulo="Ingresos por servicio">
          <div className="tarjeta"><Barras formato={pesos} datos={Object.entries(ingresos.datos?.porServicio ?? {}).map(([etiqueta, valor]) => ({ etiqueta, valor: valor as number }))} /></div>
        </Seccion>
        <Seccion titulo="Ingresos por medio de pago">
          <div className="tarjeta"><Barras formato={pesos} datos={Object.entries(ingresos.datos?.porMedioPago ?? {}).map(([etiqueta, valor]) => ({ etiqueta, valor: valor as number }))} /></div>
        </Seccion>
        <Seccion titulo="Ocupación de cupos por veterinario">
          <div className="tarjeta"><Barras maximo={100} formato={(v) => `${v} %`} datos={(ocupacion.datos ?? []).map((o) => ({ etiqueta: o.nombre, valor: o.porcentaje }))} /></div>
        </Seccion>
      </div>
      <Seccion titulo="Mascotas por especie y raza">
        <Tabla filas={mascotas.datos} cargando={mascotas.cargando} error={mascotas.error} clave={(m) => `${m.especie}-${m.raza}`} buscador="Buscar especie o raza"
          columnas={[{ titulo: 'Especie', valor: (m) => m.especie }, { titulo: 'Raza', valor: (m) => m.raza }, { titulo: 'Total', valor: (m) => m.total, alinear: 'derecha' }]} />
      </Seccion>
    </>
  );
}
