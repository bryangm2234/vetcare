import { api } from '../../api/cliente';
import type { Campo, Opcion, Valores } from '../../componentes/Formulario';
import { hora } from '../../util';

/** Franjas libres del veterinario en la fecha elegida (RN-03). */
export async function franjasLibres(v: Valores): Promise<Opcion[]> {
  if (!v.fecha || !v.veterinarioId) {
    return [];
  }
  const disponibilidad = await api<any[]>('agenda', `/api/disponibilidad?fecha=${v.fecha}&veterinarioId=${v.veterinarioId}`);
  return disponibilidad[0].franjasLibres.map((f: string) => ({ valor: f, texto: hora(f) }));
}

export const campoFranja: Campo = {
  nombre: 'hora', etiqueta: 'Hora', tipo: 'select', requerido: true, dependeDe: ['fecha', 'veterinarioId'], opcionesDe: franjasLibres,
  ayuda: 'Solo aparecen las franjas libres con cupo.',
};

export async function opcionesVeterinarios(): Promise<Opcion[]> {
  const horarios = await api<any[]>('agenda', '/api/horarios');
  return horarios.map((h) => ({ valor: h.id, texto: h.nombre }));
}
