export type Rol =
  | 'RECEPCIONISTA'
  | 'VETERINARIO'
  | 'AUXILIAR_VETERINARIO'
  | 'LABORATORIO'
  | 'FARMACIA'
  | 'PELUQUERIA'
  | 'CAJERO'
  | 'PROPIETARIO'
  | 'PROVEEDOR'
  | 'ADMINISTRADOR';

export const NOMBRE_ROL: Record<Rol, string> = {
  RECEPCIONISTA: 'Recepción',
  VETERINARIO: 'Veterinario',
  AUXILIAR_VETERINARIO: 'Auxiliar veterinario',
  LABORATORIO: 'Laboratorio',
  FARMACIA: 'Farmacia',
  PELUQUERIA: 'Peluquería y estética',
  CAJERO: 'Caja',
  PROPIETARIO: 'Propietario',
  PROVEEDOR: 'Proveedor',
  ADMINISTRADOR: 'Administrador',
};

export type NombreIcono =
  | 'panel' | 'usuarios' | 'patita' | 'calendario' | 'estetoscopio' | 'historia' | 'jeringa' | 'cama' | 'tubo'
  | 'capsula' | 'caja' | 'camion' | 'factura' | 'caja-registradora' | 'grafico' | 'escudo' | 'reloj' | 'tijeras' | 'etiqueta' | 'alerta';

export interface OpcionMenu {
  ruta: string;
  texto: string;
  icono: NombreIcono;
  roles: Rol[];
}

export const MENU: OpcionMenu[] = [
  { ruta: '/', texto: 'Panel', icono: 'panel', roles: ['RECEPCIONISTA', 'VETERINARIO', 'AUXILIAR_VETERINARIO', 'LABORATORIO', 'FARMACIA', 'PELUQUERIA', 'CAJERO', 'PROPIETARIO', 'PROVEEDOR', 'ADMINISTRADOR'] },
  { ruta: '/propietarios', texto: 'Propietarios y mascotas', icono: 'patita', roles: ['RECEPCIONISTA', 'VETERINARIO', 'ADMINISTRADOR'] },
  { ruta: '/citas', texto: 'Citas', icono: 'calendario', roles: ['RECEPCIONISTA', 'AUXILIAR_VETERINARIO', 'ADMINISTRADOR'] },
  { ruta: '/alertas-vacunas', texto: 'Alertas de vacunas', icono: 'alerta', roles: ['RECEPCIONISTA'] },
  { ruta: '/mi-agenda', texto: 'Mi agenda', icono: 'reloj', roles: ['VETERINARIO'] },
  { ruta: '/consultas', texto: 'Consultas', icono: 'estetoscopio', roles: ['VETERINARIO'] },
  { ruta: '/historia', texto: 'Historia clínica', icono: 'historia', roles: ['VETERINARIO'] },
  { ruta: '/vacunacion', texto: 'Vacunación', icono: 'jeringa', roles: ['VETERINARIO'] },
  { ruta: '/hospitalizacion', texto: 'Hospitalización', icono: 'cama', roles: ['VETERINARIO', 'AUXILIAR_VETERINARIO'] },
  { ruta: '/laboratorio', texto: 'Órdenes de laboratorio', icono: 'tubo', roles: ['LABORATORIO'] },
  { ruta: '/despachos', texto: 'Despachar fórmulas', icono: 'capsula', roles: ['FARMACIA'] },
  { ruta: '/inventario', texto: 'Inventario', icono: 'caja', roles: ['FARMACIA'] },
  { ruta: '/alertas-inventario', texto: 'Alertas de inventario', icono: 'alerta', roles: ['FARMACIA', 'ADMINISTRADOR'] },
  { ruta: '/compras', texto: 'Órdenes de compra', icono: 'camion', roles: ['FARMACIA'] },
  { ruta: '/peluqueria', texto: 'Peluquería', icono: 'tijeras', roles: ['PELUQUERIA'] },
  { ruta: '/facturas', texto: 'Facturas', icono: 'factura', roles: ['CAJERO'] },
  { ruta: '/caja', texto: 'Caja', icono: 'caja-registradora', roles: ['CAJERO'] },
  { ruta: '/mis-mascotas', texto: 'Mis mascotas', icono: 'patita', roles: ['PROPIETARIO'] },
  { ruta: '/mis-citas', texto: 'Mis citas', icono: 'calendario', roles: ['PROPIETARIO'] },
  { ruta: '/mis-facturas', texto: 'Mis facturas', icono: 'factura', roles: ['PROPIETARIO'] },
  { ruta: '/mis-ordenes', texto: 'Órdenes de compra', icono: 'camion', roles: ['PROVEEDOR'] },
  { ruta: '/usuarios', texto: 'Usuarios', icono: 'usuarios', roles: ['ADMINISTRADOR'] },
  { ruta: '/horarios', texto: 'Horarios y cupos', icono: 'reloj', roles: ['ADMINISTRADOR'] },
  { ruta: '/tarifas', texto: 'Tarifas', icono: 'etiqueta', roles: ['ADMINISTRADOR'] },
  { ruta: '/proveedores', texto: 'Proveedores', icono: 'camion', roles: ['ADMINISTRADOR'] },
  { ruta: '/reportes', texto: 'Reportes', icono: 'grafico', roles: ['ADMINISTRADOR'] },
  { ruta: '/auditoria', texto: 'Auditoría', icono: 'escudo', roles: ['ADMINISTRADOR'] },
];

export function menuDe(rol: Rol): OpcionMenu[] {
  return MENU.filter((opcion) => opcion.roles.includes(rol));
}
