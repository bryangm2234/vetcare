import { Navigate, Route, Routes } from 'react-router-dom';
import { useSesion } from './auth/sesion';
import { MENU } from './auth/roles';
import { Plantilla } from './componentes/Plantilla';
import { Encabezado } from './componentes/ui';
import { Login } from './paginas/Login';
import { Panel } from './paginas/Panel';
import { Cuenta } from './paginas/Cuenta';
import { Usuarios } from './paginas/admin/Usuarios';
import { Horarios } from './paginas/admin/Horarios';
import { Tarifas } from './paginas/admin/Tarifas';
import { Proveedores } from './paginas/admin/Proveedores';
import { Reportes } from './paginas/admin/Reportes';
import { Auditoria } from './paginas/admin/Auditoria';
import { Propietarios } from './paginas/recepcion/Propietarios';
import { Citas } from './paginas/recepcion/Citas';
import { AlertasVacunas } from './paginas/recepcion/AlertasVacunas';
import { MiAgenda } from './paginas/clinica/MiAgenda';
import { Consultas } from './paginas/clinica/Consultas';
import { ConsultaDetalle } from './paginas/clinica/ConsultaDetalle';
import { Historia, HistoriaBuscar } from './paginas/clinica/Historia';
import { Vacunacion } from './paginas/clinica/Vacunacion';
import { Hospitalizacion } from './paginas/clinica/Hospitalizacion';
import { Laboratorio } from './paginas/clinica/Laboratorio';
import { Despachos } from './paginas/farmacia/Despachos';
import { Inventario } from './paginas/farmacia/Inventario';
import { AlertasInventario } from './paginas/farmacia/AlertasInventario';
import { Compras } from './paginas/farmacia/Compras';
import { Peluqueria } from './paginas/Peluqueria';
import { Facturas } from './paginas/caja/Facturas';
import { Caja } from './paginas/caja/Caja';
import { MisMascotas, MisCitas, MisFacturas } from './paginas/Portal';
import { MisOrdenes } from './paginas/MisOrdenes';
import type { ReactElement } from 'react';
import type { Rol } from './auth/roles';

/** Oculta rutas ajenas al rol. Quien protege los datos es el backend. */
function Solo({ ruta, children }: { ruta: string; children: ReactElement }) {
  const { sesion } = useSesion();
  const opcion = MENU.find((m) => m.ruta === ruta);
  const permitido = opcion?.roles.includes(sesion!.rol as Rol);
  return permitido ? children : <Encabezado titulo="Sin permiso" descripcion="Tu rol no tiene acceso a esta sección." />;
}

export function App() {
  const { sesion } = useSesion();
  if (!sesion) {
    return (
      <Routes>
        <Route path="*" element={<Login />} />
      </Routes>
    );
  }
  const pagina = (ruta: string, elemento: ReactElement) => <Route path={ruta} element={<Solo ruta={ruta}>{elemento}</Solo>} />;
  return (
    <Routes>
      <Route element={<Plantilla />}>
        <Route index element={<Panel />} />
        <Route path="cuenta" element={<Cuenta />} />
        {pagina('/propietarios', <Propietarios />)}
        {pagina('/citas', <Citas />)}
        {pagina('/alertas-vacunas', <AlertasVacunas />)}
        {pagina('/mi-agenda', <MiAgenda />)}
        {pagina('/consultas', <Consultas />)}
        <Route path="consultas/:id" element={<Solo ruta="/consultas"><ConsultaDetalle /></Solo>} />
        {pagina('/historia', <HistoriaBuscar />)}
        <Route path="historia/:mascotaId" element={<Solo ruta="/historia"><Historia /></Solo>} />
        {pagina('/vacunacion', <Vacunacion />)}
        {pagina('/hospitalizacion', <Hospitalizacion />)}
        {pagina('/laboratorio', <Laboratorio />)}
        {pagina('/despachos', <Despachos />)}
        {pagina('/inventario', <Inventario />)}
        {pagina('/alertas-inventario', <AlertasInventario />)}
        {pagina('/compras', <Compras />)}
        {pagina('/peluqueria', <Peluqueria />)}
        {pagina('/facturas', <Facturas />)}
        {pagina('/caja', <Caja />)}
        {pagina('/mis-mascotas', <MisMascotas />)}
        {pagina('/mis-citas', <MisCitas />)}
        {pagina('/mis-facturas', <MisFacturas />)}
        {pagina('/mis-ordenes', <MisOrdenes />)}
        {pagina('/usuarios', <Usuarios />)}
        {pagina('/horarios', <Horarios />)}
        {pagina('/tarifas', <Tarifas />)}
        {pagina('/proveedores', <Proveedores />)}
        {pagina('/reportes', <Reportes />)}
        {pagina('/auditoria', <Auditoria />)}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  );
}
