const PESOS = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 });

export const pesos = (valor: number | null | undefined) => PESOS.format(valor ?? 0);

/** Fecha local en formato ISO; toISOString usaría UTC y cambiaría de día por la noche. */
export function hoy(dias = 0): string {
  const fecha = new Date();
  fecha.setDate(fecha.getDate() + dias);
  const mes = String(fecha.getMonth() + 1).padStart(2, '0');
  const dia = String(fecha.getDate()).padStart(2, '0');
  return `${fecha.getFullYear()}-${mes}-${dia}`;
}

export function fecha(valor?: string | null): string {
  if (!valor) {
    return '—';
  }
  const [anio, mes, dia] = valor.slice(0, 10).split('-').map(Number);
  return new Date(anio, mes - 1, dia).toLocaleDateString('es-CO', { day: '2-digit', month: 'short', year: 'numeric' });
}

export function fechaHora(valor?: string | null): string {
  return valor ? new Date(valor).toLocaleString('es-CO', { dateStyle: 'medium', timeStyle: 'short' }) : '—';
}

export const hora = (valor?: string | null) => (valor ? valor.slice(0, 5) : '—');

export const consulta = (parametros: Record<string, string | number | undefined | null>) => {
  const texto = new URLSearchParams();
  Object.entries(parametros).forEach(([clave, valor]) => {
    if (valor !== undefined && valor !== null && valor !== '') {
      texto.set(clave, String(valor));
    }
  });
  const resultado = texto.toString();
  return resultado ? `?${resultado}` : '';
};
