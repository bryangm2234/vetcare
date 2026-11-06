import { useEffect, useState } from 'react';
import { NavLink, Outlet, useLocation } from 'react-router-dom';
import { useSesion } from '../auth/sesion';
import { menuDe, NOMBRE_ROL } from '../auth/roles';
import { Icono, Marca } from './Icono';

export function Plantilla() {
  const { sesion, cerrar } = useSesion();
  const [abierto, setAbierto] = useState(false);
  const ubicacion = useLocation();

  useEffect(() => setAbierto(false), [ubicacion.pathname]);

  const usuario = sesion!;
  return (
    <div className="plantilla">
      <aside className={abierto ? 'lateral lateral-abierto' : 'lateral'} aria-label="Menú principal">
        <div className="lateral-marca">
          <Marca />
          <div>
            <strong>VetCare</strong>
            <span>Clínica veterinaria</span>
          </div>
        </div>
        <nav>
          {menuDe(usuario.rol).map((opcion) => (
            <NavLink key={opcion.ruta} to={opcion.ruta} end={opcion.ruta === '/'} className={({ isActive }) => (isActive ? 'enlace enlace-activo' : 'enlace')}>
              <Icono nombre={opcion.icono} />
              <span>{opcion.texto}</span>
            </NavLink>
          ))}
        </nav>
      </aside>
      {abierto && <div className="lateral-fondo" onClick={() => setAbierto(false)} />}
      <div className="contenido">
        <header className="cabecera">
          <button type="button" className="boton-menu" aria-label="Abrir menú" aria-expanded={abierto} onClick={() => setAbierto(!abierto)}>
            <Icono nombre="menu" tamano={22} />
          </button>
          <div className="cabecera-usuario">
            <NavLink to="/cuenta" className="usuario">
              <span className="usuario-avatar"><Icono nombre="cuenta" /></span>
              <span className="usuario-datos">
                <strong>{usuario.correo}</strong>
                <span>{NOMBRE_ROL[usuario.rol]}</span>
              </span>
            </NavLink>
            <button type="button" className="boton boton-suave" onClick={() => cerrar()}>
              <Icono nombre="salir" />
              <span>Cerrar sesión</span>
            </button>
          </div>
        </header>
        <main className="principal">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
