export type Servicio = 'pacientes' | 'agenda' | 'historia' | 'farmacia' | 'facturacion';

const URLS: Record<Servicio, string> = {
  pacientes: import.meta.env.VITE_URL_PACIENTES ?? 'http://localhost:8081',
  agenda: import.meta.env.VITE_URL_AGENDA ?? 'http://localhost:8082',
  historia: import.meta.env.VITE_URL_HISTORIA ?? 'http://localhost:8083',
  farmacia: import.meta.env.VITE_URL_FARMACIA ?? 'http://localhost:8084',
  facturacion: import.meta.env.VITE_URL_FACTURACION ?? 'http://localhost:8085',
};

export class ApiError extends Error {
  readonly status: number;
  readonly detalles: Record<string, string>;

  constructor(status: number, mensaje: string, detalles: Record<string, string> = {}) {
    super(mensaje);
    this.status = status;
    this.detalles = detalles;
  }
}

let token: string | null = null;
let alExpirar: () => void = () => {};

export function configurarToken(nuevo: string | null) {
  token = nuevo;
}

export function configurarExpiracion(funcion: () => void) {
  alExpirar = funcion;
}

function cabeceras(extra: Record<string, string> = {}): Record<string, string> {
  return token ? { ...extra, Authorization: `Bearer ${token}` } : extra;
}

async function ejecutar(servicio: Servicio, ruta: string, opciones: RequestInit): Promise<Response> {
  let respuesta: Response;
  try {
    respuesta = await fetch(URLS[servicio] + ruta, opciones);
  } catch {
    throw new ApiError(0, 'No se pudo conectar con el servidor. Verifica que el servicio esté disponible.');
  }
  if (respuesta.ok) {
    return respuesta;
  }
  const cuerpo = await respuesta.json().catch(() => null);
  if (respuesta.status === 401 && token) {
    alExpirar();
  }
  throw new ApiError(respuesta.status, cuerpo?.mensaje ?? 'Ocurrió un error inesperado', cuerpo?.detalles ?? {});
}

export async function api<T = any>(servicio: Servicio, ruta: string, metodo = 'GET', cuerpo?: unknown): Promise<T> {
  const respuesta = await ejecutar(servicio, ruta, {
    method: metodo,
    headers: cabeceras(cuerpo !== undefined ? { 'Content-Type': 'application/json' } : {}),
    body: cuerpo !== undefined ? JSON.stringify(cuerpo) : undefined,
  });
  const texto = await respuesta.text();
  return (texto ? JSON.parse(texto) : undefined) as T;
}

export async function subirArchivo(servicio: Servicio, ruta: string, archivo: File): Promise<void> {
  const datos = new FormData();
  datos.append('archivo', archivo);
  await ejecutar(servicio, ruta, { method: 'POST', headers: cabeceras(), body: datos });
}

export async function descargar(servicio: Servicio, ruta: string, nombre: string): Promise<void> {
  const respuesta = await ejecutar(servicio, ruta, { headers: cabeceras() });
  const url = URL.createObjectURL(await respuesta.blob());
  const enlace = document.createElement('a');
  enlace.href = url;
  enlace.download = nombre;
  enlace.click();
  URL.revokeObjectURL(url);
}
